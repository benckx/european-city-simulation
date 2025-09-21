package simulation

import io.github.oshai.kotlinlogging.KotlinLogging
import simulation.model.Layout
import simulation.services.*

private val logger = KotlinLogging.logger {}

fun main() {
    logger.info { "generating layout" }
    logger.info { "size: ${WIDTH}x${HEIGHT}" }
    logger.info { "#points: $DEFAULT_NUMBER_OF_POINTS" }
    logger.info { "min between points: $MIN_DISTANCE_BETWEEN_POINTS" }
    logger.info { "max between points: $MAX_DISTANCE_BETWEEN_POINTS" }

    val ratio = .33
    val triangles = createBaseTriangulation(numberOfPoints = 34, requiredMinAngle = 20)
    val polygons = mergeTrianglesToQuadrilaterals(triangles, 2)
    logger.info {
        "polygons: ${polygons.size}, " +
                "#triangles = ${polygons.count { it.isTriangle() }}, " +
                "#quadrilaterals = ${polygons.count { it.isQuadrilateral() }}"
    }

    val layout = Layout(polygons)
    val ladders = detectLadders(layout)
    logger.info { "#ladders: ${ladders.size}" }
    val crossLines = ladders.map { it.crossingLine(ratio = ratio) }

    outputToPng(
        layout = layout,
        clustersOfEdges = crossLines.map { line -> line.edges },
        fileName = "layout_crossings",
        clusterDifferentiationByColor = false
    )
}
