(function () {
  function authHeaders() {
    const headers = { 'Content-Type': 'application/json' };
    const token = window.userState?.token;
    if (token) headers.Authorization = `Bearer ${token}`;
    return headers;
  }

  async function votePost(postId, dir) {
    const p = window.getPostById ? window.getPostById(postId) : null;
    if (!window.userState?.isLoggedIn) {
      window.showToast?.('Bitte zuerst anmelden', 'error');
      window.openModal?.('login');
      return false;
    }
    const voteType = dir === 1 ? 'up' : 'down';
    const res = await fetch(window.apiUrl(`/api/posts/${postId}/vote`), {
      method: 'POST',
      headers: authHeaders(),
      body: JSON.stringify({ voteType })
    });
    const data = await res.json().catch(() => ({}));
    if (!res.ok) {
      window.showToast?.(data.error || `HTTP ${res.status}`, 'error');
      return false;
    }
    if (p) {
      p.score = data.score ?? p.score;
      p.likes = data.likes ?? p.likes;
      p.dislikes = data.dislikes ?? p.dislikes;
      p.userVote = data.viewerVote === 'up' ? 1 : data.viewerVote === 'down' ? -1 : 0;
    }
    if (typeof window.renderPosts === 'function') window.renderPosts();
    if (window.openPostId === postId && typeof window.openPost === 'function') window.openPost(postId);
    return true;
  }

  async function voteComment(postId, commentId, dir) {
    if (!window.userState?.isLoggedIn) {
      window.showToast?.('Bitte zuerst anmelden', 'error');
      window.openModal?.('login');
      return false;
    }
    const voteType = dir === 1 ? 'up' : 'down';
    const res = await fetch(window.apiUrl(`/api/comments/${commentId}/vote`), {
      method: 'POST',
      headers: authHeaders(),
      body: JSON.stringify({ voteType })
    });
    const data = await res.json().catch(() => ({}));
    if (!res.ok) {
      window.showToast?.(data.error || `HTTP ${res.status}`, 'error');
      return false;
    }
    return true;
  }

  window.voteApi = {
    votePost,
    voteComment
  };
})();
