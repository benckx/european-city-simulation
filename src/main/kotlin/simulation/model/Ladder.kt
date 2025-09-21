package simulation.model

import simulation.model.Ladder.NextPointStrategy.MIN_ANGLE
import simulation.model.Ladder.NextPointStrategy.MIN_DISTANCE
import kotlin.math.sqrt

/**
 * Ladder structure is a list of quasi parallel edges a series of adjacent quadrilaterals (adjacent in the same direction).
 */
data class Ladder(
    val edges: List<Edge>
) {

    enum class NextPointStrategy {
        MIN_ANGLE,
        MIN_DISTANCE
    }

    val size = edges.size

    fun crossingLine(ratio: Double, nextPointStrategy: NextPointStrategy = MIN_ANGLE): Line {
        val result = mutableListOf<Edge>()

        // among all 4 possible edges, find the shortest one to start with
        val points1 = edges[0].pointsAt(ratio)
        val points2 = edges[1].pointsAt(ratio)
        val firstEdge = points1.flatMap { p1 -> points2.map { p2 -> Edge(p1, p2) } }.minBy { it.length }
        result += firstEdge

        var previousPoint = firstEdge.points.find { points2.contains(it) }!!
        edges.drop(2).forEach { nextLadderEdge ->
            val nextPoint = when (nextPointStrategy) {
                MIN_ANGLE -> nextPointByMinAngle(ratio, nextLadderEdge, result.last(), previousPoint)
                MIN_DISTANCE -> nextLadderEdge.pointsAt(ratio).minBy { it.distanceTo(previousPoint) }
            }

            result += Edge(previousPoint, nextPoint)
            previousPoint = nextPoint
        }
        return Line(result.toList())
    }

    private fun nextPointByMinAngle(
        ratio: Double,
        nextLadderEdge: Edge,
        previousLadderEdge: Edge,
        previousPoint: Point
    ): Point {
        return nextLadderEdge.pointsAt(ratio).minBy { point ->
            val currentVector = Point(point.x - previousPoint.x, point.y - previousPoint.y)
            val prevVector = Point(previousPoint.x - previousLadderEdge.p1.x, previousPoint.y - previousLadderEdge.p1.y)

            // Calculate angle between vectors using dot product
            val dotProduct = currentVector.x * prevVector.x + currentVector.y * prevVector.y
            val magnitude1 = sqrt(currentVector.x * currentVector.x + currentVector.y * currentVector.y)
            val magnitude2 = sqrt(prevVector.x * prevVector.x + prevVector.y * prevVector.y)

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
