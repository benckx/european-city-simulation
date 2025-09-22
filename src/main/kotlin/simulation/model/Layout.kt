package simulation.model

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable

private val logger = KotlinLogging.logger {}

@Serializable
data class Layout(
    val polygons: List<Polygon>,
    val secondaryEdges: List<Edge> = emptyList()
) {

    init {
        val polygonsEdges = polygons.flatMap { it.edges }.toSet()
        val secondaryEdgesSet = secondaryEdges.toSet()
        require(secondaryEdgesSet.all { edge -> polygonsEdges.contains(edge) }) {
            "Secondary edges must be a subset of the main polygons' edges"
        }
    }

    fun triangles(): List<Polygon> = polygons.filter { it.isTriangle() }
    fun quadrilaterals(): List<Polygon> = polygons.filter { it.isQuadrilateral() }

    fun splitQuadrilateralsAlongEdges(splitEdges: Collection<Edge>): Layout {
        val newPolygons = polygons.toMutableList()
        val newSecondaryEdges = secondaryEdges.toMutableList()

        quadrilaterals().forEach { quadrilateral ->
            val polygonCrossingEdges = splitEdges
                .filter { edge -> quadrilateral.containsMidPointOf(edge) }

            when (polygonCrossingEdges.size) {
                0 -> {
                    // no crossing edge, do nothing
                }

                1 -> {
                    val crossingEdge = polygonCrossingEdges.first()
                    newPolygons += quadrilateral.splitQuadrilateralInTwo(crossingEdge)
                    newPolygons -= quadrilateral
                    newSecondaryEdges += crossingEdge
                }

                2 -> {
                    val crossingEdge1 = polygonCrossingEdges[0]
                    val crossingEdge2 = polygonCrossingEdges[1]
                    val splitResult = quadrilateral.splitQuadrilateralInFourAtIntersection(crossingEdge1, crossingEdge2)
                    newPolygons += splitResult.polygons
                    newPolygons -= quadrilateral
                    newSecondaryEdges += splitResult.secondaryEdges
                }

                else -> {
                    logger.warn { "split with ${polygonCrossingEdges.size} edges not supported" }
                }
            }
        }

        return Layout(newPolygons, newSecondaryEdges)
    }

}
