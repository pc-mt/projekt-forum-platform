(function () {
  function authHeaders() {
    const headers = { 'Content-Type': 'application/json' };
    const token = window.userState?.token;
    if (token) headers.Authorization = `Bearer ${token}`;
    return headers;
  }

  function initials(name) {
    return (name || 'U').split(' ').map((x) => x[0]).join('').slice(0, 2).toUpperCase();
  }

  function mapComment(node) {
    const author = node.author || {};
    const replies = Array.isArray(node.replies) ? node.replies.map(mapComment) : [];
    return {
      id: node.id,
      postId: node.postId,
      parentCommentId: node.parentCommentId ?? null,
      body: node.content || '',
      author: author.fullName || 'Unknown',
      initials: initials(author.fullName),
      date: node.createdAt || '',
      score: node.stats?.score ?? 0,
      likes: node.stats?.likes ?? Math.max(0, node.stats?.score ?? 0),
      dislikes: node.stats?.dislikes ?? Math.max(0, -(node.stats?.score ?? 0)),
      userVote: node.viewerVote === 'up' ? 1 : node.viewerVote === 'down' ? -1 : 0,
      replies
    };
  }

  function flattenCount(comments) {
    return comments.reduce((acc, c) => acc + 1 + flattenCount(c.replies || []), 0);
  }

  async function loadCommentsForPost(postId) {
    const res = await fetch(`/api/posts/${postId}/comments`, { headers: authHeaders() });
    const data = await res.json().catch(() => ({}));
    if (!res.ok) throw new Error(data.error || `HTTP ${res.status}`);
    const items = (data.items || []).map(mapComment);
    return { items, totalCount: flattenCount(items) };
  }

  function renderNode(postId, c, depth) {
    const ml = depth * 22;
    const replyBoxId = `reply-box-${postId}-${c.id}`;
    const replyInputId = `reply-input-${postId}-${c.id}`;
    return `
      <div class="comment" style="margin-left:${ml}px;">
        <div class="comment-meta">
          <div class="comment-av" style="background:rgba(61,184,122,0.1);color:var(--green);">${c.initials}</div>
          <div class="comment-author">${c.author}</div>
          <div class="comment-date">${c.date}</div>
        </div>
        <div class="comment-body">${c.body}</div>
        <div class="comment-actions">
          <span class="comment-action ${c.userVote===1?'active-like':''}" onclick="voteComment(${postId},${c.id},1)">👍 Like (${c.likes ?? 0})</span>
          <span class="comment-action ${c.userVote===-1?'active-dislike':''}" onclick="voteComment(${postId},${c.id},-1)">👎 Dislike (${c.dislikes ?? 0})</span>
          <span class="comment-action" onclick="toggleReplyBox('${replyBoxId}')">↩️ Antworten</span>
        </div>
        <div id="${replyBoxId}" style="display:none;margin-top:8px;">
          <textarea class="form-input" id="${replyInputId}" placeholder="Antwort schreiben..." style="min-height:70px;margin-bottom:8px;"></textarea>
          <button class="btn btn-ghost btn-sm" onclick="submitReply(${postId},${c.id},'${replyInputId}')">Antwort senden</button>
        </div>
        ${(c.replies || []).map((r) => renderNode(postId, r, depth + 1)).join('')}
      </div>
    `;
  }

  function renderCommentsHtml(postId, comments) {
    if (!comments.length) {
      return '<p style="color:var(--text3);font-size:0.85rem;">Noch keine Kommentare.</p>';
    }
    return comments.map((c) => renderNode(postId, c, 0)).join('');
  }

  async function submitComment(postId) {
    if (!window.userState?.isLoggedIn) {
      window.showToast?.('Bitte zuerst anmelden', 'error');
      window.openModal?.('login');
      return;
    }
    const input = document.getElementById(`comment-input-${postId}`);
    const content = input?.value?.trim();
    if (!content) {
      window.showToast?.('Schreibe zuerst einen Kommentar', 'error');
      return;
    }
    const res = await fetch(`/api/posts/${postId}/comments`, {
      method: 'POST',
      headers: authHeaders(),
      body: JSON.stringify({ content })
    });
    const data = await res.json().catch(() => ({}));
    if (!res.ok) {
      window.showToast?.(data.error || `HTTP ${res.status}`, 'error');
      return;
    }
    if (input) input.value = '';
    window.openPost?.(postId);
    window.showToast?.('Kommentar veroeffentlicht ✓', 'success');
  }

  async function submitReply(postId, parentCommentId, inputId) {
    if (!window.userState?.isLoggedIn) {
      window.showToast?.('Bitte zuerst anmelden', 'error');
      window.openModal?.('login');
      return;
    }
    const input = document.getElementById(inputId);
    const content = input?.value?.trim();
    if (!content) {
      window.showToast?.('Antwort darf nicht leer sein', 'error');
      return;
    }
    const res = await fetch(`/api/posts/${postId}/comments`, {
      method: 'POST',
      headers: authHeaders(),
      body: JSON.stringify({ content, parentCommentId })
    });
    const data = await res.json().catch(() => ({}));
    if (!res.ok) {
      window.showToast?.(data.error || `HTTP ${res.status}`, 'error');
      return;
    }
    window.openPost?.(postId);
    window.showToast?.('Antwort gesendet ✓', 'success');
  }

  async function voteComment(postId, commentId, dir) {
    if (window.voteApi?.voteComment) {
      const ok = await window.voteApi.voteComment(postId, commentId, dir);
      if (ok) {
        window.openPost?.(postId);
      }
    }
  }

  function toggleReplyBox(id) {
    const el = document.getElementById(id);
    if (!el) return;
    el.style.display = el.style.display === 'none' ? 'block' : 'none';
  }

  window.commentApi = {
    loadCommentsForPost,
    renderCommentsHtml
  };
  window.submitComment = submitComment;
  window.submitReply = submitReply;
  window.voteComment = voteComment;
  window.toggleReplyBox = toggleReplyBox;
})();
