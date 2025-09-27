package simulation

import io.github.oshai.kotlinlogging.KotlinLogging
import simulation.model.Ladder
import simulation.model.Layout
import simulation.model.Point
import simulation.model.Quadrilateral
import simulation.services.LadderDetection.Companion.detectLadders
import simulation.services.createBaseTriangulation
import simulation.services.mergeTrianglesToQuadrilaterals
import simulation.services.outputToPng
import kotlin.math.ceil

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
//            "area: %.2f".format(q.area() / 1_000),
            "${(lengths.min()).toInt()} - ${lengths.max().toInt()}"
        )

        q.findCentroid() to lines.joinToString("\n")
    }
}

private fun subDivisionLabels(layout: Layout): Map<Point, String> {
    return layout
        .quadrilaterals()
        .mapNotNull { quadrilateral ->
            quadrilateralSubdivision(quadrilateral)?.let { (shortDiv, longDiv) ->
                quadrilateral.findCentroid() to "${shortDiv}x${longDiv}"
            }
        }
        .toMap()
}

private fun quadrilateralSubdivision(q: Quadrilateral): Pair<Int, Int>? {
    if (q.irregularityIndex() <= .6) {
        val maxEdgeLength = 110
        val pairs = q.oppositeEdgesTuples().toList()
        val shortEdges = pairs.minBy { it.avgLength() }
        val longEdges = pairs.maxBy { it.avgLength() }
        val shortLength = shortEdges.minLength()
        val longLength = longEdges.minLength()
        val shortDiv = ceil(shortLength / maxEdgeLength).toInt()
        val longDiv = ceil(longLength / maxEdgeLength).toInt()
        logger.debug {
            val shortEdge = String.format("%.1f", shortLength)
            val longEdge = String.format("%.1f", longLength)
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

        val subdivisions = splitLayout
            .quadrilaterals()
            .mapNotNull { quadrilateral ->
                val divisor = quadrilateralSubdivision(quadrilateral)
                if (divisor != null) {
                    val shortDiv = minOf(divisor.first, divisor.second)
                    val longDiv = maxOf(divisor.first, divisor.second)
                    quadrilateral.calculateSubdivision(shortDiv, longDiv)
                } else {
                    null
                }
            }

        outputToPng(
            layout = splitLayout,
            fileName = "${output}_split_subdivisions",
            labelsAt = subDivisionLabels(splitLayout),
            fontSize = 18f,
            clustersOfEdges = subdivisions.map { it.shortSideEdges + it.longSideEdges },
            clustersOfPoints = setOf(
                setOf(Point(0.0, 0.0))
            )
        )

        outputToPng(
            layout = splitLayout.splitQuadrilaterals(subdivisions),
            fileName = "${output}_split_subdivisions_effective",
            fillPolygons = true,
            mainEdgeStroke = 18f,
            secondaryEdgeStroke = 4f,
        )

        val allEdgeLengths = splitLayout.polygons.flatMap { it.edges }.map { it.length }
        logger.info {
            "[length] min: ${"%.2f".format(allEdgeLengths.min())}, " +
                    "max: ${"%.2f".format(allEdgeLengths.max())}, " +
                    "avg: ${"%.2f".format(allEdgeLengths.average())}"
        }
    }
}
