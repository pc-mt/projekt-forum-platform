let selectedTag = 'news';
let currentFilter = 'all';
let currentSort = 'popular';
let openPostId = null;
let posts = [];
let globalCounts = { all: 0, news: 0, idea: 0, discussion: 0, comments: 0, votes: 0 };

window.getPostById = (id) => posts.find((x) => x.id === id);

function getAuthHeaders() {
  const headers = { 'Content-Type': 'application/json' };
  const token = window.userState?.token;
  if (token) headers.Authorization = `Bearer ${token}`;
  return headers;
}

function getTagHTML(type){
  if(type==='news') return '<span class="tag tag-news">📰 Neuigkeit</span>';
  if(type==='idea') return '<span class="tag tag-idea">💡 Idee</span>';
  return '<span class="tag tag-disc">💬 Diskussion</span>';
}

function getCurrentUser(){
  if (window.userState && window.userState.currentUser) return window.userState.currentUser;
  return { name: 'Gast', initials: 'G', role: 'user' };
}

function isLoggedIn() {
  return Boolean(window.userState?.isLoggedIn && window.userState?.token);
}

function feedPinBadgesHtml(p) {
  const parts = [];
  if (p.viewerPinned) {
    parts.push('<span class="post-pinned-badge post-pinned-personal">📌 Für dich angeheftet</span>');
  }
  if (p.isGloballyPinned) {
    parts.push('<span class="post-pinned-badge">📌 Angeheftet</span>');
  }
  return parts.join('');
}

function getFeedSearchQuery() {
  return (document.getElementById('feed-search')?.value || '').trim().toLowerCase();
}

function postsMatchingSearch() {
  const q = getFeedSearchQuery();
  if (!q) return posts;
  return posts.filter((p) => {
    const hay = `${p.title} ${p.excerpt} ${p.author}`.toLowerCase();
    return hay.includes(q);
  });
}

function applyFeedSearch() {
  renderPosts();
}

function mapPostDto(item) {
  const isGloballyPinned = Boolean(item.isPinned);
  const viewerPinned = Boolean(item.viewerPinned);
  return {
    id: item.id,
    type: item.postType,
    isGloballyPinned,
    viewerPinned,
    pinned: isGloballyPinned || viewerPinned,
    title: item.title,
    excerpt: item.contentPreview || '',
    content: item.content || item.contentPreview || '',
    author: item.author?.fullName || 'Unknown',
    role: (item.author?.role || 'user').toLowerCase(),
    initials: (item.author?.fullName || 'U').split(' ').map(s => s[0]).join('').slice(0,2).toUpperCase(),
    avColor: 'rgba(61,184,122,0.1)',
    avTextColor: 'var(--green)',
    date: item.createdAt || '',
    score: item.stats?.score ?? 0,
    likes: item.stats?.likes ?? Math.max(0, item.stats?.score ?? 0),
    dislikes: item.stats?.dislikes ?? Math.max(0, -(item.stats?.score ?? 0)),
    userVote: item.viewerVote === 'up' ? 1 : item.viewerVote === 'down' ? -1 : 0,
    comments: [],
    commentsCount: item.stats?.commentsCount ?? 0
  };
}

async function refreshFeed() {
  await refreshGlobalCounts();
  const params = new URLSearchParams();
  if (currentFilter !== 'all') params.set('type', currentFilter);
  params.set('sort', currentSort);
  const res = await fetch(window.apiUrl(`/api/posts?${params.toString()}`), {
    headers: getAuthHeaders()
  });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(data.error || `HTTP ${res.status}`);
  posts = (data.items || []).map(mapPostDto);
  window.posts = posts;
  updateFeedWidgets();
  renderPosts();
  renderAdminTable();
}

async function refreshGlobalCounts() {
  const res = await fetch(window.apiUrl('/api/posts?sort=recent&limit=200'), {
    headers: getAuthHeaders()
  });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) return;
  const allPosts = (data.items || []).map(mapPostDto);
  globalCounts = {
    all: allPosts.length,
    news: allPosts.filter((p) => p.type === 'news').length,
    idea: allPosts.filter((p) => p.type === 'idea').length,
    discussion: allPosts.filter((p) => p.type === 'discussion').length,
    comments: allPosts.reduce((acc, p) => acc + (p.commentsCount || 0), 0),
    votes: allPosts.reduce((acc, p) => acc + (p.likes || 0) + (p.dislikes || 0), 0)
  };
}

