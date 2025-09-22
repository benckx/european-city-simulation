package simulation.services

import io.github.oshai.kotlinlogging.KotlinLogging
import simulation.model.Edge
import simulation.model.Layout
import simulation.model.Point
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_RGB
import java.io.File
import javax.imageio.ImageIO

private val logger = KotlinLogging.logger {}

// similar to "pastel rainbow"
private val palette1 =
    listOf(
        "70D6FF",
        "FF70A6",
        "FF9770",
        "FFF670",
        "70FFEA",
        "D6FF70",
        "E9FF70"
    ).map { Color.decode("#$it") }

// earthy tones
private val palette2 =
    listOf(
        "797D62",
        "9B9B7A",
        "BAA587",
        "D9AE94",
        "F1DCA7",
        "FFCB69",
        "E8AC65",
        "D08C60",
        "B58463",
        "997B66"
    ).map { Color.decode("#$it") }

fun outputToPng(
    layout: Layout,
    fillPolygons: Boolean = false,
    clustersOfEdges: Collection<Collection<Edge>> = emptySet(),
    clustersOfPoints: Collection<Collection<Point>> = emptySet(),
    clusterDifferentiationByColor: Boolean = true,
    mainEdgeColor: Color = Color.LIGHT_GRAY,
    secondaryEdgeColor: Color = Color.DARK_GRAY,
    clusterEdgeColor: Color = Color.DARK_GRAY,
    mainEdgeStroke: Float = 12f,
    secondaryEdgeStroke: Float = 6f,
    clusterEdgeStroke: Float = 6f,
    pointThickness: Int = 24,
    labelsAt: Map<Point, String> = emptyMap(),
    subDirectory: String? = null,
    fileName: String = "layout"
) {
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

    logger.debug { "offset: ${offsetX}x${offsetY}" }

    // image
    val image = BufferedImage(width, height, TYPE_INT_RGB)
    val graphics = image.createGraphics()

    // anti-aliasing
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

    // background color
    graphics.color = Color.BLACK
    graphics.fillRect(0, 0, width, height)

    // fill polygons
    if (fillPolygons) {
        layout.polygons.forEachIndexed { index, polygon ->
            graphics.color = palette2[index % palette2.size]
            val orderedPoints = polygon.orderedPoints().map { point -> point.shift(offsetX, offsetY) }
            val xPoints = orderedPoints.map { it.x.toInt() }.toIntArray()
            val yPoints = orderedPoints.map { it.y.toInt() }.toIntArray()
            graphics.fillPolygon(xPoints, yPoints, orderedPoints.size)
        }
    }

    // draw clusters
    if (clusterDifferentiationByColor) {
        // draw points
        clustersOfPoints.forEachIndexed { index, points ->
            val color = palette1[index % palette1.size]
            graphics.color = color
            points
                .map { point -> point.shift(offsetX, offsetY) }
                .forEach { graphics.drawPoint(it, pointThickness) }
        }

        // draw edges
        graphics.stroke = BasicStroke(clusterEdgeStroke)
        clustersOfEdges.forEachIndexed { index, edges ->
            val color = palette1[index % palette1.size]
            graphics.color = color
            edges
                .map { edge -> edge.shift(offsetX, offsetY) }
                .forEach { graphics.drawEdge(it) }
        }
    } else {
        graphics.color = clusterEdgeColor

        // draw points
        clustersOfPoints.forEach { points ->
            points
                .map { point -> point.shift(offsetX, offsetY) }
                .forEach { graphics.drawPoint(it, pointThickness) }
        }

        // draw edges
        graphics.stroke = BasicStroke(clusterEdgeStroke)
        clustersOfEdges.forEach { edges ->
            edges
                .map { edge -> edge.shift(offsetX, offsetY) }
                .forEach { graphics.drawEdge(it) }
        }
    }

    // draw secondary edges
    graphics.color = secondaryEdgeColor
    graphics.stroke = BasicStroke(secondaryEdgeStroke)
    layout.secondaryEdges
        .map { edge -> edge.shift(offsetX, offsetY) }
        .forEach { edge -> graphics.drawEdge(edge) }

    // draw main polygon edges
    val excludedFromMainPolygonEdges = (clustersOfEdges.flatten().distinct() + layout.secondaryEdges).toSet()
    graphics.color = mainEdgeColor
//    graphics.stroke = BasicStroke(mainEdgeStroke)
    graphics.stroke = BasicStroke(mainEdgeStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
    layout.polygons.flatMap { it.edges }.distinct()
        .filterNot { mainEdge -> excludedFromMainPolygonEdges.contains(mainEdge) }
        .map { edge -> edge.shift(offsetX, offsetY) }
        .forEach { edge -> graphics.drawEdge(edge) }

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
