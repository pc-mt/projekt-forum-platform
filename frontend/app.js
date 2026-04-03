// ─── DATA ───
let selectedTag = 'news';
let currentFilter = 'all';
let currentSort = 'popular';
let openPostId = null;

const posts = [
  {
    id:0, type:'news', pinned:true,
    title:'Lancement officiel de la plateforme DMTree Community v1.0',
    excerpt:'Wir freuen uns, den offiziellen Start unseres Community-Hubs bekannt zu geben. Diese Plattform ermoeglicht allen Partnerorganisationen Zusammenarbeit, Ideenaustausch und Information in Echtzeit.',
    author:'Marie Aufret', role:'admin', initials:'MA', avColor:'rgba(201,168,76,0.15)', avTextColor:'var(--gold)',
    date:'Aujourd\'hui, 09h15', score:142, userVote:0, comments:[
      {id:0,author:'Jonas Klein',initials:'JK',avColor:'rgba(61,184,122,0.1)',avTextColor:'var(--green)',date:'Vor 2 Std.',body:'Glueckwunsch an das gesamte Team! Das ist ein grosser Schritt fuer unsere Organisationen.'},
      {id:1,author:'Sofia Radić',initials:'SR',avColor:'rgba(74,144,226,0.1)',avTextColor:'var(--blue)',date:'Vor 1 Std.',body:'Ich freue mich sehr darauf, alle Funktionen zu testen. Der Bereich "Ideen" interessiert mich besonders.'},
      {id:2,author:'Luca Bernardi',initials:'LB',avColor:'rgba(139,111,232,0.1)',avTextColor:'var(--purple)',date:'Vor 45 Min.',body:'Est-ce que la version mobile sera disponible prochainement ?'}
    ]
  },
  {
    id:1, type:'discussion', pinned:false,
    title:'Erfahrungsbericht: Wie koennen wir das Onboarding neuer Mitglieder verbessern?',
    excerpt:'Nach 3 Wochen Beta-Nutzung haben mehrere Mitglieder Schwierigkeiten beim ersten Login gemeldet. Welche Vorschlaege habt ihr, um den Prozess fluessiger zu machen?',
    author:'Jonas Klein', role:'user', initials:'JK', avColor:'rgba(61,184,122,0.1)', avTextColor:'var(--green)',
    date:'Gestern, 16:30', score:54, userVote:0, comments:[
      {id:0,author:'Marie Aufret',initials:'MA',avColor:'rgba(201,168,76,0.15)',avTextColor:'var(--gold)',date:'Vor 18 Std.',body:'Gutes Feedback, Jonas. Ein interaktives Tutorial beim ersten Login waere sinnvoll.'},
      {id:1,author:'Sofia Radić',initials:'SR',avColor:'rgba(74,144,226,0.1)',avTextColor:'var(--blue)',date:'Vor 16 Std.',body:'Eine Willkommens-E-Mail mit den ersten 5 Schritten waere sehr hilfreich.'}
    ]
  },
  {
    id:2, type:'idea', pinned:false,
    title:'Integration Slack native pour les notifications DMTree',
    excerpt:'Vorschlag: Ein offizieller Slack-Bot sendet Echtzeit-Benachrichtigungen fuer neue Beitraege, Kommentare und Stimmen. So muss die Plattform nicht staendig geprueft werden.',
    author:'Sofia Radić', role:'user', initials:'SR', avColor:'rgba(74,144,226,0.1)', avTextColor:'var(--blue)',
    date:'Vor 2 Tagen', score:98, userVote:0, comments:[
      {id:0,author:'Jonas Klein',initials:'JK',avColor:'rgba(61,184,122,0.1)',avTextColor:'var(--green)',date:'Vor 2 Tagen',body:'Ausgezeichnete Idee! Die Slack-API ist bereits verfuegbar, das waere schnell umsetzbar.'},
    ]
  },
  {
    id:3, type:'discussion', pinned:false,
    title:'Welche Veroeffentlichungsfrequenz wuenscht ihr fuer offizielle Neuigkeiten?',
    excerpt:'Das Redaktionsteam erstellt einen Veroeffentlichungsplan fuer offizielle News. Was bevorzugt ihr: taeglich, woechentlich oder ereignisbasiert?',
    author:'Marie Aufret', role:'admin', initials:'MA', avColor:'rgba(201,168,76,0.15)', avTextColor:'var(--gold)',
    date:'Vor 3 Tagen', score:31, userVote:0, comments:[
      {id:0,author:'Luca Bernardi',initials:'LB',avColor:'rgba(139,111,232,0.1)',avTextColor:'var(--purple)',date:'Vor 3 Tagen',body:'Woechentlich erscheint ideal. Zu viele taegliche News koennen wichtige Informationen untergehen lassen.'}
    ]
  },
  {
    id:4, type:'idea', pinned:false,
    title:'Dashboard Analytics v2 — visualisation des tendances von organisation',
    excerpt:'Idee fuer die naechste Version: Ein Dashboard fuer Administratoren mit Grafiken zur Community-Aktivitaet, Wochentrends und den aktivsten Mitgliedern.',
    author:'Luca Bernardi', role:'user', initials:'LB', avColor:'rgba(139,111,232,0.1)', avTextColor:'var(--purple)',
    date:'Vor 4 Tagen', score:76, userVote:0, comments:[]
  },
  {
    id:5, type:'news', pinned:false,
    title:'Geplante Wartung - Samstag, 5. April, 02:00 bis 06:00',
    excerpt:'Am Samstag, 5. April, ist eine Wartung fuer Datenbankmigration und Sicherheitsupdate geplant. Es gehen keine Daten verloren.',
    author:'Marie Aufret', role:'admin', initials:'MA', avColor:'rgba(201,168,76,0.15)', avTextColor:'var(--gold)',
    date:'Vor 5 Tagen', score:18, userVote:0, comments:[]
  }
];

