package simulation.model

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.atan2

private val logger = KotlinLogging.logger {}

@Serializable
data class Polygon(
    val points: Set<Point>
) {
    init {
        require(points.size >= 3) { "A polygon must have at least 3 distinct points." }
    }

    val edges: List<Edge>
        get() {
            val pts = if (points.size > 3) {
                orderedPoints()
            } else {
                points.toList()
            }
            val edgeList = mutableListOf<Edge>()
            for (i in pts.indices) {
                val nextIndex = (i + 1) % pts.size
                edgeList.add(Edge(pts[i], pts[nextIndex]))
            }
            return edgeList
        }

    fun isTriangle(): Boolean = points.size == 3

    fun isQuadrilateral(): Boolean = points.size == 4

    fun findCentroid(): Point {
        val centerX = points.map { it.x }.average()
        val centerY = points.map { it.y }.average()
        return Point(centerX, centerY)
    }

    fun oppositeEdge(baseEdge: Edge): Edge {
        require(edges.contains(baseEdge)) { "The polygon does not contain the specified edge." }
        require(isQuadrilateral()) { "The polygon must be a quadrilateral to have an opposite edge." }
        return edges.find { edge -> edge != baseEdge && !edge.sharesPointWith(baseEdge) }!!
    }

    fun oppositeEdgesTuples(): Pair<OppositeEdgesTuple, OppositeEdgesTuple> {
        require(isQuadrilateral()) { "The polygon must be a quadrilateral to have 2 pairs of facing edges" }
        val e1 = edges.random()
        val e2 = oppositeEdge(e1)
        val pair1 = Pair(e1, e2)
        val otherEdges = edges.filter { e1 != it && e2 != it }
        val pair2 = Pair(otherEdges[0], otherEdges[1])
        return OppositeEdgesTuple(pair1) to OppositeEdgesTuple(pair2)
    }

    /**
     * Order the points in clockwise, convexly order around the centroid
     */
    fun orderedPoints(): List<Point> {
        val center = findCentroid()

        // Sort points by angle from center
        return points.sortedBy { point ->
            atan2(point.y - center.y, point.x - center.x)
        }
    }

    /**
     * Assuming is convex
     */
    fun area(): Double {
        val orderedPoints = orderedPoints()
        var area = 0.0

        for (i in orderedPoints.indices) {
            val j = (i + 1) % orderedPoints.size
            area += orderedPoints[i].x * orderedPoints[j].y
            area -= orderedPoints[j].x * orderedPoints[i].y
        }

        return abs(area) / 2.0
    }

    fun angles(): List<Double> {
        val ordered = orderedPoints()
        val n = ordered.size
        val angles = mutableListOf<Double>()

        for (i in ordered.indices) {
            val prev = ordered[(i - 1 + n) % n]
            val curr = ordered[i]
            val next = ordered[(i + 1) % n]

            val v1x = prev.x - curr.x
            val v1y = prev.y - curr.y
            val v2x = next.x - curr.x
            val v2y = next.y - curr.y

            val dot = v1x * v2x + v1y * v2y
            val det = v1x * v2y - v1y * v2x
            val angle = abs(atan2(det, dot))
            angles.add(Math.toDegrees(angle))
        }
        return angles
    }

    fun quadrilateralIrregularityIndex(): Double {
        require(isQuadrilateral()) { "Must be a quadrilateral to calculate the irregularity index" }
        val tuples = oppositeEdgesTuples()
        return (tuples.first.ratioOfPair() + tuples.second.ratioOfPair()) / 2
    }

    fun quadrilateralElongationIndex(): Double {
        require(isQuadrilateral()) { "Must be a quadrilateral to calculate the elongation index" }

        val pairs = oppositeEdgesTuples()
        val lengthAverage1 = pairs.first.avgLength()
        val lengthAverage2 = pairs.second.avgLength()
        return maxOf(lengthAverage1, lengthAverage2) / minOf(lengthAverage1, lengthAverage2)
    }

    fun isConvex(): Boolean {
        val orderedPoints = orderedPoints()

        for (i in orderedPoints.indices) {
            val prev = orderedPoints[(i - 1 + orderedPoints.size) % orderedPoints.size]
            val current = orderedPoints[i]
            val next = orderedPoints[(i + 1) % orderedPoints.size]

            // Calculate cross product to determine turn direction
            val crossProduct = (current.x - prev.x) * (next.y - current.y) - (current.y - prev.y) * (next.x - current.x)

            // If cross product is negative, we have a right turn (concave)
            if (crossProduct < 0) {
                return false
            }
        }
        return true
    }

    // workaround approximation of "crosses through the polygon"
    fun containsMidPointOf(edge: Edge): Boolean {
        return containsPoint(edge.pointsAt(.5).first())
    }

    /**
     * Check if a point is inside the polygon using the ray casting algorithm
     */
    fun containsPoint(point: Point): Boolean {
        val orderedPoints = orderedPoints()
        var inside = false
        var j = orderedPoints.size - 1

        for (i in orderedPoints.indices) {
            val xi = orderedPoints[i].x
            val yi = orderedPoints[i].y
            val xj = orderedPoints[j].x
            val yj = orderedPoints[j].y

            if (((yi > point.y) != (yj > point.y)) &&
                (point.x < (xj - xi) * (point.y - yi) / (yj - yi) + xi)
            ) {
                inside = !inside
            }
            j = i
        }

        return inside
    }

    // 1x2
    fun splitQuadrilateralInTwo(crossingEdge: Edge): List<Polygon> {
        require(isQuadrilateral())

        val polygonEdges = edges
        val edgesToSplit =
            polygonEdges.filter { polygonEdge -> crossingEdge.points.any { polygonEdge.containsPoint(it) } }
        val remainingEdges = polygonEdges - edgesToSplit.toSet()

        return if (edgesToSplit.size == 2) {
            val p1 = setOf(remainingEdges[0].p1, remainingEdges[0].p2, crossingEdge.p1, crossingEdge.p2)
            val p2 = setOf(remainingEdges[1].p1, remainingEdges[1].p2, crossingEdge.p1, crossingEdge.p2)
            listOf(Polygon(p1), Polygon(p2))
        } else {
            logger.warn { "couldn't split $this" }
            listOf(this)
        }
    }

    // 2x2 (with intersection)
    fun splitQuadrilateralInFourAtIntersection(crossingEdge1: Edge, crossingEdge2: Edge): Layout {
        require(isQuadrilateral())

        val intersectionPoint = crossingEdge1.intersectionPoint(crossingEdge2)
        if (intersectionPoint == null) {
            logger.warn { "the two crossing edges do not intersect within the quadrilateral" }
            return Layout(listOf(this), listOf(crossingEdge1, crossingEdge2))
        } else {
            // split in 2
            val halfPolygons = splitQuadrilateralInTwo(crossingEdge1)

            // split the resulting 2 polygons into 4 polygons
            val newPolygons = mutableListOf<Polygon>()
            val subEdges1 = crossingEdge2.splitAtCrossingWith(crossingEdge1)
            newPolygons += subEdges1
                .find { edge -> halfPolygons[0].containsMidPointOf(edge) }
                ?.let { edge -> halfPolygons[0].splitQuadrilateralInTwo(edge) }
                .orEmpty()

            newPolygons += subEdges1
                .find { edge -> halfPolygons[1].containsMidPointOf(edge) }
                ?.let { edge -> halfPolygons[1].splitQuadrilateralInTwo(edge) }
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
