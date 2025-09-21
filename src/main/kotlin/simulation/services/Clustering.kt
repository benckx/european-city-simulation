package simulation.services

import simulation.model.Polygon
import simulation.model.Triangle

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
