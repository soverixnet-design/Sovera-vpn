const PAGE = `<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>SOVERA VPN • Control Center</title>
<style nonce="__NONCE__">
:root{color-scheme:dark;font-family:Inter,system-ui,-apple-system,Segoe UI,sans-serif;--gold:#f6ce68;--blue:#123b94;--line:#ffffff20;--muted:#b4c6e8}
*{box-sizing:border-box}body{margin:0;background:radial-gradient(circle at 80% -20%,#2855a2 0,transparent 37%),linear-gradient(155deg,#03091a,#0d1b3d 62%,#030a1f);color:#f6f8ff;min-height:100vh}
header{padding:20px min(5vw,60px);border-bottom:1px solid var(--line);display:flex;align-items:center;gap:12px;justify-content:space-between;background:#031027ad}
.brand{font-size:1.15rem;font-weight:900;letter-spacing:.09em}.brand b{color:var(--gold)}.pill{border:1px solid #d9b96180;border-radius:50px;padding:6px 12px;color:var(--gold);font-size:.75rem}
main{max-width:1200px;margin:30px auto;padding:0 18px}.hero{margin-bottom:22px}.hero h1{font-size:clamp(1.8rem,4vw,2.7rem);margin:0 0 6px}.hero p{color:var(--muted);margin:0}
.card{background:linear-gradient(150deg,#142753dc,#06112be8);border:1px solid var(--line);border-radius:18px;padding:22px;box-shadow:0 20px 40px #0002;margin-bottom:18px}
.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(290px,1fr));gap:18px}h2{margin:0 0 18px;font-size:1.13rem;color:#f9db8e}label{display:block;margin:12px 0 6px;color:#d6dff2;font-size:.9rem}
input,select{border:1px solid #6686b650;background:#050d22;color:white;padding:12px 13px;border-radius:10px;width:100%;font:inherit;outline:none}input:focus,select:focus{border-color:var(--gold)}button{border:1px solid #e6bd6c;background:linear-gradient(100deg,#ebc56e,#fff0b0,#d7a94c);color:#06142e;font-weight:800;padding:11px 16px;border-radius:10px;cursor:pointer}button:hover{filter:brightness(1.1)}button.secondary{background:#102e66;color:#eaf1ff;border-color:#5275b5}button.danger{background:#662c42;color:#fff;border-color:#b44a70}button:disabled{opacity:.5;cursor:not-allowed}
.actions{display:flex;gap:8px;flex-wrap:wrap;margin-top:16px}.muted{color:var(--muted)}.hint{font-size:.84rem;color:var(--muted);margin:10px 0}.status{margin:12px 0;color:var(--gold);min-height:20px}.hidden{display:none!important}
.tablewrap{overflow:auto}table{border-collapse:collapse;width:100%;min-width:610px}th,td{padding:12px 8px;border-bottom:1px solid var(--line);text-align:left;font-size:.88rem}th{color:var(--gold);font-weight:700}td button{font-size:.75rem;padding:7px 9px;margin-right:5px}
small{font-size:.78rem;color:var(--muted)}footer{text-align:center;padding:18px;color:#a9bbdf;font-size:.78rem}
</style></head><body>
<header><div class="brand">🛡 SOVERA <b>VPN</b></div><span class="pill">STAGING CONTROL CENTER</span></header>
<main>
<div class="hero"><h1>Royal VPN Management</h1><p>Users, entitlements and server metadata in one secure place.</p></div>
<div class="card"><strong>Transport status: NOT CONFIGURED</strong><div class="hint">This panel cannot issue live VPN credentials until a separately authorized VPN endpoint and provisioning service are integrated.</div></div>
<section id="loginPanel" class="card"><h2>Administrator sign in</h2><form id="loginForm"><label for="email">Email</label><input id="email" type="email" autocomplete="username" required><label for="password">Password</label><input id="password" type="password" autocomplete="current-password" required><div class="actions"><button type="submit">Sign in</button></div></form><p class="hint">An administrator must first be promoted through a trusted D1 operation; public signup cannot grant admin permissions.</p></section>
<section id="dashboard" class="hidden">
<div class="actions" style="margin-bottom:18px"><button id="refresh" class="secondary">Refresh</button><button id="logout" class="secondary">Log out</button></div>
<div class="grid">
<div class="card"><h2>Register server metadata</h2><form id="serverForm"><label for="serverName">Display name</label><input id="serverName" placeholder="Singapore 01" maxlength="70" required><label for="country">Country code</label><input id="country" maxlength="2" value="SG" required><label for="protocol">Protocol</label><select id="protocol"><option>wireguard</option><option>openvpn</option><option>ssh</option><option>v2ray</option><option>udp</option></select><label><input id="vipOnly" type="checkbox" style="width:auto"> VIP only</label><div class="actions"><button type="submit">Add placeholder server</button></div></form><p class="hint">Server stays disabled until the real VPN endpoint passes connectivity testing.</p></div>
<div class="card"><h2>Release readiness</h2><p>Free tier: in-app ads planned.</p><p>VIP tier: monthly Play subscription planned.</p><p class="muted">Billing verification, AdMob IDs and connection provisioning are still pending.</p></div>
</div>
<div class="card"><h2>Users</h2><div class="tablewrap"><table><thead><tr><th>Email</th><th>Status</th><th>Tier</th><th>VIP expiry</th><th>Actions</th></tr></thead><tbody id="users"></tbody></table></div></div>
<div class="card"><h2>Servers</h2><div class="tablewrap"><table><thead><tr><th>Name</th><th>Country</th><th>Protocol</th><th>Tier</th><th>Transport</th></tr></thead><tbody id="servers"></tbody></table></div></div>
</section>
<div id="status" class="status" role="status" aria-live="polite"></div>
</main><footer>SOVERA VPN • Access is restricted to administrators.</footer>
<script nonce="__NONCE__">
(() => {
  let token = null;
  const byId = id => document.getElementById(id);
  const status = message => { byId('status').textContent = message; };
  const cell = (row,value) => {const td=document.createElement('td');td.textContent=String(value??'—');row.appendChild(td);return td;};
  async function api(path,method='GET',body) {
    const options={method,headers:{'accept':'application/json'}};
    if(token)options.headers.authorization='Bearer '+token;
    if(body!==undefined){options.headers['content-type']='application/json';options.body=JSON.stringify(body);}
    const res=await fetch(path,options); const data=await res.json();
    if(!res.ok)throw Error(data.error||('HTTP '+res.status)); return data;
  }
  async function refresh(){
    const [u,s]=await Promise.all([api('/v1/admin/users'),api('/v1/admin/servers')]);
    byId('users').replaceChildren();byId('servers').replaceChildren();
    for(const user of u.users){
      const tr=document.createElement('tr');cell(tr,user.email);cell(tr,user.status);cell(tr,user.tier||'free');
      cell(tr,user.expires_at?new Date(user.expires_at*1000).toLocaleDateString():'—');
      const actions=cell(tr,'');
      const vip=document.createElement('button');vip.textContent='+30d VIP';vip.onclick=async()=>{try{await api('/v1/admin/users/'+user.id+'/vip','POST',{days:30});status('VIP updated');await refresh();}catch(e){status(e.message);}};actions.appendChild(vip);
      const block=document.createElement('button');block.className=user.status==='active'?'danger':'secondary';block.textContent=user.status==='active'?'Block':'Unblock';
      block.onclick=async()=>{try{await api('/v1/admin/users/'+user.id,'PATCH',{status:user.status==='active'?'blocked':'active'});status('User updated');await refresh();}catch(e){status(e.message);}};actions.appendChild(block);
      byId('users').appendChild(tr);
    }
    for(const server of s.servers){const tr=document.createElement('tr');cell(tr,server.name);cell(tr,server.country_code);cell(tr,server.protocol);cell(tr,server.vip_only?'VIP':'Free');cell(tr,server.endpoint_ready?'Ready':'Not configured');byId('servers').appendChild(tr);}
  }
  byId('loginForm').addEventListener('submit',async event=>{
    event.preventDefault();status('Signing in…');
    try{const session=await api('/v1/auth/login','POST',{email:byId('email').value,password:byId('password').value});token=session.token;byId('password').value='';const me=await api('/v1/me');if(me.role!=='admin')throw Error('Administrator permission required');byId('loginPanel').classList.add('hidden');byId('dashboard').classList.remove('hidden');await refresh();status('Signed in');}
    catch(e){token=null;status(e.message);}
  });
  byId('refresh').onclick=()=>refresh().then(()=>status('Updated')).catch(e=>status(e.message));
  byId('logout').onclick=async()=>{try{await api('/v1/auth/logout','POST');}catch{}token=null;byId('dashboard').classList.add('hidden');byId('loginPanel').classList.remove('hidden');status('Signed out');};
  byId('serverForm').addEventListener('submit',async e=>{e.preventDefault();try{await api('/v1/admin/servers','POST',{name:byId('serverName').value,country_code:byId('country').value,protocol:byId('protocol').value,vip_only:byId('vipOnly').checked});status('Placeholder server added');byId('serverName').value='';await refresh();}catch(e){status(e.message);}});
})();
</script></body></html>`;

export function adminPage(nonce) {
  return PAGE.replaceAll("__NONCE__", nonce);
}
