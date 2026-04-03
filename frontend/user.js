(function () {
  const TOKEN_KEY = "chat_platform_token";

  const userState = {
    token: localStorage.getItem(TOKEN_KEY),
    currentUser: null,
    profile: null,
    isLoggedIn: false,
  };
  window.userState = userState;

  function authHeaders() {
    const headers = { "Content-Type": "application/json" };
    if (userState.token) headers.Authorization = `Bearer ${userState.token}`;
    return headers;
  }

  function saveToken(token) {
    userState.token = token || null;
    if (token) localStorage.setItem(TOKEN_KEY, token);
    else localStorage.removeItem(TOKEN_KEY);
  }

  function formatMemberSince(raw) {
    if (!raw) return "-";
    const date = new Date(raw);
    if (Number.isNaN(date.getTime())) return raw;
    return date.toLocaleDateString("de-DE", { year: "numeric", month: "long" });
  }

  function initialsFromName(name) {
    if (!name) return "U";
    return name
      .trim()
      .split(/\s+/)
      .map((p) => p[0])
      .join("")
      .slice(0, 2)
      .toUpperCase();
  }

  const WIDGETS_TOGGLE = `
    <button class="widgets-toggle" id="widgets-toggle" onclick="toggleWidgetsPanel()" aria-label="Widgets anzeigen">
      <span></span><span></span><span></span>
    </button>`;

  function applyRoleVisibility() {
    const role = (userState.currentUser?.role || "user").toLowerCase();
    const isAdmin = userState.isLoggedIn && role === "admin";

    const navAdmin = document.getElementById("nav-admin");
    const sidebarAdmin = document.getElementById("sidebar-admin-link");
    const adminPage = document.getElementById("page-admin");

    if (navAdmin) navAdmin.style.display = isAdmin ? "" : "none";
    if (sidebarAdmin) sidebarAdmin.style.display = isAdmin ? "" : "none";
    if (adminPage) adminPage.style.display = isAdmin ? "" : "none";
  }

  function updateNavLoggedIn() {
    const nav = document.getElementById("nav-right");
    if (!nav) return;
    const firstName = (userState.currentUser?.name || "User").split(" ")[0];
    const initials = userState.currentUser?.initials || "U";
    nav.innerHTML = `
      ${WIDGETS_TOGGLE}
      <span style="font-size:0.82rem;color:var(--text2);">Hallo, <strong>${firstName}</strong></span>
      <div class="avatar" onclick="navigate('profile')" title="Mein Profil">${initials}</div>
    `;
    applyRoleVisibility();
  }

  function updateNavLoggedOut() {
    const nav = document.getElementById("nav-right");
    if (!nav) return;
    nav.innerHTML = `
      ${WIDGETS_TOGGLE}
      <button class="btn btn-ghost btn-sm" onclick="openModal('login')">Anmelden</button>
      <button class="btn btn-gold btn-sm" onclick="openModal('register')">Registrieren</button>
    `;
    applyRoleVisibility();
  }

  function applyProfileToUi(payload) {
    const user = payload?.user || {};
    const stats = payload?.stats || {};
    const role = (user.role || "user").toLowerCase();

    const nameText = document.getElementById("profile-name-text");
    const roleBadge = document.getElementById("profile-role-badge");
    const emailText = document.getElementById("profile-email-text");
    const memberSince = document.getElementById("profile-member-since");
    const postsCount = document.getElementById("profile-posts-count");
    const receivedVotes = document.getElementById("profile-received-votes");
    const commentsCount = document.getElementById("profile-comments-count");
    const points = document.getElementById("profile-points");
    const avatar = document.querySelector(".profile-av-lg");

    if (nameText) nameText.textContent = user.fullName || "-";
    if (roleBadge) {
      roleBadge.textContent = role === "admin" ? "Admin" : "User";
      roleBadge.className = `role-badge ${role === "admin" ? "role-admin" : "role-user"}`;
    }
    if (emailText) emailText.textContent = user.email || "-";
    if (memberSince) memberSince.textContent = `Mitglied seit ${formatMemberSince(user.memberSince)}`;
    if (postsCount) postsCount.textContent = String(stats.postsCount ?? 0);
    if (receivedVotes) receivedVotes.textContent = String(stats.receivedVotes ?? 0);
    if (commentsCount) commentsCount.textContent = String(stats.commentsCount ?? 0);
    if (points) points.textContent = String(stats.points ?? 0);
    if (avatar) avatar.textContent = initialsFromName(user.fullName);
  }

  async function handleAuth(url, body) {
    const response = await fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
      throw new Error(data.error || `HTTP ${response.status}`);
    }
    saveToken(data.token);
    userState.currentUser = {
      id: data.user.id,
      name: data.user.fullName,
      email: data.user.email,
      role: data.user.role,
      initials: initialsFromName(data.user.fullName),
      avatarUrl: data.user.avatarUrl ?? null,
      memberSince: data.user.memberSince ?? null,
    };
    userState.profile = { user: data.user, stats: data.stats || {}, recentPosts: [] };
    userState.isLoggedIn = true;
    updateNavLoggedIn();
    applyProfileToUi(userState.profile);
    return data;
  }

  window.doLogin = async function doLogin() {
    const email = document.getElementById("login-email")?.value?.trim();
    const password = document.getElementById("login-pass")?.value ?? "";
    if (!email) return window.showToast?.("Bitte E-Mail eingeben", "error");
    if (!password) return window.showToast?.("Bitte Passwort eingeben", "error");
    try {
      await handleAuth("/api/auth/login", { email, password });
      window.closeModal?.("login");
      window.showToast?.("Erfolgreich angemeldet ✓", "success");
    } catch (e) {
      window.showToast?.(e.message, "error");
    }
  };

  window.doRegister = async function doRegister() {
    const fullName = document.getElementById("reg-name")?.value?.trim();
    const email = document.getElementById("reg-email")?.value?.trim();
    const password = document.getElementById("reg-pass")?.value ?? "";
    if (!fullName || !email || !password) return window.showToast?.("Bitte alle Felder ausfuellen", "error");
    try {
      await handleAuth("/api/auth/register", { fullName, email, password });
      window.closeModal?.("register");
      window.showToast?.("Konto erfolgreich erstellt ✓", "success");
    } catch (e) {
      window.showToast?.(e.message, "error");
    }
  };

  window.logout = function logout() {
    saveToken(null);
    userState.currentUser = null;
    userState.profile = null;
    userState.isLoggedIn = false;
    updateNavLoggedOut();
    applyRoleVisibility();
    if (typeof window.navigate === "function") window.navigate("feed");
    window.showToast?.("Abgemeldet", "success");
  };

  window.loadProfileData = async function loadProfileData() {
    if (!userState.token) return;
    try {
      const response = await fetch("/api/profile/me", { headers: authHeaders() });
      if (!response.ok) {
        if (response.status === 401) {
          window.logout();
        }
        return;
      }
      const data = await response.json();
      userState.profile = data;
      if (data.user) {
        userState.currentUser = {
          id: data.user.id,
          name: data.user.fullName,
          email: data.user.email,
          role: data.user.role,
          initials: initialsFromName(data.user.fullName),
          avatarUrl: data.user.avatarUrl ?? null,
          memberSince: data.user.memberSince ?? null,
        };
      }
      userState.isLoggedIn = true;
      updateNavLoggedIn();
      applyProfileToUi(data);
      if (typeof window.renderProfile === "function") window.renderProfile();
    } catch {
      // silent fallback
    }
  };

  (async function initAuth() {
    if (!userState.token) {
      updateNavLoggedOut();
      return;
    }
    try {
      const res = await fetch("/api/auth/me", { headers: authHeaders() });
      if (!res.ok) {
        window.logout();
        return;
      }
      const data = await res.json();
      const user = data.user || {};
      userState.currentUser = {
        id: user.id,
        name: user.fullName,
        email: user.email,
        role: user.role || "user",
        initials: initialsFromName(user.fullName),
        avatarUrl: user.avatarUrl ?? null,
        memberSince: user.memberSince ?? null,
      };
      userState.isLoggedIn = true;
      updateNavLoggedIn();
      applyRoleVisibility();
      window.loadProfileData();
    } catch {
      window.logout();
    }
  })();

  applyRoleVisibility();
})();

