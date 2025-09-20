package simulation.model

import simulation.geometry.distanceBetween

data class Edge(val p1: Point, val p2: Point) {
    val length: Double
        get() = distanceBetween(p1, p2)

    val points
        get() = setOf(p1, p2)

    fun shift(dx: Double, dy: Double): Edge =
        Edge(p1.shift(dx, dy), p2.shift(dx, dy))

    fun sharesPointWith(other: Edge): Boolean =
        points.intersect(other.points).isNotEmpty()

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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        (other as Edge)
        return other.points == points
    }

    override fun hashCode(): Int = p1.hashCode() + p2.hashCode()

}
