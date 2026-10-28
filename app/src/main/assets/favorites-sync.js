/* FASE 4: contrato local de sincronización. No realiza llamadas a la nube sin sesión verificada. */
(function(global){'use strict';
  var STORAGE='tucolectivo:favorites:v1', BACKUP='tucolectivo:favorites:premerge:v1';
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
  function importRemote(remote,uid){
    // Nunca importar datos privados si la identidad no ha sido confirmada por el proveedor.
    if(!uid||typeof uid!=='string')throw new Error('SE REQUIERE IDENTIDAD AUTENTICADA');
    if(!global.TcVerifiedSyncSession||global.TcVerifiedSyncSession.uid!==uid)throw new Error('SESIÓN NO VERIFICADA');
    var previous=localStorage.getItem(STORAGE),combined=merge(read(),remote);
    try{
      localStorage.setItem(BACKUP,previous===null?'':previous);
      localStorage.setItem(STORAGE,JSON.stringify(combined));
      global.dispatchEvent(new CustomEvent('tc:favoritesimported',{detail:{uid:uid,counts:{lines:combined.lines.length,stops:combined.stops.length,arrivals:combined.arrivals.length}}}));
      return combined;
    }catch(error){
      if(previous===null)localStorage.removeItem(STORAGE);else localStorage.setItem(STORAGE,previous);
      throw error;
    }
  }
  global.TcFavoritesSync={normalize:normalize,merge:merge,read:read,importRemote:importRemote};
})(window);