// ─── RENDER ───
function getTagHTML(type){
  if(type==='news') return '<span class="tag tag-news">📰 Neuigkeit</span>';
  if(type==='idea') return '<span class="tag tag-idea">💡 Idee</span>';
  return '<span class="tag tag-disc">💬 Diskussion</span>';
}

function getCurrentUser(){
  if (window.userState && window.userState.currentUser) {
    return window.userState.currentUser;
  }
  return { name: 'Gast', initials: 'G', role: 'user' };
}

function renderPosts(){
  const container = document.getElementById('posts-container');
  let filtered = posts.filter(p => currentFilter==='all' || p.type===currentFilter);
  if(currentSort==='popular') filtered.sort((a,b)=>b.score-a.score);
  else if(currentSort==='recent') filtered.sort((a,b)=>b.id-a.id);
  else filtered.sort((a,b)=>b.comments.length-a.comments.length);

  if(filtered.length===0){
    container.innerHTML=`<div class="empty-state"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg><h3>Keine Beitraege gefunden</h3><p>Sei der Erste, der in dieser Kategorie einen Beitrag veroeffentlicht.</p></div>`;
    return;
  }

  container.innerHTML = filtered.map((p,i) => `
    <div class="post-card ${p.pinned?'pinned':''}" onclick="openPost(${p.id})" style="animation-delay:${i*0.07}s">
      <div class="post-card-body">
        <div class="post-meta">
          ${getTagHTML(p.type)}
          <span class="post-author">von <strong>${p.author}</strong> <span class="role-badge ${p.role==='admin'?'role-admin':'role-user'}">${p.role==='admin'?'Admin':'User'}</span></span>
          <span class="post-author">${p.date}</span>
          ${p.pinned?'<span class="post-pinned-badge">📌 Angeheftet</span>':''}
        </div>
        <div class="post-title">${p.title}</div>
        <div class="post-excerpt">${p.excerpt.substring(0,120)}...</div>
      </div>
      <div class="post-footer" onclick="event.stopPropagation()">
        <button class="vote-btn up ${p.userVote===1?'active':''}" onclick="vote(${p.id},1)">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="18 15 12 9 6 15"/></svg>
        </button>
        <span class="score ${p.score>0?'positive':p.score<0?'negative':''}">${p.score}</span>
        <button class="vote-btn down ${p.userVote===-1?'active':''}" onclick="vote(${p.id},-1)">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="6 9 12 15 18 9"/></svg>
        </button>
        <div class="post-action" onclick="openPost(${p.id})">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
          ${p.comments.length} Kommentar${p.comments.length!==1?'s':''}
        </div>
        ${getCurrentUser().role==='admin'?`
        <div class="post-action" onclick="togglePin(${p.id},event)" style="margin-left:auto;">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="17" x2="12" y2="22"/><path d="M5 17h14v-1.76a2 2 0 0 0-1.11-1.79l-1.78-.9A2 2 0 0 1 15 10.76V6h1a2 2 0 0 0 0-4H8a2 2 0 0 0 0 4h1v4.76a2 2 0 0 1-1.11 1.79l-1.78.9A2 2 0 0 0 5 15.24V17z"/></svg>
          ${p.pinned?'Loesen':'Anheften'}
        </div>
        <div class="post-action delete-btn" onclick="deletePost(${p.id},event)">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/></svg>
          Loeschen
        </div>`:''}
      </div>
    </div>
  `).join('');
}

