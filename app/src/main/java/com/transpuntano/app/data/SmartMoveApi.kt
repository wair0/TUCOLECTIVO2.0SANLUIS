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
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Cliente nativo para los servicios que utiliza la app oficial.
 *
 * La app oficial expone operaciones SmartMove como:
 * RecuperarLineaPorCuandoLlega, RecuperarCallesPrincipalPorLinea,
 * RecuperarInterseccionPorLineaYCalle, RecuperarParadasPorLineaCalleEInterseccion,
 * RecuperarProximosArribos y RecuperarRecorridoParaMapaAbrevYAmpliPorEntidadYLinea.
 */
class SmartMoveApi {

    companion object {
        private const val BASE = "http://clsw.smartmovepro.net/"
        private const val TIMEOUT = 12_000

        private const val SOAP_NS = "http://tempuri.org/"
    }

    fun getLines(): List<TransitLine> {
        val body = soap("RecuperarLineaPorCuandoLlega", emptyMap())
        return parseObjects(body, listOf("CodigoLinea", "codigoLinea"), listOf("DescripcionLinea", "descripcionLinea"))
            .mapNotNull {
                val code = number(it, "CodigoLinea", "codigoLinea") ?: return@mapNotNull null
                val name = text(it, "DescripcionLinea", "descripcionLinea") ?: "Línea $code"
                TransitLine(code, name, it.toString())
            }
            .distinctBy { it.code }
    }

    fun getStreets(lineCode: Int): List<TransitStreet> {
        val body = soap("RecuperarCallesPrincipalPorLinea", mapOf("CodigoLinea" to lineCode))
        return parseObjects(body, listOf("CodigoCalle", "codigoCalle"), listOf("NombreCalle", "nombreCalle", "DescripcionCalle", "descripcionCalle"))
            .mapNotNull {
                val code = number(it, "CodigoCalle", "codigoCalle") ?: return@mapNotNull null
                TransitStreet(code, text(it, "NombreCalle", "nombreCalle", "DescripcionCalle", "descripcionCalle") ?: "Calle $code")
            }
            .distinctBy { it.code }
    }

    fun getIntersections(lineCode: Int, streetCode: Int): List<TransitIntersection> {
        val body = soap(
            "RecuperarInterseccionPorLineaYCalle",
            mapOf("CodigoLinea" to lineCode, "CodigoCalle" to streetCode)
        )
        return parseObjects(body, listOf("CodigoInterseccion", "codigoInterseccion"), listOf("NombreInterseccion", "nombreInterseccion", "Interseccion", "interseccion"))
            .mapNotNull {
                val code = number(it, "CodigoInterseccion", "codigoInterseccion") ?: return@mapNotNull null
                TransitIntersection(code, text(it, "NombreInterseccion", "nombreInterseccion", "Interseccion", "interseccion") ?: "Intersección $code")
            }
            .distinctBy { it.code }
    }

    fun getStops(lineCode: Int, streetCode: Int, intersectionCode: Int): List<TransitStop> {
        val body = soap(
            "RecuperarParadasPorLineaCalleEInterseccion",
            mapOf(
                "CodigoLinea" to lineCode,
                "CodigoCalle" to streetCode,
                "CodigoInterseccion" to intersectionCode
            )
        )
        return parseStops(body, lineCode)
    }

    fun getArrivals(stopIdentifier: String, lineCode: Int): List<TransitArrival> {
        val body = soap(
            "RecuperarProximosArribos",
            mapOf(
                "IdentificadorParada" to stopIdentifier,
                "CodigoLinea" to lineCode
            )
        )
        return parseArrivals(body)
    }

    fun getNearby(latitude: Double, longitude: Double): List<TransitStop> {
        val body = soap(
            "RecuperarParadasMasCercanasPorLocalidadProvinciaPais",
            mapOf(
                "Latitud" to latitude,
                "Longitud" to longitude,
                "Localidad" to "San Luis",
                "Provincia" to "San Luis",
                "Pais" to "Argentina"
            )
        )
        return parseStops(body, 0)
    }

    fun getRoute(lineCode: Int): List<Pair<Double, Double>> {
        val body = soap(
            "RecuperarRecorridoParaMapaAbrevYAmpliPorEntidadYLinea",
            mapOf("CodigoEntidad" to 1, "CodigoLinea" to lineCode)
        )
        val result = mutableListOf<Pair<Double, Double>>()
        val regex = Pattern.compile(
            "(?is)<(?:Latitud|latitud)[^>]*>\\s*([-+0-9.,]+)\\s*</(?:Latitud|latitud)>.*?<(?:(?:Longitud)|(?:longitud))[^>]*>\\s*([-+0-9.,]+)\\s*</",
        )
        val m = regex.matcher(body)
        while (m.find()) {
            val lat = m.group(1)?.replace(",", ".")?.toDoubleOrNull()
            val lon = m.group(2)?.replace(",", ".")?.toDoubleOrNull()
            if (lat != null && lon != null) result += lat to lon
        }
        return result
    }

