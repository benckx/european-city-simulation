package simulation

import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

fun main() {
    listFiles().forEach { fileName ->
        val layout = readLayoutFromJson(fileName)
        logger.info { "found layout $fileName with ${layout.polygons.size} polygons" }
        outputToPng(layout, fileName)
    }
}
