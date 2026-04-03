# DMTree Community — Plattform-Prototyp

**Full-Stack**-Prototyp für eine interne Community: Feed mit **News, Ideen und Diskussionen**, **Authentifizierung**, **Votings**, **Kommentaren** (inkl. Antworten), **Rollen** (Nutzer / Administrator), **globalem Anheften** (Admin) und **persönlichem Anheften** pro Nutzer. Die Benutzeroberfläche ist überwiegend auf **Deutsch**.

---

## Inhalt

1. [Voraussetzungen](#voraussetzungen)
2. [Schnellstart mit Docker (empfohlen)](#schnellstart-mit-docker-empfohlen)
3. [Zugriff nach dem Start](#zugriff-nach-dem-start)
4. [Nutzerkonto](#nutzerkonto)
5. [Repository-Struktur](#repository-struktur)
6. [Technischer Stack](#technischer-stack)
7. [Frontend: Aktualisierung (Polling) und UI-Updates](#frontend-aktualisierung-polling-und-ui-updates)
8. [REST-API-Routen](#rest-api-routen)
9. [API-Abfrageparameter](#api-abfrageparameter)
10. [Umgebungsvariablen](#umgebungsvariablen)
11. [Fehlerbehebung](#fehlerbehebung)
12. [Entwicklung ohne Docker](#entwicklung-ohne-docker)

---

## Voraussetzungen

- **Docker Desktop** (oder Docker Engine + Docker Compose v2) installiert und gestartet.
- Ein aktueller Browser.
- *(Optional)* **Java 21** + **Maven 3.9+**, falls du das Backend lokal ohne Docker starten möchtest.

Unter **Windows**: Prüfen, ob die Standard-Ports nicht bereits belegt sind (siehe [Fehlerbehebung](#fehlerbehebung)).

---

## Schnellstart mit Docker (empfohlen)

1. **Repository klonen** (oder ZIP entpacken).

2. **(Optional)** Beispielkonfiguration kopieren:
   ```bash
   copy .env.example .env
   ```
   Unter Linux/macOS: `cp .env.example .env`  
   Für lokale Tests können die Standardwerte beibehalten werden.

3. **Alle Dienste starten** (Datenbank, API, Nginx-Frontend, phpMyAdmin):
   ```bash
   docker compose up -d --build
   ```
   Beim ersten Mal können Image-Download und Java-Build einige Minuten dauern.

4. Warten, bis die Container **„healthy“** sind (insbesondere MariaDB, danach das Java-Backend).

5. Die Anwendung im Browser öffnen (siehe unten).

**Stoppen** ohne Daten zu löschen:
```bash
docker compose down
```

**Vollständiger Reset** (inkl. Datenbank):
```bash
docker compose down -v
```
⚠️ Das Volume löscht alle MariaDB-Daten; die Skripte unter `mariadb/mariadb_init/` laufen beim nächsten `up` erneut.

---

## Zugriff nach dem Start

| Dienst | Standard-URL | Hinweis |
|--------|--------------|---------|
| **Anwendung (Frontend)** | [http://localhost](http://localhost) oder [http://127.0.0.1](http://127.0.0.1) | UI; API-Aufrufe laufen über `/api` (Nginx-Proxy). |
| **API (Backend direkt)** | [http://localhost:8080](http://localhost:8080) | Direktzugriff auf Vert.x (Tests oder wenn Port 80 belegt ist). |
| **Health-Check** | [http://localhost:8080/api/health](http://localhost:8080/api/health) | JSON mit `status: ok`. |
| **phpMyAdmin** | [http://localhost:8081](http://localhost:8081) | SQL-Oberfläche (Nutzer/Passwort wie in `.env`, z. B. `chatuser` / `change_me`). |
| **MariaDB (Host)** | `localhost:3307` | Standard-Host-Port (`DB_PUBLISH_PORT`); im Docker-Netzwerk nutzt das Backend `mariadb:3306`. |

Wenn `FRONTEND_PORT` oder `API_PORT` in der `.env` geändert werden, die URLs entsprechend anpassen.

---

## Nutzerkonto

- Einfachster Test: In der Oberfläche **„Registrieren“** nutzen und ein Konto anlegen (E-Mail + Passwort; API-Minimum für die Passwortlänge: **8 Zeichen**).
- Die SQL-Seed-Daten können Beispielnutzer mit **ungültigen** Passwort-Hashes enthalten; für einen echten Login **nicht darauf verlassen**, solange keine gültigen BCrypt-Hashes in den Seeds stehen.
- Die Rolle **admin** aktiviert erweiterte Funktionen (global anheften, Admin-Ansicht). Für Demos einen Nutzer in der Datenbank hochstufen oder die Seeds anpassen.

---

## Repository-Struktur

```
plattform/
├── docker-compose.yml      # Orchestrierung: MariaDB, Java-Backend, Nginx, phpMyAdmin
├── .env.example            # Vorlage für Umgebungsvariablen (nach .env kopieren)
├── frontend/               # Statische Website + JS
│   ├── index.html          # Leichtgewichtige „SPA“ (kein npm-Build nötig)
│   ├── dmtree-community.html # Optional: Weiterleitung zur Docker-App
│   ├── *.js                # user, vote, comment, post (Feed, Polling), app (Navigation)
│   ├── styles.css
│   └── nginx/conf.d/       # Proxy /api → Backend
├── java-backend/           # Vert.x-API (Maven)
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/example/
│       ├── MainVerticle.java
│       ├── http/           # HTTP-Router
│       ├── user/ post/ comment/ vote/
│       ├── auth/           # JWT, BCrypt
│       └── database/
├── mariadb/
│   └── mariadb_init/       # SQL-Skripte beim ersten Volume-Start
```

---

## Technischer Stack

| Schicht | Technologie |
|---------|-------------|
| **Frontend** | HTML, CSS, JavaScript (kein Framework erforderlich) |
| **Webserver** | Nginx (Container `frontend`) — statische Dateien + Proxy für `/api` |
| **Backend** | Java **21**, **Eclipse Vert.x 4.5** (HTTP, JSON, JDBC) |
| **Datenbank** | **MariaDB** (Schema + Seeds in `mariadb/mariadb_init/`) |
| **Auth** | JWT (Bearer), Passwörter gehasht (BCrypt / jBCrypt) |
| **Tools** | Docker Compose; **phpMyAdmin** optional für SQL-Administration |

---

## Frontend: Aktualisierung (Polling) und UI-Updates

Es gibt **keine WebSockets** und kein Echtzeit-Protokoll: der Feed wirkt dennoch „frisch“, weil das Frontend **regelmäßig** die API abfragt und den Bildschirm **ohne komplettes Neuzeichnen** anpasst.

| Thema | Verhalten |
|-------|-----------|
| **Intervall** | Auf der **Feed-Seite** ca. **alle 500 ms** (`FEED_POLL_MS` in `frontend/post.js`) — `GET /api/posts` mit aktuellem Filter, Sortierung und höherem Limit; bei **ausgeblendetem Tab** (`document.hidden`) wird nicht gepollt. |
| **Feed-Liste** | Sind die **IDs in derselben Reihenfolge** wie zuvor, werden nur geänderte **Karten im DOM** angepasst (Likes, Dislikes, Kommentarzahl, Pin-Badges, Vote-Zustand). Ändert sich die **Reihenfolge** (z. B. nach Sortierung durch andere Nutzer), wird die Liste neu gerendert. |
| **Detail-Panel** | Ist ein Beitrag geöffnet, holt der Poll u. a. **Detail + Kommentare**; der Kommentarbaum wird nur ersetzt, wenn sich die Daten **inhaltlich** geändert haben (Vergleich per Serialisierung). |
| **Seitenwechsel** | Beim Verlassen des Feeds stoppt das Polling; Beim Zurückkehren startet es wieder (u. a. nach dem ersten `refreshFeed()` und über `navigate()` → `syncFeedPollingState`). |
| **Globale Widgets** | Seitenleisten-Zähler und ähnliches werden **seltener** mitaktualisiert (nur jedes *n.* Poll-Intervall, z. B. Top-Contributors), um Last zu reduzieren. |
| **Stimmen (Vote)** | Ein Klick auf 👍/👎 im Feed oder im Detail **rendert nicht die ganze Liste neu**: es werden nur **die betroffene Karte** und ggf. die **Vote-Zeile im Detail** per DOM-Update angepasst (`patchPostCardDom` / `patchDetailVoteDom` in `post.js`, Aufruf aus `vote.js`). |

**Hinweis zur Last:** Viele gleichzeitige Nutzer mit sehr kurzem Intervall erzeugen mehr `GET`-Anfragen; Intervall und Widget-Häufigkeit lassen sich zentral in `frontend/post.js` anpassen.

---

## REST-API-Routen

Alle Routen haben das Präfix **`/api`**. Das Frontend ruft in der Regel **`http://<Host>/api/...`** auf (gleiche Origin über Nginx).

**Legende Authentifizierung:**

- **Nein** — kein Token nötig (einige Felder hängen vom Besucher ab, wenn `Authorization: Bearer` gesendet wird).
- **Ja** — Header `Authorization: Bearer <jwt>` erforderlich.

| Methode | Pfad | Auth | Beschreibung |
|---------|------|------|--------------|
| `GET` | `/api/health` | Nein | Dienststatus. |
| `GET` | `/api/stats/top-contributors` | Nein | Top-Mitwirkende (`?limit=`, Standard serverseitig). |
| `POST` | `/api/auth/register` | Nein | Registrierung JSON: `fullName`, `email`, `password`. |
| `POST` | `/api/auth/login` | Nein | Login JSON: `email`, `password` → `token` + `user`. |
| `GET` | `/api/auth/me` | Ja | Aktueller Nutzer aus JWT. |
| `GET` | `/api/profile/me` | Ja | Profil + Statistiken + letzte Beiträge. |
| `GET` | `/api/posts` | Nein | Paginierte Liste veröffentlichter Beiträge (Parameter siehe unten). |
| `GET` | `/api/posts/:id` | Nein | Beitragsdetail (`viewerPinned`, `viewerVote` mit Token). |
| `POST` | `/api/posts` | Ja | Erstellen JSON: `postType`, `title`, `content`. |
| `PATCH` | `/api/posts/:id/pinned` | Admin (JWT mit Rolle admin) | JSON `pinned: true/false` — **globales** Anheften. |
| `POST` | `/api/posts/:id/my-pin/toggle` | Ja | Persönliches Anheften umschalten. |
| `POST` | `/api/posts/:postId/vote` | Ja | JSON `voteType`: `up` oder `down`. |
| `GET` | `/api/posts/:postId/comments` | Nein | Kommentare zum Beitrag. |
| `POST` | `/api/posts/:postId/comments` | Ja | Erstellen; JSON `content`, optional `parentCommentId` für Antwort. |
| `POST` | `/api/comments/:commentId/vote` | Ja | JSON `voteType`: `up` oder `down`. |

Fehlerantworten liefern typischerweise JSON `{ "error": "Nachricht" }` mit passendem HTTP-Status (400, 401, 403, 404, 409, 500).

---

## API-Abfrageparameter

**`GET /api/posts`**

| Parameter | Beispiel | Beschreibung |
|-----------|----------|--------------|
| `type` | `news`, `idea`, `discussion` | Filter nach Typ; weglassen = alle. |
| `sort` | `popular`, `recent`, `comments` | Sortierung (bei ungültigem Wert z. B. `recent`). |
| `page` | `1` | Seite (Standard `1`). |
| `limit` | `20` | Seitengröße (serverseitig begrenzt, z. B. max. 100). |

**Global angeheftete** und vom **Besucher persönlich angeheftete** Beiträge erscheinen oben (bei persönlichen Pins nur mit JWT).

---

## Umgebungsvariablen

Datei **`.env`** im Projektroot (siehe `.env.example`):

| Variable | Bedeutung |
|----------|-----------|
| `DB_HOST` | MariaDB-Host **aus Sicht des Backend-Containers** (in Compose: `mariadb`). |
| `DB_PUBLISH_PORT` | MariaDB-Port auf **deinem Rechner** (Standard `3307`). |
| `DB_NAME`, `DB_USER`, `DB_PASSWORD` | Datenbankzugang. |
| `DB_ROOT_PASSWORD` | MariaDB-Root-Passwort im Container. |
| `API_PORT` | Host-Port des Backends (Standard `8080`). |
| `FRONTEND_PORT` | Nginx-Port für die Website (Standard `80` — unter Windows ggf. Administratorrechte). |
| `PHPMYADMIN_PORT` | phpMyAdmin-Port (Standard `8081`). |

---

## Fehlerbehebung

| Problem | Lösungsansatz |
|---------|----------------|
| **Port 80 belegt** | In `.env` z. B. `FRONTEND_PORT=3000` setzen, `docker compose up -d --build`, dann `http://localhost:3000` öffnen. |
| **`Failed to fetch` / API unreachable** | App über **http** öffnen (nicht `file://`). Container prüfen: `docker compose ps`. |
| **SQL-Fehler beim Laden des Feeds** | Logs: `docker logs chat-platform-backend`. Nach manuellen Migrationen prüfen, ob alle Tabellen existieren (z. B. `user_post_pins`). |
| **Docker-Image: TLS/Timeout** | Später erneut versuchen; Proxy/Firewall prüfen oder Images manuell `docker pull`en. |
| **Leere oder inkonsistente DB** | Tabellen unter `http://localhost:8081` (phpMyAdmin) prüfen; im Zweifel `docker compose down -v` und neu starten (**Datenverlust**). |

---

## Entwicklung ohne Docker

1. **MariaDB** lokal installieren, Datenbank und Tabellen anlegen (Skripte in `mariadb/mariadb_init/` in Reihenfolge ausführen, oder nur `00-init.sql` für ein Mindestschema).

2. In `java-backend/`:
   ```bash
   mvn clean package
   java -jar target/chat-platform-backend-1.0-SNAPSHOT.jar
   ```
   Umgebungsvariablen **`DB_HOST`**, **`DB_PORT`**, **`DB_NAME`**, **`DB_USER`**, **`DB_PASSWORD`** auf die lokale Instanz setzen.

3. `frontend/` mit einem lokalen HTTP-Server ausliefern **oder** `index.html` öffnen: Bei `file://` zeigt das eingebettete Skript typischerweise auf `http://127.0.0.1:8080`; alternativ Nginx oder z. B. `npx serve` mit passendem Proxy.

4. **JWT-Geheimnisse**: Das Backend nutzt für Tests eine eingebettete Konfiguration (Code unter `com.example.auth` für Produktionshärtung anpassen).

---

## Weitere Dokumentation

- `BACKEND_STRUCTURE.md` — Überblick Java-Pakete (kann vom tatsächlichen Code abweichen).
- `ROADMAP.md` — Ideen für die Weiterentwicklung (falls gepflegt).

---

## Lizenz

Keine
