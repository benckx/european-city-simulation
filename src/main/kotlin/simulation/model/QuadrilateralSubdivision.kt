package simulation.model

data class QuadrilateralSubdivision(
    val quadrilateral: Quadrilateral,
    val shortSideEdges: List<Edge>,
    val longSideEdges: List<Edge>
)