function updateFeedWidgets() {
  const setText = (id, val) => {
    const el = document.getElementById(id);
    if (el) el.textContent = String(val);
  };

  setText('badge-all', globalCounts.all);
  setText('badge-news', globalCounts.news);
  setText('badge-idea', globalCounts.idea);
  setText('badge-discussion', globalCounts.discussion);

  setText('community-members', '-');
  setText('community-posts', globalCounts.all);
  setText('community-comments', globalCounts.comments);
  setText('community-votes', globalCounts.votes);

  const trending = document.getElementById('trending-list');
  if (trending) {
    const top = [...posts]
      .sort((a, b) => (b.likes || 0) - (a.likes || 0))
      .slice(0, 4);
    if (!top.length) {
      trending.innerHTML = `
        <div class="trending-item">
          <div class="trending-rank">-</div>
          <div class="trending-info"><div class="trending-title">Keine Daten</div><small>-</small></div>
        </div>
      `;
    } else {
      trending.innerHTML = top.map((p, idx) => `
        <div class="trending-item" onclick="openPost(${p.id})">
          <div class="trending-rank">${idx + 1}</div>
          <div class="trending-info">
            <div class="trending-title">${p.title}</div>
            <small>${p.likes || 0} likes · ${p.commentsCount || 0} Kommentare</small>
          </div>
        </div>
      `).join('');
    }
  }
}

