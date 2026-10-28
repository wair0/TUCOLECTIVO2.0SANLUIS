/* FASE 4: contrato local de sincronización. No realiza llamadas a la nube sin sesión verificada. */
(function(global){'use strict';
  var STORAGE='tucolectivo:favorites:v1';
  function empty(){return {lines:[],stops:[],arrivals:[]};}
  function normalize(source){
    source=source&&typeof source==='object'?source:{};
    var out=empty(), seen={lines:new Set(),stops:new Set(),arrivals:new Set()};
    (Array.isArray(source.lines)?source.lines:[]).forEach(function(v){var n=Number(v);if(Number.isInteger(n)&&n>=1&&n<=14&&!seen.lines.has(n)){seen.lines.add(n);out.lines.push(n);}});
    (Array.isArray(source.stops)?source.stops:[]).forEach(function(v){var id=String(v==null?'':v).trim();if(id&&!seen.stops.has(id)){seen.stops.add(id);out.stops.push(id);}});
    (Array.isArray(source.arrivals)?source.arrivals:[]).forEach(function(v){
      if(!v||typeof v!=='object'||!v.stop)return;
      var line=Number(v.line),stop=String(v.stop.identifier||v.stop.id||v.stop.code||'').trim();
      if(!Number.isInteger(line)||line<1||line>14||!stop)return;
      var key=stop+'::'+line;
      if(seen.arrivals.has(key))return;
      seen.arrivals.add(key);
      out.arrivals.push(Object.assign({},v,{key:key,line:line}));
    });
    return out;
  }
  function merge(local,remote){
    var a=normalize(local),b=normalize(remote);
    return normalize({lines:a.lines.concat(b.lines),stops:a.stops.concat(b.stops),arrivals:a.arrivals.concat(b.arrivals)});
  }
  function read(){try{return normalize(JSON.parse(localStorage.getItem(STORAGE)||'{}'));}catch(_){return empty();}}
  global.TcFavoritesSync={normalize:normalize,merge:merge,read:read};
})(window);
