package simulation.model

import kotlinx.serialization.Serializable
import simulation.utils.distanceBetween
import kotlin.math.abs

@Serializable
data class Point(
    val x: Double,
    val y: Double
) {

    fun shift(offset: Point): Point =
        Point(x + offset.x, y + offset.y)

    fun shift(dx: Double, dy: Double): Point =
        Point(x + dx, y + dy)

    fun distanceTo(other: Point): Double =
        distanceBetween(this, other)

    fun equalsWithTolerance(other: Point, tolerance: Double): Boolean =
        abs(x - other.x) <= tolerance && abs(y - other.y) <= tolerance

}
