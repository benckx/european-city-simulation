package simulation

import simulation.model.Point
import simulation.model.Triangle
import java.awt.Color
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_RGB
import java.io.File
import javax.imageio.ImageIO
import kotlin.collections.forEach

fun outputToPng(points: List<Point>, triangles: List<Triangle>) {
    val thickness = 12

    // image
    val image = BufferedImage(WIDTH, HEIGHT, TYPE_INT_RGB)
    val graphics = image.createGraphics()

    // background color
    graphics.color = Color.BLACK
    graphics.fillRect(0, 0, WIDTH, HEIGHT)

    // draw points
    graphics.color = Color.CYAN
    points.forEach { point ->
        graphics.fillOval(
            (point.x - (thickness / 2)).toInt(),
            (point.y - (thickness / 2)).toInt(),
            thickness,
            thickness
        )
    }

    // draw edges
    val edges = triangles.flatMap { it.getEdges() }.distinct()
    graphics.color = Color.YELLOW
    edges.forEach { edge ->
        graphics.drawLine(
            edge.p1.x.toInt(),
            edge.p1.y.toInt(),
            edge.p2.x.toInt(),
            edge.p2.y.toInt()
        )
    }

    graphics.dispose()

    val outputDir = File("output")
    if (!outputDir.exists()) {
        outputDir.mkdirs()
    }

    // save
    ImageIO.write(image, "PNG", File("output/points.png"))
}