function renderAdminTable(){
  const tbody = document.getElementById('admin-table-body');
  if(!tbody) return;
  tbody.innerHTML = posts.map(p=>`
    <tr>
      <td style="max-width:200px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">${p.pinned?'📌 ':''}${p.title}</td>
      <td><span class="role-badge ${p.role==='admin'?'role-admin':'role-user'}">${p.author}</span></td>
      <td>${getTagHTML(p.type)}</td>
      <td style="color:var(--green);font-weight:700;">+${p.score}</td>
      <td><span class="status-dot ${p.pinned?'status-pinned':'status-active'}"></span>${p.pinned?'Angeheftet':'Actif'}</td>
      <td style="display:flex;gap:6px;">
        <button class="btn btn-ghost btn-sm" style="font-size:0.75rem;" onclick="togglePin(${p.id},event)">📌</button>
        <button class="btn btn-sm" style="font-size:0.75rem;background:rgba(224,85,85,0.1);color:var(--red);border:1px solid var(--red);" onclick="deletePost(${p.id},event)">🗑️</button>
      </td>
    </tr>
  `).join('');
}

function renderProfile(){
  const el = document.getElementById('profile-posts');
  if(!el) return;
  const recentPosts = (window.userState && window.userState.profile && Array.isArray(window.userState.profile.recentPosts))
    ? window.userState.profile.recentPosts
    : [];
  el.innerHTML = recentPosts.length ? recentPosts.map(p=>`
    <div style="padding:12px 0;border-bottom:1px solid var(--border);cursor:pointer;" onclick="navigate('feed');setTimeout(()=>openPost(${p.id}),300)">
      <div style="display:flex;align-items:center;gap:8px;margin-bottom:6px;">${getTagHTML(p.postType)}<span style="font-size:0.8rem;color:var(--text3);">${p.createdAt}</span></div>
      <div style="font-weight:600;font-size:0.9rem;margin-bottom:4px;">${p.title}</div>
      <div style="font-size:0.8rem;color:var(--text3);">Score: <strong style="color:var(--green);">+${p.score}</strong> · ${p.commentsCount} Kommentare</div>
    </div>
  `).join('') : '<p style="color:var(--text3);font-size:0.875rem;">Zurzeit keine Veroeffentlichungen.</p>';
}

