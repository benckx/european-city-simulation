package simulation.model

import kotlinx.serialization.Serializable
import kotlin.math.atan2

@Serializable
open class Polygon(
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Polygon) return false
        return points == other.points
    }

    override fun hashCode(): Int {
        return points.hashCode()
    }

}
