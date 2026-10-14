import base64
TH={'light':{'acc':(37,99,235),'alert':(225,29,72),'a1':.46,'a2':.24,'pc':'rgb(14,60,200)','pg':'rgba(37,99,235,.30)','pca':'rgb(170,0,40)','pga':'rgba(225,29,72,.30)'},
    'dark':{'acc':(34,211,238),'alert':(251,113,133),'a1':.55,'a2':.28,'pc':'rgb(200,253,255)','pg':'rgba(34,211,238,.38)','pca':'rgb(255,222,228)','pga':'rgba(251,113,133,.40)'}}
def col(c,a): return "rgba(%d,%d,%d,%s)"%(c[0],c[1],c[2],a)
class S:
    def __init__(s,w,h,c1,c2,cp='#fff',cg='rgba(255,255,255,.3)'): s.w,s.h,s.c1,s.c2,s.cp,s.cg,s.b,s.anim=w,h,c1,c2,cp,cg,[],False
    def pulse(s,d,dur=3.2,delay=0):
        s.anim=True;st='style="--e:%d;animation-duration:%ss;animation-delay:%ss"'%(-(plen(d)+18),dur,-delay)
        s.b.append('<path class="pg" d="%s" stroke="%s" %s/><path class="pu" d="%s" stroke="%s" %s/>'%(d,s.cg,st,d,s.cp,st))
    def halo(s,x,y,dur=3.2,delay=0):
        s.anim=True;s.b.append('<circle class="ph" cx="%s" cy="%s" r="4.4" fill="%s" stroke="none" style="animation-duration:%ss;animation-delay:%ss"/>'%(x,y,s.cg,dur,-delay))
    def path(s,d,soft=False,sw=1.1): s.b.append('<path d="%s" stroke="%s" stroke-width="%s"/>'%(d,s.c2 if soft else s.c1,sw))
    def ring(s,x,y,r=2.2,soft=False):
        c=s.c2 if soft else s.c1
        s.b.append('<circle cx="%s" cy="%s" r="%s" stroke="%s" stroke-width="1"/><circle cx="%s" cy="%s" r=".9" fill="%s" stroke="none"/>'%(x,y,r,c,x,y,c))
    def via(s,x,y,r=1.3,soft=False):
        c=s.c2 if soft else s.c1
        s.b.append('<circle cx="%s" cy="%s" r="%s" fill="%s" stroke="none"/>'%(x,y,r,c))
    def uri(s):
        st=STYLE if s.anim else ''
        svg='<svg xmlns="http://www.w3.org/2000/svg" width="%d" height="%d" viewBox="0 0 %d %d">%s<g fill="none" stroke-linecap="round" stroke-linejoin="round">%s</g></svg>'%(s.w,s.h,s.w,s.h,st,''.join(s.b))
        return 'url("data:image/svg+xml;base64,%s")'%base64.b64encode(svg.encode()).decode()
STYLE='<style>.pg,.pu{fill:none;stroke-linecap:round;stroke-dasharray:18 900;animation:dash 3s linear infinite}.pg{stroke-width:4.5}.pu{stroke-width:1.8}.ph{opacity:0;animation:halo 3s linear infinite}@keyframes dash{0%{stroke-dashoffset:18}55%{stroke-dashoffset:var(--e)}100%{stroke-dashoffset:var(--e)}}@keyframes halo{0%,50%{opacity:0}58%{opacity:1}80%,100%{opacity:0}}@media (prefers-reduced-motion:reduce){.pg,.pu,.ph{animation:none;display:none}}</style>'
def plen(d):
    import re,math
    toks=re.findall(r'[MLHVZ]|-?[\d.]+',d);i=0;cmd=None;x=y=0.0;L=0.0
    while i<len(toks):
        t=toks[i]
        if t in 'MLHVZ': cmd=t;i+=1;continue
        if cmd=='M': x,y=float(toks[i]),float(toks[i+1]);i+=2;cmd='L'
        elif cmd=='L': nx,ny=float(toks[i]),float(toks[i+1]);L+=math.hypot(nx-x,ny-y);x,y=nx,ny;i+=2
        elif cmd=='H': nx=float(toks[i]);L+=abs(nx-x);x=nx;i+=1
        elif cmd=='V': ny=float(toks[i]);L+=abs(ny-y);y=ny;i+=1
        else: i+=1
    return L
