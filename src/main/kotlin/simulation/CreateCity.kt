package simulation

import simulation.geometry.delaunayTriangulation
import simulation.model.Point

const val WIDTH = 1920
const val HEIGHT = 1080
const val NUMBER_OF_POINTS = 16

fun createPoints(): List<Point> {
    val marginX = WIDTH * .15
    val marginY = HEIGHT * .15
    val points = mutableListOf<Point>()
    (1..NUMBER_OF_POINTS).forEach { _ ->
        val x = (marginX.toInt() until (WIDTH - marginX).toInt()).random()
        val y = (marginY.toInt() until (HEIGHT - marginY).toInt()).random()
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
