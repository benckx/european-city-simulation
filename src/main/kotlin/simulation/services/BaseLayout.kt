package simulation.services

import io.github.oshai.kotlinlogging.KotlinLogging
import simulation.utils.delaunayTriangulation
import simulation.utils.distanceBetween
import simulation.model.Point
import simulation.model.Triangle
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Stream

const val WIDTH = 2_400
const val HEIGHT = 2_400
const val MIN_DISTANCE_BETWEEN_POINTS = 200
const val MAX_DISTANCE_BETWEEN_POINTS = 500

const val DEFAULT_MIN_ANGLE = 24
const val DEFAULT_NUMBER_OF_POINTS = 22

const val TIMEOUT = 10_000L

private val logger = KotlinLogging.logger {}

fun createBaseTriangulation(
    numberOfPoints: Int = DEFAULT_NUMBER_OF_POINTS,
    requiredMinAngle: Int = DEFAULT_MIN_ANGLE
): List<Triangle> {
    val attempts = AtomicInteger(0)

    val triangles = Stream.generate {
        val points = createPoints(numberOfPoints)
        val triangles = delaunayTriangulation(points)
        val minAngle = triangles.flatMap { it.angles() }.minOf { it }
        val currentAttempts = attempts.incrementAndGet()

        if (currentAttempts % 1_000 == 0) {
            logger.info { "attempts: $currentAttempts" }
        }

        triangles to minAngle
    }
        .parallel()
        .filter { (_, actualMinAngle) -> actualMinAngle >= requiredMinAngle }
        .findFirst()
        .map { (triangle, _) -> triangle }
        .orElseThrow { RuntimeException("Could not find valid triangulation") }

    logger.info { "triangulation found after ${attempts.get()} attempts" }

    return triangles
}

private fun createPoints(numberOfPoints: Int): List<Point> {
    val points = mutableListOf<Point>()
    val timeoutAt = System.currentTimeMillis() + TIMEOUT

    fun isTooCloseToAnotherPoint(candidate: Point) =
        points.any { distanceBetween(it, candidate) < MIN_DISTANCE_BETWEEN_POINTS }

    fun isTooFarFromAllPoints(candidate: Point) =
        !points.isEmpty() && points.all { distanceBetween(it, candidate) > MAX_DISTANCE_BETWEEN_POINTS }

    while (points.size < numberOfPoints && System.currentTimeMillis() < timeoutAt) {
        val x = (0 until WIDTH).random()
        val y = (0 until HEIGHT).random()
        val candidate = Point(x.toDouble(), y.toDouble())
        if (!isTooCloseToAnotherPoint(candidate) && !isTooFarFromAllPoints(candidate)) {
            points += candidate
        }
    }

    return points
}
