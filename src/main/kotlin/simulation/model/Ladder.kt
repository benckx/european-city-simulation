package simulation.model

import simulation.model.Ladder.NextPointStrategy.MIN_ANGLE
import simulation.model.Ladder.NextPointStrategy.MIN_DISTANCE
import kotlin.math.sqrt

/**
 * A Ladder is a list of quasi-parallel, non-touching edges taken from a series of adjacent
 * quadrilaterals (each edge acting like a rung).
 *
 * Its purpose is to derive a single axis ([crossingLine]) that cuts through all those adjacent
 * quadrilaterals at once, so the initial large quadrilaterals can be subdivided consistently along
 * that shared axis.
 */
data class Ladder(
    val edges: List<Edge>
) {

    /**
     * Strategy used by [crossingLine] to pick, on each successive ladder edge, which of the two
     * candidate points (see [Edge.pointsAt]) to connect to next.
     */
    enum class NextPointStrategy {
        /**
         * Pick the point that keeps the crossing line as straight as possible, i.e. that minimizes
         * the turning angle with respect to the previous segment.
         */
        MIN_ANGLE,

        /**
         * Pick the point closest to the previous one, i.e. that minimizes the segment length.
         */
        MIN_DISTANCE
    }

    /**
     * Number of edges (rungs) in the ladder.
     */
    val size = edges.size

    /**
     * Build a [Line] that crosses every edge of the ladder, connecting one point per edge into a
     * continuous poly-line.
     *
     * On each ladder edge, [Edge.pointsAt] yields two candidate crossing points (located [ratio]
     * along the edge from either end); the line starts from the shortest possible first segment
     * between the first two edges, then extends edge by edge, choosing the next point according to
     * [nextPointStrategy].
     *
     * @param ratio position along each edge (0.0..1.0) at which the crossing points are taken.
     * @param nextPointStrategy how to pick the next point on each subsequent edge; see [NextPointStrategy].
     */
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

    /**
     * Among the two candidate points on [nextLadderEdge], return the one that minimizes the angle
     * between the incoming segment (arriving at [previousPoint]) and the outgoing segment.
     *
     * The angle is compared via the cosine of the two vectors (computed with the dot product):
     * returning the negative cosine turns "maximize cosine" into a "minimize" selection, which
     * keeps the resulting crossing line as straight as possible.
     */
    private fun nextPointByMinAngle(
        ratio: Double,
        nextLadderEdge: Edge,
        previousLadderEdge: Edge,
        previousPoint: Point
    ): Point {
        return nextLadderEdge.pointsAt(ratio).minBy { point ->
            val currentVector = Point(point.x - previousPoint.x, point.y - previousPoint.y)
            val prevVector = Point(previousPoint.x - previousLadderEdge.p1.x, previousPoint.y - previousLadderEdge.p1.y)

            // calculate angle between vectors using dot product
            val dotProduct = currentVector.x * prevVector.x + currentVector.y * prevVector.y
            val magnitude1 = sqrt(currentVector.x * currentVector.x + currentVector.y * currentVector.y)
            val magnitude2 = sqrt(prevVector.x * prevVector.x + prevVector.y * prevVector.y)

            // return negative cosine to minimize angle (maximize cosine)
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
