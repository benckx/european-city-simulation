package simulation

import io.github.oshai.kotlinlogging.KotlinLogging
import simulation.model.Ladder
import simulation.model.Layout
import simulation.model.Point
import simulation.services.LadderDetection.Companion.detectLadders
import simulation.services.Palette.Companion.blueSerenity
import simulation.services.createBaseTriangulation
import simulation.services.mergeTrianglesToQuadrilaterals
import simulation.services.outputToPng

private val logger = KotlinLogging.logger {}

const val NUMBER_OF_LAYOUT = 1

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

private fun infoLabels(layout: Layout): Map<Point, String> {
    return layout.quadrilaterals().associate { q ->
        val lengths = q.edges.map { it.length }

        val lines = listOf(
            "elong: %.2f".format(q.elongationIndex()),
            "irreg: %.2f".format(q.irregularityIndex()),
            "${(lengths.min()).toInt()} - ${lengths.max().toInt()}"
        )

        q.findCentroid() to lines.joinToString("\n")
    }
}

private fun logLayout(layout: Layout, name: String = "layout"): String {
    return "[$name] #polygons: ${layout.polygons.size}, " +
            "#triangles: ${layout.triangles().size}, " +
            "#quadrilaterals: ${layout.quadrilaterals().size}"
}

fun main() {
    logger.info { "generating layout" }
    (1..NUMBER_OF_LAYOUT).forEach { i ->
        val output = "layout_${String.format("%04d", i)}"
        val crossLineRatio = .33
        val triangles = createBaseTriangulation(numberOfPoints = 30, requiredMinAngle = 20)
        val polygons = mergeTrianglesToQuadrilaterals(triangles)
        val layout1 = Layout(polygons)

        val ladders = detectLadders(layout1)
        val ladderCrossLines = ladders.map { it.crossingLine(ratio = crossLineRatio) }
        val ladderCrossLinesEdges = ladderCrossLines.flatMap { it.edges }
        logger.info {
            "#ladders: ${ladders.size}, " +
                    "#ladderCrossLines: ${ladderCrossLines.size}, " +
                    "#ladderCrossLinesEdges: ${ladderCrossLinesEdges.size}"
        }

        // render ladder structures
        outputToPng(
            layout = layout1,
            clustersOfEdges = ladders.map { it.edges },
            labelsAt = ladderLabels(ladders),
            fileName = "${output}_phase1_ladders",
            clusterEdgeStroke = 12f,
        )

        // render ladder cross lines
        outputToPng(
            layout = layout1,
            clustersOfEdges = ladderCrossLines.map { it.edges },
            fileName = "${output}_phase1_ladders_crosslines",
        )

        // actually split along ladder cross lines
        val layout2 = layout1.splitQuadrilateralsAlongEdges(ladderCrossLinesEdges)

        // calculate and apply further subdivisions
        outputToPng(
            layout = layout2,
            fileName = "${output}_phase2_metrics",
            labelsAt = infoLabels(layout2),
            labelFontSize = 14f
        )

        val subdivisions = layout2.calculateQuadrilateralSubdivisions()
        val layout3 = layout2.splitQuadrilaterals(subdivisions)

        outputToPng(
            layout = layout2,
            fileName = "${output}_phase2_subdivisions",
            clustersOfEdges = subdivisions.map { it.shortSideEdges + it.longSideEdges }
        )

        outputToPng(
            layout = layout3,
            fileName = "${output}_phase2_subdivisions_applied",
            mainEdgeStroke = 18f,
            secondaryEdgeStroke = 4f,
        )

        outputToPng(
            layout = layout3,
            fileName = "${output}_phase2_subdivisions_applied_filled",
            fillPolygons = true,
            polygonFillingPalette = blueSerenity,
            mainEdgeStroke = 18f,
            secondaryEdgeStroke = 4f,
        )

        logger.info { logLayout(layout1) }
        logger.info { logLayout(layout2) }
        logger.info { logLayout(layout3) }
    }
}
