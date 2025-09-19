package simulation

import simulation.model.Edge
import simulation.model.Polygon
import simulation.model.Triangle
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_RGB
import java.io.File
import javax.imageio.ImageIO
import kotlin.collections.forEach

private fun Graphics2D.drawEdge(edge: Edge) {
    this.drawLine(
        edge.p1.x.toInt(),
        edge.p1.y.toInt(),
        edge.p2.x.toInt(),
        edge.p2.y.toInt()
    )
}

fun outputToPng(polygons: List<Polygon>) {
    val edges = polygons.flatMap { it.edges }.distinct()

    // image
    val image = BufferedImage(WIDTH, HEIGHT, TYPE_INT_RGB)
    val graphics = image.createGraphics()

    // background color
    graphics.color = Color.BLACK
    graphics.fillRect(0, 0, WIDTH, HEIGHT)


    // draw
    graphics.color = Color.YELLOW
    graphics.stroke = BasicStroke(8f)
    edges
        .forEach { graphics.drawEdge(it) }

    graphics.dispose()

    val outputDir = File("output")
    if (!outputDir.exists()) {
        outputDir.mkdirs()
    }

    // save
    ImageIO.write(image, "PNG", File("output/layout.png"))
}
