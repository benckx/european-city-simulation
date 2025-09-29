package simulation.model

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.math.ceil

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
        val tuples = oppositeEdgesTuples()
        val lengthAverage1 = tuples.first.avgLength()
        val lengthAverage2 = tuples.second.avgLength()
        return maxOf(lengthAverage1, lengthAverage2) / minOf(lengthAverage1, lengthAverage2)
    }

    fun calculateSubdivisionFactor(): Pair<Int, Int>? {
        val angleToMaxEdge = listOf(
            20 to 70.0,
            30 to 80.0,
            60 to 100.0,
            70 to 110.0,
            80 to 130.0,
            90 to 140.0,
            100 to 150.0,
            120 to 180.0,
            140 to 200.0
        )

        val angles = interiorAngles()
        val minAngle = angles.min()
        val maxAngle = angles.max()
        val angleDelta = maxAngle - minAngle

        val max = angleToMaxEdge.maxOf { it.second }
        val referenceEdgeLength = angleToMaxEdge.find { (angle, _) -> angleDelta <= angle }?.second ?: max
        val pairs = oppositeEdgesTuples().toList()
        val shortEdges = pairs.minBy { it.avgLength() }
        val longEdges = pairs.maxBy { it.avgLength() }
        val shortLength = shortEdges.minLength()
        val longLength = longEdges.minLength() // FIXME: should this be maxLength() for longEdges?
        val shortDiv = ceil(shortLength / referenceEdgeLength).toInt()
        val longDiv = ceil(longLength / referenceEdgeLength).toInt()
        logger.debug {
            val shortEdge = String.format("%.1f", shortLength)
            val longEdge = String.format("%.1f", longLength)
            "[subdivision] Δ${angleDelta.toInt()}°, maxEdgeLength=${referenceEdgeLength} -> ${shortDiv}x${longDiv} (${shortEdge}x${longEdge})"
        }
        return if (shortDiv >= 1 && longDiv >= 1) {
            shortDiv to longDiv
        } else {
            null
        }
    }

    fun calculateSubdivision(shortDiv: Int, longDiv: Int): QuadrilateralSubdivision {
        fun calculateSubDivisionOnOpposeEdgeTuple(edgesTuple: OppositeEdgesTuple, div: Int): List<Edge> {
            val oppositeEdges = edgesTuple.edges.toList()
            val edge1 = oppositeEdges[0]
            val edge2 = oppositeEdges[1]
            val edgeLength = listOf(edge1.length, edge2.length).min()
            val segmentLength = (edgeLength / div)

            var points1: List<Point>
            var points2: List<Point>

            if (div == 2) {
                points1 = edge1.pointsAt(.5).toList()
                points2 = edge2.pointsAt(.5).toList()
            } else {
                points1 = edge1.pointsDividedBy(segmentLength).take(div - 1)
                points2 = edge2.pointsDividedBy(segmentLength).take(div - 1)
            }

            // if list of points are in opposite directions (however both sorted), reverse one of them
            val firstEdge = Edge(points1.first(), points2.first())
            val lastEdge = Edge(points1.last(), points2.last())
            val intersects = firstEdge.intersectionPoint(lastEdge) != null

            if (intersects) {
                points2 = edge2.reverse().pointsDividedBy(segmentLength).take(div - 1)
            }

            require(points1.size == div - 1) { "Expected ${div - 1} points but got ${points1.size} for divisor $div on edge 1" }
            require(points2.size == div - 1) { "Expected ${div - 1} points but got ${points2.size} for divisor $div on edge 2" }

            val newEdges = points1.zip(points2) { p1, p2 -> Edge(p1, p2) }

            if (newEdges.size != (div - 1)) {
                logger.warn {
                    "${shortDiv}x${longDiv}, expected ${div - 1} new edges, but got ${newEdges.size} instead, " +
                            "#points1: ${points1.size}, #points2: ${points2.size}"
                }
            }

            return newEdges
        }

        require(shortDiv <= longDiv) { "shortDiv ($shortDiv) must be <= longDiv ($longDiv)" }
        require(shortDiv > 0 && longDiv > 0) { "Division factors must be positive, ${shortDiv}x${longDiv}" }

        val tuples = oppositeEdgesTuples()
        val shortEdgesTuple = tuples.toList().minBy { it.avgLength() }
        val longEdgesTuple = tuples.toList().maxBy { it.avgLength() }

        val shortSideEdges = mutableListOf<Edge>()
        val longSideEdges = mutableListOf<Edge>()
        if (shortDiv > 1) {
            shortSideEdges += calculateSubDivisionOnOpposeEdgeTuple(shortEdgesTuple, shortDiv)
        }
        if (longDiv > 1) {
            longSideEdges += calculateSubDivisionOnOpposeEdgeTuple(longEdgesTuple, longDiv)
        }

        return QuadrilateralSubdivision(this, shortSideEdges, longSideEdges)
    }

    // TODO: can be replaced by the more generic M x N split feature?
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

    // TODO: can be replaced by the more generic M x N split feature?
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
