package simulation

import io.github.oshai.kotlinlogging.KotlinLogging
import simulation.model.Edge
import simulation.model.Ladder
import simulation.model.Layout
import simulation.model.Point
import simulation.services.createBaseTriangulation
import simulation.services.detectLadders
import simulation.services.mergeTrianglesToQuadrilaterals
import simulation.services.outputToPng
import java.awt.Color

private val logger = KotlinLogging.logger {}

private fun ladderLabels(ladders: List<Ladder>): Map<Point, String> {
    return ladders.flatMapIndexed { ladderIndex, ladder ->
        ladder.edges.mapIndexed { edgeIndex, edge ->
            val point = edge.pointsAt(.5).first()
            val ladderLetter = ('a' + ladderIndex).toString()
            val label = "[$ladderLetter] ${edgeIndex + 1}"
            point to label
        }
    }.toMap()
}

private fun numberOfCrossingsLabels(layout: Layout, crossLinesEdges: List<Edge>): Map<Point, String> {
    return layout.quadrilaterals().mapNotNull { quadrilateral ->
        val count = crossLinesEdges.count { edge -> quadrilateral.containsMidPointOf(edge) }
        if (count > 0) {
            val centroid = quadrilateral.findCentroid()
            centroid to count.toString()
        } else {
            null
        }
    }.toMap()
}

private fun logLayout(layout: Layout): String {
    return "[layout] #polygons: ${layout.polygons.size}, " +
            "#triangles: ${layout.triangles().size}, " +
            "#quadrilaterals: ${layout.quadrilaterals().size}"
}

fun main() {
    logger.info { "generating layout" }
    val output = "layout"
    val ratio = .33
    val triangles = createBaseTriangulation(numberOfPoints = 30, requiredMinAngle = 20)
    val polygons = mergeTrianglesToQuadrilaterals(triangles, 2)
    val layout = Layout(polygons)
    logger.info { logLayout(layout) }

    val ladders = detectLadders(layout)
    val crossLines = ladders.map { it.crossingLine(ratio = ratio) }
    val crossLinesEdges = crossLines.flatMap { it.edges }
    logger.info { "#ladders: ${ladders.size}, #crosslines: ${crossLines.size}, #crossLinesEdges: ${crossLinesEdges.size}" }

    // render ladder structures
    outputToPng(
        layout = layout,
        clustersOfEdges = ladders.map { it.edges },
        labelsAt = ladderLabels(ladders),
        fileName = "${output}_ladders",
        clusterEdgeStroke = 12f,
    )

    // render splitting edges blueprint info
    outputToPng(
        layout = layout,
        clustersOfEdges = crossLines.map { it.edges },
//        labelsAt = numberOfCrossingsLabels(layout, crossLinesEdges),
        fileName = "${output}_split_blueprint"
    )

    // actually split
    val splitLayout = layout.splitQuadrilateralsAlongEdges(crossLinesEdges)
    logger.info { logLayout(splitLayout) }

    outputToPng(
        layout = splitLayout,
        fileName = "${output}_split_result",
        fillPolygons = true,
        mainEdgeColor = Color.DARK_GRAY,
        secondaryEdgeColor = Color.DARK_GRAY,
    )
}
