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
  private const val BASE="http://clsw.smartmovepro.net/"
  private const val TIMEOUT=12000
  private const val SOAP_NS="http://tempuri.org/"
 }
 fun getLines():List<TransitLine>{val body=soap("RecuperarLineaPorCuandoLlega",emptyMap());return parseObjects(body,"CodigoLinea","DescripcionLinea").mapNotNull{o->val c=intValue(o,"CodigoLinea","codigoLinea")?:return@mapNotNull null;TransitLine(c,stringValue(o,"DescripcionLinea","descripcionLinea")?:"Línea "+c)}.distinctBy{it.code}}
 fun getStreets(line:Int):List<TransitStreet>{val body=soap("RecuperarCallesPrincipalPorLinea",mapOf("CodigoLinea" to line));return parseObjects(body,"CodigoCalle","NombreCalle","DescripcionCalle").mapNotNull{o->val c=intValue(o,"CodigoCalle","codigoCalle")?:return@mapNotNull null;TransitStreet(c,stringValue(o,"NombreCalle","nombreCalle","DescripcionCalle","descripcionCalle")?:"Calle "+c)}.distinctBy{it.code}}
 fun getIntersections(line:Int,street:Int):List<TransitIntersection>{val body=soap("RecuperarInterseccionPorLineaYCalle",mapOf("CodigoLinea" to line,"CodigoCalle" to street));return parseObjects(body,"CodigoInterseccion","NombreInterseccion","Interseccion").mapNotNull{o->val c=intValue(o,"CodigoInterseccion","codigoInterseccion")?:return@mapNotNull null;TransitIntersection(c,stringValue(o,"NombreInterseccion","nombreInterseccion","Interseccion","interseccion")?:"Intersección "+c)}.distinctBy{it.code}}
 fun getStops(line:Int,street:Int,intersection:Int):List<TransitStop>{return parseStops(soap("RecuperarParadasPorLineaCalleEInterseccion",mapOf("CodigoLinea" to line,"CodigoCalle" to street,"CodigoInterseccion" to intersection)),line)}
 fun getArrivals(id:String,line:Int):List<TransitArrival>{return parseArrivals(soap("RecuperarProximosArribos",mapOf("IdentificadorParada" to id,"CodigoLinea" to line)))}
 fun getNearby(lat:Double,lon:Double):List<TransitStop>{return parseStops(soap("RecuperarParadasMasCercanasPorLocalidadProvinciaPais",mapOf("Latitud" to lat,"Longitud" to lon,"Localidad" to "San Luis","Provincia" to "San Luis","Pais" to "Argentina")),0)}
 fun getRoute(line:Int):List<Pair<Double,Double>>{val raw=soap("RecuperarRecorridoParaMapaAbrevYAmpliPorEntidadYLinea",mapOf("CodigoEntidad" to 1,"CodigoLinea" to line));val out=mutableListOf<Pair<Double,Double>>();val p=Pattern.compile("(?is)<(?:Latitud|latitud)[^>]*>\\s*([-+0-9.,]+)\\s*</(?:Latitud|latitud)>.*?<(?:(?:Longitud)|(?:longitud))[^>]*>\\s*([-+0-9.,]+)\\s*</");val m=p.matcher(raw);while(m.find()){val a=m.group(1)?.replace(",",".")?.toDoubleOrNull();val b=m.group(2)?.replace(",",".")?.toDoubleOrNull();if(a!=null&&b!=null)out+=a to b};return out}
 private fun soap(action:String,params:Map<String,Any>):String{
  val env=buildString{append("""<?xml version="1.0" encoding="utf-8"?>""");append("""<soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">""");append("<soap:Body><");append(action);append(" xmlns=\"");append(SOAP_NS);append("\">");params.forEach{(k,v)->append("<").append(k).append(">");append(xmlEscape(v.toString()));append("</").append(k).append(">")};append("</").append(action).append("></soap:Body></soap:Envelope>")}
  val c=(URL(BASE+action).openConnection() as HttpURLConnection).apply{requestMethod="POST";connectTimeout=TIMEOUT;readTimeout=TIMEOUT;doOutput=true;setRequestProperty("Content-Type","text/xml; charset=utf-8");setRequestProperty("SOAPAction","\""+SOAP_NS+action+"\"");setRequestProperty("Accept","text/xml, application/xml, application/json, */*");setRequestProperty("User-Agent","TU-COLECTIVO-2.0")}
  return try{c.outputStream.use{it.write(env.toByteArray(StandardCharsets.UTF_8))};val s=if(c.responseCode in 200..299)c.inputStream else c.errorStream;BufferedReader(InputStreamReader(s,StandardCharsets.UTF_8)).use{it.readText()}}finally{c.disconnect()}
 }
 private fun parseObjects(raw:String,vararg names:String):List<JSONObject>{val out=mutableListOf<JSONObject>();try{collectJsonObjects(JSONObject(raw),out)}catch(_:Exception){};if(out.isNotEmpty())return out;val code=names[0];val textNames=names.drop(1).joinToString("|");val rx="(?is)<(?:"+code+"|"+code.lowercase()+")\\b[^>]*>\\s*([^<]+).*?<(?:"+textNames+")\\b[^>]*>\\s*([^<]+)";val m=Pattern.compile(rx).matcher(raw);while(m.find())out+=JSONObject().put(code,m.group(1).trim()).put(names.getOrElse(1){"DescripcionLinea"},m.group(2).trim());return out}
 private fun parseStops(raw:String,line:Int):List<TransitStop>{val out=mutableListOf<TransitStop>();val m=Pattern.compile("(?is)<(?:Parada|parada)[^>]*>(.*?)</(?:Parada|parada)>").matcher(raw);while(m.find()){val x=m.group(1)?:continue;val c=tagInt(x,"CodigoParada","codigoParada")?:continue;out+=TransitStop(c,tag(x,"DescripcionParada","descripcionParada")?:"Parada "+c,tag(x,"IdentificadorParada","identificadorParada")?:c.toString(),tagDouble(x,"Latitud","latitud")?:0.0,tagDouble(x,"Longitud","longitud")?:0.0,tag(x,"NombreCalle","nombreCalle").orEmpty(),tag(x,"InterseccionCalle","inteserccionCalle").orEmpty(),line)};return out.distinctBy{it.code to it.identifier}}
 private fun parseArrivals(raw:String):List<TransitArrival>{val out=mutableListOf<TransitArrival>();val m=Pattern.compile("(?is)<(?:.*?:)?(?:ArriboParada|Arribo)[^>]*>(.*?)</(?:.*?:)?(?:ArriboParada|Arribo)>").matcher(raw);while(m.find()){val x=m.group(1)?:continue;out+=TransitArrival(tag(x,"DescripcionLinea","descripcionLinea","Linea","linea").orEmpty(),tag(x,"Destino","destino","Bandera","bandera").orEmpty(),tagInt(x,"Minutos","minutos","Tiempo","tiempo","MinutosArribo","minutosArribo"),tag(x,"Estado","estado").orEmpty())};return out}
 private fun collectJsonObjects(v:Any?,out:MutableList<JSONObject>){when(v){is JSONObject->{var ok=false;v.keys().forEach{if(it.equals("CodigoLinea",true)||it.equals("CodigoParada",true)||it.equals("CodigoCalle",true))ok=true};if(ok)out+=v;v.keys().forEach{collectJsonObjects(v.opt(it),out)}};is JSONArray->for(i in 0 until v.length())collectJsonObjects(v.opt(i),out)}}
 private fun intValue(o:JSONObject,vararg n:String):Int?=n.firstNotNullOfOrNull{o.optString(it,"").toIntOrNull()}
 private fun stringValue(o:JSONObject,vararg n:String):String?=n.firstNotNullOfOrNull{o.optString(it,"").takeIf(String::isNotBlank)}
 private fun tag(raw:String,vararg n:String):String?{for(x in n){val m=Pattern.compile("(?is)<(?:.*?:)?"+x+"\\b[^>]*>\\s*([^<]+)").matcher(raw);if(m.find())return m.group(1).trim()};return null}
 private fun tagInt(raw:String,vararg n:String):Int?=tag(raw,*n)?.replace(",",".")?.toDoubleOrNull()?.toInt()
 private fun tagDouble(raw:String,vararg n:String):Double?=tag(raw,*n)?.replace(",",".")?.toDoubleOrNull()
 private fun xmlEscape(v:String)=v.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;")
}