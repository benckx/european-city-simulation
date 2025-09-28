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

    fun calculateQuadrilateralSubdivisions(): List<QuadrilateralSubdivision> {
        return quadrilaterals()
            .mapNotNull { quadrilateral ->
                val factor = quadrilateral.calculateSubdivisionFactor()
                if (factor != null) {
                    val shortDiv = minOf(factor.first, factor.second)
                    val longDiv = maxOf(factor.first, factor.second)
                    quadrilateral.calculateSubdivision(shortDiv, longDiv)
                } else {
                    null
                }
            }
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
        val allSubQuadrilaterals = mutableSetOf<Quadrilateral>()

        for (subdivision in subdivisions) {
            require(polygons.contains(subdivision.quadrilateral)) {
                "The quadrilateral to split must be part of the current layout"
            }

            // apply the generalized M x N split
            val subQuadrilaterals = subdivision.subQuadrilaterals()

            // update the layout
            currentPolygons -= subdivision.quadrilateral
            currentPolygons += subQuadrilaterals

            // collect all sub-quadrilaterals for later processing
            // (so we don't have to run subQuadrilaterals twice for the same polygon)
            allSubQuadrilaterals += subQuadrilaterals

            // add internal subdivision edges as secondary edges
            // these are edges from the subdivided quadrilaterals that are not part of the original boundary
            val originalQuadrilateralEdges = subdivision.quadrilateral.edges.toSet()
            val subQuadrilateralsEdges = subQuadrilaterals.flatMap { it.edges }.toSet()
            val internalEdges = subQuadrilateralsEdges.filterNot { subEdge ->
                originalQuadrilateralEdges.any { originalEdge -> subEdge.isSubEdgeOf(originalEdge) }
            }
            newSecondaryEdges += internalEdges
            newSecondaryEdges -= originalQuadrilateralEdges
        }

        // add the sub-edges that are sub-edges of original secondary edges
        val originalSecondaryEdges = secondaryEdges.toSet()
        val allSubQuadrilateralsEdges = allSubQuadrilaterals.flatMap { it.edges }

        val originalSecondaryEdgesToReAdd = mutableSetOf<Edge>()
        originalSecondaryEdgesToReAdd += allSubQuadrilateralsEdges.filter { subEdge ->
            originalSecondaryEdges.any { originalSecondaryEdge ->
                originalSecondaryEdge == subEdge ||
                        subEdge.isSubEdgeOf(originalSecondaryEdge)
            }
        }

        // secondary edges that were split but belong to another polygon that was not impacted by the subdivisions
        // (so we'll have those edges and their sub-edges in the final layout)
        val unImpactedPolygonsEdges = polygons
            .filter { polygon -> subdivisions.none { subdivision -> subdivision.quadrilateral == polygon } }
            .flatMap { it.edges }

        originalSecondaryEdgesToReAdd += originalSecondaryEdges.filter { edge -> unImpactedPolygonsEdges.contains(edge) }

        // result
        return Layout(currentPolygons, (newSecondaryEdges + originalSecondaryEdgesToReAdd).toList())
    }
}
