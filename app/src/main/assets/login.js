/* Pantallas independientes de acceso con enlaces seguros de Firebase. */
(function(){'use strict';
var root=document.getElementById('tc-auth');if(!root)return;
var KEY='tc-auth-guest-v1',page='login',pendingEmail='',busy=false;
var el=id=>document.getElementById(id),status=el('tc-auth-status');
function message(t){status.textContent=t}
function navigate(name){page=name;root.querySelectorAll('[data-auth-page]').forEach(function(x){x.hidden=x.getAttribute('data-auth-page')!==name});message('');}
function close(){root.hidden=true;document.body.classList.remove('tc-auth-open')}
function show(){root.hidden=false;document.body.classList.add('tc-auth-open');navigate('login')}
function native(){return window.TuColectivoNative}
function payload(x){try{return typeof x==='string'?JSON.parse(x):x||{}}catch(_){return {}}}
function done(){busy=false;root.querySelectorAll('button').forEach(function(b){b.disabled=false})}
function send(mode,email,pass,first,last){if(busy)return;if(!native()||typeof native().emailAuth!=='function'){message('ACTUALIZÁ LA APLICACIÓN PARA USAR FIREBASE.');return}busy=true;root.querySelectorAll('button').forEach(function(b){b.disabled=true});message('PROCESANDO SOLICITUD...');try{if(mode==='register'){if(typeof native().registerAccount!=='function')throw Error('Actualizá la aplicación.');native().registerAccount(email,pass,first,last)}else native().emailAuth(mode,email,pass||'')}catch(e){done();message(e.message||'NO SE PUDO CONECTAR CON FIREBASE.')}}
root.querySelectorAll('[data-eye]').forEach(function(b){b.addEventListener('click',function(){var input=el(b.getAttribute('data-eye')),visible=input.type==='password';input.type=visible?'text':'password';b.setAttribute('aria-pressed',String(visible));b.setAttribute('aria-label',visible?'Ocultar contraseña':'Mostrar contraseña')})});
el('tc-auth-register').addEventListener('click',function(){navigate('register')});
el('tc-auth-recover').addEventListener('click',function(){navigate('recover')});
root.querySelectorAll('[data-auth-back]').forEach(function(b){b.addEventListener('click',function(){navigate('login')})});
el('tc-auth-guest').addEventListener('click',function(){try{localStorage.setItem(KEY,'1')}catch(_){}close()});
el('tc-auth-form').addEventListener('submit',function(e){e.preventDefault();send('login',el('tc-auth-email').value.trim(),el('tc-auth-password').value)});
el('tc-auth-register-form').addEventListener('submit',function(e){e.preventDefault();var first=el('tc-auth-first').value.trim(),last=el('tc-auth-last').value.trim(),email=el('tc-auth-register-email').value.trim(),pass=el('tc-auth-register-password').value,repeat=el('tc-auth-register-repeat').value;if(!first||!last){message('COMPLETÁ NOMBRE Y APELLIDO.');return}if(pass!==repeat){message('LAS CONTRASEÑAS NO COINCIDEN.');return}if(pass.length<6){message('LA CONTRASEÑA DEBE TENER AL MENOS 6 CARACTERES.');return}pendingEmail=email;send('register',email,pass,first,last)});
el('tc-auth-recover-form').addEventListener('submit',function(e){e.preventDefault();send('recover',el('tc-auth-recover-email').value.trim(),'')});
el('tc-auth-google').addEventListener('click',function(){if(!native()||typeof native().signInWithGoogle!=='function'){message('ACTUALIZÁ LA APLICACIÓN PARA USAR GOOGLE.');return}message('ABRIENDO GOOGLE...');native().signInWithGoogle()});
function success(data,provider){try{localStorage.removeItem(KEY)}catch(_){}close();document.dispatchEvent(new CustomEvent('tc:authsuccess',{detail:{provider:provider,uid:data.uid,email:data.email||'',name:data.name||'',verified:!!data.verified}}))}
window.onNativeGoogleAuthSuccess=function(x){var d=payload(x);if(d.uid)success(d,'google');else message('NO SE PUDO CONFIRMAR LA SESIÓN.')};
window.onNativeGoogleAuthError=function(x){message(payload(x).message||'NO SE PUDO INICIAR SESIÓN CON GOOGLE.')};
window.onNativeEmailAuthSuccess=function(x){done();var d=payload(x);if(!d.uid){message('NO SE PUDO CONFIRMAR LA SESIÓN.');return}if(!d.verified&&d.provider!=='google'){pendingEmail=d.email||pendingEmail;el('tc-auth-verify-email').textContent=pendingEmail;navigate('verify');message('VERIFICÁ TU CORREO PARA CONTINUAR.');return}success(d,'password')};
window.onNativeEmailAuthError=function(x){done();message(payload(x).message||'NO SE PUDO COMPLETAR LA OPERACIÓN.')};
window.onNativeEmailAuthRecovery=function(x){done();message(payload(x).message||'REVISÁ TU CORREO ELECTRÓNICO.')};
el('tc-auth-verify-check').addEventListener('click',function(){if(!native()||typeof native().checkEmailVerification!=='function'){message('ACTUALIZÁ LA APLICACIÓN.');return}message('COMPROBANDO VERIFICACIÓN...');native().checkEmailVerification()});
el('tc-auth-verify-resend').addEventListener('click',function(){if(!native()||typeof native().resendEmailVerification!=='function'){message('ACTUALIZÁ LA APLICACIÓN.');return}native().resendEmailVerification()});
window.onNativeEmailVerificationStatus=function(x){var d=payload(x);if(d.verified&&d.uid)success(d,'password');else message(d.message||'TODAVÍA NO SE VERIFICÓ EL CORREO.')};
window.onNativeEmailVerificationSent=function(x){message(payload(x).message||'REVISÁ TU CORREO.')};
function restoreSession(){try{var bridge=native();if(bridge&&typeof bridge.getCurrentAccount==='function'){var current=payload(bridge.getCurrentAccount());if(current.uid){window.onNativeEmailAuthSuccess(current);return}}if(localStorage.getItem(KEY)==='1')close();else show()}catch(_){show()}}
restoreSession();
window.TcAuthGate={open:show,close:close};
})();