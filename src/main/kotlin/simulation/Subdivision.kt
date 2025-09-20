package simulation

import io.github.oshai.kotlinlogging.KotlinLogging
import simulation.model.Line
import simulation.services.detectLadders
import simulation.services.listFiles
import simulation.services.outputToPng
import simulation.services.readLayoutFromJson

private val logger = KotlinLogging.logger {}

private fun filterOutCrossings(lines: List<Line>): List<Line> {
    val results = mutableListOf<Line>()
    lines
        .sortedByDescending { line -> line.size }
        .forEach { line ->
            if (results.none { it.shareAnyPointWith(line) }) {
                results += line
            }
        }

    return results.toList()
}

fun main() {
    listFiles().forEach { fileName ->
        logger.info { "processing layout $fileName" }
        val ratio = .33
        val layout = readLayoutFromJson(fileName)
        val ladders = detectLadders(layout).sortedBy { it.size }
        val crossLines = mutableListOf<Line>()
        val points = ladders.flatMap { it.edges }.flatMap { edge -> edge.pointsAt(ratio) }.toSet()

        crossLines += ladders
            .map { ladder ->
                ladder.crossingLine(ratio)
            }

//        crossLines += ladders
//            .map { ladder -> ladder.reverse() }
//            .map { ladder ->
//                ladder.crossingLine(ratio)
//            }

        println("#ladders: ${ladders.size}")

        outputToPng(
            layout = layout,
            clustersOfEdges = crossLines.map { line -> line.edges },
            clustersOfPoints = setOf(points),
            subDirectory = "clustered",
            fileName = "${fileName}_clustered"
        )
    }
}