def mirror(d,W):
    # espeja una ruta con M/L/H/V absolutas respecto al ancho W
    import re
    out=[];toks=re.findall(r'[MLHVZ]|-?[\d.]+',d);cmd=None;i=0
    while i<len(toks):
        t=toks[i]
        if t in 'MLHVZ': cmd=t;out.append(t);i+=1;continue
        if cmd in 'ML': out.append("%g,%s"%(W-float(toks[i]),toks[i+1]));i+=2
        elif cmd=='H': out.append("%g"%(W-float(toks[i])));i+=1
        else: out.append(toks[i]);i+=1
    return " ".join(out)
LAY=lambda fn,w,h,size,pos,rep='no-repeat':dict(fn=fn,w=w,h=h,size=size,pos=pos,rep=rep)
# ---------- HEADER (80 px; centro del titulo = 50% - 24 px; titulo c-77.5..c+77.5) ----------
def hdelay(L,D): return 0.58*D-0.55*D*L/(L+36.0)
def header_layers():
    W=1000
    def Bf(s,mir=False):
        P=lambda d: mirror(d,W) if mir else d
        X=lambda x: W-x if mir else x
        k=1.4 if mir else 0
        s.path(P("M0,25.5 H968")); s.path(P("M971,17 H968 V34 H971")); s.via(X(968),25.5,r=1.5)
        s.path(P("M0,54 H1000")); s.via(X(1000),54,r=1.2)
        s.path(P("M0,10 H930 L940,0"),soft=True); s.via(X(930),10,soft=True)
        s.path(P("M0,70 H900 L906,64 H925"),soft=True)
    def Al(s):
        s.path("M57,40 H62 L76.5,25.5 H160"); s.path("M57,40 H62 L76,54 H160"); s.ring(57,40,r=2.4)
    def Ar(s):
        s.path("M56,40 H51 L36.5,25.5 H0"); s.path("M56,40 H51 L37,54 H0"); s.ring(56,40,r=2.4)
    L=[LAY(lambda s:Bf(s,False),1000,80,"1000px 80px","calc(50% - 576px) 0"),
       LAY(lambda s:Bf(s,True),1000,80,"1000px 80px","calc(50% + 528px) 0"),
       LAY(Al,160,80,"160px 80px","0 0"),LAY(Ar,160,80,"160px 80px","100% 0")]
    def tile(s):
        s.path("M0,72.5 H20 L25,67.5 H43 L48,72.5 H56",soft=True); s.via(20,72.5,soft=True); s.via(48,72.5,soft=True)
    L.append(LAY(tile,56,80,"56px 80px","0 0","repeat-x"))
    return L
# ---------- BARRA INFERIOR (78 px; etiquetas y=63; iconos hasta y~53) ----------
HALF=[20.5,20.5,13.5,24,31]
def nav_layers():
    L=[]
    for i,h in enumerate(HALF):
        c=(2*i+1)/10.0
        def f(s,h=h,i=i):
            xl=48-h-4; xr=48+h+4; D=2.4; k=i*0.5
            s.path("M0,63 H%g"%xl); s.path("M%g,57 H%g V69 H%g"%(xl+3,xl,xl+3)); s.via(xl,63,r=1.4)
            s.path("M96,63 H%g"%xr); s.path("M%g,57 H%g V69 H%g"%(xr-3,xr,xr-3)); s.via(xr,63,r=1.4)
            s.path("M48,40 V56"); s.via(48,56.5,r=1.3)
            s.pulse("M0,63 H%g"%xl,D,k); s.halo(xl,63,D,hdelay(xl,D)+k)
            s.pulse("M96,63 H%g"%xr,D,k+1.2); s.halo(xr,63,D,hdelay(96-xr,D)+k+1.2)
            s.pulse("M48,40 V56",D,k+0.6); s.halo(48,56.5,D,hdelay(16,D)+k+0.6)
        L.append(LAY(f,96,78,"96px 78px","calc(%g%% + %gpx) 0"%(c*100,round(96*(c-.5),2))))
    def tile(s):
        s.path("M0,48 H22 L26,44 H38 L42,48 H64",soft=True); s.via(22,48,soft=True); s.via(42,48,soft=True)
    L.append(LAY(tile,64,78,"64px 78px","0 0","repeat-x"))
    return L
