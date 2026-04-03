function openModal(name){
  document.getElementById('modal-'+name).classList.add('open');
}
function closeModal(name){
  document.getElementById('modal-'+name).classList.remove('open');
}
function closeModalOutside(e, name){
  if(e.target===document.getElementById('modal-'+name)) closeModal(name);
}
function switchModal(from, to){
  closeModal(from);
  setTimeout(()=>openModal(to),200);
}

function toggleWidgetsPanel(){
  const panel = document.querySelector('.widgets-area');
  const backdrop = document.getElementById('widgets-backdrop');
  if(!panel || !backdrop) return;
  panel.classList.toggle('open');
  backdrop.classList.toggle('open');
}

function closeWidgetsPanel(){
  const panel = document.querySelector('.widgets-area');
  const backdrop = document.getElementById('widgets-backdrop');
  if(!panel || !backdrop) return;
  panel.classList.remove('open');
  backdrop.classList.remove('open');
}

function navigate(page){
  if (page === 'admin') {
    const role = (window.userState?.currentUser?.role || 'user').toLowerCase();
    const isAdmin = window.userState?.isLoggedIn && role === 'admin';
    if (!isAdmin) {
      showToast('Kein Zugriff auf Admin-Bereich', 'error');
      page = 'feed';
    }
  }
  document.querySelectorAll('.page,.page-full').forEach(p=>{p.classList.remove('active');});
  const el = document.getElementById('page-'+page);
  if(el) el.classList.add('active');
  document.querySelectorAll('.nav-link').forEach(l=>{ l.classList.toggle('active', l.id==='nav-'+page); });
  closeDetail();
  closeWidgetsPanel();
  if(page==='admin') renderAdminTable();
  if(page==='profile') {
    if (typeof window.loadProfileData === 'function') window.loadProfileData();
    else renderProfile();
  }
}

let toastTimer;
function showToast(msg, type='success'){
  const t = document.getElementById('toast');
  const m = document.getElementById('toast-msg');
  t.className = 'toast '+type;
  m.textContent = msg;
  t.classList.add('show');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(()=>t.classList.remove('show'), 3000);
}

if (typeof refreshFeed === 'function') {
  refreshFeed().catch(e => showToast(e.message || 'Feed konnte nicht geladen werden', 'error'));
}
if (typeof renderProfile === 'function') {
  renderProfile();
}
