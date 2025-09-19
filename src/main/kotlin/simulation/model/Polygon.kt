package simulation.model

data class Polygon(
    val points: Set<Point>
) {
    init {
        require(points.size >= 3) { "A polygon must have at least 3 distinct points." }
    }

    val edges: Set<Edge>
        get() {
            val pts = if (points.size > 3) {
                orderPointsConvexly(points.toList())
            } else {
                points.toList()
            }
            val edgeList = mutableListOf<Edge>()
            for (i in pts.indices) {
                val nextIndex = (i + 1) % pts.size
                edgeList.add(Edge(pts[i], pts[nextIndex]))
            }
            return edgeList.toSet()
        }

    fun isTriangle(): Boolean = points.size == 3
    fun isQuadrilateral(): Boolean = points.size == 4
}

private fun orderPointsConvexly(points: List<Point>): List<Point> {
    // Find centroid
    val centerX = points.map { it.x }.average()
    val centerY = points.map { it.y }.average()
    val center = Point(centerX, centerY)

    // Sort points by angle from center
    return points.sortedBy { point ->
        kotlin.math.atan2(point.y - center.y, point.x - center.x)
    }
}