# ---------- PANELES del header (menu / buscar / notificaciones) ----------
def panel_layers():
    def a(s): s.path("M22,0 V28 L12,38 V90"); s.ring(12,96,r=2); s.path("M22,40 V64",soft=True); s.via(22,64,soft=True)
    def b(s): s.path("M0,55 H40 L46,59 H96"); s.via(40,55); s.ring(100,59,r=1.8,soft=True)
    return [LAY(a,24,110,"24px 110px","100% 0"),LAY(b,120,60,"120px 60px","0 100%")]
# ---------- TARJETAS DE INICIO (173x160) ----------
def home_layers():
    def h1(s):
        s.path("M13,37 H6 V141 L16,151"); s.ring(6,90); s.via(6,60,soft=True)
        s.path("M49,16 V8 L56,1",soft=True); s.via(49,16,soft=True)
    def under(s):
        s.path("M0,151.5 H36"); s.via(30,151.5,r=1.2)
    def right(s):
        s.path("M36,58 H22 L14,66 V108 L22,116 H36",soft=True); s.ring(14,87,r=2,soft=True)
    return [LAY(h1,80,160,"80px 160px","0 0"),LAY(under,36,160,"36px 160px","0 0","repeat-x"),LAY(right,36,160,"36px 160px","100% 0")]
def sync_layers():
    def s1(s):
        s.path("M62,38 H64 L67,35 V30 L69.5,27.5 H71.5"); s.ring(73,27.5,r=1.8)
        s.path("M64,38 L67,41 V47 L69.5,49.5 V50 L71.5,52.5"); s.ring(73,52.5,r=1.8)
        s.path("M38,14 V3 L44,0",soft=True)
    def under(s): s.path("M0,69.5 H40"); s.via(33,69.5,r=1.2)
    def right(s):
        s.path("M60,27 H49"); s.ring(47.5,27,r=1.8); s.path("M60,52.5 H49"); s.ring(47.5,52.5,r=1.8)
        s.path("M56,27 V52.5",soft=True)
    return [LAY(s1,90,76,"90px 76px","0 0"),LAY(under,40,76,"40px 76px","0 0","repeat-x"),LAY(right,60,76,"60px 76px","100% 0")]
# ---------- LINEAS ----------
def linecard_layers():
    def l1(s):
        s.path("M55,39 H57 L60,36 V34 L62,32 H62.5"); s.ring(63,31.5,r=1.7)
        s.path("M57,39 L60,42 V47 L62,49.5 V50 L62.5,50.5"); s.ring(63,50.5,r=1.7)
        s.path("M33,17 V6 L27,0",soft=True); s.via(33,17,soft=True)
    def under(s): s.path("M0,69.5 H30"); s.via(24,69.5,r=1.2)
    return [LAY(l1,70,78,"70px 78px","0 0"),LAY(under,30,78,"30px 78px","0 0","repeat-x")]
def detail_layers():
    def d1(s):
        s.path("M0,10.5 H12"); s.ring(6,10.5,r=1.8); s.path("M26,10.5 H31.5"); s.ring(33,10.5,r=1.7)
    def under(s): s.path("M0,5.5 H30",soft=True); s.via(24,5.5,r=1.1,soft=True)
    return [LAY(d1,40,21,"40px 21px","0 50%"),LAY(under,30,8,"30px 8px","0 100%","repeat-x")]
