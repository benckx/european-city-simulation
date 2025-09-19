package simulation.model

import kotlinx.serialization.Serializable

@Serializable
data class Layout(
    val polygons: List<Polygon>
)
