-- Seed users dataset (depends on 00-init.sql tables)
SET NAMES utf8mb4;
SET time_zone = '+00:00';

INSERT IGNORE INTO users (id, full_name, email, password_hash, role, avatar_url)
VALUES
    (10, 'Marie Aufret', 'marie@dmtree.local', '$2b$10$seedseedseedseedseedseedseedseedseedseedseedseedseed', 'admin', NULL),
    (11, 'Sofia Radic', 'sofia@dmtree.local', '$2b$10$seedseedseedseedseedseedseedseedseedseedseedseedseed', 'user', NULL),
    (12, 'Luca Bernardi', 'luca@dmtree.local', '$2b$10$seedseedseedseedseedseedseedseedseedseedseedseedseed', 'user', NULL),
    (13, 'Elena Schmidt', 'elena@dmtree.local', '$2b$10$seedseedseedseedseedseedseedseedseedseedseedseedseed', 'user', NULL),
    (14, 'Noah Weber', 'noah@dmtree.local', '$2b$10$seedseedseedseedseedseedseedseedseedseedseedseedseed', 'user', NULL);

INSERT IGNORE INTO schema_migrations (name) VALUES ('01-seed-users');
