package com.transpuntano.app.data

import com.transpuntano.app.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

class SmartMoveApi {
    companion object {
        private const val BASE_URL = "http://clsw.smartmovepro.net/"
        private const val TIMEOUT_MS = 12000
        private const val SOAP_NS = "http://tempuri.org/"
    }

    fun getLines(): List<TransitLine> {
        val raw = soap("RecuperarLineaPorCuandoLlega", emptyMap())
        return parseObjects(raw, "CodigoLinea", "DescripcionLinea", "NombreLinea").mapNotNull { obj ->
            val code = intValue(obj, "CodigoLinea", "codigoLinea") ?: return@mapNotNull null
            TransitLine(code, stringValue(obj, "DescripcionLinea", "descripcionLinea", "NombreLinea", "nombreLinea") ?: "Línea " + code)
        }.distinctBy { it.code }
    }

    fun getStreets(line: Int): List<TransitStreet> {
        val raw = soap("RecuperarCallesPrincipalPorLinea", mapOf("CodigoLinea" to line))
        return parseObjects(raw, "CodigoCalle", "NombreCalle", "DescripcionCalle").mapNotNull { obj ->
            val code = intValue(obj, "CodigoCalle", "codigoCalle") ?: return@mapNotNull null
            TransitStreet(code, stringValue(obj, "NombreCalle", "nombreCalle", "DescripcionCalle", "descripcionCalle") ?: "Calle " + code)
        }.distinctBy { it.code }
    }

    fun getIntersections(line: Int, street: Int): List<TransitIntersection> {
        val raw = soap("RecuperarInterseccionPorLineaYCalle", mapOf("CodigoLinea" to line, "CodigoCalle" to street))
        return parseObjects(raw, "CodigoInterseccion", "NombreInterseccion", "Interseccion").mapNotNull { obj ->
            val code = intValue(obj, "CodigoInterseccion", "codigoInterseccion") ?: return@mapNotNull null
            TransitIntersection(code, stringValue(obj, "NombreInterseccion", "nombreInterseccion", "Interseccion", "interseccion") ?: "Intersección " + code)
        }.distinctBy { it.code }
    }

    fun getStops(line: Int, street: Int, intersection: Int): List<TransitStop> {
        val raw = soap("RecuperarParadasPorLineaCalleEInterseccion", mapOf(
            "CodigoLinea" to line,
            "CodigoCalle" to street,
            "CodigoInterseccion" to intersection
        ))
        return parseStops(raw, line)
    }

    fun getArrivals(identifier: String, line: Int): List<TransitArrival> {
        val raw = soap("RecuperarProximosArribos", mapOf(
            "IdentificadorParada" to identifier,
            "CodigoLinea" to line
        ))
        return parseArrivals(raw)
    }

    fun getNearby(latitude: Double, longitude: Double): List<TransitStop> {
        val raw = soap("RecuperarParadasMasCercanasPorLocalidadProvinciaPais", mapOf(
            "Latitud" to latitude,
            "Longitud" to longitude,
            "Localidad" to "San Luis",
            "Provincia" to "San Luis",
            "Pais" to "Argentina"
        ))
        return parseStops(raw, 0)
    }

    fun getRoute(line: Int): List<Pair<Double, Double>> {
        val raw = soap("RecuperarRecorridoParaMapaAbrevYAmpliPorEntidadYLinea", mapOf(
            "CodigoEntidad" to 1,
            "CodigoLinea" to line
        ))
        val points = mutableListOf<Pair<Double, Double>>()
        val pattern = Pattern.compile(
            "(?is)<(?:Latitud|latitud)\\b[^>]*>\\s*([-+0-9.,]+)\\s*</(?:Latitud|latitud)>.*?<(?:Longitud|longitud)\\b[^>]*>\\s*([-+0-9.,]+)\\s*</(?:Longitud|longitud)>"
        )
        val matcher = pattern.matcher(raw)
        while (matcher.find()) {
            val latitude = matcher.group(1)?.replace(",", ".")?.toDoubleOrNull()
            val longitude = matcher.group(2)?.replace(",", ".")?.toDoubleOrNull()
            if (latitude != null && longitude != null) points += latitude to longitude
        }
        return points
    }

