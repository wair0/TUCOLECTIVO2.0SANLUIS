/* Perfil + menú: refleja nombre, correo y estado de sesión en la cabecera del perfil y en el menú hamburguesa.
   Cargar ANTES de profile.js (profile.js emite tc:profilechange al iniciar). No toca datos ni Firebase. */
(function(){'use strict';
var $=function(id){return document.getElementById(id)};
function sync(e){
  var d=(e&&e.detail)||{},st=$('tc-profile-account-state'),on=!!st&&/^SESI[ÓO]N FIREBASE ACTIVA/i.test(st.textContent||'');
  var n=$('tc-pf-name'),m=$('tc-pf-mail');
  if(n)n.textContent=d.displayName||'MI PERFIL';
  if(m)m.textContent=d.email||'Agregá tus datos de perfil';
  ['tc-pf-chip','tc-hd-profile-chip'].forEach(function(id){var c=$(id);if(!c)return;c.classList.toggle('on',on);var b=c.querySelector('b');if(b)b.textContent=on?'CONECTADO':'INVITADO'});
}
document.addEventListener('tc:profilechange',sync);
document.addEventListener('tc:authsuccess',function(){setTimeout(sync,0)});
document.addEventListener('tc:authsignedout',function(){setTimeout(sync,0)});
sync();
})();
