package simulation.services

import io.github.oshai.kotlinlogging.KotlinLogging
import simulation.model.Edge
import simulation.model.Layout
import simulation.model.Point
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_RGB
import java.io.File
import javax.imageio.ImageIO

private val logger = KotlinLogging.logger {}

private val palette = listOf("70D6FF", "FF70A6", "FF9770", "FFF670", "70FFEA", "D6FF70").map { Color.decode("#$it") }

private fun Graphics2D.drawEdge(edge: Edge) {
    this.drawLine(
        edge.p1.x.toInt(),
        edge.p1.y.toInt(),
        edge.p2.x.toInt(),
        edge.p2.y.toInt()
    )
}

private fun Graphics2D.drawPoint(point: Point, thickness: Int) {
    val thicknessOffset = thickness / 2
    fillOval(
        point.x.toInt() - thicknessOffset,
        point.y.toInt() - thicknessOffset,
        thickness,
        thickness
    )
}

fun outputToPng(
    layout: Layout,
    clustersOfEdges: Collection<Collection<Edge>> = emptySet(),
    clustersOfPoints: Collection<Collection<Point>> = emptySet(),
    pointThickness: Int = 24,
    labelsAt: Map<Point, String> = emptyMap(),
    subDirectory: String? = null,
    fileName: String = "layout"
) {
    val layoutEdges = layout.polygons.flatMap { it.edges }.distinct()
    val layoutPoints = layout.polygons.flatMap { it.points }.distinct()
    val minX = layoutPoints.minOf { it.x }
    val maxX = layoutPoints.maxOf { it.x }
    val minY = layoutPoints.minOf { it.y }
    val maxY = layoutPoints.maxOf { it.y }

    // Calculate dynamic image dimensions with padding
    val padding = 50.0
    val width = (maxX - minX + 2 * padding).toInt()
    val height = (maxY - minY + 2 * padding).toInt()

    // Calculate offsets to center content
    val offsetX = padding - minX
    val offsetY = padding - minY

    logger.info { "offset: ${offsetX}x${offsetY}" }

    // image
    val image = BufferedImage(width, height, TYPE_INT_RGB)
    val graphics = image.createGraphics()

    // background color
    graphics.color = Color.BLACK
    graphics.fillRect(0, 0, width, height)

    // draw layout edges
    graphics.color = Color.DARK_GRAY
    graphics.stroke = BasicStroke(8f)
    layoutEdges
        .map { edge -> edge.shift(offsetX, offsetY) }
        .forEach { graphics.drawEdge(it) }

    // draw edges
    clustersOfEdges.forEachIndexed { index, cluster ->
        val color = palette[index % palette.size]
        graphics.color = color
        graphics.stroke = BasicStroke(12f)
        cluster
            .map { edge -> edge.shift(offsetX, offsetY) }
            .forEach { graphics.drawEdge(it) }
    }

    // draw points
    clustersOfPoints.forEachIndexed { index, points ->
        val color = palette[index % palette.size]
        graphics.color = color
        graphics.stroke = BasicStroke(12f)
        points
            .map { point -> point.shift(offsetX, offsetY) }
            .forEach { graphics.drawPoint(it, pointThickness) }
    }

    // draw text
    graphics.color = Color.WHITE
    graphics.font = graphics.font.deriveFont(40f)
    labelsAt.forEach { (point, text) ->
        val shiftedPoint = point.shift(offsetX, offsetY)
        graphics.drawString(text, shiftedPoint.x.toInt(), shiftedPoint.y.toInt())
    }

    graphics.dispose()

    // directories
    val outputDir = File("output")
    if (!outputDir.exists()) {
        outputDir.mkdirs()
    }

    val outputFile = if ((subDirectory) != null) {
        val subDir = File(outputDir, subDirectory)
        if (!subDir.exists()) {
            subDir.mkdirs()
        }
        File(subDir, "$fileName.png")
    } else {
        File(outputDir, "$fileName.png")
    }

    // write
    ImageIO.write(image, "PNG", outputFile)
}
