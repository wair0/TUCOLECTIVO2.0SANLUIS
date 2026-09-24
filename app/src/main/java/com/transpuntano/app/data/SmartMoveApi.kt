package com.transpuntano.app.data

import com.transpuntano.app.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class SmartMoveApi {
    companion object {
        private const val BASE_URL = "http://clsw.smartmovepro.net/"
        private const val TIMEOUT_MS = 15000
    }

    fun getLines(): List<TransitLine> {
        val root = post("RecuperarLineaPorCuandoLlega")
        val array = root.optJSONArray("lineas") ?: return emptyList()
        return jsonArrayObjects(array).mapNotNull { item ->
            val code = firstInt(item, "CodigoLinea", "CodigoLineaParada", "codigoLinea", "codigoLineaParada")
                ?: return@mapNotNull null
            val name = firstString(item, "Descripcion", "DescripcionLinea", "descripcionLinea", "NombreLinea")
                ?: "Línea " + code
            TransitLine(code, name)
        }.distinctBy { it.code }
    }

    fun getStreets(line: Int): List<TransitStreet> {
        val root = post("RecuperarCallesPrincipalPorLinea", mapOf("codLinea" to line.toString()))
        val array = root.optJSONArray("calles") ?: return emptyList()
        return jsonArrayObjects(array).mapNotNull { item ->
            val code = firstInt(item, "Codigo", "CodigoCalle", "codigoCalle") ?: return@mapNotNull null
            val name = firstString(item, "Descripcion", "NombreCalle", "nombreCalle") ?: "Calle " + code
            TransitStreet(code, name)
        }.distinctBy { it.code }
    }

    fun getIntersections(line: Int, street: Int): List<TransitIntersection> {
        val root = post(
            "RecuperarInterseccionPorLineaYCalle",
            mapOf("codLinea" to line.toString(), "codCalle" to street.toString())
        )
        val array = root.optJSONArray("calles") ?: return emptyList()
        return jsonArrayObjects(array).mapNotNull { item ->
            val code = firstInt(item, "Codigo", "CodigoInterseccion", "codigoInterseccion") ?: return@mapNotNull null
            val name = firstString(item, "Descripcion", "NombreInterseccion", "Interseccion") ?: "Intersección " + code
            TransitIntersection(code, name)
        }.distinctBy { it.code }
    }

    fun getStops(line: Int, street: Int, intersection: Int): List<TransitStop> {
        val root = post(
            "RecuperarParadasConBanderaPorLineaCalleEInterseccion",
            mapOf(
                "codLinea" to line.toString(),
                "codCalle" to street.toString(),
                "codInterseccion" to intersection.toString()
            )
        )
        return parseStops(root, line)
    }

    fun getArrivals(identifier: String, line: Int): List<TransitArrival> {
        val root = post(
            "RecuperarProximosArribosW",
            mapOf(
                "identificadorParada" to identifier,
                "codigoLineaParada" to line.toString()
            )
        )
        val array = root.optJSONArray("arribos") ?: return emptyList()
        return jsonArrayObjects(array).map {
            TransitArrival(
                firstString(it, "DescripcionLinea", "descripcionLinea", "Linea", "linea").orEmpty(),
                firstString(it, "Destino", "destino", "DescripcionBandera", "descripcionBandera", "Bandera").orEmpty(),
                firstInt(it, "Minutos", "minutos", "Tiempo", "tiempo", "MinutosArribo", "minutosArribo"),
                firstString(it, "Estado", "estado").orEmpty()
            )
        }
    }

    fun getNearby(latitude: Double, longitude: Double): List<TransitStop> {
        val root = post(
            "RecuperarParadasMasCercanasPorLocalidadProvinciaPais",
            mapOf(
                "latitud" to latitude.toString(),
                "longitud" to longitude.toString(),
                "nombreCiudad" to "San Luis",
                "nombreProvincia" to "San Luis",
                "nombrePais" to "Argentina"
            )
        )
        return parseStops(root, 0)
    }

    fun getRoute(line: Int): List<Pair<Double, Double>> {
        val root = post(
            "RecuperarRecorridoParaMapaAbrevYAmpliPorEntidadYLinea",
            mapOf("codLinea" to line.toString(), "isSublinea" to "0")
        )
        val array = root.optJSONArray("puntos") ?: return emptyList()
        return jsonArrayObjects(array).mapNotNull { item ->
            val lat = firstDouble(item, "Latitud", "latitud", "LatitudParada")
            val lon = firstDouble(item, "Longitud", "longitud", "LongitudParada")
            if (lat != null && lon != null) lat to lon else null
        }
    }

    private fun post(action: String, params: Map<String, String> = emptyMap()): JSONObject {
        val body = buildString {
            append("accion=")
            append(encode(action))
            params.forEach { (key, value) ->
                append('&').append(encode(key)).append('=').append(encode(value))
            }
        }

        val connection = (URL(BASE_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            doOutput = true
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            setRequestProperty("Accept", "application/json, text/plain, */*")
            setRequestProperty("User-Agent", "TU-COLECTIVO-2.0")
        }

        return try {
            connection.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val responseText = if (stream != null) {
                BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { it.readText() }
            } else ""
            if (code !in 200..299) {
                throw IllegalStateException("SmartMove HTTP " + code + ": " + responseText.take(180))
            }
            parseResponse(responseText)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseResponse(raw: String): JSONObject {
        val cleaned = raw.trim().removePrefix("\uFEFF")
        if (cleaned.startsWith("{")) return JSONObject(cleaned)
        if (cleaned.startsWith("[")) return JSONObject().put("data", JSONArray(cleaned))
        throw IllegalStateException("Respuesta SmartMove no válida: " + cleaned.take(180))
    }

    private fun parseStops(root: JSONObject, line: Int): List<TransitStop> {
        val raw = root.opt("paradas") ?: return emptyList()
        val entries = mutableListOf<JSONObject>()

        when (raw) {
            is JSONArray -> entries += jsonArrayObjects(raw)
            is JSONObject -> {
                val keys = raw.keys()
                while (keys.hasNext()) {
                    val value = raw.opt(keys.next())
                    if (value is JSONArray) entries += jsonArrayObjects(value)
                    else if (value is JSONObject) entries += value
                }
            }
        }

        return entries.mapNotNull { item ->
            val code = firstInt(item, "Codigo", "CodigoParada", "codigoParada") ?: return@mapNotNull null
            TransitStop(
                code = code,
                description = firstString(item, "Descripcion", "DescripcionParada", "descripcionParada") ?: "Parada " + code,
                identifier = firstString(item, "Identificador", "IdentificadorParada", "identificadorParada") ?: code.toString(),
                latitude = firstDouble(item, "LatitudParada", "Latitud", "latitud") ?: 0.0,
                longitude = firstDouble(item, "LongitudParada", "Longitud", "longitud") ?: 0.0,
                street = firstString(item, "NombreCalle", "nombreCalle").orEmpty(),
                intersection = firstString(item, "InterseccionCalle", "inteserccionCalle", "interseccionCalle").orEmpty(),
                lineCode = line
            )
        }.distinctBy { it.code to it.identifier }
    }

    private fun jsonArrayObjects(array: JSONArray): List<JSONObject> =
        (0 until array.length()).mapNotNull { array.optJSONObject(it) }

    private fun firstString(obj: JSONObject, vararg names: String): String? =
        names.firstNotNullOfOrNull { name ->
            obj.optString(name, "").takeIf { it.isNotBlank() && !it.equals("null", true) }
        }

    private fun firstInt(obj: JSONObject, vararg names: String): Int? =
        names.firstNotNullOfOrNull { name ->
            when (val value = obj.opt(name)) {
                is Number -> value.toInt()
                is String -> value.trim().replace(',', '.').toDoubleOrNull()?.toInt()
                else -> null
            }
        }

    private fun firstDouble(obj: JSONObject, vararg names: String): Double? =
        names.firstNotNullOfOrNull { name ->
            when (val value = obj.opt(name)) {
                is Number -> value.toDouble()
                is String -> value.trim().replace(',', '.').toDoubleOrNull()
                else -> null
            }
        }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
}
