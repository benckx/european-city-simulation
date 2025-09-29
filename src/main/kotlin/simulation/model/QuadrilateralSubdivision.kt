package simulation.model

data class QuadrilateralSubdivision(
    val quadrilateral: Quadrilateral,
    val shortSideEdges: List<Edge>,
    val longSideEdges: List<Edge>
) {

    fun bothSidesEdges(): List<Edge> =
        shortSideEdges + longSideEdges

    fun divisionFactors(): Pair<Int, Int> =
        Pair(shortSideEdges.size + 1, longSideEdges.size + 1)

    fun allPoints(): Set<Point> {
        val points = mutableSetOf<Point>()
        points += quadrilateral.points
        points += shortSideEdges
            .flatMap { shortEdge -> longSideEdges.map { longEdge -> shortEdge.intersectionPoint(longEdge)!! } }
        points += shortSideEdges.flatMap { it.points }
        points += longSideEdges.flatMap { it.points }
        return points.toSet()
    }

    fun subQuadrilaterals(): List<Quadrilateral> {
        val allPoints = allPoints().toList()

        fun isConnected(polygon: Polygon): Boolean {
            val edges = polygon.edges
            val points = polygon.points
            return points.all { p -> edges.count { edge -> edge.containsPoint(p) } == 2 }
        }

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
