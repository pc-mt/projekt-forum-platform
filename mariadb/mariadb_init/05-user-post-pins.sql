-- Per-user personal pins (run once on DBs created before user_post_pins existed)

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS user_post_pins (
    user_id BIGINT UNSIGNED NOT NULL,
    post_id BIGINT UNSIGNED NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, post_id),
    KEY idx_user_post_pins_post (post_id),
    CONSTRAINT fk_user_post_pins_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_user_post_pins_post
        FOREIGN KEY (post_id) REFERENCES posts(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT IGNORE INTO schema_migrations (name) VALUES ('05-user-post-pins');
