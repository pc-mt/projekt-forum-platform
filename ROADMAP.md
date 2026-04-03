# DMTree Community - Roadmap Implementation

## Goal
Build feature by feature, in stable increments, starting with the authentication foundation and then profile, exactly as planned.

## Current Frontend Reality (must drive backend contracts)
The `Mein Profil` page currently displays:
- Full name
- Role (`Admin` / `User`)
- Email + member since date
- Stats: posts count, received votes, comments count, points
- User initials/avatar
- Recent posts list

So after `Anmelden`, backend must already return enough user data to render this profile without fake placeholders.

---

## Phase 0 - Clean baseline (Day 0)
1. Freeze current UI split (`frontend/index.html`, `frontend/styles.css`, `frontend/app.js`)
2. Keep language consistency (German or English only)
3. Confirm DB schema applied (`users`, `posts`, `comments`, `votes`)
4. Create `.env` locally and run full stack once

**Deliverable**
- App starts with Docker
- Database tables present and seeded

---

## Phase 1 - Authentication first (Anmelden / Registrieren) (Priority 1)

### 1.1 Backend auth endpoints
Implement:
- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me` (JWT protected)

### 1.2 Password & security basics
- Hash passwords with BCrypt
- Validate email uniqueness
- Minimal JWT with expiration
- Return sanitized user object (never password hash)

### 1.3 Response contract (important)
After successful login/register, return:
- `token`
- `user`: `{ id, fullName, email, role, avatarUrl, memberSince }`
- optional profile stats snapshot:
  `{ postsCount, receivedVotes, commentsCount, points }`

### 1.4 Frontend wiring
- `doRegister()` -> call register endpoint
- `doLogin()` -> call login endpoint
- Save token (`localStorage`)
- Update nav state from real backend response
- Handle API errors (invalid credentials, duplicate email)

**Deliverable**
- User can register, login, refresh page, stay authenticated

---

## Phase 2 - My Profile right after login (Priority 2)

### 2.1 Profile data endpoint
Implement:
- `GET /api/profile/me` (or reuse `/api/auth/me` + stats endpoint)

Returned fields (minimum):
- identity: `fullName`, `email`, `role`, `avatarUrl`, `memberSince`
- stats: `postsCount`, `receivedVotes`, `commentsCount`, `points`
- recent posts list: `[{ id, title, postType, createdAt, score, commentsCount }]`

### 2.2 Frontend profile integration
- Replace hardcoded profile values with API data
- Fill `#profile-posts` from backend
- Fallback states: loading / empty / error

**Deliverable**
- After `Anmelden`, `Mein Profil` is fully real-data driven

---

## Phase 3 - Logout flow (Abmelden) (Priority 3)
- Clear token + local auth state
- Reset nav to guest mode
- Protect profile/admin pages when unauthenticated

**Deliverable**
- Clean logout + route protection baseline

---

## Phase 4 - Posts core (Priority 4)
- `GET /api/posts` (filters: type, sort, pagination)
- `POST /api/posts`
- `GET /api/posts/:id`
- optional admin actions: pin/unpin, soft-delete

Frontend:
- connect feed rendering to API
- replace in-memory `posts` with backend data

**Deliverable**
- Feed and post details persisted in DB

---

## Phase 5 - Comments (Priority 5)
- `GET /api/posts/:id/comments`
- `POST /api/posts/:id/comments`
- `DELETE /api/comments/:id` (owner or admin)

**Deliverable**
- Post discussion works end-to-end

---

## Phase 6 - Votes / Prioritization (Priority 6)
- `POST /api/posts/:id/vote` (`up` / `down`)
- update, remove, or switch vote
- enforce unique `(post_id, user_id)` at DB level
- expose computed score in post DTO

**Deliverable**
- Reliable voting and ranking by popularity

---

## Phase 7 - Admin essentials (Priority 7)
- role-based authorization middleware
- admin-only endpoints for moderation
- wire admin table actions to backend

**Deliverable**
- Admin dashboard actions are real and secured

---

## Phase 8 - Hardening & polish (Priority 8)
- input validation everywhere
- consistent error model
- CORS/auth edge cases
- basic tests (auth + profile + posts)
- README + process document (required by assignment)

**Deliverable**
- Submission-ready prototype

---

## Suggested execution order for this week
1. Auth endpoints + JWT + frontend login/register
2. Profile endpoint + frontend `Mein Profil` real binding
3. Logout + route guard
4. Posts API integration
5. Comments
6. Votes
7. Admin actions
8. Cleanup/tests/docs

---

## Definition of done for the next step (immediate)
For the next commit, only target:
- `register/login/me` complete
- token persisted
- profile values loaded from backend

If this is stable, we continue with posts.
