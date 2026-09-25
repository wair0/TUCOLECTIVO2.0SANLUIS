package com.transpuntano.app.ui

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import java.net.HttpURLConnection
import java.net.URL
import java.util.LinkedHashMap
import kotlin.math.*

data class MapStop(val id:Int,val title:String,val subtitle:String,val latitude:Double,val longitude:Double)

class CyberMapView(context:Context):View(context){
 private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
 private val cache=object:LinkedHashMap<String,Bitmap>(64,.75f,true){override fun removeEldestEntry(e:MutableMap.MutableEntry<String,Bitmap>?)=size>60}
 private var route=emptyList<Pair<Double,Double>>();private var stops=emptyList<MapStop>();private var user:Pair<Double,Double>?=null
 private var lat=-33.3017;private var lon=-66.3378;private var zoom=13;private var downX=0f;private var downY=0f;private var moved=false
 private var tap:((MapStop)->Unit)?=null
 private val inFlight=HashSet<String>()
 private val scaleDetector=ScaleGestureDetector(context,object:ScaleGestureDetector.SimpleOnScaleGestureListener(){override fun onScale(d:ScaleGestureDetector):Boolean{val old=zoom;zoom=(zoom+if(d.scaleFactor>1f)1 else -1).coerceIn(11,18);if(zoom!=old)invalidate();return true}})

 fun setRoute(v:List<Pair<Double,Double>>){route=v;if(v.isNotEmpty())fit(v);invalidate()}
 fun setStops(v:List<MapStop>,fit:Boolean=true){stops=v;if(fit&&v.isNotEmpty())fit(v.map{it.latitude to it.longitude});invalidate()}
 fun setUserLocation(a:Double,b:Double,center:Boolean=true){user=a to b;if(center){lat=a;lon=b};invalidate()}
 fun setOnStopTap(v:(MapStop)->Unit){tap=v}

 private fun fit(v:List<Pair<Double,Double>>){val a=v.minOf{it.first};val b=v.maxOf{it.first};val c=v.minOf{it.second};val d=v.maxOf{it.second};lat=(a+b)/2;lon=(c+d)/2;val s=max(b-a,(d-c)*cos(Math.toRadians(lat)));zoom=when{ s<.002->17;s<.005->16;s<.012->15;s<.025->14;s<.06->13;s<.12->12;else->11}}
 override fun onDraw(c:Canvas){super.onDraw(c);c.drawColor(Color.rgb(5,7,12));tiles(c);grid(c);line(c);stops(c);user(c);overlay(c)}

 private fun world(a:Double,b:Double,z:Int):Pair<Double,Double>{val n=2.0.pow(z);val x=(b+180)/360*256*n;val s=sin(Math.toRadians(a)).coerceIn(-.9999,.9999);val y=(.5-ln((1+s)/(1-s))/(4*Math.PI))*256*n;return x to y}
 private fun screen(a:Double,b:Double):PointF{val q=world(lat,lon,zoom);val p=world(a,b,zoom);return PointF((p.first-q.first+width/2).toFloat(),(p.second-q.second+height/2).toFloat())}

 private fun tiles(c:Canvas){
  val q=world(lat,lon,zoom);val left=q.first-width/2;val top=q.second-height/2;val minX=floor(left/256).toInt()-1;val maxX=floor((left+width)/256).toInt()+1;val minY=floor(top/256).toInt()-1;val maxY=floor((top+height)/256).toInt()+1;val n=1 shl zoom
  for(rx in minX..maxX)for(y in minY..maxY){if(y !in 0 until n)continue;val x=((rx%n)+n)%n;val key="$zoom/$x/$y";val bm=synchronized(cache){cache[key]};val dx=(rx*256-left).toFloat();val dy=(y*256-top).toFloat()
   if(bm!=null)c.drawBitmap(bm,null,RectF(dx,dy,dx+256,dy+256),paint)else{paint.color=Color.rgb(10,17,23);c.drawRect(dx,dy,dx+256,dy+256,paint);download(key,x,y,zoom)}
  }
 }
 private fun download(key:String,x:Int,y:Int,z:Int){synchronized(cache){if(cache.containsKey(key)||!inFlight.add(key))return};Thread{runCatching{val h=URL("https://tile.openstreetmap.org/$z/$x/$y.png").openConnection() as HttpURLConnection;h.connectTimeout=6000;h.readTimeout=6000;h.setRequestProperty("User-Agent","TU-COLECTIVO-2.0 Android");h.inputStream.use{BitmapFactory.decodeStream(it)}}.getOrNull()?.let{b->synchronized(cache){cache[key]=b};postInvalidate()}}.start()}

