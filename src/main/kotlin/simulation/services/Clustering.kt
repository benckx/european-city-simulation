package simulation.services

import simulation.model.Edge
import simulation.model.Ladder
import simulation.model.Layout
import simulation.model.Polygon
import simulation.model.Triangle
import kotlin.collections.plusAssign

/**
 * Merges pairs of triangles that share a hypotenuse into quadrilaterals.
 * Triangles that do not share a hypotenuse with another triangle remain unchanged.
 *
 * @param triangles The list of triangles to process.
 * @return A list of polygons, which may include both triangles and quadrilaterals.
 */
fun mergeTrianglesToQuadrilaterals(triangles: List<Triangle>, sizeIndex: Int): List<Polygon> {
    require(sizeIndex in 1..3) { "sizeIndex must be between 1 and 3" }

    val longestEdges = triangles
        .flatMap { triangle ->
            triangle.edges.sortedByDescending { it.length }.take(sizeIndex)
        }

    val sharedLongEdges = longestEdges.filter { edge -> longestEdges.count { it == edge } > 1 }.distinct()
    val mergedTriangles = mutableSetOf<Triangle>()
    val quadrilaterals = mutableListOf<Polygon>()

    for (edge in sharedLongEdges) {
        val trianglesToMerge = triangles.filter { it.edges.contains(edge) && !mergedTriangles.contains(it) }

        if (trianglesToMerge.size == 2) {
            val points = trianglesToMerge.flatMap { it.points }.toSet()
            if (points.size == 4) {
                val quadrilateral = Polygon(points)

                // Check if quadrilateral is convex
                if (quadrilateral.isConvex()) {
                    quadrilaterals.add(quadrilateral)
                    mergedTriangles.addAll(trianglesToMerge)
                }
            }
        }
    }

    val trianglesToKeep = triangles.filterNot { mergedTriangles.contains(it) }
    return trianglesToKeep.map { triangle -> triangle.asPolygon() } + quadrilaterals
}

/**
 * Detect all clusters of facing edges in a series of contiguous quadrilaterals (i.e. a ladder structure)..
 * Each quadrilateral can only belong to one [Ladder].
 * Each [Edge] can only belong to one [Ladder] structure.
 *
 * @param layout the layout to analyze
 * @return a set of clusters, each cluster is a set of edges
 */
fun detectLadders(layout: Layout): List<Ladder> {
    val allQuadrilaterals = layout.quadrilaterals()

    val allLadders = listAllPairOfNeighbors(allQuadrilaterals)
        .map { pairOfNeighbors -> detectLadderFromPairOfNeighbours(allQuadrilaterals, pairOfNeighbors) }
        .distinct()
        .sortedByDescending { it.size }

    val nonOverlappingLadders = mutableListOf<Ladder>()
    val usedEdges = mutableSetOf<Edge>()

    for (ladder in allLadders) {
        // Check if this ladder shares any edges with already selected ladders
        if (ladder.edges.none { it in usedEdges }) {
            nonOverlappingLadders += ladder
            usedEdges += ladder.edges
        }
    }

    return nonOverlappingLadders
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

    // if only 3 edges, return in the requested order
    if (clusterEdgeList.size == 3) {
        return Ladder(listOf(oppositeEdge1, sharedEdge, oppositeEdge2))
    }

    // for more than 3, order edges in contiguous sequence
    // find terminal edge (only one neighbor in cluster)
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
