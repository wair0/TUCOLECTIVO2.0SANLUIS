(function(){'use strict';
var panel=document.getElementById('tc-profile');if(!panel)return;
var KEY='tc-local-profile-v1',el=function(id){return document.getElementById(id)},state=el('tc-profile-state'),photo=el('tc-profile-photo'),img=el('tc-profile-avatar-img'),placeholder=el('tc-profile-avatar-placeholder');
var account=null,profile={first:'',last:'',nickname:'',photo:''};
function notify(t){state.textContent=t}
function load(){try{var v=JSON.parse(localStorage.getItem(KEY)||'{}');profile={first:String(v.first||''),last:String(v.last||''),nickname:String(v.nickname||''),photo:String(v.photo||'')}}catch(_){profile={first:'',last:'',nickname:'',photo:''}}render()}
function render(){el('tc-profile-first').value=profile.first;el('tc-profile-last').value=profile.last;el('tc-profile-nickname').value=profile.nickname;img.hidden=!profile.photo;placeholder.hidden=!!profile.photo;if(profile.photo)img.src=profile.photo;else img.removeAttribute('src');el('tc-profile-account-state').textContent=account?'SESIÓN FIREBASE: '+(account.email||'CONECTADA')+'. EL PERFIL ES LOCAL.':'SIN SESIÓN FIREBASE. PODÉS EDITAR TU PERFIL IGUALMENTE.';el('tc-profile-logout').hidden=!account;el('tc-profile-login').hidden=!!account}
function save(){try{localStorage.setItem(KEY,JSON.stringify(profile));notify('PERFIL GUARDADO EN ESTE DISPOSITIVO.');render()}catch(_){notify('NO SE PUDO GUARDAR. REVISÁ EL ESPACIO DISPONIBLE O ELEGÍ UNA FOTO MÁS PEQUEÑA.')}}
function close(){panel.hidden=true}
function open(){if(window.TcHeader)window.TcHeader.closeAll();load();panel.hidden=false;el('tc-profile-close').focus()}
document.addEventListener('tc:profileopen',open);el('tc-profile-close').addEventListener('click',close);
el('tc-profile-form').addEventListener('submit',function(e){e.preventDefault();profile.first=el('tc-profile-first').value.trim();profile.last=el('tc-profile-last').value.trim();profile.nickname=el('tc-profile-nickname').value.trim();save()});
el('tc-profile-photo-button').addEventListener('click',function(){photo.click()});
photo.addEventListener('change',function(){var file=photo.files&&photo.files[0];if(!file)return;if(!/^image\/(jpeg|png|webp)$/.test(file.type)||file.size>5*1024*1024){notify('ELEGÍ UNA IMAGEN JPG, PNG O WEBP DE HASTA 5 MB.');photo.value='';return}var reader=new FileReader();reader.onload=function(){var source=new Image();source.onload=function(){var canvas=document.createElement('canvas'),max=256,scale=Math.min(1,max/Math.max(source.width,source.height));canvas.width=Math.max(1,Math.round(source.width*scale));canvas.height=Math.max(1,Math.round(source.height*scale));canvas.getContext('2d').drawImage(source,0,0,canvas.width,canvas.height);profile.photo=canvas.toDataURL('image/jpeg',.76);save();photo.value=''};source.onerror=function(){notify('NO SE PUDO LEER LA IMAGEN.')};source.src=reader.result};reader.onerror=function(){notify('NO SE PUDO LEER LA FOTO.')};reader.readAsDataURL(file)});
el('tc-profile-login').addEventListener('click',function(){close();if(window.TcAuthGate)window.TcAuthGate.open()});
el('tc-profile-logout').addEventListener('click',function(){if(window.TuColectivoNative&&typeof window.TuColectivoNative.signOutAccount==='function'){window.TuColectivoNative.signOutAccount();account=null;render();notify('SESIÓN CERRADA. TU PERFIL LOCAL SE CONSERVA.');if(window.TcAuthGate)window.TcAuthGate.open()}else notify('ACTUALIZÁ LA APLICACIÓN PARA CERRAR LA SESIÓN.')});
el('tc-profile-delete').addEventListener('click',function(){if(!window.confirm('¿BORRAR EL NOMBRE, APELLIDO, APODO Y FOTO GUARDADOS EN ESTE DISPOSITIVO? NO SE BORRARÁ TU CUENTA FIREBASE.'))return;try{localStorage.removeItem(KEY)}catch(_){}load();notify('DATOS LOCALES DEL PERFIL ELIMINADOS.')});
document.addEventListener('tc:authsuccess',function(e){account=e.detail||{};render()});
document.addEventListener('keydown',function(e){if(e.key==='Escape'&&!panel.hidden){close();e.preventDefault()}});
load();window.TcProfile={open:open,close:close};
})();