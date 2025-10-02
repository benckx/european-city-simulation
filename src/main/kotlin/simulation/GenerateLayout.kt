package simulation

import io.github.oshai.kotlinlogging.KotlinLogging
import simulation.model.Layout
import simulation.services.LadderDetection.Companion.detectLadders
import simulation.services.Palette.Companion.blueSerenity
import simulation.services.Palette.Companion.pastelRainbow
import simulation.services.Palette.Companion.softRainbow
import simulation.services.Palette.Companion.springGreenHarmony
import simulation.services.createBaseTriangulation
import simulation.services.mergeTrianglesToQuadrilaterals
import simulation.services.outputToPng
import simulation.services.shutdownRenderingService
import java.awt.Color

private val logger = KotlinLogging.logger {}

private const val NUMBER_OF_LAYOUT = 1

private fun applyMultiPhasesSubdivisions(fileNamePrefix: String, layout: Layout): Layout {
    var newLayout = layout
    var subdivisions = layout.calculateQuadrilateralSubdivisions()
    var iteration = 1

    while (subdivisions.isNotEmpty()) {
        outputToPng(
            layout = newLayout,
            fileName = "${fileNamePrefix}_phase2_subdivisions_iter${iteration}",
            clustersOfEdges = subdivisions.map { it.bothSidesEdges() },
            labelsAt = postDivisionInfoLabels(subdivisions),
            labelFontSize = 14f,
        )
        newLayout = newLayout.splitQuadrilaterals(subdivisions)
        subdivisions = newLayout.calculateQuadrilateralSubdivisions()
        iteration++
    }
    return newLayout
}

fun main() {
    logger.info { "generating layout" }
    val palettes = listOf(pastelRainbow, softRainbow, blueSerenity, springGreenHarmony)

    (1..NUMBER_OF_LAYOUT).forEach { i ->
        val fileNamePrefix = "layout_${String.format("%04d", i)}"
        val crossLineRatio = .33
        val triangulationHistory = createBaseTriangulation(54, 20)
        val layout1 = mergeTrianglesToQuadrilaterals(triangulationHistory.last())

        triangulationHistory.forEachIndexed { i, layout ->
            outputToPng(
                layout = layout,
                fileName = "${fileNamePrefix}_phase1_triangulation_step_${String.format("%02d", i + 1)}",
                mainEdgeColor = Color.YELLOW
            )
        }

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
            fileName = "${fileNamePrefix}_phase2_ladders",
            clusterEdgeStroke = 12f,
        )

        // render ladder cross lines
        outputToPng(
            layout = layout1,
            clustersOfEdges = ladderCrossLines.map { it.edges },
            fileName = "${fileNamePrefix}_phase2_ladders_crosslines",
        )

        // actually split along ladder cross lines
        val layout2 = layout1.splitQuadrilateralsAlongEdges(ladderCrossLinesEdges)

        outputToPng(
            layout = layout2,
            fileName = "${fileNamePrefix}_phase2_angle_metrics",
            labelsAt = allAnglesInfoLabels(layout2),
            labelFontSize = 14f
        )

        outputToPng(
            layout = layout2,
            fileName = "${fileNamePrefix}_phase2_angle_metrics_flagged",
            clustersOfEdges = layout2.quadrilaterals().filter { it.isTrapezoidal() }.map { it.edges },
            clusterEdgeStroke = 20f
        )

        outputToPng(
            layout = layout2,
            fileName = "${fileNamePrefix}_phase3_metrics",
            labelsAt = preDivisionInfoLabels(layout2),
            labelFontSize = 20f
        )

        // calculate and apply further subdivisions
        val layout3 = applyMultiPhasesSubdivisions(fileNamePrefix, layout2)

        outputToPng(
            layout = layout3,
            fileName = "${fileNamePrefix}_phase3_subdivisions_applied",
            mainEdgeColor = Color.GRAY
        )

        palettes.forEachIndexed { i, palette ->
            outputToPng(
                layout = layout3,
                fileName = "${fileNamePrefix}_phase3_subdivisions_applied_filled_${String.format("%02d", i + 1)}",
                fillPolygons = true,
                polygonFillingPalette = palette
            )
        }

        logger.info { logLayout(triangulationHistory.last(), "baseLayout") }
        logger.info { logLayout(layout1, "layout1") }
        logger.info { logLayout(layout2, "layout2") }
        logger.info { logLayout(layout3, "layout3") }

        shutdownRenderingService()
    }
}
