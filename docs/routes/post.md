# Post API Documentation (`/api/posts`)

Diese Datei beschreibt die Feed-/Post-Routen fuer:
- `all` (alles anzeigen)
- Filter pro Typ (`news`, `idea`, `discussion`)
- Beitrag erstellen (Titel + Inhalt + Typ)

Sie ist auf das aktuelle Frontend-Verhalten abgestimmt.

---

## Zielverhalten im Frontend

### Feed-Filter
- Klick auf **Alle** -> zeigt alle Beitraege
- Klick auf **Neuigkeiten** -> zeigt nur `post_type = news`
- Klick auf **Ideen** -> zeigt nur `post_type = idea`
- Klick auf **Diskussionen** -> zeigt nur `post_type = discussion`

### Neuer Beitrag
- Nutzer waehlt Typ + Titel + Inhalt
- Beitrag wird gespeichert
- Feed wird direkt neu geladen

Empfehlung: Nach erfolgreichem `POST /api/posts` direkt `GET /api/posts` neu aufrufen (oder optimistisch lokal ergaenzen + spaeter synchronisieren).

---

## 1) Get posts (all + filter)

Liefert den Feed. Dieselbe Route deckt **all** und **typ-spezifische Filter** ab.

**HTTP:** `GET`  
**URL:** `/api/posts`

### Query-Parameter
- `type` (optional): `news | idea | discussion`
- `sort` (optional): `popular | recent | comments`
- `page` (optional): integer, default `1`
- `limit` (optional): integer, default `20`

### Beispiele
- Alle Beitraege:
  - `GET /api/posts`
- Nur Neuigkeiten:
  - `GET /api/posts?type=news`
- Nur Ideen:
  - `GET /api/posts?type=idea`
- Nur Diskussionen:
  - `GET /api/posts?type=discussion`
- Ideen nach Popularitaet:
  - `GET /api/posts?type=idea&sort=popular`

### Success
`200 OK`
```json
{
  "items": [
    {
      "id": 11,
      "postType": "idea",
      "title": "Slack Notification Integration",
      "contentPreview": "Idee: Ein Slack-Bot fuer neue Posts...",
      "isPinned": false,
      "status": "published",
      "createdAt": "2026-04-01T11:30:00Z",
      "updatedAt": "2026-04-01T11:30:00Z",
      "author": {
        "id": 11,
        "fullName": "Sofia Radic",
        "role": "user",
        "avatarUrl": null
      },
      "stats": {
        "score": 2,
        "commentsCount": 2
      },
      "viewerVote": "up"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 42
  }
}
```

### Errors
`400 Bad Request`
```json
{ "error": "invalid query parameter: type" }
```

`500 Internal Server Error`
```json
{ "error": "internal server error" }
```

---

## 2) Create post

Erstellt eine Neuigkeit, Idee oder Diskussion.

**HTTP:** `POST`  
**URL:** `/api/posts`  
**Auth:** `Authorization: Bearer <token>`

### Request (JSON)
```json
{
  "postType": "discussion",
  "title": "Onboarding Verbesserung",
  "content": "Welche 3 Aenderungen helfen neuen Nutzern am meisten?"
}
```

### Validation
- `postType` erforderlich, nur `news|idea|discussion`
- `title` erforderlich, z. B. 3-200 Zeichen
- `content` erforderlich, z. B. 10-10000 Zeichen

### Success
`201 Created`
```json
{
  "id": 101,
  "postType": "discussion",
  "title": "Onboarding Verbesserung",
  "content": "Welche 3 Aenderungen helfen neuen Nutzern am meisten?",
  "isPinned": false,
  "status": "published",
  "createdAt": "2026-04-03T19:15:00Z",
  "author": {
    "id": 42,
    "fullName": "Jonas Klein",
    "role": "user",
    "avatarUrl": null
  }
}
```

### Errors
`400 Bad Request`
```json
{ "error": "title is required" }
```

`400 Bad Request`
```json
{ "error": "postType must be one of: news, idea, discussion" }
```

`401 Unauthorized`
```json
{ "error": "unauthorized" }
```

`500 Internal Server Error`
```json
{ "error": "internal server error" }
```

---

## 3) Get post detail

Liefert einen einzelnen Beitrag (fuer Detailansicht), typischerweise inklusive Kommentaranzahl und Score.

**HTTP:** `GET`  
**URL:** `/api/posts/:id`

### Success
`200 OK`
```json
{
  "id": 11,
  "postType": "idea",
  "title": "Slack Notification Integration",
  "content": "Idee: Ein Slack-Bot fuer neue Posts, Kommentare und Abstimmungen.",
  "isPinned": false,
  "status": "published",
  "createdAt": "2026-04-01T11:30:00Z",
  "author": {
    "id": 11,
    "fullName": "Sofia Radic",
    "role": "user"
  },
  "stats": {
    "score": 2,
    "commentsCount": 2
  },
  "viewerVote": null
}
```

### Errors
`404 Not Found`
```json
{ "error": "post not found" }
```

---

## Umsetzungsempfehlung (einfach und robust)

1. **Nur eine Feed-Route** (`GET /api/posts`) mit `type`-Filter  
   -> weniger Komplexitaet als mehrere fast identische Endpunkte.

2. **SQL-Filter dynamisch**
   - ohne `type`: alle `status='published'`
   - mit `type`: plus `post_type = ?`

3. **Sortierung in SQL**
   - `recent`: `ORDER BY created_at DESC`
   - `popular`: nach Vote-Score
   - `comments`: nach Kommentaranzahl

4. **Nach Create sofort Feed refreshen**
   - Frontend: `POST /api/posts` -> bei Erfolg `GET /api/posts?currentFilters...`
   - garantiert konsistente Anzeige.

5. **Soft delete / moderation spaeter**
   - Statusfeld (`published|archived|deleted`) ist schon vorbereitet.

---

## Frontend mapping checklist

- `setFilter('all')` -> `GET /api/posts`
- `setFilter('news')` -> `GET /api/posts?type=news`
- `setFilter('idea')` -> `GET /api/posts?type=idea`
- `setFilter('discussion')` -> `GET /api/posts?type=discussion`
- `sortPosts('popular'|'recent'|'comments')` -> `GET /api/posts?sort=...`
- `submitPost()` -> `POST /api/posts` then refresh list

