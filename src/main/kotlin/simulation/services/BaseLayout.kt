package simulation.services

import io.github.oshai.kotlinlogging.KotlinLogging
import simulation.model.Layout
import simulation.model.Point
import simulation.model.Triangle
import simulation.utils.delaunayTriangulation
import simulation.utils.distanceBetween
import java.lang.System.currentTimeMillis
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Stream

private const val WIDTH = 3_000
private const val HEIGHT = 3_000
private const val MIN_DISTANCE_BETWEEN_POINTS = 200
private const val MAX_DISTANCE_BETWEEN_POINTS = 500

private const val THRESHOLD_NUMBER_OF_POINTS = 12
private const val INCREMENT_NUMBER_OF_POINTS = 4

private const val POINTS_TIMEOUT = 10_000L
private const val GLOBAL_TRIANGULATION_TIMEOUT = 60_000L
private const val SINGLE_TRIANGULATION_TIMEOUT = 20_000L

private val logger = KotlinLogging.logger {}

fun createBaseTriangulation(requestedNumberOfPoints: Int, requiredMinAngle: Int): List<Layout> {
    if (requestedNumberOfPoints <= THRESHOLD_NUMBER_OF_POINTS) {
        return listOf(
            Layout(
                growTriangulation(
                    numberOfNewPoints = requestedNumberOfPoints,
                    requiredMinAngle = requiredMinAngle,
                    existingPoints = emptySet(),
                    existingTriangles = emptySet()
                )
            )
        )
    } else {
        val timeoutAt = currentTimeMillis() + GLOBAL_TRIANGULATION_TIMEOUT
        val existingPoints = mutableSetOf<Point>()
        val layoutHistory = mutableListOf<Layout>()
        var numberOfNewPoints = THRESHOLD_NUMBER_OF_POINTS
        while (existingPoints.size < requestedNumberOfPoints && currentTimeMillis() < timeoutAt) {
            val existingTriangles = layoutHistory.lastOrNull()?.triangles()?.toSet() ?: emptySet()
            val triangles = growTriangulation(
                numberOfNewPoints = numberOfNewPoints,
                requiredMinAngle = requiredMinAngle,
                existingPoints = existingPoints,
                existingTriangles = existingTriangles
            )
            if (triangles.isNotEmpty()) {
                layoutHistory += Layout(triangles)
                existingPoints += triangles.flatMap { triangle -> triangle.points }
            }
            numberOfNewPoints = minOf(INCREMENT_NUMBER_OF_POINTS, requestedNumberOfPoints - existingPoints.size)
        }

        return if (layoutHistory.isEmpty() || existingPoints.size < requestedNumberOfPoints) {
            logger.warn { "could only fulfill ${existingPoints.size}/$requestedNumberOfPoints points in ${GLOBAL_TRIANGULATION_TIMEOUT / 1_000} sec., starting over" }
            createBaseTriangulation(requestedNumberOfPoints, requiredMinAngle)
        } else {
            layoutHistory
        }
    }
}

private fun growTriangulation(
    numberOfNewPoints: Int,
    requiredMinAngle: Int,
    existingPoints: Set<Point>,
    existingTriangles: Set<Triangle>
): List<Triangle> {
    val attempts = AtomicInteger(0)

    val logTag = "[${existingPoints.size}+${numberOfNewPoints}]"
    val timeoutAt = currentTimeMillis() + SINGLE_TRIANGULATION_TIMEOUT

    val triangles = Stream.generate {
        val triangulationPoints = existingPoints + createPoints(numberOfNewPoints, existingPoints, existingTriangles)
        val triangles = delaunayTriangulation(triangulationPoints)
        val minAngle = triangles.flatMap { triangle -> triangle.angles() }.min()
        val currentAttempts = attempts.incrementAndGet()

        if (currentAttempts % 1_000 == 0) {
            logger.info { "$logTag attempts: $currentAttempts" }
        }

        triangles to minAngle
    }
        .parallel()
        .takeWhile { currentTimeMillis() < timeoutAt }
        .filter { (_, actualMinAngle) -> actualMinAngle >= requiredMinAngle }
        .findFirst()
        .map { (triangle, _) -> triangle }

    if (triangles.isPresent) {
        logger.info { "$logTag triangulation found after ${attempts.get()} attempts" }
    } else {
        logger.warn { "$logTag triangulation NOT found after ${attempts.get()} attempts in ${SINGLE_TRIANGULATION_TIMEOUT / 1_000} sec." }
    }

    return triangles.orElse(emptyList())
}

private fun createPoints(
    numberOfNewPoints: Int,
    existingPoints: Set<Point>,
    existingTriangles: Set<Triangle>
): Set<Point> {
    val points = mutableSetOf<Point>()
    val timeoutAt = currentTimeMillis() + POINTS_TIMEOUT

    fun allPoints() = existingPoints + points

    fun isTooCloseToAnotherPoint(candidate: Point): Boolean {
        return allPoints().any { otherPoint ->
            distanceBetween(otherPoint, candidate) < MIN_DISTANCE_BETWEEN_POINTS
        }
    }

    fun isTooFarFromAllPoints(candidate: Point): Boolean {
        return !allPoints().isEmpty() && allPoints().all { otherPoint ->
            distanceBetween(otherPoint, candidate) > MAX_DISTANCE_BETWEEN_POINTS
        }
    }

    fun isContainedInAnyTriangle(candidate: Point): Boolean {
        return existingTriangles.any { triangle -> triangle.containsPoint(candidate) }
    }

    while (points.size < numberOfNewPoints && currentTimeMillis() < timeoutAt) {
        val x = (0 until WIDTH).random()
        val y = (0 until HEIGHT).random()
        val candidate = Point(x.toDouble(), y.toDouble())
        if (!isTooCloseToAnotherPoint(candidate) &&
            !isTooFarFromAllPoints(candidate) &&
            !isContainedInAnyTriangle(candidate)
        ) {
            points += candidate
        }
    }

    return points
}
