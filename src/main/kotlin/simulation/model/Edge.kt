package simulation.model

import kotlinx.serialization.Serializable
import simulation.utils.distanceBetween
import kotlin.math.abs

@Serializable
data class Edge(val p1: Point, val p2: Point) {
    val length: Double
        get() = distanceBetween(p1, p2)

    val points
        get() = setOf(p1, p2)

    fun shift(dx: Double, dy: Double): Edge =
        Edge(p1.shift(dx, dy), p2.shift(dx, dy))

    fun sharesPointWith(other: Edge): Boolean =
        points.intersect(other.points).isNotEmpty()

    fun lengthDelta(other: Edge): Double =
        abs(length - other.length)

    fun pointsAt(percentage: Double): Set<Point> {
        require(percentage in 0.0..1.0) { "Percentage must be between 0.0 and 1.0" }

        val x1 = p1.x + (p2.x - p1.x) * percentage
        val y1 = p1.y + (p2.y - p1.y) * percentage

        if (percentage == .5) {
            return setOf(Point(x1, y1))
        }

        val x2 = p2.x + (p1.x - p2.x) * percentage
        val y2 = p2.y + (p1.y - p2.y) * percentage
        return setOf(Point(x1, y1), Point(x2, y2))
    }

    fun intersectionPoint(edge: Edge): Point? {
        val x1 = p1.x
        val y1 = p1.y
        val x2 = p2.x
        val y2 = p2.y

        val x3 = edge.p1.x
        val y3 = edge.p1.y
        val x4 = edge.p2.x
        val y4 = edge.p2.y

        val denominator = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4)

        // lines are parallel
        if (abs(denominator) < 1e-9) {
            return null
        }

        val t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / denominator
        val u = -((x1 - x2) * (y1 - y3) - (y1 - y2) * (x1 - x3)) / denominator

        // check if intersection point lies within both line segments
        if (t in 0.0..1.0 && u in 0.0..1.0) {
            val intersectionX = x1 + t * (x2 - x1)
            val intersectionY = y1 + t * (y2 - y1)
            return Point(intersectionX, intersectionY)
        }

        return null
    }

    fun splitAtCrossingWith(edge: Edge): List<Edge> {
        val intersectionPoint = intersectionPoint(edge)
        return if (intersectionPoint != null &&
            intersectionPoint != p1 && intersectionPoint != p2 &&
            intersectionPoint != edge.p1 && intersectionPoint != edge.p2
        ) {
            listOf(
                Edge(p1, intersectionPoint),
                Edge(intersectionPoint, p2)
            )
        } else {
            emptyList()
        }
    }

    fun containsPoint(point: Point, tolerance: Double = 1e-9): Boolean {
        val start = p1
        val end = p2

        // check if point is collinear with the edge endpoints
        val crossProduct = (point.y - start.y) * (end.x - start.x) - (point.x - start.x) * (end.y - start.y)

        // if cross product is not zero (within tolerance), point is not on the line
        if (abs(crossProduct) > tolerance) {
            return false
        }

        // check if point is within the bounds of the line segment
        val dotProduct = (point.x - start.x) * (end.x - start.x) + (point.y - start.y) * (end.y - start.y)
        val squaredLength = (end.x - start.x) * (end.x - start.x) + (end.y - start.y) * (end.y - start.y)

        return dotProduct in 0.0..squaredLength
    }

    fun equalsWithTolerance(other: Edge, tolerance: Double = .001): Boolean {
        val p1EqualsOtherP1 = p1.equalsWithTolerance(other.p1, tolerance)
        val p2EqualsOtherP2 = p2.equalsWithTolerance(other.p2, tolerance)
        val p1EqualsOtherP2 = p1.equalsWithTolerance(other.p2, tolerance)
        val p2EqualsOtherP1 = p2.equalsWithTolerance(other.p1, tolerance)
        return (p1EqualsOtherP1 && p2EqualsOtherP2) || (p1EqualsOtherP2 && p2EqualsOtherP1)
    }

    override fun toString(): String {
        return "Edge(x1=${p1.x}, y1=${p1.y}, x2=${p2.x}, y2=${p2.y})"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        (other as Edge)
        return other.points == points
    }

    override fun hashCode(): Int = p1.hashCode() + p2.hashCode()

}
