(function(){
var CFG=Object.assign({duration:11000,loop:false,
messages:["CARGANDO LINEAS...","CARGANDO MAPA...","CARGANDO PARADAS CERCANAS...","CARGANDO FAVORITOS...","CARGANDO RECORRIDOS...","SINCRONIZANDO GPS..."]},window.SPLASH_CONFIG||{});
var root=document.getElementById("splash"),fill=document.getElementById("fill"),pct=document.getElementById("pct"),
txt=document.getElementById("statusText"),steps=document.getElementById("steps"),n=CFG.messages.length,
cur=-1,typer=null,t0=0;
for(var i=0;i<n;i++)steps.appendChild(document.createElement("i"));
function type(s){clearInterval(typer);var k=0;txt.textContent="";
typer=setInterval(function(){txt.textContent=s.slice(0,++k);if(k>=s.length)clearInterval(typer)},28)}
function ease(t){return t<.5?2*t*t:1-Math.pow(-2*t+2,2)/2}
function frame(now){
var t=Math.min((now-t0)/CFG.duration,1),p=Math.round(ease(t)*100);
fill.style.width=p+"%";pct.firstChild.nodeValue=String(p);
var idx=Math.min(n-1,Math.floor(p/(100/n)));
if(idx!==cur){cur=idx;type(CFG.messages[idx]);
for(var j=0;j<n;j++)steps.children[j].className=j<=idx?"on":""}
if(t<1)requestAnimationFrame(frame);else done()}
function done(){
document.dispatchEvent(new CustomEvent("splash:complete"));
setTimeout(function(){root.classList.add("out");
setTimeout(function(){if(CFG.loop)start();else root.style.visibility="hidden"},700)},900)}
function start(){root.classList.remove("out");root.style.visibility="";cur=-1;t0=performance.now();requestAnimationFrame(frame)}
window.TuColectivoSplash={restart:start};
start();
})();