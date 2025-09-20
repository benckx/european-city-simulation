package simulation.model

data class Line(val edges: List<Edge>) {

    init {
        require(edges.size >= 2) { "A line must have at least 2 edges." }
        for (i in 0 until edges.size - 1) {
            require(edges[i].sharesPointWith(edges[i + 1])) {
                "Edges must be connected. Edge ${edges[i]} is not connected to ${edges[i + 1]}"
            }
        }
    }

    val size: Int
        get() = edges.size

    fun shareAnyPointWith(other: Line): Boolean =
        edges.any { edge -> other.edges.any { it.sharesPointWith(edge) } }

}
