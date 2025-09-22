package simulation.utils

import simulation.model.Edge
import simulation.model.Point
import simulation.model.Triangle
import kotlin.math.hypot

fun delaunayTriangulation(points: List<Point>): List<Triangle> {
    // determine bounds for the super-triangle
    val minX = points.minOf { it.x }
    val maxX = points.maxOf { it.x }
    val minY = points.minOf { it.y }
    val maxY = points.maxOf { it.y }
    val deltaX = maxX - minX
    val deltaY = maxY - minY
    val maxDelta = maxOf(deltaX, deltaY)
    val midX = (minX + maxX) / 2
    val midY = (minY + maxY) / 2

    // create a super-triangle that encloses all points
    val p1 = Point(midX - 2 * maxDelta, midY - maxDelta)
    val p2 = Point(midX + 2 * maxDelta, midY - maxDelta)
    val p3 = Point(midX, midY + 2 * maxDelta)

    val triangles = mutableListOf(Triangle(p1, p2, p3))

    // main insertion loop
    for (p in points) {
        val badTriangles = mutableListOf<Triangle>()
        val polygon = mutableSetOf<Edge>()

        // find all triangles whose circumcircle contains the point
        for (t in triangles) {
            if (t.isPointInCircumcircle(p)) {
                badTriangles.add(t)
                polygon.addAll(t.edges)
            }
        }

        // remove shared edges from the polygon
        for (t in badTriangles) {
            t.edges.forEach { edge ->
                if (badTriangles.count { it.edges.contains(edge) } > 1) {
                    polygon.remove(edge)
                }
            }
        }

        // remove bad triangles from the simulation.services.main list
        triangles.removeAll(badTriangles)

        // create new triangles from the polygon edges and the new point
        for (edge in polygon) {
            triangles.add(Triangle(edge.p1, edge.p2, p))
        }
    }

    // final cleanup: remove any triangles that contain a vertex from the super-triangle
    return triangles.filterNot { t ->
        t.a == p1 || t.a == p2 || t.a == p3 ||
                t.b == p1 || t.b == p2 || t.b == p3 ||
                t.c == p1 || t.c == p2 || t.c == p3
    }
}

fun distanceBetween(p1: Point, p2: Point): Double {
    return hypot((p1.x - p2.x), (p1.y - p2.y))
}