 private fun grid(c:Canvas){paint.style=Paint.Style.STROKE;paint.strokeWidth=1f;paint.color=Color.argb(45,0,240,255);var x=0f;while(x<width){c.drawLine(x,0f,x,height.toFloat(),paint);x+=80};var y=0f;while(y<height){c.drawLine(0f,y,width.toFloat(),y,paint);y+=80}}
 private fun line(c:Canvas){if(route.size<2)return;paint.style=Paint.Style.STROKE;paint.strokeCap=Paint.Cap.ROUND;paint.strokeWidth=11f;paint.color=Color.argb(100,0,240,255);path(c,route);paint.strokeWidth=4f;paint.color=Color.rgb(0,240,255);path(c,route)}
 private fun path(c:Canvas,v:List<Pair<Double,Double>>){val p=Path();v.forEachIndexed{i,x->val q=screen(x.first,x.second);if(i==0)p.moveTo(q.x,q.y)else p.lineTo(q.x,q.y)};c.drawPath(p,paint)}
 private fun stops(c:Canvas){stops.forEach{s->val p=screen(s.latitude,s.longitude);if(p.x !in -20f..width+20f||p.y !in -20f..height+20f)return@forEach;paint.style=Paint.Style.FILL;paint.color=Color.argb(90,255,45,178);c.drawCircle(p.x,p.y,11f,paint);paint.color=Color.rgb(255,45,178);c.drawCircle(p.x,p.y,5f,paint);paint.style=Paint.Style.STROKE;paint.color=Color.WHITE;c.drawCircle(p.x,p.y,7f,paint)}}
 private fun user(c:Canvas){val u=user?:return;val p=screen(u.first,u.second);paint.style=Paint.Style.STROKE;paint.strokeWidth=2f;paint.color=Color.rgb(0,240,255);c.drawCircle(p.x,p.y,18f,paint);paint.style=Paint.Style.FILL;c.drawCircle(p.x,p.y,6f,paint)}
 private fun overlay(c:Canvas){paint.style=Paint.Style.FILL;paint.color=Color.argb(210,5,7,12);c.drawRect(0f,0f,width.toFloat(),44f,paint);paint.typeface=Typeface.MONOSPACE;paint.textSize=11f;paint.color=Color.rgb(0,240,255);c.drawText("LIVE MAP • OSM • Z$zoom",14f,27f,paint);paint.textSize=9f;paint.color=Color.WHITE;c.drawText("© OpenStreetMap contributors",14f,height-10f,paint)}
 override fun onTouchEvent(e:MotionEvent):Boolean{when(e.action){MotionEvent.ACTION_DOWN->{downX=e.x;downY=e.y;moved=false;return true};MotionEvent.ACTION_MOVE->{if(e.pointerCount>1)return true;val dx=e.x-downX;val dy=e.y-downY;if(abs(dx)+abs(dy)>3)moved=true;val q=world(lat,lon,zoom);val n=unworld(q.first-dx,q.second-dy,zoom);lat=n.first.coerceIn(-85.0,85.0);lon=n.second;downX=e.x;downY=e.y;invalidate();return true};MotionEvent.ACTION_UP->{if(!moved){stops.minByOrNull{val p=screen(it.latitude,it.longitude);hypot(p.x-e.x,p.y-e.y)}}?.let{val p=screen(it.latitude,it.longitude);if(hypot(p.x-e.x,p.y-e.y)<32)tap?.invoke(it)};return true}};return true}
 private fun unworld(x:Double,y:Double,z:Int):Pair<Double,Double>{val n=Math.PI-2*Math.PI*y/(256*2.0.pow(z));return Math.toDegrees(atan(sinh(n))) to (x/(256*2.0.pow(z))*360-180)}
}