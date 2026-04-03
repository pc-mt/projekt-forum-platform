# User API Documentation (`/api/auth` + `/api/profile`)

Diese Datei dokumentiert die User-/Auth-Routen fuer das aktuelle Frontend.
Ziel: Login/Registrierung implementieren und direkt danach `Mein Profil` mit echten Daten fuellen.

## Frontend input/output reference

Aus `frontend/index.html` + `frontend/app.js` ergibt sich:

- **Registrierung Felder:** `reg-name`, `reg-email`, `reg-pass`
- **Login Felder:** `login-email`, `login-pass`
- **Profil Felder (nach Login):**
  - Name
  - Rolle
  - E-Mail + Mitglied seit
  - Stats: Posts, Erhaltene Stimmen, Kommentare, Punkte
  - Letzte eigene Beitraege

---

## 1) Registrierung

Legt einen neuen Nutzer an.

**HTTP:** `POST`  
**URL:** `/api/auth/register`

### Request (JSON)
```json
{
  "fullName": "Jonas Klein",
  "email": "jonas@org.eu",
  "password": "secret123"
}
```

### Success
`201 Created`
```json
{
  "token": "JWT_TOKEN_HERE",
  "user": {
    "id": 42,
    "fullName": "Jonas Klein",
    "email": "jonas@org.eu",
    "role": "user",
    "avatarUrl": null,
    "memberSince": "2026-04-03T18:20:00Z"
  },
  "stats": {
    "postsCount": 0,
    "receivedVotes": 0,
    "commentsCount": 0,
    "points": 0
  }
}
```

### Errors
`400 Bad Request`
```json
{ "error": "fullName is required" }
```

`400 Bad Request`
```json
{ "error": "invalid email format" }
```

`400 Bad Request`
```json
{ "error": "password must be at least 8 characters" }
```

`409 Conflict`
```json
{ "error": "email already exists" }
```

`500 Internal Server Error`
```json
{ "error": "internal server error" }
```

---

## 2) Login

Authentifiziert den Nutzer und liefert JWT + User-Profil-Kerndaten.

**HTTP:** `POST`  
**URL:** `/api/auth/login`

### Request (JSON)
```json
{
  "email": "jonas@org.eu",
  "password": "secret123"
}
```

### Success
`200 OK`
```json
{
  "token": "JWT_TOKEN_HERE",
  "user": {
    "id": 42,
    "fullName": "Jonas Klein",
    "email": "jonas@org.eu",
    "role": "user",
    "avatarUrl": null,
    "memberSince": "2026-04-03T18:20:00Z"
  },
  "stats": {
    "postsCount": 18,
    "receivedVotes": 342,
    "commentsCount": 67,
    "points": 1920
  }
}
```

### Errors
`400 Bad Request`
```json
{ "error": "email is required" }
```

`400 Bad Request`
```json
{ "error": "password is required" }
```

`401 Unauthorized`
```json
{ "error": "invalid credentials" }
```

`500 Internal Server Error`
```json
{ "error": "internal server error" }
```

---

## 3) Current user (`me`)

Liefert den aktuell angemeldeten Benutzer basierend auf JWT.

**HTTP:** `GET`  
**URL:** `/api/auth/me`  
**Auth:** `Authorization: Bearer <token>`

### Success
`200 OK`
```json
{
  "user": {
    "id": 42,
    "fullName": "Jonas Klein",
    "email": "jonas@org.eu",
    "role": "user",
    "avatarUrl": null,
    "memberSince": "2026-04-03T18:20:00Z"
  }
}
```

### Errors
`401 Unauthorized`
```json
{ "error": "missing token" }
```

`401 Unauthorized`
```json
{ "error": "invalid token" }
```

---

## 4) Profile details for `Mein Profil`

Liefert Profil + Stats + letzte Posts fuer die Profilseite.

**HTTP:** `GET`  
**URL:** `/api/profile/me`  
**Auth:** `Authorization: Bearer <token>`

### Success
`200 OK`
```json
{
  "user": {
    "id": 42,
    "fullName": "Jonas Klein",
    "email": "jonas@org.eu",
    "role": "user",
    "avatarUrl": null,
    "memberSince": "2026-03-01T10:00:00Z"
  },
  "stats": {
    "postsCount": 18,
    "receivedVotes": 342,
    "commentsCount": 67,
    "points": 1920
  },
  "recentPosts": [
    {
      "id": 201,
      "title": "Integration Slack",
      "postType": "idea",
      "createdAt": "2026-04-02T12:34:00Z",
      "score": 98,
      "commentsCount": 15
    }
  ]
}
```

### Errors
`401 Unauthorized`
```json
{ "error": "unauthorized" }
```

`500 Internal Server Error`
```json
{ "error": "internal server error" }
```

---

## Validation rules (recommended)

- `fullName`: required, min 2 chars, max 120
- `email`: required, valid email format, max 150, unique
- `password`: required, min 8 chars, max 72 (bcrypt-safe)
- `role`: server-side controlled (`user` default)

---

## Frontend mapping checklist

- `doRegister()` uses `POST /api/auth/register`
- `doLogin()` uses `POST /api/auth/login`
- store `token` in `localStorage`
- update nav greeting from `user.fullName`
- profile page fields from `GET /api/profile/me`

