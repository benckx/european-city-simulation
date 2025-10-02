package simulation.services

import simulation.model.Layout
import simulation.model.Polygon
import simulation.model.Triangle

const val CLUSTERING_MAX_ANGLE = 150.0

/**
 * Merges pairs of triangles that share a hypotenuse into quadrilaterals.
 * Triangles that do not share a hypotenuse with another triangle remain unchanged.
 *
 * @param layout that only contains [Triangle]
 * @return A [Layout], which may include both triangles and quadrilaterals.
 */
fun mergeTrianglesToQuadrilaterals(layout: Layout, sizeIndex: Int = 2): Layout {
    require(sizeIndex in 1..3) { "sizeIndex must be between 1 and 3" }

    val triangles = layout.triangles()
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

                // check if quadrilateral is convex
                // and has all interior angles are smaller than a threshold (to avoid non-convex-looking shapes)
                if (quadrilateral.isConvex() && quadrilateral.interiorAngles().max() <= CLUSTERING_MAX_ANGLE) {
                    quadrilaterals += quadrilateral
                    mergedTriangles += trianglesToMerge
                }
            }
        }
    }

    val trianglesToKeep = triangles.filterNot { mergedTriangles.contains(it) }
    return Layout(trianglesToKeep + quadrilaterals)
}
