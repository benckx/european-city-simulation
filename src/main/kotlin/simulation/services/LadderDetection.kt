package simulation.services

import io.github.oshai.kotlinlogging.KotlinLogging
import simulation.model.*

private val logger = KotlinLogging.logger {}

class LadderDetection(layout: Layout) {

    private val allQuadrilaterals = layout.quadrilaterals()

    /**
     * Detect all clusters of facing edges in a series of contiguous quadrilaterals (i.e. a ladder structure).
     * Each quadrilateral can only belong to one [Ladder].
     * Each [Edge] can only belong to one [Ladder] structure.
     *
     * @param layout the layout to analyze
     * @return a set of clusters, each cluster is a set of edges
     */
    fun detectLadders(): List<Ladder> {
        val visitedEdges = mutableSetOf<Edge>()
        val ladders = mutableListOf<Ladder>()

        listAllPairsOfNeighbors(allQuadrilaterals).forEach { pair ->
            val ladderEdges = detectLadderFromPairOfNeighbours(pair)
            if (!ladderEdges.any { visitedEdges.contains(it) }) {
                ladders += orderLadderEdges(ladderEdges)
            }
            visitedEdges += ladderEdges
        }
        return ladders
    }

    private fun orderLadderEdges(ladderEdges: Set<Edge>): Ladder {
        fun areLadderNeighbors(p1: Polygon, p2: Polygon): Boolean =
            p1 != p2 && p1.edges.intersect(p2.edges.toSet()).intersect(ladderEdges).isNotEmpty()

        fun ladderNeighborsOf(polygon: Polygon): List<Polygon> =
            allQuadrilaterals.filter { otherPolygon -> areLadderNeighbors(polygon, otherPolygon) }

        fun ladderEdgesPresentInOnlyOnePolygon(): List<Edge> =
            ladderEdges.filter { edge -> allQuadrilaterals.flatMap { it.edges }.count { it == edge } == 1 }

        val ladderQuadrilaterals = allQuadrilaterals.filter { q -> ladderEdges.any { edge -> q.edges.contains(edge) } }
        val ladderNeighborsOf = ladderQuadrilaterals.associateWith { q -> ladderNeighborsOf(q) }
        val terminalQuadrilaterals = ladderNeighborsOf.entries.filter { (_, v) -> v.size == 1 }.map { it.key }

        require(terminalQuadrilaterals.isNotEmpty()) { "There should be at least 1 terminal quadrilaterals in a ladder polygons but there are ${terminalQuadrilaterals.size}" }

        logger.debug { "#edgesUsedByOnlyOnePolygon: ${ladderEdgesPresentInOnlyOnePolygon().size}" }
        logger.debug { "#terminalQuadrilaterals: ${terminalQuadrilaterals.size}" }

        var currentEdge =
            ladderEdgesPresentInOnlyOnePolygon().find { terminalQuadrilaterals.flatMap { q -> q.edges }.contains(it) }!!
        var currentPolygon: Polygon? = terminalQuadrilaterals.first { it.edges.contains(currentEdge) }
        val orderedEdges = mutableListOf<Edge>()
        val visitedPolygons = mutableSetOf<Polygon>()
        orderedEdges += currentEdge
        visitedPolygons += currentPolygon!!
        while (currentPolygon != null) {
            val nextEdge = currentPolygon.oppositeEdge(currentEdge)
            orderedEdges += nextEdge
            currentPolygon = ladderNeighborsOf[currentPolygon]?.find { neighbor ->
                neighbor.edges.contains(nextEdge) && !visitedPolygons.contains(neighbor)
            }
            currentEdge = nextEdge
            currentPolygon?.let { visitedPolygons += it }
        }

        return Ladder(orderedEdges)
    }

    private fun listAllPairsOfNeighbors(allQuadrilaterals: List<Polygon>): Set<Set<Polygon>> {
        require(allQuadrilaterals.all { it.isQuadrilateral() })

        val pairOfNeighbors = mutableSetOf<Set<Polygon>>()
        allQuadrilaterals.forEach { quadrilateral ->
            val otherQuadrilaterals = allQuadrilaterals.filter { it != quadrilateral }
            val edges = quadrilateral.edges
            val otherEdges = otherQuadrilaterals.flatMap { it.edges }.toSet()
            val sharedEdges = edges.intersect(otherEdges)
            sharedEdges.forEach { sharedEdge ->
                otherQuadrilaterals
                    .find { it.edges.contains(sharedEdge) }
                    ?.let { neighbor ->
                        pairOfNeighbors += setOf(quadrilateral, neighbor)
                    }
            }
        }

        return pairOfNeighbors
    }

    private fun detectLadderFromPairOfNeighbours(
        pairOfNeighbors: Set<Polygon>
    ): Set<Edge> {
        val p1 = pairOfNeighbors.first()
        val p2 = pairOfNeighbors.last()
        val sharedEdge = p1.edges.intersect(p2.edges.toSet()).firstOrNull()

        require(pairOfNeighbors.size == 2) { "pair must contain exactly two polygons" }
        require(sharedEdge != null) { "the two polygons must share an edge" }
        require(p1 != p2) { "the two polygons must be different" }

        val oppositeEdge1 = p1.oppositeEdge(sharedEdge)
        val oppositeEdge2 = p2.oppositeEdge(sharedEdge)
        val ladderEdges = mutableSetOf<Edge>()
        ladderEdges += listOf(sharedEdge, oppositeEdge1, oppositeEdge2)

        val otherQuadrilaterals = allQuadrilaterals.filter { other -> other != p1 && other != p2 }.toSet()
        var continueSearch = true
        var nextEdges = listOf(oppositeEdge1, oppositeEdge2)

        while (continueSearch) {
            val newEdges = nextEdges.flatMap { edge ->
                otherQuadrilaterals
                    .filter { quadrilateral -> quadrilateral.edges.contains(edge) }
                    .map { quadrilateral -> quadrilateral.oppositeEdge(edge) }
                    .filterNot { oppositeEdge -> ladderEdges.contains(oppositeEdge) }
            }

            continueSearch = newEdges.isNotEmpty()
            ladderEdges += newEdges
            nextEdges = newEdges.toList()
        }

        return ladderEdges.toSet()
    }

    companion object {
        fun detectLadders(layout: Layout): List<Ladder> =
            LadderDetection(layout).detectLadders()
    }

}