def arrival_layers(ph=0,arriving=False):
    k=ph*1.7; E=994 if arriving else 997
    MAIN="M0,48 H870 L874,44 H900 L904,48 H%d"%E
    PUL="M640,48 H870 L874,44 H900 L904,48 H%d"%E
    def l(s):
        s.path("M6,0 V120"); D=3.6
        for y in (16.5,34,57.5,75): s.path("M6,%g H10"%y); s.ring(11.5,y,r=1.7)
        s.via(6,48,r=1.6)
        s.pulse("M6,0 V118",D,k)
        for y in (16.5,34,57.5,75): s.halo(11.5,y,D,0.58*D-0.55*D*y/154.0+k)
    def m(s):
        s.path(MAIN); s.via(874,44,soft=True); s.via(904,48,soft=True); s.ring(E,48,r=2.4)
        D=3.4; Lp=plen(PUL)
        s.pulse(PUL,D,k+0.3); s.halo(E,48,D,0.58*D-0.55*D*Lp/(Lp+36.0)+k+0.3)
    def r(s):
        if arriving:
            s.path("M62,43 V38 H124 V43"); s.path("M62,53 V58 H124 V53")
            s.via(62,38,r=1.2); s.via(124,38,r=1.2); s.via(62,58,r=1.2); s.via(124,58,r=1.2)
            s.pulse("M62,38 H124",2.6,k); s.pulse("M124,58 H62",2.6,k+1.3)
        else:
            s.path("M93,13 V0",soft=True); s.path("M93,83 V120",soft=True); s.via(93,13,soft=True); s.via(93,83,soft=True)
            s.pulse("M93,13 V0",3.2,k+0.4); s.pulse("M93,83 V120",3.2,k+1.5)
    return [LAY(l,24,120,"24px 120px","0 0"),LAY(m,1000,120,"1000px 120px","calc(100%% - %dpx) 0"%(76 if arriving else 75)),LAY(r,140,120,"140px 120px","100% 0")]
def stop_layers():
    def l(s):
        s.path("M7,0 V74"); 
        for y in (26,51.5): s.path("M7,%g H10"%y); s.ring(11.5,y,r=1.7)
    def r(s): s.path("M80,0 V10 L68,22 H46",soft=True); s.ring(44,22,r=1.9,soft=True)
    return [LAY(l,24,74,"24px 74px","0 0"),LAY(r,80,74,"80px 74px","100% 0")]
# ---------- SECCIONES: MAPA, PARADAS CERCANAS, ARRIBOS (paradas) y FAVORITOS ----------
def under(w,h,y,pos):
    def f(s): s.path("M0,%g H%g"%(y,w)); s.via(w-7,y,r=1.2)
    return LAY(f,w,h,"%dpx %dpx"%(w,h),pos,"repeat-x")
def busl(rows,H=400,w=24):
    def f(s):
        s.path("M8,0 V%d"%H)
        for y in rows: s.path("M8,%g H13.5"%y); s.ring(15.5,y,r=1.7)
    return LAY(f,w,H,"%dpx %dpx"%(w,H),"0 0")
def corners(w=110,h=24):
    d="M0,12 H34 L42,4 H88"; e="M0,12 H34 L42,20 H88"
    def tl(s): s.path(d); s.via(34,12); s.ring(88,4,soft=True)
    def tr(s): s.path(mirror(d,w)); s.via(w-34,12); s.ring(w-88,4,soft=True)
    def bl(s): s.path(e); s.via(34,12); s.ring(88,20,soft=True)
    def br(s): s.path(mirror(e,w)); s.via(w-34,12); s.ring(w-88,20,soft=True)
    return [LAY(tl,w,h,"%dpx %dpx"%(w,h),"0 0"),LAY(tr,w,h,"%dpx %dpx"%(w,h),"100% 0"),LAY(bl,w,h,"%dpx %dpx"%(w,h),"0 100%"),LAY(br,w,h,"%dpx %dpx"%(w,h),"100% 100%")]
