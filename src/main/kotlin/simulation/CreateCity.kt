package simulation

import simulation.geometry.delaunayTriangulation
import simulation.geometry.distanceBetween
import simulation.model.Point
import simulation.model.Triangle
import kotlin.math.min

const val WIDTH = 1600
const val HEIGHT = 1600
const val BORDER_MARGIN_X = WIDTH * .04
const val BORDER_MARGIN_Y = HEIGHT * .04

val MIN_DISTANCE = min(WIDTH * .15, HEIGHT * .15)
val MAX_DISTANCE = MIN_DISTANCE * 2

const val TIMEOUT = 10_000L
const val NUMBER_OF_POINTS = 24

fun createPoints(): List<Point> {
    val points = mutableListOf<Point>()
    val timeoutAt = System.currentTimeMillis() + TIMEOUT

    while (points.size < NUMBER_OF_POINTS && System.currentTimeMillis() < timeoutAt) {
        val x = (BORDER_MARGIN_X.toInt() until (WIDTH - BORDER_MARGIN_X).toInt()).random()
        val y = (BORDER_MARGIN_Y.toInt() until (HEIGHT - BORDER_MARGIN_Y).toInt()).random()
        val candidate = Point(x.toDouble(), y.toDouble())
        val isTooCloseToAnotherPoint = points.any { distanceBetween(it, candidate) < MIN_DISTANCE }
        val isTooFarFromAllPoints = !points.isEmpty() && points.all { distanceBetween(it, candidate) > MAX_DISTANCE }
        val isValid = !isTooCloseToAnotherPoint && !isTooFarFromAllPoints
        if (isValid) {
            points += candidate
        }
    }

    return points
}

fun createTriangulation(minAngleCriteria: Int): List<Triangle> {
    var goOn = true
    var triangles: List<Triangle> = emptyList()
    var attempts = 0

    while (goOn) {
        val points = createPoints()
        triangles = delaunayTriangulation(points)
        val minAngle = triangles.flatMap { it.angles() }.minOf { it }
        goOn = minAngle <= minAngleCriteria
        attempts++

        if (attempts % 1_000 == 0) {
            println("attempts: $attempts")
        }
    }

    println("triangulation found after $attempts attempts")

    return triangles
}

fun main() {
    println("creating layout")
    val triangles = createTriangulation(30)
    outputToPng(triangles)
}