    private fun soap(action: String, params: Map<String, Any>): String {
        val envelope = buildString {
            append("""<?xml version="1.0" encoding="utf-8"?>""")
            append("""<soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">""")
            append("<soap:Body><")
            append(action)
            append(" xmlns=\"")
            append(SOAP_NS)
            append("\">")
            params.forEach { (key, value) ->
                append("<").append(key).append(">")
                append(xmlEscape(value.toString()))
                append("</").append(key).append(">")
            }
            append("</").append(action).append("></soap:Body></soap:Envelope>")
        }

        val connection = (URL(BASE_URL + action).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            doOutput = true
            setRequestProperty("Content-Type", "text/xml; charset=utf-8")
            setRequestProperty("SOAPAction", "\"" + SOAP_NS + action + "\"")
            setRequestProperty("Accept", "text/xml, application/xml, application/json, */*")
            setRequestProperty("User-Agent", "TU-COLECTIVO-2.0")
        }

        return try {
            connection.outputStream.use { it.write(envelope.toByteArray(StandardCharsets.UTF_8)) }
            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            if (stream == null) error("Servidor sin respuesta HTTP " + connection.responseCode)
            BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseObjects(raw: String, vararg names: String): List<JSONObject> {
        val jsonObjects = mutableListOf<JSONObject>()
        try { collectJsonObjects(JSONObject(raw), jsonObjects) } catch (_: Exception) { }
        if (jsonObjects.isNotEmpty()) return jsonObjects

        val codeName = names.first()
        val textNames = names.drop(1).joinToString("|")
        val regex = "(?is)<(?:.*?:)?" + codeName + "\\b[^>]*>\\s*([^<]+).*?<(?:(?:.*?:)?" + textNames + ")\\b[^>]*>\\s*([^<]+)"
        val matcher = Pattern.compile(regex).matcher(raw)
        while (matcher.find()) {
            jsonObjects += JSONObject()
                .put(codeName, matcher.group(1).trim())
                .put(names.getOrElse(1) { "Descripcion" }, matcher.group(2).trim())
        }
        return jsonObjects
    }

    private fun parseStops(raw: String, line: Int): List<TransitStop> {
        val result = mutableListOf<TransitStop>()
        val matcher = Pattern.compile("(?is)<(?:.*?:)?Parada\\b[^>]*>(.*?)</(?:.*?:)?Parada>").matcher(raw)
        while (matcher.find()) {
            val block = matcher.group(1) ?: continue
            val code = tagInt(block, "CodigoParada", "codigoParada") ?: continue
            result += TransitStop(
                code,
                tag(block, "DescripcionParada", "descripcionParada") ?: "Parada " + code,
                tag(block, "IdentificadorParada", "identificadorParada") ?: code.toString(),
                tagDouble(block, "Latitud", "latitud") ?: 0.0,
                tagDouble(block, "Longitud", "longitud") ?: 0.0,
                tag(block, "NombreCalle", "nombreCalle").orEmpty(),
                tag(block, "InterseccionCalle", "interseccionCalle").orEmpty(),
                line
            )
        }
        return result.distinctBy { it.code to it.identifier }
    }

    private fun parseArrivals(raw: String): List<TransitArrival> {
        val result = mutableListOf<TransitArrival>()
        val matcher = Pattern.compile("(?is)<(?:.*?:)?(?:ArriboParada|Arribo)\\b[^>]*>(.*?)</(?:.*?:)?(?:ArriboParada|Arribo)>").matcher(raw)
        while (matcher.find()) {
            val block = matcher.group(1) ?: continue
            result += TransitArrival(
                tag(block, "DescripcionLinea", "descripcionLinea", "Linea", "linea").orEmpty(),
                tag(block, "Destino", "destino", "Bandera", "bandera").orEmpty(),
                tagInt(block, "Minutos", "minutos", "Tiempo", "tiempo", "MinutosArribo", "minutosArribo"),
                tag(block, "Estado", "estado").orEmpty()
            )
        }
        return result
    }

    private fun collectJsonObjects(value: Any?, output: MutableList<JSONObject>) {
        when (value) {
            is JSONObject -> {
                var relevant = false
                val keys = value.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    if (key.equals("CodigoLinea", true) || key.equals("CodigoParada", true) || key.equals("CodigoCalle", true)) relevant = true
                }
                if (relevant) output += value
                val children = value.keys()
                while (children.hasNext()) collectJsonObjects(value.opt(children.next()), output)
            }
            is JSONArray -> for (index in 0 until value.length()) collectJsonObjects(value.opt(index), output)
        }
    }

    private fun intValue(obj: JSONObject, vararg names: String): Int? = names.firstNotNullOfOrNull { obj.optString(it, "").toIntOrNull() }
    private fun stringValue(obj: JSONObject, vararg names: String): String? = names.firstNotNullOfOrNull { obj.optString(it, "").takeIf { value -> value.isNotBlank() } }

    private fun tag(raw: String, vararg names: String): String? {
        for (name in names) {
            val matcher = Pattern.compile("(?is)<(?:.*?:)?" + name + "\\b[^>]*>\\s*([^<]+)").matcher(raw)
            if (matcher.find()) return matcher.group(1).trim()
        }
        return null
    }

    private fun tagInt(raw: String, vararg names: String): Int? = tag(raw, *names)?.replace(",", ".")?.toDoubleOrNull()?.toInt()
    private fun tagDouble(raw: String, vararg names: String): Double? = tag(raw, *names)?.replace(",", ".")?.toDoubleOrNull()

    private fun xmlEscape(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}