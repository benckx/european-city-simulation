package simulation

import simulation.model.Ladder
import simulation.model.Layout
import simulation.model.Point
import simulation.model.QuadrilateralSubdivision

fun ladderLabels(ladders: List<Ladder>): Map<Point, String> {
    return ladders.flatMapIndexed { ladderIndex, ladder ->
        ladder.edges.mapIndexed { edgeIndex, edge ->
            val point = edge.pointsAt(.5).first()
            val ladderLetter = ('a' + ladderIndex).toString()
            val label = "[$ladderLetter] ${edgeIndex + 1}"
            point to label
        }
    }.toMap()
}

fun preDivisionInfoLabels(layout: Layout): Map<Point, String> {
    return layout.quadrilaterals().associate { q ->
        val lengths = q.edges.map { it.length }

        val lines = listOf(
            "elong: %.2f".format(q.elongationIndex()),
            "irreg: %.2f".format(q.irregularityIndex()),
            "${(lengths.min()).toInt()} - ${lengths.max().toInt()}"
        )

        q.findCentroid() to lines.joinToString("\n")
    }
}

fun postDivisionInfoLabels(subdivisions: List<QuadrilateralSubdivision>): Map<Point, String> {
    return subdivisions.associate { subdivision ->
        val quadrilateral = subdivision.quadrilateral
        val (shortDiv, longDiv) = subdivision.divisionFactors()
        val angles = quadrilateral.interiorAngles()
        val minAngle = angles.min()
        val maxAngle = angles.max()

        val lines = listOf(
            "${shortDiv}x${longDiv}",
            "${minAngle.toInt()}°-${maxAngle.toInt()}° (Δ${(maxAngle - minAngle).toInt()})°",
            "irreg: %.2f".format(quadrilateral.irregularityIndex())
        )

        quadrilateral.findCentroid() to lines.joinToString("\n")
    }
}

fun logLayout(layout: Layout, name: String = "layout"): String {
    return "[$name] #polygons: ${layout.polygons.size}, " +
            "#triangles: ${layout.triangles().size}, " +
            "#quadrilaterals: ${layout.quadrilaterals().size}"
}
