package simulation.model

import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

class Quadrilateral(points: Set<Point>) : Polygon(points) {

    init {
        require(points.size == 4) { "A quadrilateral must have exactly 4 distinct points." }
    }

    fun oppositeEdge(baseEdge: Edge): Edge {
        require(edges.contains(baseEdge)) { "The polygon does not contain the specified edge." }
        return edges.find { edge -> edge != baseEdge && !edge.sharesPointWith(baseEdge) }!!
    }

    fun oppositeEdgesTuples(): Pair<OppositeEdgesTuple, OppositeEdgesTuple> {
        val e1 = edges.random()
        val e2 = oppositeEdge(e1)
        val pair1 = Pair(e1, e2)
        val otherEdges = edges.filter { e1 != it && e2 != it }
        val pair2 = Pair(otherEdges[0], otherEdges[1])
        return OppositeEdgesTuple(pair1) to OppositeEdgesTuple(pair2)
    }

    fun irregularityIndex(): Double {
        val tuples = oppositeEdgesTuples()
        return (tuples.first.ratioOfPair() + tuples.second.ratioOfPair()) / 2
    }

    fun elongationIndex(): Double {
        val pairs = oppositeEdgesTuples()
        val lengthAverage1 = pairs.first.avgLength()
        val lengthAverage2 = pairs.second.avgLength()
        return maxOf(lengthAverage1, lengthAverage2) / minOf(lengthAverage1, lengthAverage2)
    }

    fun split1x2(crossingEdge: Edge): List<Quadrilateral> {
        val polygonEdges = edges
        val edgesToSplit =
            polygonEdges.filter { polygonEdge -> crossingEdge.points.any { polygonEdge.containsPoint(it) } }
        val remainingEdges = polygonEdges - edgesToSplit.toSet()

        return if (edgesToSplit.size == 2) {
            val p1 = setOf(remainingEdges[0].p1, remainingEdges[0].p2, crossingEdge.p1, crossingEdge.p2)
            val p2 = setOf(remainingEdges[1].p1, remainingEdges[1].p2, crossingEdge.p1, crossingEdge.p2)
            listOf(Quadrilateral(p1), Quadrilateral(p2))
        } else {
            logger.warn { "couldn't split $this" }
            listOf(this)
        }
    }

    fun split2x2AtIntersection(crossingEdge1: Edge, crossingEdge2: Edge): Layout {
        val intersectionPoint = crossingEdge1.intersectionPoint(crossingEdge2)
        if (intersectionPoint == null) {
            logger.warn { "the two crossing edges do not intersect within the quadrilateral" }
            return Layout(listOf(this), listOf(crossingEdge1, crossingEdge2))
        } else {
            // split in 2
            val halfPolygons = split1x2(crossingEdge1)

            // split the resulting 2 polygons into 4 polygons
            val newPolygons = mutableListOf<Polygon>()
            val subEdges1 = crossingEdge2.splitAtCrossingWith(crossingEdge1)
            newPolygons += subEdges1
                .find { edge -> halfPolygons[0].containsMidPointOf(edge) }
                ?.let { edge -> halfPolygons[0].split1x2(edge) }
                .orEmpty()

            newPolygons += subEdges1
                .find { edge -> halfPolygons[1].containsMidPointOf(edge) }
                ?.let { edge -> halfPolygons[1].split1x2(edge) }
                .orEmpty()

            require(newPolygons.size == 4) { "expected to have 4 new polygons, but got ${newPolygons.size}" }

            // calculate the two other sub-edges, resulting from the second split
            val approximationSubEdges2 = crossingEdge1.splitAtCrossingWith(crossingEdge2)

            // to avoid discrepancies due to rounding differences,
            // find the two edges existing in the polygons that are closest to the calculated expected sub-edges
            // (in practice, they seem to differ by a factor ~1e-14)
            val newPolygonEdges = newPolygons.flatMap { it.edges }
            val subEdges2 =
                listOf(
                    newPolygonEdges.find { it.equalsWithTolerance(approximationSubEdges2[0], 1e-4) }!!,
                    newPolygonEdges.find { it.equalsWithTolerance(approximationSubEdges2[1], 1e-4) }!!
                )

            if (logger.isDebugEnabled()) {
                val lengthDelta1 = subEdges2[0].lengthDelta(approximationSubEdges2[0])
                val lengthDelta2 = subEdges2[1].lengthDelta(approximationSubEdges2[1])

                if (lengthDelta1 > 0 || lengthDelta2 > 0) {
                    logger.debug {
                        "lengthDelta1: ${subEdges2[0].lengthDelta(approximationSubEdges2[0])}, " +
                                "lengthDelta2: ${subEdges2[1].lengthDelta(approximationSubEdges2[1])}"
                    }
                }
            }

            return Layout(newPolygons, subEdges1 + subEdges2)
        }
    }

}