// ─── ACTIONS ───
function vote(id, dir){
  const p = posts.find(x=>x.id===id);
  if(!p) return;
  if(p.userVote===dir){ p.score-=dir; p.userVote=0; }
  else { p.score-=p.userVote; p.score+=dir; p.userVote=dir; }
  renderPosts();
  if(openPostId===id) openPost(id);
  showToast(dir===1?'Positive Stimme gespeichert ✓':'Negative Stimme gespeichert','success');
}

function togglePin(id, e){
  if(e) e.stopPropagation();
  const p = posts.find(x=>x.id===id);
  p.pinned = !p.pinned;
  renderPosts();
  renderAdminTable();
  showToast(p.pinned?'Beitrag angeheftet 📌':'Beitrag geloest','success');
}

function deletePost(id, e){
  if(e) e.stopPropagation();
  if(!confirm('Diesen Beitrag endgueltig loeschen?')) return;
  const idx = posts.findIndex(x=>x.id===id);
  posts.splice(idx,1);
  renderPosts();
  renderAdminTable();
  closeDetail();
  showToast('Beitrag geloescht','success');
}

function deleteComment(postId, commentId){
  const p = posts.find(x=>x.id===postId);
  const idx = p.comments.findIndex(c=>c.id===commentId);
  p.comments.splice(idx,1);
  openPost(postId);
  showToast('Kommentar geloescht','success');
}

function submitComment(id){
  const input = document.getElementById('comment-input-'+id);
  const val = input.value.trim();
  if(!val){ showToast('Schreibe zuerst einen Kommentar','error'); return; }
  const p = posts.find(x=>x.id===id);
  p.comments.push({
    id: Date.now(), author: getCurrentUser().name,
    initials: getCurrentUser().initials,
    avColor:'rgba(61,184,122,0.1)', avTextColor:'var(--green)',
    date:'Gerade eben', body:val
  });
  input.value='';
  openPost(id);
  showToast('Kommentar veroeffentlicht ✓','success');
}

// ─── OPEN POST DETAIL ───
function openPost(id){
  const p = posts.find(x=>x.id===id);
  if(!p) return;
  openPostId = id;
  const panel = document.getElementById('detail-panel');
  const adminBtns = document.getElementById('detail-admin-btns');

  adminBtns.innerHTML = getCurrentUser().role==='admin' ? `
    <button class="btn btn-ghost btn-sm" onclick="togglePin(${id},event)">${p.pinned?'Loesen':'Anheften'}</button>
    <button class="btn btn-sm" style="background:rgba(224,85,85,0.1);color:var(--red);border:1px solid var(--red);" onclick="deletePost(${id},event)">Loeschen</button>
  ` : '';

  document.getElementById('detail-content').innerHTML = `
    <div class="post-meta">${getTagHTML(p.type)}<span class="post-author">von <strong>${p.author}</strong> <span class="role-badge ${p.role==='admin'?'role-admin':'role-user'}">${p.role==='admin'?'Admin':'User'}</span></span><span class="post-author">${p.date}</span>${p.pinned?'<span class="post-pinned-badge">📌 Angeheftet</span>':''}</div>
    <div class="detail-title">${p.title}</div>
    <div style="display:flex;align-items:center;gap:10px;margin-bottom:20px;">
      <button class="vote-btn up ${p.userVote===1?'active':''}" onclick="vote(${p.id},1)"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="18 15 12 9 6 15"/></svg></button>
      <span class="score ${p.score>0?'positive':p.score<0?'negative':''}" style="font-size:1rem;">${p.score}</span>
      <button class="vote-btn down ${p.userVote===-1?'active':''}" onclick="vote(${p.id},-1)"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="6 9 12 15 18 9"/></svg></button>
    </div>
    <div class="detail-body">${p.excerpt}<br/><br/>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.</div>
    <div class="comments-section">
      <div class="comments-title">💬 ${p.comments.length} Kommentar${p.comments.length!==1?'s':''}</div>
      ${p.comments.map(c=>`
        <div class="comment">
          <div class="comment-meta">
            <div class="comment-av" style="background:${c.avColor};color:${c.avTextColor};">${c.initials}</div>
            <div class="comment-author">${c.author}</div>
            <div class="comment-date">${c.date}</div>
          </div>
          <div class="comment-body">${c.body}</div>
          <div class="comment-actions">
            <span class="comment-action">👍 Gefällt mir</span>
            <span class="comment-action">↩️ Antworten</span>
            ${(getCurrentUser().role==='admin' || c.author===getCurrentUser().name)?`<span class="comment-action delete-btn" onclick="deleteComment(${p.id},${c.id})">🗑️ Loeschen</span>`:''}
          </div>
        </div>
      `).join('')}
      <div class="comment-form">
        <div class="comment-form-title">Dein Kommentar</div>
        <textarea class="form-input" id="comment-input-${p.id}" placeholder="Teile deine Meinung..." style="min-height:80px;margin-bottom:10px;"></textarea>
        <button class="btn btn-gold btn-sm" onclick="submitComment(${p.id})">Veroeffentlichen</button>
      </div>
    </div>
  `;
  panel.classList.add('open');
}

