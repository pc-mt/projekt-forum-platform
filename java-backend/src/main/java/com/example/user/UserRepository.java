package com.example.user;

import java.util.ArrayList;
import java.util.List;

import com.example.database.DatabaseClient;

import io.vertx.core.Future;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class UserRepository {

    private final JDBCPool pool;

    public UserRepository() {
        this.pool = DatabaseClient.getInstance();
    }

    public Future<JsonObject> findByEmail(String email) {
        String sql = """
                SELECT id, full_name, email, password_hash, role, avatar_url, created_at
                FROM users
                WHERE email = ?
                LIMIT 1
                """;
        return pool.preparedQuery(sql)
                .execute(Tuple.of(email))
                .map(rows -> rows.iterator().hasNext() ? toUser(rows.iterator().next()) : null);
    }

    public Future<JsonObject> findById(long id) {
        String sql = """
                SELECT id, full_name, email, password_hash, role, avatar_url, created_at
                FROM users
                WHERE id = ?
                LIMIT 1
                """;
        return pool.preparedQuery(sql)
                .execute(Tuple.of(id))
                .map(rows -> rows.iterator().hasNext() ? toUser(rows.iterator().next()) : null);
    }

    public Future<JsonObject> createUser(String fullName, String email, String passwordHash, String role) {
        String insertSql = """
                INSERT INTO users (full_name, email, password_hash, role)
                VALUES (?, ?, ?, ?)
                """;
        return pool.preparedQuery(insertSql)
                .execute(Tuple.of(fullName, email, passwordHash, role))
                .compose(ignore -> findByEmail(email));
    }

    public Future<JsonObject> fetchProfileStats(long userId) {
        String sql = """
                SELECT
                    (SELECT COUNT(*) FROM posts p WHERE p.author_id = ? AND p.status = 'published') AS posts_count,
                    (SELECT COALESCE(SUM(CASE WHEN v.vote_type='up' THEN 1 WHEN v.vote_type='down' THEN -1 ELSE 0 END), 0)
                       FROM votes v
                       JOIN posts p2 ON p2.id = v.post_id
                      WHERE p2.author_id = ? AND p2.status = 'published') AS received_votes,
                    (SELECT COUNT(*) FROM comments c WHERE c.author_id = ? AND c.status = 'visible') AS comments_count
                """;

        return pool.preparedQuery(sql)
                .execute(Tuple.of(userId, userId, userId))
                .map(rows -> {
                    if (!rows.iterator().hasNext()) {
                        return new JsonObject()
                                .put("postsCount", 0)
                                .put("receivedVotes", 0)
                                .put("commentsCount", 0)
                                .put("points", 0);
                    }
                    Row row = rows.iterator().next();
                    int postsCount = row.getInteger("posts_count");
                    int receivedVotes = row.getInteger("received_votes");
                    int commentsCount = row.getInteger("comments_count");
                    int points = postsCount * 5 + receivedVotes * 3 + commentsCount;

                    return new JsonObject()
                            .put("postsCount", postsCount)
                            .put("receivedVotes", receivedVotes)
                            .put("commentsCount", commentsCount)
                            .put("points", points);
                });
    }

    /**
     * Points = posts×5 + received votes on own posts×3 + visible comments (same rule as profile stats).
     */
    public Future<JsonArray> fetchTopContributors(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        String sql = """
                SELECT ranked.id, ranked.full_name, ranked.role, ranked.points
                FROM (
                    SELECT
                        u.id,
                        u.full_name,
                        u.role,
                        (
                            (SELECT COUNT(*) FROM posts p WHERE p.author_id = u.id AND p.status = 'published') * 5
                            + (SELECT COALESCE(SUM(
                                    CASE WHEN v.vote_type='up' THEN 1 WHEN v.vote_type='down' THEN -1 ELSE 0 END
                                ), 0)
                                FROM votes v
                                JOIN posts p2 ON p2.id = v.post_id
                                WHERE p2.author_id = u.id AND p2.status = 'published') * 3
                            + (SELECT COUNT(*) FROM comments c WHERE c.author_id = u.id AND c.status = 'visible')
                        ) AS points
                    FROM users u
                ) ranked
                WHERE ranked.points > 0
                ORDER BY ranked.points DESC, ranked.id ASC
                LIMIT ?
                """;
        return pool.preparedQuery(sql)
                .execute(Tuple.of(safeLimit))
                .map(rows -> {
                    List<JsonObject> items = new ArrayList<>();
                    for (Row row : rows) {
                        Object pv = row.getValue("points");
                        int points = pv == null ? 0 : ((Number) pv).intValue();
                        items.add(new JsonObject()
                                .put("id", row.getLong("id"))
                                .put("fullName", row.getString("full_name"))
                                .put("role", row.getString("role"))
                                .put("points", points));
                    }
                    return new JsonArray(items);
                });
    }

    public Future<JsonArray> fetchRecentPosts(long userId, int limit) {
        String sql = """
                SELECT
                    p.id,
                    p.title,
                    p.post_type,
                    p.created_at,
                    COALESCE(SUM(CASE WHEN v.vote_type='up' THEN 1 WHEN v.vote_type='down' THEN -1 ELSE 0 END), 0) AS score,
                    COUNT(DISTINCT c.id) AS comments_count
                FROM posts p
                LEFT JOIN votes v ON v.post_id = p.id
                LEFT JOIN comments c ON c.post_id = p.id AND c.status = 'visible'
                WHERE p.author_id = ? AND p.status = 'published'
                GROUP BY p.id, p.title, p.post_type, p.created_at
                ORDER BY p.created_at DESC
                LIMIT ?
                """;
        return pool.preparedQuery(sql)
                .execute(Tuple.of(userId, limit))
                .map(this::toRecentPosts);
    }

    private JsonObject toUser(Row row) {
        return new JsonObject()
                .put("id", row.getLong("id"))
                .put("fullName", row.getString("full_name"))
                .put("email", row.getString("email"))
                .put("passwordHash", row.getString("password_hash"))
                .put("role", row.getString("role"))
                .put("avatarUrl", row.getString("avatar_url"))
                .put("memberSince", stringifyTimestamp(row.getValue("created_at")));
    }

    private JsonArray toRecentPosts(RowSet<Row> rows) {
        List<JsonObject> items = new ArrayList<>();
        for (Row row : rows) {
            items.add(new JsonObject()
                    .put("id", row.getLong("id"))
                    .put("title", row.getString("title"))
                    .put("postType", row.getString("post_type"))
                    .put("createdAt", stringifyTimestamp(row.getValue("created_at")))
                    .put("score", row.getInteger("score"))
                    .put("commentsCount", row.getInteger("comments_count")));
        }
        return new JsonArray(items);
    }

    private String stringifyTimestamp(Object value) {
        return value == null ? null : value.toString();
    }
}

