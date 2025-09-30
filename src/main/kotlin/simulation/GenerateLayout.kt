package simulation

import io.github.oshai.kotlinlogging.KotlinLogging
import simulation.model.Ladder
import simulation.model.Layout
import simulation.model.Point
import simulation.model.QuadrilateralSubdivision
import simulation.services.LadderDetection.Companion.detectLadders
import simulation.services.Palette.Companion.blueSerenity
import simulation.services.Palette.Companion.pastelRainbow
import simulation.services.Palette.Companion.softRainbow
import simulation.services.Palette.Companion.springGreenHarmony
import simulation.services.createBaseTriangulation
import simulation.services.mergeTrianglesToQuadrilaterals
import simulation.services.outputToPng
import java.awt.Color

private val logger = KotlinLogging.logger {}

const val NUMBER_OF_LAYOUT = 1

// calculate and apply further subdivisions
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

private fun preDivisionInfoLabels(layout: Layout): Map<Point, String> {
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

private fun postDivisionInfoLabels(subdivisions: List<QuadrilateralSubdivision>): Map<Point, String> {
    return subdivisions.associate { subdivision ->
        val quadrilateral = subdivision.quadrilateral
        val (shortDiv, longDiv) = subdivision.divisionFactors()
        val angles = quadrilateral.interiorAngles()
        val minAngle = angles.min()
        val maxAngle = angles.max()

        val lines = listOf(
            "${shortDiv}x${longDiv}",
            "${minAngle.toInt()}°-${maxAngle.toInt()}° (Δ${(maxAngle - minAngle).toInt()})°",
            "irreg: %.2f".format(quadrilateral.irregularityIndex())
        )

        quadrilateral.findCentroid() to lines.joinToString("\n")
    }
}

private fun allAnglesInfoLabels(layout: Layout): Map<Point, String> {
    return layout.quadrilaterals().associate { q ->
        val angles = q.interiorAngles()
        val lines = listOf(
            "${angles[0].toInt()}°, ${angles[1].toInt()}°",
            "${angles[2].toInt()}°, ${angles[3].toInt()}°"
        )

        q.findCentroid() to lines.joinToString("\n")
    }
}

private fun logLayout(layout: Layout, name: String = "layout"): String {
    return "[$name] #polygons: ${layout.polygons.size}, " +
            "#triangles: ${layout.triangles().size}, " +
            "#quadrilaterals: ${layout.quadrilaterals().size}"
}

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
        val triangles = createBaseTriangulation(numberOfPoints = 48, requiredMinAngle = 16)
        val polygons = mergeTrianglesToQuadrilaterals(triangles)
        val layout1 = Layout(polygons)
        // TODO: we can also merge triangle to quadri 4 by 4 by find points shared between 4 triangles

        // init triangles
        outputToPng(
            layout = Layout(triangles),
            fileName = "${fileNamePrefix}_phase0_triangles",
            mainEdgeColor = Color.YELLOW
        )

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
            fileName = "${fileNamePrefix}_phase1_ladders",
            clusterEdgeStroke = 12f,
        )

        // render ladder cross lines
        outputToPng(
            layout = layout1,
            clustersOfEdges = ladderCrossLines.map { it.edges },
            fileName = "${fileNamePrefix}_phase1_ladders_crosslines",
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
            fileName = "${fileNamePrefix}_phase2_metrics",
            labelsAt = preDivisionInfoLabels(layout2),
            labelFontSize = 20f
        )

        // calculate and apply further subdivisions
        val layout3 = applyMultiPhasesSubdivisions(fileNamePrefix, layout2)

        outputToPng(
            layout = layout3,
            fileName = "${fileNamePrefix}_phase2_subdivisions_applied",
            mainEdgeColor = Color.GRAY
        )

        palettes.forEachIndexed { i, palette ->
            val fileName = "${fileNamePrefix}_phase2_subdivisions_applied_filled_${String.format("%02d", i + 1)}"
            outputToPng(
                layout = layout3,
                fileName = fileName,
                fillPolygons = true,
                polygonFillingPalette = palette
            )
        }

        logger.info { logLayout(layout1, "layout1") }
        logger.info { logLayout(layout2, "layout2") }
        logger.info { logLayout(layout3, "layout3") }
    }
}
