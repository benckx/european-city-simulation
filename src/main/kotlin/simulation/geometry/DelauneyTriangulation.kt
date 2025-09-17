package simulation.geometry

import simulation.model.Triangle

data class DelauneyTriangulation(
    val triangles: List<Triangle>,
    val superTriangle: Triangle
)
