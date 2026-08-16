package simulation.model

/**
 * Result of subdividing a [Quadrilateral] into a grid of smaller cells.
 *
 * The subdivision is described by the internal edges that cut across the original quadrilateral:
 * [shortSideEdges] run parallel to (and split) the shorter pair of opposite sides, while
 * [longSideEdges] run parallel to (and split) the longer pair. Together with the original
 * boundary, these edges form a grid whose cells can be recovered via [subQuadrilaterals].
 */
data class QuadrilateralSubdivision(
    val quadrilateral: Quadrilateral,
    val shortSideEdges: List<Edge>,
    val longSideEdges: List<Edge>
) {

    /**
     * All internal subdivision edges (both the short-side and long-side cuts).
     */
    fun bothSidesEdges(): List<Edge> =
        shortSideEdges + longSideEdges

    /**
     * The grid dimensions as (columns, rows), i.e. the number of cells along each axis.
     *
     * Each set of internal edges splits its axis into one more cell than the number of edges.
     */
    fun divisionFactors(): Pair<Int, Int> =
        Pair(shortSideEdges.size + 1, longSideEdges.size + 1)

    /**
     * Every distinct point involved in the subdivision: the original corners, the endpoints of
     * the internal edges, and the interior intersection points where short-side and long-side
     * edges cross.
     */
    fun allPoints(): Set<Point> {
        val points = mutableSetOf<Point>()
        points += quadrilateral.points
        points += shortSideEdges
            .flatMap { shortEdge -> longSideEdges.map { longEdge -> shortEdge.intersectionPoint(longEdge)!! } }
        points += shortSideEdges.flatMap { it.points }
        points += longSideEdges.flatMap { it.points }
        return points.toSet()
    }

    /**
     * Reconstruct the individual grid cells produced by the subdivision.
     *
     * Because the subdivision is only known as a set of points and edges, the cells are recovered
     * geometrically: every combination of 4 points is tested, keeping only convex quadrilaterals
     * whose four sides all lie along subdivision edges (original boundary or internal cuts) and
     * that contain no other subdivision point. A final pass discards any non-minimal cell that
     * fully encloses another, leaving only the smallest grid cells.
     */
    fun subQuadrilaterals(): List<Quadrilateral> {
        val allPoints = allPoints().toList()

        fun isConnected(polygon: Polygon): Boolean {
            val edges = polygon.edges
            val points = polygon.points
            return points.all { p -> edges.count { edge -> edge.containsPoint(p) } == 2 }
        }

        // FIXME: it's probably not worth checking since it's filtered out later anyway
        fun containAnyOtherPoints(polygon: Polygon) =
            allPoints.any { p -> !polygon.points.contains(p) && polygon.containsPoint(p) }

        fun isValidQuadrilateral(points: Set<Point>): Boolean {
            if (points.size != 4) return false
            try {
                val polygon = Quadrilateral(points)
                return polygon.isConvex() && isConnected(polygon) && !containAnyOtherPoints(polygon)
            } catch (_: Exception) {
                return false
            }
        }

        val foundQuadrilaterals = mutableSetOf<Quadrilateral>()

        // get all subdivision edges (both original boundary and internal)
        val allSubdivisionEdges = mutableListOf<Edge>()

        // add original quadrilateral boundary edges
        allSubdivisionEdges += quadrilateral.edges

        // add subdivision edges
        allSubdivisionEdges += shortSideEdges
        allSubdivisionEdges += longSideEdges

        // try all combinations of 4 points to see if they form a valid grid cell
        for (i in 0 until allPoints.size - 3) {
            for (j in i + 1 until allPoints.size - 2) {
                for (k in j + 1 until allPoints.size - 1) {
                    for (l in k + 1 until allPoints.size) {
                        val candidatePoints = setOf(
                            allPoints[i],
                            allPoints[j],
                            allPoints[k],
                            allPoints[l]
                        )

                        if (isValidQuadrilateral(candidatePoints)) {
                            val quad = Quadrilateral(candidatePoints)

                            // check that all edges of this quadrilateral lie along subdivision edges
                            val allEdgesValid = quad.edges.all { quadEdge ->
                                allSubdivisionEdges.any { subdivisionEdge ->
                                    subdivisionEdge.containsPoint(quadEdge.p1) &&
                                            subdivisionEdge.containsPoint(quadEdge.p2)
                                }
                            }

                            if (allEdgesValid && foundQuadrilaterals.none { existing -> existing.points == quad.points }) {
                                foundQuadrilaterals += quad
                            }
                        }
                    }
                }
            }
        }

        // filter out quadrilaterals that are too large (not minimal cells)
        val minimalQuads = foundQuadrilaterals.filter { quad ->
            // a quadrilateral is minimal if no other quadrilateral is completely contained within it
            foundQuadrilaterals.none { other ->
                other != quad && other.points.all { point ->
                    quad.containsPoint(point) || quad.edges.any { edge -> edge.containsPoint(point) }
                }
            }
        }

        return minimalQuads.toList()
    }

}