def hexcard_layers(vx,vy,r1,r2,H,under_y):
    """pistas desde el vertice derecho del hexagono (vx,vy) hasta las dos filas de texto (r1,r2) y subrayado"""
    def a(s):
        s.path("M%g,%g H%g L%g,%g V%g L%g,%g H%g"%(vx,vy,vx+2,vx+5,vy-3,r1+2,vx+7,r1,vx+8)); s.ring(vx+10,r1,r=1.8)
        s.path("M%g,%g H%g L%g,%g V%g L%g,%g H%g"%(vx+2,vy,vx+2,vx+5,vy+3,r2-2,vx+7,r2,vx+8)); s.ring(vx+10,r2,r=1.8)
        s.path("M%g,%g V3 L%g,0"%(vx-24,vy-22,vx-18),soft=True)
    return [LAY(a,110,H,"110px %dpx"%H,"0 0"),under(40,H,under_y,"0 0")]
def locate_layers(): return hexcard_layers(62,38,29,49.8,76,71.5)
def refresh_layers(): return hexcard_layers(60,36,27,53.9,72,67.5)
def nearby_layers():
    def r(s): s.via(13,21.5,r=1.3); s.path("M13,21.5 H44"); s.ring(46.5,21.5,r=1.8); s.path("M13,32 H30 L36,38 H60",soft=True); s.via(60,38,soft=True)
    return [busl([25.5,47.7,76.8],160),LAY(r,100,160,"100px 160px","100% 0"),under(40,8,5.5,"0 100%")]
def favitem_layers():
    def r(s): s.path("M28,34 H18"); s.ring(16,34,r=1.8)
    return [busl([24.5,46.4],120),LAY(r,80,120,"80px 120px","100% 0"),under(40,8,5.5,"0 100%")]
def sched_layers():
    def r(s): s.path("M60,0 V14 L48,26 H24"); s.ring(22,26,r=1.9,soft=True); s.path("M60,40 V90",soft=True); s.via(60,90,soft=True)
    return [busl([28.75,54,113.5,149,192],400),LAY(r,80,120,"80px 120px","100% 0"),under(40,8,5.5,"0 100%")]
def popup_layers(): return [busl([28],400),under(40,8,5.5,"0 100%")]
def arrp_layers(ph=0):
    k=ph*1.7; E=997; D=3.4
    MAIN="M0,38.5 H870 L874,34.5 H900 L904,38.5 H%d"%E
    PUL="M640,38.5 H870 L874,34.5 H900 L904,38.5 H%d"%E
    def l(s):
        s.path("M8,0 V120"); s.via(8,38.5,r=1.6)
        for y in (21.1,54.2): s.path("M8,%g H13.5"%y); s.ring(15.5,y,r=1.7)
        s.pulse("M8,0 V118",3.6,k)
        for y in (21.1,54.2): s.halo(15.5,y,3.6,0.58*3.6-0.55*3.6*y/154.0+k)
    def m(s):
        s.path(MAIN); s.via(874,34.5,soft=True); s.via(904,38.5,soft=True); s.ring(E,38.5,r=2.4)
        Lp=plen(PUL); s.pulse(PUL,D,k+0.3); s.halo(E,38.5,D,0.58*D-0.55*D*Lp/(Lp+36.0)+k+0.3)
    return [LAY(l,24,120,"24px 120px","0 0"),LAY(m,1000,120,"1000px 120px","calc(100% - 80px) 0")]
