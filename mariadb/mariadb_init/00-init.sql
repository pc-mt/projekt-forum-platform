-- Chat platform skeleton — extend with users, posts, messages, etc.

SET NAMES utf8mb4;
SET time_zone = '+00:00';

CREATE TABLE IF NOT EXISTS users (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('user', 'admin') NOT NULL DEFAULT 'user',
    avatar_url VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_email (email),
    KEY idx_users_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS posts (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    author_id BIGINT UNSIGNED NOT NULL,
    post_type ENUM('news', 'idea', 'discussion') NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    status ENUM('published', 'archived', 'deleted') NOT NULL DEFAULT 'published',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_posts_author_created (author_id, created_at),
    KEY idx_posts_type_created (post_type, created_at),
    KEY idx_posts_status_created (status, created_at),
    CONSTRAINT fk_posts_author
        FOREIGN KEY (author_id) REFERENCES users(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS comments (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    post_id BIGINT UNSIGNED NOT NULL,
    parent_comment_id BIGINT UNSIGNED NULL,
    author_id BIGINT UNSIGNED NOT NULL,
    content TEXT NOT NULL,
    status ENUM('visible', 'hidden', 'deleted') NOT NULL DEFAULT 'visible',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_comments_post_created (post_id, created_at),
    KEY idx_comments_author_created (author_id, created_at),
    CONSTRAINT fk_comments_post
        FOREIGN KEY (post_id) REFERENCES posts(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_comments_parent
        FOREIGN KEY (parent_comment_id) REFERENCES comments(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_comments_author
        FOREIGN KEY (author_id) REFERENCES users(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS votes (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    post_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    vote_type ENUM('up', 'down') NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_vote (post_id, user_id),
    KEY idx_votes_post (post_id),
    KEY idx_votes_user (user_id),
    CONSTRAINT fk_votes_post
        FOREIGN KEY (post_id) REFERENCES posts(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_votes_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS comment_votes (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    comment_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    vote_type ENUM('up', 'down') NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_comment_vote (comment_id, user_id),
    KEY idx_comment_votes_comment (comment_id),
    KEY idx_comment_votes_user (user_id),
    CONSTRAINT fk_comment_votes_comment
        FOREIGN KEY (comment_id) REFERENCES comments(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_comment_votes_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS schema_migrations (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL,
    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_schema_migrations_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT IGNORE INTO users (id, full_name, email, password_hash, role, avatar_url)
VALUES
    (1, 'Admin DMTree', 'admin@dmtree.local', '$2b$10$exampleexampleexampleexampleexampleexampleexampleexample', 'admin', NULL),
    (2, 'Jonas Klein', 'jonas@dmtree.local', '$2b$10$exampleexampleexampleexampleexampleexampleexampleexample', 'user', NULL);

INSERT IGNORE INTO posts (id, author_id, post_type, title, content, is_pinned, status)
VALUES
    (1, 1, 'news', 'Bienvenue sur DMTree Community', 'Premiere actualite officielle de la plateforme.', TRUE, 'published'),
    (2, 2, 'idea', 'Integration Slack', 'Proposition d integration pour notifications de posts et commentaires.', FALSE, 'published');

INSERT IGNORE INTO comments (id, post_id, author_id, content, status)
VALUES
    (1, 1, 2, 'Super lancement, hate de contribuer.', 'visible'),
    (2, 2, 1, 'Bonne idee, a prioriser rapidement.', 'visible');

INSERT IGNORE INTO votes (post_id, user_id, vote_type)
VALUES
    (1, 2, 'up'),
    (2, 1, 'up');

INSERT IGNORE INTO schema_migrations (name) VALUES ('00-init');
