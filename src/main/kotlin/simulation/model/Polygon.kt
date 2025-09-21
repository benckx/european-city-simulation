package simulation.model

import kotlinx.serialization.Serializable

@Serializable
data class Polygon(
    val points: Set<Point>
) {
    init {
        require(points.size >= 3) { "A polygon must have at least 3 distinct points." }
    }

    fun findCentroid(): Point {
        val centerX = points.map { it.x }.average()
        val centerY = points.map { it.y }.average()
        return Point(centerX, centerY)
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

    fun oppositeEdge(baseEdge: Edge): Edge {
        require(edges.contains(baseEdge)) { "The polygon does not contain the specified edge." }
        require(isQuadrilateral()) { "The polygon must be a quadrilateral to have an opposite edge." }
        return edges.find { edge -> edge != baseEdge && !edge.sharesPointWith(baseEdge) }!!
    }

    /**
     * Order the points in clockwise, convexly order around the centroid
     */
    fun orderedPoints(): List<Point> {
        val center = findCentroid()

        // Sort points by angle from center
        return points.sortedBy { point ->
            kotlin.math.atan2(point.y - center.y, point.x - center.x)
        }
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

}