# ---------- ensamblado ----------
GR180=("linear-gradient(180deg,var(--tcn-bg-a),var(--tcn-bg-b))","100% 100%","0 0","no-repeat")
GR0=("linear-gradient(0deg,var(--tcn-bg-a),var(--tcn-bg-b))","100% 100%","0 0","no-repeat")
GLOW=("radial-gradient(120% 90% at 100% 100%,rgba(var(--tcn-acc),.2),transparent 65%)","100% 100%","0 0","no-repeat")
SELT=("linear-gradient(90deg,rgba(var(--tcn-acc),.2),transparent 55%)","100% 100%","0 0","no-repeat")
ARRT=("linear-gradient(90deg,rgba(var(--tcn-alert),.16),transparent 55%)","100% 100%","0 0","no-repeat")
STOPT=("linear-gradient(90deg,rgba(var(--tcn-acc),.16),transparent 38%)","100% 100%","0 0","no-repeat")
LC=['.line-card','.lineas-grid>[data-line]']; DC=['.line-detail-card']; AC=['.arrival-card']
SPECS=[ # (selectors, pseudo, layers, tails, color)
 (['.tc-hd'],'::before',header_layers(),[GR0],'acc'),
 (['.tc-hd-panel'],'::before',panel_layers(),[GR180],'acc'),
 (['.tc-nav'],'::before',nav_layers(),[GR180],'acc'),
 (['.tc-card'],'::before',home_layers(),[GR180],'acc'),
 (['.tc-card.tc-sync'],'::before',sync_layers(),[GR180],'acc'),
 ([s+'' for s in LC],'::before',linecard_layers(),[GR180],'acc'),
 ([s+'.hot' for s in LC],'::before',linecard_layers(),[GLOW,GR180],'acc'),
 (DC,'::before',detail_layers(),[GR180],'acc'),
 ([s+'.hot' for s in DC],'::before',detail_layers(),[GLOW,GR180],'acc'),
 (['.tc-stop'],'::before',stop_layers(),[STOPT,GR180],'acc'),
 (['html body [data-screen="paradas"] .locate-stops'],'::before',locate_layers(),[GR180],'acc'),
 (['html body [data-screen="mapa"] .map-refresh-card'],'::before',refresh_layers(),[GR180],'acc'),
 (['html body [data-screen="mapa"] .map-stop-popup'],'::before',popup_layers(),[GR180],'acc'),
 (['html body [data-screen="paradas"] .nearby-stop'],'::before',nearby_layers(),[GR180],'acc'),
 (['html body [data-screen="paradas"] .nearby-empty','html body [data-screen="favoritos"] .favorites-empty'],'::before',corners(),[GR180],'acc'),
 (['html body [data-screen="favoritos"] .notification-schedule'],'::before',sched_layers(),[GR180],'acc'),
 (['html body [data-screen="favoritos"] .favorite-item'],'::before',favitem_layers(),[GR180],'acc'),
]
def body(layers,tails,theme,key):
    t=TH[theme];c=t[key];c1=col(c,t['a1']);c2=col(c,t['a2'])
    imgs=[];sz=[];ps=[];rp=[]
    for L in layers:
        s=S(L['w'],L['h'],c1,c2,t['pca' if key=='alert' else 'pc'],t['pga' if key=='alert' else 'pg']);L['fn'](s);imgs.append(s.uri());sz.append(L['size']);ps.append(L['pos']);rp.append(L['rep'])
    for (im,a,b,r) in tails: imgs.append(im);sz.append(a);ps.append(b);rp.append(r)
    return "background-image:%s;background-size:%s;background-position:%s;background-repeat:%s"%(",".join(imgs),",".join(sz),",".join(ps),",".join(rp))
out=['/* TU COLECTIVO 2.0 - Circuitos electronicos (reemplaza la retícula de puntos). Cargar AL FINAL, despues de header.css, bottom-nav.css, home-cards.css, lineas.css y theme-dark.css.',
 '   Pistas con giros de 45 grados, pads y vias dibujadas en SVG (sin imagenes externas) que salen de los iconos y terminan junto a cada texto:',
 '   titulo y chip del header, etiquetas de la barra inferior, nombre de cada tarjeta, barra de SINCRONIZAR LINEAS, filas de arribos y parada.',
 '   Los colores siguen el tema (azul en claro, cian en oscuro; rojo en ARRIBANDO). Fondos siempre opacos. */']
# claro: base ; oscuro: media + data-theme
def sels(lst,ps,pre=''):
    r=[]
    for x in lst:
        if x.startswith('html body ') and pre:
            cond=':not([data-theme="light"])' if 'not' in pre else '[data-theme="dark"]'
            r.append('html%s body %s%s'%(cond,x[len('html body '):],ps))
        else: r.append('%s%s%s'%(pre,x,ps))
    return ",".join(r)
