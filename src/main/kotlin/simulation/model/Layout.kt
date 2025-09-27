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

    fun triangles(): List<Polygon> {
        return polygons
            .filter { polygon -> polygon.isTriangle() }
            .map { polygon -> Triangle(polygon.points) }
    }

    fun quadrilaterals(): List<Quadrilateral> {
        return polygons
            .filter { polygon -> polygon.isQuadrilateral() }
            .map { polygon -> Quadrilateral(polygon.points) }
    }

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
                    newPolygons += quadrilateral.split1x2(crossingEdge)
                    newPolygons -= quadrilateral
                    newSecondaryEdges += crossingEdge
                }

                2 -> {
                    val crossingEdge1 = polygonCrossingEdges[0]
                    val crossingEdge2 = polygonCrossingEdges[1]
                    val splitResult = quadrilateral.split2x2AtIntersection(crossingEdge1, crossingEdge2)
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

    /**
     * Splits multiple quadrilaterals in the layout using a list of
     * QuadrilateralSubdivision objects, one for each targeted split.
     */
    fun splitQuadrilaterals(subdivisions: List<QuadrilateralSubdivision>): Layout {
        val currentPolygons = polygons.toMutableList()
        val newSecondaryEdges = secondaryEdges.toMutableSet()

        for (subdivision in subdivisions) {
            val quadrilateralToSplit = subdivision.quadrilateral

            require(polygons.contains(quadrilateralToSplit)) {
                "The quadrilateral to split must be part of the current layout"
            }

            // apply the generalized M x N split
            val splitResultLayout = quadrilateralToSplit.split(subdivision)

            // update the layout
            currentPolygons -= quadrilateralToSplit
            currentPolygons += splitResultLayout.polygons
            newSecondaryEdges += splitResultLayout.secondaryEdges
        }

        return Layout(currentPolygons, newSecondaryEdges.toList())
    }
}
