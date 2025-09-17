package simulation

import simulation.geometry.delaunayTriangulation
import simulation.model.Point

const val WIDTH = 1920
const val HEIGHT = 1080
const val BORDER_MARGIN_X = WIDTH * .15
const val BORDER_MARGIN_Y = HEIGHT * .15

const val NUMBER_OF_POINTS = 16

fun createPoints(): List<Point> {
    val points = mutableListOf<Point>()
    (1..NUMBER_OF_POINTS).forEach { _ ->
        val x = (BORDER_MARGIN_X.toInt() until (WIDTH - BORDER_MARGIN_X).toInt()).random()
        val y = (BORDER_MARGIN_Y.toInt() until (HEIGHT - BORDER_MARGIN_Y).toInt()).random()
        points += Point(x.toDouble(), y.toDouble())
    }

    return points
}

fun main() {
    val points = createPoints()
    val result = delaunayTriangulation(points)
    println("points: ${points.size}, triangles: ${result.triangles.size}")
    outputToPng(points, result.triangles)
}