for theme in ('light','dark'):
    for (lst,ps,layers,tails,key) in SPECS:
        b=body(layers,tails,theme,key)
        if theme=='light': out.append("%s{%s}"%(sels(lst,ps),b))
        else:
            out.append('@media (prefers-color-scheme:dark){%s{%s}}'%(sels(lst,ps,':root:not([data-theme="light"]) '),b))
            out.append('%s{%s}'%(sels(lst,ps,':root[data-theme="dark"] '),b))

# ---- tarjetas de arribos: URIs una sola vez por tema/fase/color y reutilizadas con variables CSS ----
def uri_pair(theme,key,ph):
    t=TH[theme];c=t[key];c1=col(c,t['a1']);c2=col(c,t['a2']);res=[]
    for L in arrival_layers(ph,key=='alert'):
        x=S(L['w'],L['h'],c1,c2,t['pca' if key=='alert' else 'pc'],t['pga' if key=='alert' else 'pg']);L['fn'](x);res.append(x.uri())
    return res
def vrule(pre,extra,key,ph,theme):
    l,m,r=uri_pair(theme,key,ph)
    return "%s.arrival-card%s{--ckl:%s;--ckm:%s;--ckr:%s}"%(pre,extra,l,m,r)
for theme in ('light','dark'):
    for key,state in (('acc',''),('alert','.arriving')):
        for ph,nth in ((0,''),(1,':nth-child(even)')):
            extra=state+nth
            if theme=='light': out.append(vrule('',extra,key,ph,theme))
            else:
                out.append('@media (prefers-color-scheme:dark){%s}'%vrule(':root:not([data-theme="light"]) ',extra,key,ph,theme))
                out.append(vrule(':root[data-theme="dark"] ',extra,key,ph,theme))
def arule(sel,tails,mid):
    return "%s::before{background-image:var(--ckl),var(--ckm),var(--ckr),%s;background-size:24px 120px,1000px 120px,140px 120px%s;background-position:0 0,calc(100%% - %dpx) 0,100%% 0%s;background-repeat:no-repeat}"%(sel,",".join(t[0] for t in tails),"".join(",100% 100%" for _ in tails),mid,"".join(",0 0" for _ in tails))
out.append(arule('.arrival-card',[GR180],75))
out.append(arule('.arrival-card.hot',[GLOW,GR180],75))
out.append(arule('.arrival-card.selected',[SELT,GR180],75))
out.append(arule('.arrival-card.arriving',[ARRT,GR180],76))

# ---- ARRIBOS de PARADAS CERCANAS: pistas animadas con variables CSS ----
def uri_arrp(theme,ph):
    t=TH[theme];c=t['acc'];c1=col(c,t['a1']);c2=col(c,t['a2']);res=[]
    for L in arrp_layers(ph):
        x=S(L['w'],L['h'],c1,c2,t['pc'],t['pg']);L['fn'](x);res.append(x.uri())
    return res
AP='html body [data-screen="arribos"] .arrival-card'
for theme in ('light','dark'):
    for ph,nth in ((0,''),(1,':nth-child(even)')):
        a1,a2=uri_arrp(theme,ph); body_="{--cka1:%s;--cka2:%s}"%(a1,a2)
        if theme=='light': out.append("%s%s%s"%(AP,nth,body_))
        else:
            out.append('@media (prefers-color-scheme:dark){%s%s}'%(sels([AP+nth],'',':root:not([data-theme="light"]) '),body_))
            out.append('%s%s'%(sels([AP+nth],'',':root[data-theme="dark"] '),body_))
out.append(AP+"::before{background-image:var(--cka1),var(--cka2),"+GR180[0]+";background-size:24px 120px,1000px 120px,100% 100%;background-position:0 0,calc(100% - 80px) 0,0 0;background-repeat:no-repeat}")
css="\n".join(out)+"\n"
import os
OUT=os.path.join(os.path.dirname(os.path.abspath(__file__)),'..','app','src','main','assets','circuits.css')
open(OUT,'w',encoding='utf-8').write(css)
print(len(css))
