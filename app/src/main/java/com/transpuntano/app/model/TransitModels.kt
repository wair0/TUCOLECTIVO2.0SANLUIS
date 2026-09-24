package com.transpuntano.app.model

data class TransitLine(
    val code: Int,
    val name: String,
    val raw: String = ""
)

data class TransitStreet(
    val code: Int,
    val name: String
)

data class TransitIntersection(
    val code: Int,
    val name: String
)

data class TransitStop(
    val code: Int,
    val description: String,
    val identifier: String,
    val latitude: Double,
    val longitude: Double,
    val street: String = "",
    val intersection: String = "",
    val lineCode: Int = 0
)

data class TransitArrival(
    val line: String,
    val destination: String,
    val minutes: Int?,
    val status: String = ""
)
