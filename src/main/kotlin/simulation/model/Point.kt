package simulation.model

import kotlinx.serialization.Serializable
import simulation.geometry.distanceBetween

@Serializable
data class Point(
    val x: Double,
    val y: Double
) {

    fun shift(dx: Double, dy: Double): Point =
        Point(x + dx, y + dy)

    fun distanceTo(other: Point): Double =
        distanceBetween(this, other)

}
