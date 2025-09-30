package simulation.model

import kotlinx.serialization.Serializable
import java.lang.Math.toDegrees
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
     * Calculate all interior angles of the polygon in degrees
     * Assumes the polygon is convex
     */
    fun interiorAnglesAndPoints(): List<Pair<Point, Double>> {
        val orderedPoints = orderedPoints()
        val anglesAndPoints = mutableListOf<Pair<Point, Double>>()

        for (i in orderedPoints.indices) {
            val prev = orderedPoints[(i - 1 + orderedPoints.size) % orderedPoints.size]
            val current = orderedPoints[i]
            val next = orderedPoints[(i + 1) % orderedPoints.size]

            // calculate vectors from current point to previous and next points
            val vector1 = Point(prev.x - current.x, prev.y - current.y)
            val vector2 = Point(next.x - current.x, next.y - current.y)

            // calculate dot product and magnitudes
            val dotProduct = vector1.x * vector2.x + vector1.y * vector2.y
            val magnitude1 = kotlin.math.sqrt(vector1.x * vector1.x + vector1.y * vector1.y)
            val magnitude2 = kotlin.math.sqrt(vector2.x * vector2.x + vector2.y * vector2.y)

            // calculate angle using acos of normalized dot product
            val cosAngle = dotProduct / (magnitude1 * magnitude2)
            val angleRadians = kotlin.math.acos(cosAngle.coerceIn(-1.0, 1.0))

            // convert to degrees
            anglesAndPoints += current to toDegrees(angleRadians)
        }

        return anglesAndPoints
    }

    /**
     * See [interiorAnglesAndPoints]
     */
    fun interiorAngles(): List<Double> = interiorAnglesAndPoints().map { it.second }

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
