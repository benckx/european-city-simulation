package simulation

import io.github.oshai.kotlinlogging.KotlinLogging
import simulation.model.Ladder
import simulation.model.Layout
import simulation.model.Point
import simulation.model.Polygon
import simulation.services.LadderDetection.Companion.detectLadders
import simulation.services.createBaseTriangulation
import simulation.services.mergeTrianglesToQuadrilaterals
import simulation.services.outputToPng
import kotlin.math.floor

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
            "elong: %.2f".format(q.quadrilateralElongationIndex()),
            "irreg: %.2f".format(q.quadrilateralIrregularityIndex()),
//            "area: %.2f".format(q.area() / 1_000),
            "${(lengths.min()).toInt()} - ${lengths.max().toInt()}"
        )

        q.findCentroid() to lines.joinToString("\n")
    }
}

private fun subDivisionLabels(layout: Layout): Map<Point, String> {
    return layout.quadrilaterals().mapNotNull { quadrilateral ->
        quadrilateralSubdivision(quadrilateral)?.let { (shortDiv, longDiv) ->
            quadrilateral.findCentroid() to "${shortDiv}x${longDiv}"
        }
    }.toMap()
}

private fun quadrilateralSubdivision(q: Polygon): Pair<Int, Int>? {
    if (q.isQuadrilateral() && q.quadrilateralIrregularityIndex() < 1) {
        val maxEdgeLength = 100
        val pairs = q.oppositeEdgesTuples().toList()
        val shortEdges = pairs.minBy { it.avgLength() }
        val longEdges = pairs.maxBy { it.avgLength() }
        val shortDiv = floor(shortEdges.minLength() / maxEdgeLength).toInt()
        val longDiv = floor(longEdges.minLength() / maxEdgeLength).toInt()
        logger.debug {
            val shortEdge = String.format("%.1f", shortEdges.minLength())
            val longEdge = String.format("%.1f", longEdges.minLength())
            "[subdivision] ${shortEdge}x${longEdge}, ${shortDiv}x${longDiv}"
        }
        if (shortDiv >= 1 && longDiv >= 1) {
            return shortDiv to longDiv
        }
    }

    return null
}

private fun logLayout(layout: Layout): String {
    return "[layout] #polygons: ${layout.polygons.size}, " +
            "#triangles: ${layout.triangles().size}, " +
            "#quadrilaterals: ${layout.quadrilaterals().size}"
}

fun main() {
    logger.info { "generating layout" }
    (1..NUMBER_OF_LAYOUT).forEach { i ->
        val output = "layout_${String.format("%04d", i)}"
        val ratio = .33
        val triangles = createBaseTriangulation(numberOfPoints = 30, requiredMinAngle = 20)
        val polygons = mergeTrianglesToQuadrilaterals(triangles)
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
            fileName = "${output}_split_blueprint"
        )

        // actually split
        val splitLayout = layout.splitQuadrilateralsAlongEdges(crossLinesEdges)
        logger.info { logLayout(splitLayout) }

        outputToPng(
            layout = splitLayout,
            fileName = "${output}_split_metrics",
            labelsAt = infoLabels(splitLayout),
            fontSize = 14f,
            clustersOfPoints = setOf(
                setOf(Point(0.0, 0.0)),
                splitLayout.quadrilaterals().map { it.findCentroid() }
            )
        )

        outputToPng(
            layout = splitLayout,
            fileName = "${output}_split_subdivisions",
            labelsAt = subDivisionLabels(splitLayout),
            fontSize = 18f,
            clustersOfPoints = setOf(
                setOf(Point(0.0, 0.0))
            )
        )

        val allEdgeLengths = splitLayout.polygons.flatMap { it.edges }.map { it.length }
        logger.info {
            "[length] min: ${"%.2f".format(allEdgeLengths.min())}, " +
                    "max: ${"%.2f".format(allEdgeLengths.max())}, " +
                    "avg: ${"%.2f".format(allEdgeLengths.average())}"
        }
    }
}
