package simulation.model

data class Edge(val p1: Point, val p2: Point) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Edge
        return (p1 == other.p1 && p2 == other.p2) || (p1 == other.p2 && p2 == other.p1)
    }

    override fun hashCode(): Int = p1.hashCode() + p2.hashCode()
}
