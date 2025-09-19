package simulation

import simulation.model.Edge
import simulation.model.Layout
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_RGB
import java.io.File
import javax.imageio.ImageIO

private fun Graphics2D.drawEdge(edge: Edge) {
    this.drawLine(
        edge.p1.x.toInt(),
        edge.p1.y.toInt(),
        edge.p2.x.toInt(),
        edge.p2.y.toInt()
    )
}

fun outputToPng(layout: Layout, fileName: String = "layout") {
    val edges = layout.polygons.flatMap { it.edges }.distinct()
    val points = layout.polygons.flatMap { it.points }.distinct()
    val minX = points.minOf { it.x }
    val maxX = points.maxOf { it.x }
    val minY = points.minOf { it.y }
    val maxY = points.maxOf { it.y }

    // Calculate dynamic image dimensions with padding
    val padding = 50.0
    val width = (maxX - minX + 2 * padding).toInt()
    val height = (maxY - minY + 2 * padding).toInt()

    // Calculate offsets to center content
    val offsetX = padding - minX
    val offsetY = padding - minY

    // image
    val image = BufferedImage(width, height, TYPE_INT_RGB)
    val graphics = image.createGraphics()

    // background color
    graphics.color = Color.BLACK
    graphics.fillRect(0, 0, width, height)

    // draw
    graphics.color = Color.YELLOW
    graphics.stroke = BasicStroke(8f)
    edges
        .map { edge -> edge.shift(offsetX, offsetY) }
        .forEach { graphics.drawEdge(it) }

    graphics.dispose()

    val outputDir = File("output")
    if (!outputDir.exists()) {
        outputDir.mkdirs()
    }

    // save
    ImageIO.write(image, "PNG", File("output/$fileName.png"))
}
