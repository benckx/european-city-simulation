package simulation

import io.github.oshai.kotlinlogging.KotlinLogging
import simulation.geometry.delaunayTriangulation
import simulation.geometry.distanceBetween
import simulation.model.Point
import simulation.model.Polygon
import simulation.model.Triangle
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Stream

const val WIDTH = 1600
const val HEIGHT = 1600
const val BORDER_MARGIN_X = WIDTH * .03
const val BORDER_MARGIN_Y = HEIGHT * .03
const val MIN_DISTANCE_BETWEEN_POINTS = 200
const val MAX_DISTANCE_BETWEEN_POINTS = 500
const val MIN_ANGLE = 30
const val NUMBER_OF_POINTS = 22

const val TIMEOUT = 10_000L

private val logger = KotlinLogging.logger {}

fun createPoints(): List<Point> {
    val points = mutableListOf<Point>()
    val timeoutAt = System.currentTimeMillis() + TIMEOUT

    fun isTooCloseToAnotherPoint(candidate: Point) =
        points.any { distanceBetween(it, candidate) < MIN_DISTANCE_BETWEEN_POINTS }

    fun isTooFarFromAllPoints(candidate: Point) =
        !points.isEmpty() && points.all { distanceBetween(it, candidate) > MAX_DISTANCE_BETWEEN_POINTS }

    while (points.size < NUMBER_OF_POINTS && System.currentTimeMillis() < timeoutAt) {
        val x = (BORDER_MARGIN_X.toInt() until (WIDTH - BORDER_MARGIN_X).toInt()).random()
        val y = (BORDER_MARGIN_Y.toInt() until (HEIGHT - BORDER_MARGIN_Y).toInt()).random()
        val candidate = Point(x.toDouble(), y.toDouble())
        if (!isTooCloseToAnotherPoint(candidate) && !isTooFarFromAllPoints(candidate)) {
            points += candidate
        }
    }

    return points
}

fun createTriangulation(): List<Triangle> {
    val attempts = AtomicInteger(0)

    val triangles = Stream.generate {
        val points = createPoints()
        val triangles = delaunayTriangulation(points)
        val minAngle = triangles.flatMap { it.angles() }.minOf { it }
        val currentAttempts = attempts.incrementAndGet()

        if (currentAttempts % 1_000 == 0) {
            println("attempts: $currentAttempts")
        }

        triangles to minAngle
    }
        .parallel()
        .filter { (_, minAngle) -> minAngle > MIN_ANGLE }
        .findFirst()
        .map { (triangle, _) -> triangle }
        .orElseThrow { RuntimeException("Could not find valid triangulation") }

    logger.info { "triangulation found after ${attempts.get()} attempts" }

    return triangles
}

/**
 * Merges pairs of triangles that share a hypotenuse into quadrilaterals.
 * Triangles that do not share a hypotenuse with another triangle remain unchanged.
 *
 * @param triangles The list of triangles to process.
 * @return A list of polygons, which may include both triangles and quadrilaterals.
 */
fun mergeTrianglesToQuadrilaterals(triangles: List<Triangle>): List<Polygon> {
    val hypotenuses = triangles.map { triangle -> triangle.hypotenuse }
    val sharedHypotenuses = hypotenuses.filter { edge -> hypotenuses.count { it == edge } > 1 }.distinct()
    val trianglesToKeep = triangles.filterNot { it.edges.any { edge -> sharedHypotenuses.contains(edge) } }.toSet()
    val quadrilaterals = sharedHypotenuses.map { hypotenuse ->
        val trianglesToMerge = triangles.filter { it.edges.contains(hypotenuse) }
        require(trianglesToMerge.size == 2) { "exactly two triangles must share the hypotenuse" }
        val points = trianglesToMerge.flatMap { it.points }.toSet()
        require(points.size == 4) { "merging two triangles must result in a quadrilateral with 4 distinct points" }
        Polygon(points.toSet())
    }

    logger.info {
        val totalPoints = triangles.flatMap { it.points }.distinct().size
        "triangles: ${triangles.size}, #points = $totalPoints, #sharedHypotenuses: ${sharedHypotenuses.size}"
    }

    return trianglesToKeep.map { triangle -> triangle.asPolygon() } + quadrilaterals
}

fun main() {
    logger.info { "creating layout" }
    logger.info { "size: ${WIDTH}x$HEIGHT" }
    logger.info { "#points: $NUMBER_OF_POINTS" }
    logger.info { "min between points: $MIN_DISTANCE_BETWEEN_POINTS" }
    logger.info { "max between points: $MAX_DISTANCE_BETWEEN_POINTS" }

    val triangles = createTriangulation()
    val polygons = mergeTrianglesToQuadrilaterals(triangles)
    logger.info {
        "polygons: ${polygons.size}, " +
                "#triangles = ${polygons.count { it.isTriangle() }}, " +
                "#quadrilaterals = ${polygons.count { it.isQuadrilateral() }}"
    }
    outputToPng(polygons)
}