    private fun soap(action: String, params: Map<String, Any>): String {
        val envelope = buildString {
            append("""<?xml version="1.0" encoding="utf-8"?>""")
            append("""<soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">""")
            append("<soap:Body><$action xmlns="$SOAP_NS">")
            params.forEach { (key, value) ->
                append("<$key>")
                append(xmlEscape(value.toString()))
                append("</$key>")
            }
            append("</$action></soap:Body></soap:Envelope>")
        }

        val url = URL(BASE + action)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = TIMEOUT
            readTimeout = TIMEOUT
            doOutput = true
            setRequestProperty("Content-Type", "text/xml; charset=utf-8")
            setRequestProperty("SOAPAction", ""$SOAP_NS$action"")
            setRequestProperty("Accept", "text/xml, application/xml, application/json, */*")
            setRequestProperty("User-Agent", "TU-COLECTIVO-2.0")
        }

        return try {
            connection.outputStream.use { it.write(envelope.toByteArray(StandardCharsets.UTF_8)) }
            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseObjects(raw: String, codeNames: List<String>, textNames: List<String>): List<JSONObject> {
        val result = mutableListOf<JSONObject>()
        try {
            val json = JSONObject(raw)
            collectJsonObjects(json, result)
        } catch (_: Exception) {
            // SOAP/XML fallback below.
        }
        if (result.isNotEmpty()) return result

        val code = codeNames.joinToString("|")
        val value = textNames.joinToString("|")
        val p = Pattern.compile(
            "(?is)<(?:$code)\\b[^>]*>\\s*([^<]+).*?<(?:(?:$value))\\b[^>]*>\\s*([^<]+)",
        )
        val m = p.matcher(raw)
        while (m.find()) {
            result += JSONObject().put(codeNames.first(), m.group(1).trim()).put(textNames.first(), m.group(2).trim())
        }
        return result
    }

    private fun parseStops(raw: String, defaultLine: Int): List<TransitStop> {
        val result = mutableListOf<TransitStop>()
        val objectPattern = Pattern.compile(
            "(?is)<(?:Parada|parada)[^>]*>(.*?)</(?:Parada|parada)>"
        )
        val objects = objectPattern.matcher(raw)
        while (objects.find()) {
            val x = objects.group(1) ?: continue
            val code = tagInt(x, "CodigoParada", "codigoParada") ?: continue
            val desc = tag(x, "DescripcionParada", "descripcionParada") ?: "Parada $code"
            val id = tag(x, "IdentificadorParada", "identificadorParada") ?: code.toString()
            val lat = tagDouble(x, "Latitud", "latitud") ?: 0.0
            val lon = tagDouble(x, "Longitud", "longitud") ?: 0.0
            result += TransitStop(code, desc, id, lat, lon, tag(x, "NombreCalle", "nombreCalle").orEmpty(), tag(x, "InterseccionCalle", "inteserccionCalle").orEmpty(), defaultLine)
        }
        return result.distinctBy { it.code to it.identifier }
    }

    private fun parseArrivals(raw: String): List<TransitArrival> {
        val result = mutableListOf<TransitArrival>()
        val p = Pattern.compile("(?is)<(?:.*?:)?(?:ArriboParada|Arribo)[^>]*>(.*?)</(?:.*?:)?(?:ArriboParada|Arribo)>")
        val m = p.matcher(raw)
        while (m.find()) {
            val x = m.group(1) ?: continue
            val line = tag(x, "DescripcionLinea", "descripcionLinea", "Linea", "linea").orEmpty()
            val destination = tag(x, "Destino", "destino", "Bandera", "bandera").orEmpty()
            val minutes = tagInt(x, "Minutos", "minutos", "Tiempo", "tiempo", "MinutosArribo", "minutosArribo")
            result += TransitArrival(line, destination, minutes, tag(x, "Estado", "estado").orEmpty())
        }
        if (result.isEmpty()) {
            val minutePattern = Pattern.compile("(?i)(\\d+)\\s*(?:min|mins|minutos)")
            val mm = minutePattern.matcher(raw)
            while (mm.find()) result += TransitArrival("", "", mm.group(1).toIntOrNull(), "")
        }
        return result
    }

    private fun collectJsonObjects(value: Any?, output: MutableList<JSONObject>) {
        when (value) {
            is JSONObject -> {
                var hasCode = false
                value.keys().forEach { if (it.equals("CodigoLinea", true) || it.equals("CodigoParada", true)) hasCode = true }
                if (hasCode) output += value
                value.keys().forEach { collectJsonObjects(value.opt(it), output) }
            }
            is JSONArray -> for (i in 0 until value.length()) collectJsonObjects(value.opt(i), output)
        }
    }

    private fun number(o: JSONObject, vararg names: String): Int? = names.firstNotNullOfOrNull { o.optString(it, "").toIntOrNull() }
    private fun text(o: JSONObject, vararg names: String): String? = names.firstNotNullOfOrNull { o.optString(it, "").takeIf(String::isNotBlank) }

    private fun tag(raw: String, vararg names: String): String? {
        for (n in names) {
            val m = Pattern.compile("(?is)<(?:.*?:)?$n\\b[^>]*>\\s*([^<]+)").matcher(raw)
            if (m.find()) return m.group(1).trim()
        }
        return null
    }

    private fun tagInt(raw: String, vararg names: String): Int? = tag(raw, *names)?.replace(",", ".")?.toDoubleOrNull()?.toInt()
    private fun tagDouble(raw: String, vararg names: String): Double? = tag(raw, *names)?.replace(",", ".")?.toDoubleOrNull()

    private fun xmlEscape(value: String): String =
        value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace(""", "&quot;").replace("'", "&apos;")
}
