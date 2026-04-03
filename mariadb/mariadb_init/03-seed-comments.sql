-- Seed comments dataset (depends on posts and users)
SET NAMES utf8mb4;
SET time_zone = '+00:00';

INSERT IGNORE INTO comments (id, post_id, author_id, content, status, created_at)
VALUES
    (10, 10, 11, 'Super Start! Das hilft unserer Zusammenarbeit enorm.', 'visible', '2026-04-01 09:40:00'),
    (11, 10, 12, 'Danke fuer die klare Struktur zwischen News und Ideen.', 'visible', '2026-04-01 10:00:00'),
    (12, 11, 10, 'Gute Idee. Wir priorisieren das fuer Sprint 2.', 'visible', '2026-04-01 12:10:00'),
    (13, 11, 14, 'Bitte auch Teams Integration spaeter einplanen.', 'visible', '2026-04-01 12:35:00'),
    (14, 12, 13, 'Ein kurzes Tutorial beim ersten Login waere hilfreich.', 'visible', '2026-04-02 09:30:00'),
    (15, 12, 10, 'Einverstanden, wir bauen das als Quick-Win.', 'visible', '2026-04-02 09:45:00'),
    (16, 13, 11, 'Dashboard mit Filter nach Organisation waere top.', 'visible', '2026-04-02 15:00:00'),
    (17, 14, 12, 'Wichtig: Bitte Downtime im Voraus kommunizieren.', 'visible', '2026-04-03 07:45:00'),
    (18, 15, 14, 'Mehr Kategorien koennen spaeter kommen. Erstmal simpel halten.', 'visible', '2026-04-03 10:35:00');

INSERT IGNORE INTO schema_migrations (name) VALUES ('03-seed-comments');
