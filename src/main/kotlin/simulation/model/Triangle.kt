package simulation.model

import kotlin.math.acos
import kotlin.math.hypot
import kotlin.math.pow

class Triangle(points: Set<Point>) : Polygon(points) {

    constructor(a: Point, b: Point, c: Point) : this(setOf(a, b, c))

    fun angles(): List<Double> {
        val trianglePoints = points.toList()
        val a = trianglePoints[0]
        val b = trianglePoints[1]
        val c = trianglePoints[2]

        val ab = hypot(b.x - a.x, b.y - a.y)
        val bc = hypot(c.x - b.x, c.y - b.y)
        val ca = hypot(a.x - c.x, a.y - c.y)

        val angleA = acos((ab.pow(2) + ca.pow(2) - bc.pow(2)) / (2 * ab * ca)) * (180 / Math.PI)
        val angleB = acos((ab.pow(2) + bc.pow(2) - ca.pow(2)) / (2 * ab * bc)) * (180 / Math.PI)
        val angleC = 180.0 - angleA - angleB

        return listOf(angleA, angleB, angleC)
    }

    fun isPointInCircumcircle(point: Point): Boolean {
        val trianglePoints = points.toList()
        val a = trianglePoints[0]
        val b = trianglePoints[1]
        val c = trianglePoints[2]

        val d = (a.x * (b.y - c.y) + b.x * (c.y - a.y) + c.x * (a.y - b.y)) * 2
        val ax2 = a.x.pow(2)
        val ay2 = a.y.pow(2)
        val bx2 = b.x.pow(2)
        val by2 = b.y.pow(2)
        val cx2 = c.x.pow(2)
        val cy2 = c.y.pow(2)

        val centerX = ((ax2 + ay2) * (b.y - c.y) + (bx2 + by2) * (c.y - a.y) + (cx2 + cy2) * (a.y - b.y)) / d
        val centerY = ((ax2 + ay2) * (c.x - b.x) + (bx2 + by2) * (a.x - c.x) + (cx2 + cy2) * (b.x - a.x)) / d
        val radius = hypot(a.x - centerX, a.y - centerY)
        val dist = hypot(point.x - centerX, point.y - centerY)
        return dist <= radius
    }

}
