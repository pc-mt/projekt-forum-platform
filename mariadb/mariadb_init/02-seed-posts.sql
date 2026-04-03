-- Seed posts dataset (depends on users from 00-init.sql + 01-seed-users.sql)
SET NAMES utf8mb4;
SET time_zone = '+00:00';

INSERT IGNORE INTO posts (id, author_id, post_type, title, content, is_pinned, status, created_at)
VALUES
    (10, 10, 'news', 'Official Release DMTree Community', 'Wir starten offiziell mit dem Community Hub fuer alle Partnerorganisationen.', TRUE, 'published', '2026-04-01 09:00:00'),
    (11, 11, 'idea', 'Slack Notification Integration', 'Idee: Ein Slack-Bot fuer neue Posts, Kommentare und Abstimmungen.', FALSE, 'published', '2026-04-01 11:30:00'),
    (12, 12, 'discussion', 'Onboarding Verbesserung', 'Welche 3 Aenderungen helfen neuen Nutzern am meisten beim Einstieg?', FALSE, 'published', '2026-04-02 08:15:00'),
    (13, 13, 'idea', 'Analytics Dashboard v2', 'Admin Dashboard mit Trends pro Woche und Aktivitaetsmetriken.', FALSE, 'published', '2026-04-02 14:45:00'),
    (14, 14, 'news', 'Geplante Wartung am Wochenende', 'Kurze Wartung fuer Datenbankoptimierung und Security Updates.', FALSE, 'published', '2026-04-03 07:20:00'),
    (15, 10, 'discussion', 'Kategorie Struktur Feedback', 'Sollen wir weitere Kategorien ausser News/Idea/Discussion einfuehren?', FALSE, 'published', '2026-04-03 10:10:00');

INSERT IGNORE INTO schema_migrations (name) VALUES ('02-seed-posts');