function closeDetail(){
  document.getElementById('detail-panel').classList.remove('open');
  openPostId = null;
}

// ─── FILTERS ───
function setFilter(type){
  currentFilter = type;
  renderPosts();
}

function setFilterChip(el, type){
  document.querySelectorAll('.filter-chip').forEach(c=>c.classList.remove('active'));
  el.classList.add('active');
  setFilter(type);
}

function setSidebarActive(el){
  document.querySelectorAll('.sidebar-item').forEach(i=>i.classList.remove('active'));
  el.classList.add('active');
}

function sortPosts(val){
  currentSort = val;
  renderPosts();
}

// ─── MODALS ───
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

// ─── CREATE POST ───
function selectTag(tag){
  selectedTag = tag;
  document.querySelectorAll('.tag-opt').forEach(el=>{
    el.className = 'tag-opt';
  });
  document.getElementById('tag-'+tag).className = 'tag-opt sel-'+tag;
}

function submitPost(){
  if (!window.userState || !window.userState.isLoggedIn) {
    showToast('Bitte zuerst anmelden', 'error');
    openModal('login');
    return;
  }
  const title = document.getElementById('post-title').value.trim();
  const body = document.getElementById('post-body').value.trim();
  if(!title||!body){ showToast('Bitte Titel und Inhalt ausfuellen','error'); return; }
  const currentUser = getCurrentUser();
  posts.unshift({
    id: Date.now(), type:selectedTag, pinned:false,
    title, excerpt:body,
    author:currentUser.name, role:currentUser.role,
    initials:currentUser.initials,
    avColor:'rgba(61,184,122,0.1)', avTextColor:'var(--green)',
    date:'Gerade eben', score:0, userVote:0, comments:[]
  });
  closeModal('create');
  document.getElementById('post-title').value='';
  document.getElementById('post-body').value='';
  renderPosts();
  renderAdminTable();
  showToast('Beitrag erfolgreich veroeffentlicht ✓','success');
}

// ─── NAVIGATION ───
function navigate(page){
  document.querySelectorAll('.page,.page-full').forEach(p=>{p.classList.remove('active');});
  const el = document.getElementById('page-'+page);
  if(el) el.classList.add('active');
  document.querySelectorAll('.nav-link').forEach(l=>{
    l.classList.toggle('active', l.id==='nav-'+page);
  });
  closeDetail();
  if(page==='admin') renderAdminTable();
  if(page==='profile') {
    if (typeof window.loadProfileData === 'function') {
      window.loadProfileData();
    } else {
      renderProfile();
    }
  }
}

// ─── TOAST ───
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

// ─── INIT ───
renderPosts();
renderAdminTable();
renderProfile();