function renderPosts(){
  const container = document.getElementById('posts-container');
  if(!posts.length){
    container.innerHTML=`<div class="empty-state"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg><h3>Keine Beitraege gefunden</h3><p>Sei der Erste, der in dieser Kategorie einen Beitrag veroeffentlicht.</p></div>`;
    return;
  }

  const visible = postsMatchingSearch();
  if (!visible.length) {
    container.innerHTML = `<div class="empty-state"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg><h3>Keine Treffer</h3><p>Andere Suchbegriffe versuchen oder Filter in der Seitenleiste anpassen.</p></div>`;
    return;
  }

  container.innerHTML = visible.map((p,i) => `
    <div class="post-card ${p.pinned?'pinned':''}" onclick="openPost(${p.id})" style="animation-delay:${i*0.07}s">
      <div class="post-card-body">
        <div class="post-meta">
          ${getTagHTML(p.type)}
          <span class="post-author">von <strong>${p.author}</strong> <span class="role-badge ${p.role==='admin'?'role-admin':'role-user'}">${p.role==='admin'?'Admin':'User'}</span></span>
          <span class="post-author">${p.date}</span>
          ${p.pinned?'<span class="post-pinned-badge">📌 Angeheftet</span>':''}
        </div>
        <div class="post-title">${p.title}</div>
        <div class="post-excerpt">${p.excerpt}</div>
      </div>
      <div class="post-footer" onclick="event.stopPropagation()">
        <button class="vote-btn up ${p.userVote===1?'active':''}" onclick="vote(${p.id},1)">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="18 15 12 9 6 15"/></svg>
        </button>
        <span class="score">(${p.likes ?? 0})</span>
        <button class="vote-btn down ${p.userVote===-1?'active':''}" onclick="vote(${p.id},-1)">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="6 9 12 15 18 9"/></svg>
        </button>
        <span class="score">(${p.dislikes ?? 0})</span>
        <div class="post-action" onclick="openPost(${p.id})">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
          ${p.commentsCount} Kommentar${p.commentsCount!==1?'s':''}
        </div>
        ${isLoggedIn()?`
        <div class="post-action" onclick="toggleMyPin(${p.id},event)" style="margin-left:auto;">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="17" x2="12" y2="22"/><path d="M5 17h14v-1.76a2 2 0 0 0-1.11-1.79l-1.78-.9A2 2 0 0 1 15 10.76V6h1a2 2 0 0 0 0-4H8a2 2 0 0 0 0 4h1v4.76a2 2 0 0 1-1.11 1.79l-1.78.9A2 2 0 0 0 5 15.24V17z"/></svg>
          ${p.viewerPinned?'Loesen':'Fuer dich anheften'}
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
      <td style="max-width:200px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">${p.isGloballyPinned?'📌 ':''}${p.title}</td>
      <td><span class="role-badge ${p.role==='admin'?'role-admin':'role-user'}">${p.author}</span></td>
      <td>${getTagHTML(p.type)}</td>
      <td style="color:var(--green);font-weight:700;">+${p.score}</td>
      <td><span class="status-dot ${p.isGloballyPinned?'status-pinned':'status-active'}"></span>${p.isGloballyPinned?'Global angeheftet':'Aktiv'}</td>
      <td style="display:flex;gap:6px;">-</td>
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

async function vote(id, dir){
  if (window.voteApi?.votePost) {
    await window.voteApi.votePost(id, dir);
  }
}

function syncDetailPinUi(p) {
  const personal = document.getElementById('detail-pin-btn');
  if (personal) {
    personal.textContent = p.viewerPinned ? 'Loesen' : 'Fuer dich anheften';
    personal.setAttribute('aria-label', p.viewerPinned ? 'Persoenliche Anheftung aufheben' : 'Fuer dich anheften');
    personal.classList.toggle('is-pinned', Boolean(p.viewerPinned));
  }
  const globalBtn = document.getElementById('detail-global-pin-btn');
  if (globalBtn) {
    globalBtn.textContent = p.isGloballyPinned ? 'Global loesen' : 'Global anheften';
    globalBtn.setAttribute('aria-label', p.isGloballyPinned ? 'Globale Anheftung aufheben' : 'Fuer alle anheften');
    globalBtn.classList.toggle('is-pinned', Boolean(p.isGloballyPinned));
  }
  const meta = document.querySelector('#detail-content .post-meta');
  if (!meta) return;
  meta.querySelectorAll('.post-pinned-badge').forEach((b) => b.remove());
  if (p.viewerPinned) {
    const span = document.createElement('span');
    span.className = 'post-pinned-badge post-pinned-personal';
    span.textContent = '📌 Für dich angeheftet';
    meta.appendChild(span);
  }
  if (p.isGloballyPinned) {
    const span = document.createElement('span');
    span.className = 'post-pinned-badge';
    span.textContent = '📌 Angeheftet';
    meta.appendChild(span);
  }
}

function applyViewerPinState(id, viewerPinned) {
  const inList = posts.find((x) => x.id === id);
  if (inList) {
    inList.viewerPinned = viewerPinned;
    inList.pinned = Boolean(inList.isGloballyPinned || viewerPinned);
  }
}

async function toggleMyPin(id, e) {
  if (e) e.stopPropagation();
  if (!isLoggedIn()) {
    showToast('Bitte zuerst anmelden', 'error');
    openModal('login');
    return;
  }
  try {
    const res = await fetch(window.apiUrl(`/api/posts/${id}/my-pin/toggle`), {
      method: 'POST',
      headers: getAuthHeaders()
    });
    const data = await res.json().catch(() => ({}));
    if (!res.ok) {
      showToast(data.error || `HTTP ${res.status}`, 'error');
      return;
    }
    const viewerPinned = Boolean(data.viewerPinned);
    applyViewerPinState(id, viewerPinned);
    renderPosts();
    renderAdminTable();
    if (openPostId === id) {
      const cur = posts.find((x) => x.id === id);
      if (cur) syncDetailPinUi(cur);
    }
    showToast(viewerPinned ? 'Fuer dich angeheftet 📌' : 'Anheftung aufgehoben', 'success');
  } catch {
    showToast('Netzwerkfehler', 'error');
  }
}

async function toggleGlobalPin(id, e) {
  if (e) e.stopPropagation();
  if (getCurrentUser().role !== 'admin') {
    showToast('Nur Admins koennen global anheften', 'error');
    return;
  }
  let p = posts.find((x) => x.id === id);
  let nextPinned;
  if (p) {
    nextPinned = !p.isGloballyPinned;
  } else {
    try {
      const res = await fetch(window.apiUrl(`/api/posts/${id}`), { headers: getAuthHeaders() });
      if (!res.ok) return;
      const d = await res.json();
      nextPinned = !Boolean(d.isPinned);
    } catch {
      showToast('Beitrag nicht geladen', 'error');
      return;
    }
  }
  try {
    const res = await fetch(window.apiUrl(`/api/posts/${id}/pinned`), {
      method: 'PATCH',
      headers: getAuthHeaders(),
      body: JSON.stringify({ pinned: nextPinned })
    });
    const data = await res.json().catch(() => ({}));
    if (!res.ok) {
      showToast(data.error || `HTTP ${res.status}`, 'error');
      return;
    }
    const confirmed = Boolean(data.isPinned);
    const inList = posts.find((x) => x.id === id);
    if (inList) {
      inList.isGloballyPinned = confirmed;
      inList.pinned = Boolean(confirmed || inList.viewerPinned);
    }
    renderPosts();
    renderAdminTable();
    if (openPostId === id) {
      const cur = posts.find((x) => x.id === id);
      if (cur) syncDetailPinUi(cur);
    }
    showToast(confirmed ? 'Global angeheftet 📌' : 'Globale Anheftung aufgehoben', 'success');
  } catch {
    showToast('Netzwerkfehler', 'error');
  }
}

function toggleMyPinFromDetail() {
  if (openPostId) toggleMyPin(openPostId, null);
}

function toggleGlobalPinFromDetail() {
  if (openPostId) toggleGlobalPin(openPostId, null);
}

function deletePost(id, e){
  if(e) e.stopPropagation();
  const idx = posts.findIndex(x=>x.id===id);
  if (idx < 0) return;
  posts.splice(idx,1);
  renderPosts();
  renderAdminTable();
  closeDetail();
}

async function openPost(id){
  if (typeof closeSidebar === 'function') closeSidebar();
  let p = posts.find(x=>x.id===id);
  try {
    const res = await fetch(window.apiUrl(`/api/posts/${id}`), { headers: getAuthHeaders() });
    if (!res.ok) {
      showToast('Beitrag nicht gefunden', 'error');
      return;
    }
    const detail = await res.json();
    if (p) {
      p.content = detail.content || p.excerpt;
      p.score = detail.stats?.score ?? p.score;
      p.likes = detail.stats?.likes ?? p.likes;
      p.dislikes = detail.stats?.dislikes ?? p.dislikes;
      p.commentsCount = detail.stats?.commentsCount ?? p.commentsCount;
      p.userVote = detail.viewerVote === 'up' ? 1 : detail.viewerVote === 'down' ? -1 : 0;
      p.isGloballyPinned = Boolean(detail.isPinned);
      p.viewerPinned = Boolean(detail.viewerPinned);
      p.pinned = p.isGloballyPinned || p.viewerPinned;
    } else {
      p = mapPostDto({
        id: detail.id,
        postType: detail.postType,
        title: detail.title,
        contentPreview: detail.content || '',
        content: detail.content,
        isPinned: detail.isPinned,
        viewerPinned: detail.viewerPinned,
        createdAt: detail.createdAt,
        author: detail.author,
        stats: detail.stats,
        viewerVote: detail.viewerVote
      });
    }
  } catch {
    showToast('Beitrag konnte nicht geladen werden', 'error');
    return;
  }

  if (window.commentApi?.loadCommentsForPost) {
    try {
      const loaded = await window.commentApi.loadCommentsForPost(id);
      p.comments = loaded.items || [];
      p.commentsCount = loaded.totalCount ?? p.commentsCount;
    } catch {}
  }

  openPostId = id;
  window.openPostId = id;
  const panel = document.getElementById('detail-panel');
  const adminBtns = document.getElementById('detail-admin-btns');
  const pinBtn = document.getElementById('detail-pin-btn');
  const globalPinBtn = document.getElementById('detail-global-pin-btn');
  if (pinBtn) {
    pinBtn.style.display = isLoggedIn() ? '' : 'none';
  }
  if (globalPinBtn) {
    globalPinBtn.style.display = getCurrentUser().role === 'admin' ? '' : 'none';
  }

  adminBtns.innerHTML = getCurrentUser().role==='admin' ? `
    <button class="btn btn-sm" style="background:rgba(224,85,85,0.1);color:var(--red);border:1px solid var(--red);" onclick="deletePost(${id},event)">Loeschen</button>
  ` : '';

  document.getElementById('detail-content').innerHTML = `
    <div class="post-meta">${getTagHTML(p.type)}<span class="post-author">von <strong>${p.author}</strong> <span class="role-badge ${p.role==='admin'?'role-admin':'role-user'}">${p.role==='admin'?'Admin':'User'}</span></span><span class="post-author">${p.date}</span></div>
    <div class="detail-title">${p.title}</div>
    <div style="display:flex;align-items:center;gap:10px;margin-bottom:20px;">
      <button class="vote-btn up ${p.userVote===1?'active':''}" onclick="vote(${p.id},1)"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="18 15 12 9 6 15"/></svg></button>
      <span class="score" style="font-size:0.95rem;">(${p.likes ?? 0})</span>
      <button class="vote-btn down ${p.userVote===-1?'active':''}" onclick="vote(${p.id},-1)"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="6 9 12 15 18 9"/></svg></button>
      <span class="score" style="font-size:0.95rem;">(${p.dislikes ?? 0})</span>
    </div>
    <div class="detail-body">${p.content || p.excerpt}</div>
    <div class="comments-section">
      <div class="comments-title">💬 ${p.commentsCount} Kommentar${p.commentsCount!==1?'s':''}</div>
      ${window.commentApi?.renderCommentsHtml ? window.commentApi.renderCommentsHtml(p.id, p.comments || []) : ''}
      <div class="comment-form">
        <div class="comment-form-title">Dein Kommentar</div>
        <textarea class="form-input" id="comment-input-${p.id}" placeholder="Teile deine Meinung..." style="min-height:80px;margin-bottom:10px;"></textarea>
        <button class="btn btn-gold btn-sm" onclick="submitComment(${p.id})">Veroeffentlichen</button>
      </div>
    </div>
  `;
  syncDetailPinUi(p);
  panel.classList.add('open');
}

function closeDetail(){
  document.getElementById('detail-panel').classList.remove('open');
  openPostId = null;
  window.openPostId = null;
  if (typeof closeSidebar === 'function') closeSidebar();
}

function setFilter(type){
  currentFilter = type;
  if (typeof navigate === 'function') {
    navigate('feed');
  }
  if (typeof closeSidebar === 'function') closeSidebar();
  refreshFeed().catch(e => showToast(e.message, 'error'));
}

function showPopularPosts(){
  currentSort = 'popular';
  currentFilter = 'all';
  if (typeof navigate === 'function') navigate('feed');
  if (typeof closeSidebar === 'function') closeSidebar();
  const alleBtn = document.querySelector('#sidebar > button.sidebar-item');
  if (alleBtn && typeof setSidebarActive === 'function') setSidebarActive(alleBtn);
  refreshFeed().catch(e => showToast(e.message, 'error'));
}

function setSidebarActive(el){
  document.querySelectorAll('.sidebar-item').forEach(i=>i.classList.remove('active'));
  el.classList.add('active');
}

function selectTag(tag){
  selectedTag = tag;
  document.querySelectorAll('.tag-opt').forEach(el=>{ el.className = 'tag-opt'; });
  document.getElementById('tag-'+tag).className = 'tag-opt sel-'+tag;
}

async function submitPost(){
  if (!window.userState || !window.userState.isLoggedIn) {
    showToast('Bitte zuerst anmelden', 'error');
    openModal('login');
    return;
  }
  const title = document.getElementById('post-title').value.trim();
  const body = document.getElementById('post-body').value.trim();
  if(!title||!body){ showToast('Bitte Titel und Inhalt ausfuellen','error'); return; }

  const token = window.userState?.token;
  const res = await fetch(window.apiUrl('/api/posts'), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: JSON.stringify({ postType: selectedTag, title, content: body })
  });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    showToast(data.error || `HTTP ${res.status}`, 'error');
    return;
  }

  closeModal('create');
  document.getElementById('post-title').value='';
  document.getElementById('post-body').value='';
  await refreshFeed();
  showToast('Beitrag erfolgreich veroeffentlicht ✓','success');
}

window.openPost = openPost;
window.renderPosts = renderPosts;
window.showPopularPosts = showPopularPosts;
window.toggleMyPin = toggleMyPin;
window.toggleGlobalPin = toggleGlobalPin;
window.toggleMyPinFromDetail = toggleMyPinFromDetail;
window.toggleGlobalPinFromDetail = toggleGlobalPinFromDetail;
