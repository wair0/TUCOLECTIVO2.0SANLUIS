package com.tucolectivo.app.ui

enum class LinesLevel {
    CATALOG, STREETS, INTERSECTIONS, STOPS, ARRIVALS
}

data class FavoriteStopUi(
    val lineCode: Int,
    val lineName: String,
    val stopCode: Int,
    val description: String,
    val street: String,
    val intersection: String
)
