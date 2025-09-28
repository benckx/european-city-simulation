package simulation.services

import io.github.oshai.kotlinlogging.KotlinLogging
import simulation.model.Edge
import simulation.model.Layout
import simulation.model.Point
import simulation.services.Palette.Companion.earthyTones
import simulation.services.Palette.Companion.pastelRainbow
import java.awt.BasicStroke
import java.awt.BasicStroke.CAP_ROUND
import java.awt.BasicStroke.JOIN_ROUND
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints.*
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_RGB
import java.io.File
import javax.imageio.ImageIO

private val logger = KotlinLogging.logger {}

private const val LABEL_PADDING_WIDTH = 4
private const val LABEL_PADDING_HEIGHT = 2

fun outputToPng(
    layout: Layout,
    fillPolygons: Boolean = false,
    clustersOfEdges: Collection<Collection<Edge>> = emptySet(),
    clustersOfPoints: Collection<Collection<Point>> = emptySet(),
    clusterDifferentiationByColor: Boolean = true,
    mainEdgeColor: Color = Color.DARK_GRAY,
    secondaryEdgeColor: Color = Color.DARK_GRAY,
    clusterEdgeColor: Color = Color.DARK_GRAY,
    mainEdgeStroke: Float = 12f,
    secondaryEdgeStroke: Float = 6f,
    clusterEdgeStroke: Float = 6f,
    pointThickness: Int = 24,
    clusterPalette: Palette = pastelRainbow,
    polygonFillingPalette: Palette = earthyTones,
    labelsAt: Map<Point, String> = emptyMap(),
    labelFontSize: Float = 40f,
    subDirectory: String? = null,
    fileName: String = "layout"
) {
    val layoutPoints = layout.polygons.flatMap { it.points }.distinct()
    val minX = layoutPoints.minOf { it.x }
    val maxX = layoutPoints.maxOf { it.x }
    val minY = layoutPoints.minOf { it.y }
    val maxY = layoutPoints.maxOf { it.y }

    // calculate dynamic image dimensions with padding
    val padding = 80.0
    val width = (maxX - minX + 2 * padding).toInt()
    val height = (maxY - minY + 2 * padding).toInt()

    // calculate offsets to center content
    val offsetX = padding - minX
    val offsetY = padding - minY
    val offset = Point(offsetX, offsetY)

    logger.debug { "offset: ${offsetX}x${offsetY}" }

    // image
    val image = BufferedImage(width, height, TYPE_INT_RGB)
    val graphics = image.createGraphics()

    // anti-aliasing
    graphics.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON)
    graphics.setRenderingHint(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_ON)

    // background color
    graphics.color = Color.BLACK
    graphics.fillRect(0, 0, width, height)

    // fill polygons
    if (fillPolygons) {
        layout.polygons.forEachIndexed { index, polygon ->
            graphics.color = polygonFillingPalette.colorByIndex(index)
            val orderedPoints = polygon.orderedPoints().map { point -> point.shift(offset) }
            val xPoints = orderedPoints.map { it.x.toInt() }.toIntArray()
            val yPoints = orderedPoints.map { it.y.toInt() }.toIntArray()
            graphics.fillPolygon(xPoints, yPoints, orderedPoints.size)
        }
    }

    // draw clusters
    if (clusterDifferentiationByColor) {
        // draw points
        clustersOfPoints.forEachIndexed { index, points ->
            val color = clusterPalette.colorByIndex(index)
            graphics.color = color
            points
                .map { point -> point.shift(offset) }
                .forEach { graphics.drawPoint(it, pointThickness) }
        }

        // draw edges
        graphics.stroke = BasicStroke(clusterEdgeStroke)
        clustersOfEdges.forEachIndexed { index, edges ->
            val color = clusterPalette.colorByIndex(index)
            graphics.color = color
            edges
                .map { edge -> edge.shift(offset) }
                .forEach { graphics.drawEdge(it) }
        }
    } else {
        graphics.color = clusterEdgeColor

        // draw points
        clustersOfPoints.forEach { points ->
            points
                .map { point -> point.shift(offset) }
                .forEach { graphics.drawPoint(it, pointThickness) }
        }

        // draw edges
        graphics.stroke = BasicStroke(clusterEdgeStroke)
        clustersOfEdges.forEach { edges ->
            edges
                .map { edge -> edge.shift(offset) }
                .forEach { graphics.drawEdge(it) }
        }
    }

    // draw secondary edges
    if (secondaryEdgeStroke > 0f) {
        graphics.color = secondaryEdgeColor
        graphics.stroke = BasicStroke(secondaryEdgeStroke)
        layout.secondaryEdges
            .map { edge -> edge.shift(offset) }
            .forEach { edge -> graphics.drawEdge(edge) }
    }

    // draw main polygon edges
    if (mainEdgeStroke > 0f) {
        val excludedFromMainPolygonEdges = (clustersOfEdges.flatten().distinct() + layout.secondaryEdges).toSet()
        graphics.color = mainEdgeColor
        graphics.stroke = BasicStroke(mainEdgeStroke, CAP_ROUND, JOIN_ROUND)
        layout.polygons.flatMap { it.edges }.distinct()
            .filterNot { mainEdge -> excludedFromMainPolygonEdges.contains(mainEdge) }
            .map { edge -> edge.shift(offsetX, offsetY) }
            .forEach { edge -> graphics.drawEdge(edge) }
    }

    // draw text
    graphics.font = graphics.font.deriveFont(labelFontSize)
    labelsAt.forEach { (point, label) -> graphics.drawLabel(point, label, offset) }

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

private fun Graphics2D.drawLabel(point: Point, label: String, offset: Point) {
    val fontMetrics = fontMetrics
    val lines = label.split("\n")

    val shiftedPoint = point.shift(offset)
    val maxTextWidth = lines.maxOf { fontMetrics.stringWidth(it) }
    val lineHeight = fontMetrics.height
    val totalTextHeight = lineHeight * lines.size

    val labelWidth = maxTextWidth + 2 * LABEL_PADDING_WIDTH
    val labelHeight = totalTextHeight + 2 * LABEL_PADDING_HEIGHT
    val labelContainerX = shiftedPoint.x.toInt() - LABEL_PADDING_WIDTH - maxTextWidth / 2
    val labelContainerY = shiftedPoint.y.toInt() - LABEL_PADDING_HEIGHT - totalTextHeight / 2

    // draw black rectangle background
    color = Color.BLACK
    fillRect(labelContainerX, labelContainerY, labelWidth, labelHeight)

    // draw thin white border
    color = Color.WHITE
    stroke = BasicStroke(1f)
    drawRect(labelContainerX, labelContainerY, labelWidth, labelHeight)

    // draw white text line by line
    lines.forEachIndexed { index, line ->
        val textWidth = fontMetrics.stringWidth(line)
        val labelX = shiftedPoint.x.toInt() - textWidth / 2
        val labelY = shiftedPoint.y.toInt() - totalTextHeight / 2 + (index + 1) * lineHeight - fontMetrics.descent
        drawString(line, labelX, labelY)
    }
}
