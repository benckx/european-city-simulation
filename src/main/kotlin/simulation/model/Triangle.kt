package simulation.model

import kotlin.math.hypot
import kotlin.math.pow

data class Triangle(val a: Point, val b: Point, val c: Point) {
    fun isPointInCircumcircle(p: Point): Boolean {
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
        val dist = hypot(p.x - centerX, p.y - centerY)
        return dist <= radius
    }

    fun getEdges(): Set<Edge> = setOf(Edge(a, b), Edge(b, c), Edge(c, a))
}
