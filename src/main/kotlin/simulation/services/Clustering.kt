package simulation.services

import simulation.model.Edge
import simulation.model.Ladder
import simulation.model.Layout
import simulation.model.Polygon
import kotlin.collections.forEach
import kotlin.collections.plusAssign

/**
 * Detect all clusters of facing edges (i.e. a ladder structure) in the layout.
 * Each quadrilateral can only belong to one cluster.
 * Each edge can only belong to one cluster.
 * An edge belongs to a cluster if it is opposite to an edge that belongs to the cluster.
 *
 * @param layout the layout to analyze
 * @return a set of clusters, each cluster is a set of edges
 */
fun detectLadders(layout: Layout): List<Ladder> {
    val allQuadrilaterals = layout.polygons.filter { it.isQuadrilateral() }

    val allLadders = listAllPairOfNeighbors(allQuadrilaterals)
        .map { pairOfNeighbors -> detectLadderFromPairOfNeighbours(allQuadrilaterals, pairOfNeighbors) }
        .toSet()

    return allLadders.filter { ladder ->
        allLadders.none { other ->
            other != ladder && ladder.edges.toSet().all { it in other.edges.toSet() }
        }
    }
}

private fun listAllPairOfNeighbors(allQuadrilaterals: List<Polygon>): Set<Set<Polygon>> {
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
    allQuadrilaterals: List<Polygon>,
    pairOfNeighbors: Set<Polygon>
): Ladder {
    val clusterEdges = mutableSetOf<Edge>()

    val p1 = pairOfNeighbors.first()
    val p2 = pairOfNeighbors.last()
    val sharedEdge = p1.edges.intersect(p2.edges.toSet()).firstOrNull()

    require(pairOfNeighbors.size == 2) { "pair must contain exactly two polygons" }
    require(sharedEdge != null) { "the two polygons must share an edge" }
    require(p1 != p2) { "the two polygons must be different" }

    val oppositeEdge1 = p1.oppositeEdge(sharedEdge)
    val oppositeEdge2 = p2.oppositeEdge(sharedEdge)
    clusterEdges += listOf(sharedEdge, oppositeEdge1, oppositeEdge2)

    val otherQuadrilaterals = allQuadrilaterals.filter { other -> other != p1 && other != p2 }.toSet()
    var continueSearch = true
    var nextEdges = listOf(oppositeEdge1, oppositeEdge2)

    while (continueSearch) {
        val newEdges = mutableSetOf<Edge>()

        nextEdges.forEach { edge ->
            val otherQuadrilateral = otherQuadrilaterals.find { other -> other.edges.contains(edge) }
            if (otherQuadrilateral != null) {
                val oppositeEdge = otherQuadrilateral.oppositeEdge(edge)
                if (!clusterEdges.contains(oppositeEdge)) {
                    newEdges += oppositeEdge
                }
            }
        }

        continueSearch = newEdges.isNotEmpty()
        clusterEdges += newEdges
        nextEdges = newEdges.toList()
    }

    // order edges
    val clusterEdgeList = clusterEdges.toList()

    // If only 3 edges, return in the requested order
    if (clusterEdgeList.size == 3) {
        return Ladder(listOf(oppositeEdge1, sharedEdge, oppositeEdge2))
    }

    // For more than 3, order edges in contiguous sequence
    // Find terminal edge (only one neighbor in cluster)
    fun edgeNeighbors(edge: Edge): List<Edge> =
        allQuadrilaterals
            .filter { it.edges.contains(edge) }
            .flatMap { quad -> quad.edges }
            .filter { it in clusterEdges && it != edge }

    val terminalEdge = clusterEdgeList.find { edgeNeighbors(it).size == 1 }
        ?: clusterEdgeList.first()

    val orderedEdges = mutableListOf<Edge>()
    val visitedEdges = mutableSetOf<Edge>()
    var current = terminalEdge

    while (orderedEdges.size < clusterEdgeList.size) {
        orderedEdges += current
        visitedEdges += current
        val next = clusterEdgeList.firstOrNull { it !in visitedEdges && edgeNeighbors(current).contains(it) }
        if (next == null) break
        current = next
    }

    return Ladder(orderedEdges)
}
