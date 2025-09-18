package simulation

import simulation.model.Triangle
import java.awt.BasicStroke
import java.awt.Color
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_RGB
import java.io.File
import javax.imageio.ImageIO
import kotlin.collections.forEach

fun outputToPng(triangles: List<Triangle>) {
    // image
    val image = BufferedImage(WIDTH, HEIGHT, TYPE_INT_RGB)
    val graphics = image.createGraphics()

    // background color
    graphics.color = Color.BLACK
    graphics.fillRect(0, 0, WIDTH, HEIGHT)

    // draw triangles
    val edges = triangles.flatMap { it.getEdges() }.distinct()
    graphics.color = Color.YELLOW
    graphics.stroke = BasicStroke(8f)
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
