package simulation.model

/**
 * Ladder structure is a list of quasi parallel edges a series of adjacent quadrilaterals (adjacent in the same direction).
 */
data class Ladder(
    val edges: List<Edge>
) {

    val size = edges.size

    fun reverse(): Ladder = Ladder(edges.reversed())

    fun crossingLine(ratio: Double): Line {
        val result = mutableListOf<Edge>()
        val ladderEdges = edges

        // among all 4 possible edges, find the shortest one to start with
        val points1 = ladderEdges[0].pointsAt(ratio)
        val points2 = ladderEdges[1].pointsAt(ratio)
        val firstEdge = points1.flatMap { p1 -> points2.map { p2 -> Edge(p1, p2) } }.minBy { it.length }
        result += firstEdge

        var lastPoint = firstEdge.points.find { points2.contains(it) }!!
        ladderEdges.drop(2).forEach { nextLadderEdge ->
            val nextPoint = nextPointByMinAngle(ratio, nextLadderEdge, result.last(), lastPoint)
//            val nextPoint = nextLadderEdge.pointsAt(ratio).minBy { it.distanceTo(lastPoint) }
            result += Edge(lastPoint, nextPoint)
            lastPoint = nextPoint
        }
        return Line(result.toList())
    }

    private fun nextPointByMinAngle(
        ratio: Double,
        nextLadderEdge: Edge,
        lastCrossingEdge: Edge,
        lastPoint: Point
    ): Point {
        return nextLadderEdge.pointsAt(ratio).minBy { point ->
            val currentVector = Point(point.x - lastPoint.x, point.y - lastPoint.y)
            val prevVector = Point(lastPoint.x - lastCrossingEdge.p1.x, lastPoint.y - lastCrossingEdge.p1.y)

            // Calculate angle between vectors using dot product
            val dotProduct = currentVector.x * prevVector.x + currentVector.y * prevVector.y
            val magnitude1 = kotlin.math.sqrt(currentVector.x * currentVector.x + currentVector.y * currentVector.y)
            val magnitude2 = kotlin.math.sqrt(prevVector.x * prevVector.x + prevVector.y * prevVector.y)

            // Return negative cosine to minimize angle (maximize cosine)
            -(dotProduct / (magnitude1 * magnitude2))
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        (other as Ladder)
        return other.edges.toSet() == edges.toSet()
    }

    override fun hashCode(): Int {
        var result = size
        result = 31 * result + edges.toSet().hashCode()
        return result
    }

}
