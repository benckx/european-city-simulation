package simulation.geometry

import simulation.model.Edge
import simulation.model.Point
import simulation.model.Triangle

fun delaunayTriangulation(points: List<Point>): DelauneyTriangulation {
    // Determine bounds for the super-triangle
    val minX = points.minOf { it.x }
    val maxX = points.maxOf { it.x }
    val minY = points.minOf { it.y }
    val maxY = points.maxOf { it.y }
    val deltaX = maxX - minX
    val deltaY = maxY - minY
    val maxDelta = maxOf(deltaX, deltaY)
    val midX = (minX + maxX) / 2
    val midY = (minY + maxY) / 2

    // Create a super-triangle that encloses all points
    val p1 = Point(midX - 2 * maxDelta, midY - maxDelta)
    val p2 = Point(midX + 2 * maxDelta, midY - maxDelta)
    val p3 = Point(midX, midY + 2 * maxDelta)

    val triangles = mutableListOf(Triangle(p1, p2, p3))

    // Main insertion loop
    for (p in points) {
        val badTriangles = mutableListOf<Triangle>()
        val polygon = mutableSetOf<Edge>()

        // Find all triangles whose circumcircle contains the point
        for (t in triangles) {
            if (t.isPointInCircumcircle(p)) {
                badTriangles.add(t)
                polygon.addAll(t.getEdges())
            }
        }

        // Remove shared edges from the polygon
        for (t in badTriangles) {
            t.getEdges().forEach { edge ->
                if (badTriangles.count { it.getEdges().contains(edge) } > 1) {
                    polygon.remove(edge)
                }
            }
        }

        // Remove bad triangles from the simulation.main list
        triangles.removeAll(badTriangles)

        // Create new triangles from the polygon edges and the new point
        for (edge in polygon) {
            triangles.add(Triangle(edge.p1, edge.p2, p))
        }
    }

    // Final cleanup: remove any triangles that contain a vertex from the super-triangle
    return DelauneyTriangulation(triangles.filterNot { t ->
        t.a == p1 || t.a == p2 || t.a == p3 ||
                t.b == p1 || t.b == p2 || t.b == p3 ||
                t.c == p1 || t.c == p2 || t.c == p3
    }, Triangle(p1, p2, p3))
}
