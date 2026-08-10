package simulation

import simulation.model.Edge
import simulation.model.Ladder
import simulation.model.Layout
import simulation.model.Point
import simulation.model.QuadrilateralSubdivision

/**
 * Numbers every distinct edge, placing the number at the edge's midpoint.
 * Secondary edges are prefixed with "S" so they can be told apart from main edges.
 */
fun edgeLabels(edges: List<Edge>, secondaryEdges: Collection<Edge> = emptyList()): Map<Point, String> {
    val secondarySet = secondaryEdges.toSet()
    return edges.mapIndexed { index, edge ->
        val prefix = if (secondarySet.contains(edge)) "S" else ""
        edge.pointsAt(.5).first() to "$prefix${index + 1}"
    }.toMap()
}

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
