package simulation.model

data class OppositeEdgesTuple(val edges: Set<Edge>) {

    constructor(pair: Pair<Edge, Edge>) : this(setOf(pair.first, pair.second))

    init {
        require(edges.size == 2) { "A ${javaClass.simpleName} must contain exactly 2 edges" }
        val edges1 = edges.elementAt(0)
        val edges2 = edges.elementAt(1)
        require(!edges1.sharesPointWith(edges2)) { "The two edges in a ${javaClass.simpleName} must not share a point" }
    }

    fun minLength(): Double =
        edges.minOf { it.length }

    fun maxLength(): Double =
        edges.maxOf { it.length }

    fun avgLength(): Double =
        edges.map { it.length }.average()

    // if the larger edge is 10% larger, the ratio will be .10
    fun ratioOfPair(): Double =
        maxLength() / minLength() - 1

}
