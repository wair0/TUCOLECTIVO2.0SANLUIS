package com.transpuntano.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.transpuntano.app.data.SmartMoveApi
import com.transpuntano.app.model.*
import com.transpuntano.app.ui.CyberMapView
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
 private val api=SmartMoveApi(); private val ex=Executors.newFixedThreadPool(3)
 private lateinit var content:FrameLayout; private lateinit var title:TextView; private lateinit var status:TextView; private lateinit var nav:LinearLayout
 private val cyan=0xFF00F0FF.toInt(); private val pink=0xFFFF2DB2.toInt(); private val bg=0xFF05070C.toInt(); private val panel=0xFF0B1018.toInt(); private val muted=0xFF8CA5B5.toInt()
 override fun onCreate(b:Bundle?){super.onCreate(b);shell();home()}
 private fun shell(){
  val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(bg)}
  val h=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(d(20),d(16),d(20),d(10));setBackgroundColor(0xFF080C13.toInt())}
  h.addView(TextView(this).apply{text="TU COLECTIVO 2.0";textSize=24f;typeface=Typeface.MONOSPACE;setTextColor(cyan)})
  title=TextView(this).apply{text="CENTRO DE MOVILIDAD";textSize=11f;setTextColor(muted)};h.addView(title)
  status=TextView(this).apply{text="● SISTEMA LISTO";textSize=10f;setTextColor(0xFF55FFB0.toInt())};h.addView(status);root.addView(h)
  content=FrameLayout(this);root.addView(content,LinearLayout.LayoutParams(-1,0,1f))
  nav=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setBackgroundColor(0xFF080C13.toInt())};root.addView(nav,LinearLayout.LayoutParams(-1,d(64)));setContentView(root);nav(0)
 }
 private fun nav(a:Int){nav.removeAllViews();listOf("⌂\nINICIO","▤\nLÍNEAS","★\nFAVORITOS","◎\nCERCA").forEachIndexed{i,s->nav.addView(TextView(this).apply{text=s;gravity=Gravity.CENTER;textSize=10f;setTextColor(if(i==a)cyan else muted);setOnClickListener{when(i){0->home();1->lines();2->favorites();3->nearby()}}},LinearLayout.LayoutParams(0,-1,1f))}}
 private fun home(){title.text="CENTRO DE MOVILIDAD";nav(0);content.removeAllViews();val b=box();b.addView(TextView(this).apply{text="RED TRANSPUNTANO";textSize=12f;setTextColor(pink)});b.addView(TextView(this).apply{text="MOVETE\nSIN PERDER TIEMPO.";textSize=30f;typeface=Typeface.MONOSPACE;setTextColor(Color.WHITE);setPadding(0,d(8),0,d(18))});b.addView(panel("NÚCLEO NATIVO","Interfaz propia. Líneas, paradas, arribos, GPS, favoritos y recorridos."));b.addView(btn("SINCRONIZAR LÍNEAS",cyan){loadLines()});content.addView(ScrollView(this).apply{addView(b)})}
 private fun loadLines(){status.text="● SINCRONIZANDO...";ex.execute{runCatching{api.getLines()}.onSuccess{x->runOnUiThread{status.text="● "+x.size+" LÍNEAS";lines(x)}}.onFailure{e->runOnUiThread{status.text="● SIN CONEXIÓN";toast(e.message?:"Error")}}}}
 private fun lines(initial:List<TransitLine>?=null){title.text="LÍNEAS";nav(1);content.removeAllViews();val b=box();b.addView(panel("CATÁLOGO","Datos solicitados al servicio SmartMove."));val l=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};b.addView(l)
  fun draw(xs:List<TransitLine>){l.removeAllViews();xs.forEach{x->l.addView(card(x.name.uppercase(),"LÍNEA "+x.code){line(x)},LinearLayout.LayoutParams(-1,d(72)).apply{bottomMargin=d(8)})};if(xs.isEmpty())l.addView(panel("SIN DATOS","No se encontraron líneas."))}
  if(initial!=null)draw(initial)else ex.execute{runCatching{api.getLines()}.onSuccess{x->runOnUiThread{draw(x);status.text="● "+x.size+" LÍNEAS"}}.onFailure{e->runOnUiThread{l.addView(panel("ERROR",e.message?:"No se pudo consultar."))}}}
  b.addView(btn("ACTUALIZAR",cyan){loadLines()});content.addView(ScrollView(this).apply{addView(b)})}
 private fun line(x:TransitLine){title.text="LÍNEA "+x.code;content.removeAllViews();val b=box();b.addView(TextView(this).apply{text=x.name.uppercase();textSize=25f;typeface=Typeface.MONOSPACE;setTextColor(cyan)});b.addView(panel("CALLES","Seleccioná una calle para continuar."));val l=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};b.addView(l);ex.execute{runCatching{api.getStreets(x.code)}.onSuccess{xs->runOnUiThread{xs.forEach{s->l.addView(card(s.name,"VER INTERSECCIONES"){intersections(x,s)})}}.onFailure{e->runOnUiThread{l.addView(panel("ERROR",e.message?:"Sin datos"))}}};b.addView(btn("MAPA DEL RECORRIDO",pink){map(x)});content.addView(ScrollView(this).apply{addView(b)})}
 private fun intersections(x:TransitLine,s:TransitStreet){content.removeAllViews();title.text=s.name;val b=box();b.addView(panel("INTERSECCIONES","LÍNEA "+x.code+" · "+s.name));val l=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};b.addView(l);ex.execute{runCatching{api.getIntersections(x.code,s.code)}.onSuccess{xs->runOnUiThread{xs.forEach{i->l.addView(card(i.name,"VER PARADAS"){stops(x,s,i)})}}}.onFailure{e->runOnUiThread{l.addView(panel("ERROR",e.message?:"Sin datos"))}}};content.addView(ScrollView(this).apply{addView(b)})}
 private fun stops(x:TransitLine,s:TransitStreet,i:TransitIntersection){content.removeAllViews();title.text="PARADAS";val b=box();b.addView(panel("PARADAS","LÍNEA "+x.code+" · "+i.name));val l=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};b.addView(l);ex.execute{runCatching{api.getStops(x.code,s.code,i.code)}.onSuccess{xs->runOnUiThread{xs.forEach{p->l.addView(card("🚏 "+p.description,p.street+" "+p.intersection){arrivals(p,x)})}}}.onFailure{e->runOnUiThread{l.addView(panel("ERROR",e.message?:"Sin datos"))}}};content.addView(ScrollView(this).apply{addView(b)})}
 private fun arrivals(p:TransitStop,x:TransitLine){content.removeAllViews();title.text="ARRIBOS";val b=box();b.addView(panel("🚏 "+p.description,"LÍNEA "+x.code+" · ID "+p.identifier));val l=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};b.addView(l);b.addView(btn("ACTUALIZAR ARRIBOS",cyan){arrive(p,x,l)});b.addView(btn("☆ GUARDAR PARADA",pink){save(p,x);toast("Parada guardada")});content.addView(ScrollView(this).apply{addView(b)});arrive(p,x,l)}
 private fun arrive(p:TransitStop,x:TransitLine,l:LinearLayout){l.removeAllViews();l.addView(panel("LIVE","Consultando próximos arribos..."));ex.execute{runCatching{api.getArrivals(p.identifier,x.code)}.onSuccess{xs->runOnUiThread{l.removeAllViews();xs.forEach{a->l.addView(card((if(a.line.isBlank())"LÍNEA "+x.code else a.line)+" · "+(a.minutes?.toString()?:"--")+" MIN",a.destination){})};if(xs.isEmpty())l.addView(panel("SIN ARRIBOS","El servicio no devolvió datos."))}}.onFailure{e->runOnUiThread{l.removeAllViews();l.addView(panel("ERROR",e.message?:"Sin conexión"))}}}}
 private fun nearby(){title.text="PARADAS CERCANAS";nav(3);content.removeAllViews();val b=box();b.addView(panel("RADAR LOCAL","Buscá paradas próximas a tu ubicación."));val l=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};b.addView(l);b.addView(btn("BUSCAR PARADAS CERCANAS",cyan){nearbyLoad(l)});content.addView(ScrollView(this).apply{addView(b)})}
 private fun nearbyLoad(l:LinearLayout){if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION),42);return};val m=getSystemService(Context.LOCATION_SERVICE)as LocationManager;val p=m.getLastKnownLocation(LocationManager.GPS_PROVIDER)?:m.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)?:return toast("No hay ubicación");ex.execute{runCatching{api.getNearby(p.latitude,p.longitude)}.onSuccess{xs->runOnUiThread{l.removeAllViews();xs.take(30).forEach{s->l.addView(card("🚏 "+s.description,s.street+" "+s.intersection){})}}}.onFailure{e->runOnUiThread{toast(e.message?:"Error")}}}}
 private fun favorites(){title.text="FAVORITOS";nav(2);content.removeAllViews();val b=box();val s=getSharedPreferences("favorites",0).getStringSet("stops",emptySet()).orEmpty();b.addView(panel("MIS PARADAS",if(s.isEmpty())"No hay favoritos." else "Guardados localmente."));s.forEach{x->b.addView(card(x,"GUARDADO"){})};content.addView(ScrollView(this).apply{addView(b)})}
 private fun save(p:TransitStop,x:TransitLine){val q=getSharedPreferences("favorites",0);val s=q.getStringSet("stops",emptySet())?.toMutableSet()?:mutableSetOf();s.add("Línea "+x.code+" · "+p.description);q.edit().putStringSet("stops",s).apply()}
 private fun map(x:TransitLine?){title.text="MAPA";content.removeAllViews();val f=FrameLayout(this);val v=CyberMapView(this);f.addView(v);content.addView(f);if(x!=null)ex.execute{runCatching{api.getRoute(x.code)}.onSuccess{r->runOnUiThread{v.setRoute(r)}}}}
 private fun box()=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(d(18),d(16),d(18),d(24))}
 private fun card(a:String,b:String,go:()->Unit)=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(d(16),d(12),d(16),d(12));setBackgroundColor(panel);setOnClickListener{go()};addView(TextView(this@MainActivity).apply{text=a;textSize=14f;typeface=Typeface.MONOSPACE;setTextColor(Color.WHITE)});addView(TextView(this@MainActivity).apply{text=b;textSize=10f;setTextColor(muted)})}
 private fun panel(a:String,b:String)=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(d(16),d(14),d(16),d(14));setBackgroundColor(panel);addView(TextView(this@MainActivity).apply{text=a;textSize=11f;typeface=Typeface.MONOSPACE;setTextColor(cyan)});addView(TextView(this@MainActivity).apply{text=b;textSize=13f;setTextColor(muted)})}
 private fun btn(t:String,c:Int,go:()->Unit)=TextView(this).apply{text=t;gravity=Gravity.CENTER;textSize=12f;typeface=Typeface.MONOSPACE;setTextColor(c);setBackgroundColor(panel);setPadding(0,d(14),0,d(14));setOnClickListener{go()}}
 private fun d(v:Int)=(v*resources.displayMetrics.density).toInt()
 private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
 override fun onDestroy(){ex.shutdownNow();super.onDestroy()}
}