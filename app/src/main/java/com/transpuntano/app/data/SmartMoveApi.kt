package com.transpuntano.app.data

import com.transpuntano.app.model.*
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Locale

class SmartMoveApi {
    companion object {
        private const val SOAP_ENDPOINT = "http://clswsanluis.smartmovepro.net/moduloparadas/swparadas.asmx"
        private const val SOAP_NAMESPACE = "http://clsw.smartmovepro.net/"
        private const val TIMEOUT_MS = 30_000
        private const val USER = "WEB.TRANSPUNTANO"
        private const val PASSWORD = "PAR.SW.TRANSPUNTANO"
        private const val CODIGO_CUANDO_LLEGA = 35
        private const val CODIGO_APLICACION_ARRIBOS = 24
        private const val LISTA_CODIGOS_EMPRESA = "155"
        private const val PROVINCIA = "SAN LUIS"
        private const val PAIS = "ARGENTINA"
    }

    fun getLines(): List<TransitLine> {
        val root = soap("RecuperarLineaPorCuandoLlega", listOf(
            intParam("codigoCuandoLlega", CODIGO_CUANDO_LLEGA),
            stringParam("usuario", USER),
            stringParam("clave", PASSWORD),
            boolParam("isSublinea", false)
        ))
        val array = firstArray(root, "lineas", "Linea", "linea") ?: return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val code = item.optStringAny("codigoLinea", "CodigoLinea", "CodigoLineaParada", "codigoLineaParada")
                ?.toIntOrNull() ?: return@mapNotNull null
            TransitLine(
                code,
                item.optStringAny("descripcionLinea", "DescripcionLinea", "Descripcion", "descripcion", "Nombre", "nombre").orEmpty().ifBlank { "Línea " + code },
                item.optStringAny("CodigoEmpresa", "codigoEmpresa").orEmpty()
            )
        }.distinctBy { it.code }
    }

    fun getStreets(line: Int): List<TransitStreet> {
        val root = soap("RecuperarCallesPrincipalPorLinea", listOf(
            intParam("codigoLineaParada", line),
            stringParam("usuario", USER),
            stringParam("clave", PASSWORD)
        ))
        val array = firstArray(root, "calles", "Calle", "calle") ?: return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val code = item.optStringAny("codigoCalle", "CodigoCalle", "Codigo", "codigo")
                ?.toIntOrNull() ?: return@mapNotNull null
            TransitStreet(
                code,
                item.optStringAny("nombreCalle", "DescripcionCalle", "Descripcion", "descripcion", "Nombre", "nombre").orEmpty().ifBlank { "Calle " + code }
            )
        }.distinctBy { it.code }
    }

    fun getIntersections(line: Int, street: Int): List<TransitIntersection> {
        val root = soap("RecuperarInterseccionPorLineaYCalle", listOf(
            intParam("codigoLineaParada", line),
            intParam("codigoCalle", street),
            stringParam("usuario", USER),
            stringParam("clave", PASSWORD)
        ))
        val array = firstArray(root, "interseccion", "intersecciones", "Interseccion") ?: return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val code = item.optIntAny("codigoInterseccion", "CodigoInterseccion", "Codigo", "codigo")
                ?: return@mapNotNull null
            TransitIntersection(
                code,
                item.optStringAny("descripcionInterseccion", "DescripcionInterseccion", "Descripcion", "descripcion", "Nombre", "nombre").orEmpty().ifBlank { "Intersección " + code }
            )
        }.distinctBy { it.code }
    }

    fun getStops(line: Int, street: Int, intersection: Int): List<TransitStop> {
        val root = soap("RecuperarParadasPorLineaCalleEInterseccion", listOf(
            intParam("codigoLinea", line),
            intParam("codigoCalle", street),
            intParam("codigoInterseccion", intersection),
            stringParam("usuario", USER),
            stringParam("clave", PASSWORD)
        ))
        return parseStops(firstArray(root, "paradas", "Parada", "parada"), line)
    }

    fun getArrivals(identifier: String, line: Int): List<TransitArrival> {
        val root = soap("RecuperarProximosArribos", listOf(
            stringParam("identificadorParada", identifier),
            intParam("codigoLineaParada", line),
            intParam("codigoAplicacion", CODIGO_APLICACION_ARRIBOS),
            stringParam("localidad", "SAN LUIS"),
            stringParam("usuario", USER),
            stringParam("clave", PASSWORD)
        ))
        val array = firstArray(root, "arribos", "Arribo", "arribo") ?: return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val arrival = item.optStringAny("Arribo", "arribo", "Tiempo", "tiempo").orEmpty()
            TransitArrival(
                line = item.optStringAny("DescripcionLinea", "descripcionLinea", "Linea", "linea").orEmpty().ifBlank { line.toString() },
                destination = item.optStringAny("DescripcionBandera", "descripcionBandera", "Bandera", "bandera", "Destino", "destino").orEmpty(),
                minutes = parseMinutes(arrival),
                status = arrival
            )
        }
    }

    fun getNearby(latitude: Double, longitude: Double): List<TransitStop> {
        val root = soap("RecuperarParadasMasCercanasPorLocalidadProvinciaPais", listOf(
            stringParam("latitud", formatCoordinate(latitude)),
            stringParam("longitud", formatCoordinate(longitude)),
            stringParam("listaCodigosEmpresa", LISTA_CODIGOS_EMPRESA),
            stringParam("descripcionProvincia", PROVINCIA),
            stringParam("descripcionPais", PAIS),
            stringParam("usuario", USER),
            stringParam("clave", PASSWORD)
        ))
        return parseStops(firstArray(root, "paradas", "Parada", "parada"), 0)
    }

    fun getRoute(line: Int): List<Pair<Double, Double>> {
        val root = soap("RecuperarRecorridoParaMapaAbrevYAmpliPorEntidadYLinea", listOf(
            intParam("codigoLineaParada", line),
            stringParam("usuario", USER),
            stringParam("clave", PASSWORD)
        ))
        val array = firstArray(root, "puntos", "Puntos", "recorrido", "Recorrido") ?: return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val lat = item.optStringAny("Latitud", "latitud", "Latitude", "latitude")?.replace(',', '.')?.toDoubleOrNull()
            val lon = item.optStringAny("Longitud", "longitud", "Longitude", "longitude")?.replace(',', '.')?.toDoubleOrNull()
            if (lat != null && lon != null) lat to lon else null
        }
    }

    private fun soap(operation: String, params: List<SoapParam>): JSONObject {
        val connection = (URL(SOAP_ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            doInput = true
            doOutput = true
            useCaches = false
            setRequestProperty("Content-Type", "text/xml;charset=utf-8")
            setRequestProperty("Accept", "text/xml")
            setRequestProperty("SOAPAction", "\"" + SOAP_NAMESPACE + operation + "\"")
            setRequestProperty("User-Agent", "ksoap2-android/2.6.0")
        }

        return try {
            OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { writer ->
                writer.write(buildSoapRequest(operation, params))
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = if (stream != null) {
                BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { it.readText() }
            } else ""
            if (code !in 200..299) {
                throw SmartMoveException("SmartMove SOAP HTTP " + code + ": " + response.take(500))
            }
            parseSoapJson(response)
        } catch (e: SmartMoveException) {
            throw e
        } catch (e: Exception) {
            throw SmartMoveException("SmartMove SOAP: " + (e.message ?: e.javaClass.simpleName), e)
        } finally {
            connection.disconnect()
        }
    }

    private fun buildSoapRequest(operation: String, params: List<SoapParam>): String = buildString {
        append("""<?xml version="1.0" encoding="utf-8"?>""")
        append("""<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:sm="$SOAP_NAMESPACE">""")
        append("<soapenv:Body><sm:" + operation + ">")
        params.forEach { param ->
            append("<" + param.name + ">")
            append(escapeXml(param.value))
            append("</" + param.name + ">")
        }
        append("</sm:" + operation + "></soapenv:Body></soapenv:Envelope>")
    }

    private fun parseSoapJson(rawXml: String): JSONObject {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(rawXml.reader())
        val text = StringBuilder()
        val fault = StringBuilder()
        var inFault = false

        while (true) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> if (parser.name.equals("Fault", true)) inFault = true
                XmlPullParser.TEXT, XmlPullParser.CDSECT -> if (inFault) fault.append(parser.text.orEmpty()) else text.append(parser.text.orEmpty())
                XmlPullParser.END_TAG -> if (parser.name.equals("Fault", true)) inFault = false
                XmlPullParser.END_DOCUMENT -> break
            }
        }

        if (fault.isNotBlank()) {
            throw SmartMoveException("SmartMove SOAP Fault: " + fault.toString().trim().take(500))
        }

        val cleaned = text.toString().trim().removePrefix("\uFEFF")
        if (cleaned.startsWith("{")) return JSONObject(cleaned)
        if (cleaned.startsWith("[")) return JSONObject().put("data", JSONArray(cleaned))
        throw SmartMoveException("Respuesta SmartMove SOAP no válida: " + cleaned.take(500))
    }

    private fun parseStops(array: JSONArray?, line: Int): List<TransitStop> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val code = item.optIntAny("codigoParada", "CodigoParada", "Codigo", "codigo") ?: return@mapNotNull null
            TransitStop(
                code = code,
                description = item.optStringAny("descripcionParada", "DescripcionParada", "Descripcion", "descripcion", "Nombre", "nombre").orEmpty().ifBlank { "Parada " + code },
                identifier = item.optStringAny("Identificador", "identificador", "IdentificadorParada", "identificadorParada").orEmpty().ifBlank { code.toString() },
                latitude = item.optStringAny("Latitud", "latitud")?.replace(',', '.')?.toDoubleOrNull() ?: 0.0,
                longitude = item.optStringAny("Longitud", "longitud")?.replace(',', '.')?.toDoubleOrNull() ?: 0.0,
                street = item.optStringAny("nombreCalle", "CallePrincipal", "callePrincipal").orEmpty(),
                intersection = item.optStringAny("inteserccionCalle", "interseccionCalle", "CalleInterseccion", "calleInterseccion").orEmpty(),
                lineCode = line
            )
        }.distinctBy { it.code to it.identifier }
    }

    private fun parseMinutes(value: String): Int? {
        val direct = value.trim().toIntOrNull()
        if (direct != null) return direct
        return Regex("""(-?\d+)""").find(value)?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    private fun formatCoordinate(value: Double): String = String.format(Locale.US, "%.6f", value)

    private fun escapeXml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private data class SoapParam(val name: String, val value: String, val xsdType: String)
    private fun stringParam(name: String, value: String) = SoapParam(name, value, "string")
    private fun intParam(name: String, value: Int) = SoapParam(name, value.toString(), "int")
    private fun boolParam(name: String, value: Boolean) = SoapParam(name, value.toString(), "boolean")

    class SmartMoveException(message: String, cause: Throwable? = null) : IllegalStateException(message, cause)

    private fun JSONObject.optStringAny(vararg names: String): String? {
        names.forEach { name ->
            if (has(name) && !isNull(name)) return opt(name)?.toString()
        }
        return null
    }

    private fun JSONObject.optIntAny(vararg names: String): Int? {
        val value = optStringAny(*names) ?: return null
        return value.trim().toDoubleOrNull()?.toInt()
    }

    private fun firstArray(root: JSONObject, vararg names: String): JSONArray? {
        names.forEach { name ->
            root.optJSONArray(name)?.let { return it }
        }
        val nested = root.optJSONObject("data")
        if (nested != null) {
            names.forEach { name ->
                nested.optJSONArray(name)?.let { return it }
            }
        }
        return null
    }
}
