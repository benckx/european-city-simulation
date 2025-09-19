package simulation.model

import simulation.geometry.distanceBetween

data class Edge(val p1: Point, val p2: Point) {
    val length: Double
        get() = distanceBetween(p1, p2)

    fun shift(dx: Double, dy: Double): Edge =
        Edge(p1.shift(dx, dy), p2.shift(dx, dy))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Edge
        return (p1 == other.p1 && p2 == other.p2) || (p1 == other.p2 && p2 == other.p1)
    }

    override fun hashCode(): Int = p1.hashCode() + p2.hashCode()
}
