package com.example.post;

import java.util.ArrayList;
import java.util.List;

import com.example.database.DatabaseClient;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

public class PostRepository {

    private final JDBCPool pool;

    public PostRepository() {
        this.pool = DatabaseClient.getInstance();
    }

    public Future<JsonObject> createPost(long authorId, String postType, String title, String content) {
        String insertSql = """
                INSERT INTO posts (author_id, post_type, title, content, is_pinned, status)
                VALUES (?, ?, ?, ?, FALSE, 'published')
                """;
        return pool.preparedQuery(insertSql)
                .execute(Tuple.of(authorId, postType, title, content))
                .compose(ignore -> findLatestByAuthor(authorId));
    }

    public Future<JsonObject> findLatestByAuthor(long authorId) {
        String sql = """
                SELECT
                    p.id, p.post_type, p.title, p.content, p.is_pinned, p.status, p.created_at, p.updated_at,
                    u.id AS author_id, u.full_name, u.role, u.avatar_url
                FROM posts p
                JOIN users u ON u.id = p.author_id
                WHERE p.author_id = ?
                ORDER BY p.id DESC
                LIMIT 1
                """;
        return pool.preparedQuery(sql)
                .execute(Tuple.of(authorId))
                .map(rows -> rows.iterator().hasNext() ? toCreatedPost(rows.iterator().next()) : null);
    }

    public Future<JsonObject> findPostById(long postId, Long viewerUserId) {
        long viewer = viewerUserId == null ? -1L : viewerUserId;
        String sql = """
                SELECT
                    p.id, p.post_type, p.title, p.content, p.is_pinned, p.status, p.created_at,
                    u.id AS author_id, u.full_name, u.role, u.avatar_url,
                    COUNT(DISTINCT CASE WHEN v.vote_type='up' THEN v.id END) AS likes,
                    COUNT(DISTINCT CASE WHEN v.vote_type='down' THEN v.id END) AS dislikes,
                    MAX(CASE WHEN v.user_id = ? THEN v.vote_type ELSE NULL END) AS viewer_vote,
                    MAX(upp.post_id) AS viewer_pin_id,
                    COUNT(DISTINCT c.id) AS comments_count
                FROM posts p
                JOIN users u ON u.id = p.author_id
                LEFT JOIN votes v ON v.post_id = p.id
                LEFT JOIN user_post_pins upp ON upp.post_id = p.id AND upp.user_id = ?
                LEFT JOIN comments c ON c.post_id = p.id AND c.status = 'visible'
                WHERE p.id = ? AND p.status = 'published'
                GROUP BY p.id, p.post_type, p.title, p.content, p.is_pinned, p.status, p.created_at, u.id, u.full_name, u.role, u.avatar_url
                """;
        return pool.preparedQuery(sql)
                .execute(Tuple.of(viewer, viewer, postId))
                .map(rows -> rows.iterator().hasNext() ? toPostDetail(rows.iterator().next()) : null);
    }

    public Future<JsonObject> listPosts(String type, String sort, int page, int limit, Long viewerUserId) {
        int safePage = Math.max(1, page);
        int safeLimit = Math.max(1, Math.min(limit, 100));
        int offset = (safePage - 1) * safeLimit;

        String where = " WHERE p.status = 'published' ";
        List<Object> params = new ArrayList<>();
        if (type != null) {
            where += " AND p.post_type = ? ";
            params.add(type);
        }

        String orderBy;
        // Global pin first, then this viewer's personal pins (user_post_pins), then normal sort.
        String pinPrefix = " ORDER BY p.is_pinned DESC, (MAX(upp.post_id) IS NOT NULL) DESC, ";
        if ("popular".equals(sort)) {
            orderBy = pinPrefix + " likes DESC, dislikes ASC, p.created_at DESC ";
        } else if ("comments".equals(sort)) {
            orderBy = pinPrefix + " comments_count DESC, p.created_at DESC ";
        } else {
            orderBy = pinPrefix + " p.created_at DESC ";
        }

        String listSql = """
                SELECT
                    p.id,
                    p.post_type,
                    p.title,
                    p.content,
                    p.is_pinned,
                    p.status,
                    p.created_at,
                    p.updated_at,
                    u.id AS author_id,
                    u.full_name,
                    u.role,
                    u.avatar_url,
                    COUNT(DISTINCT CASE WHEN v.vote_type='up' THEN v.id END) AS likes,
                    COUNT(DISTINCT CASE WHEN v.vote_type='down' THEN v.id END) AS dislikes,
                    MAX(CASE WHEN v.user_id = ? THEN v.vote_type ELSE NULL END) AS viewer_vote,
                    MAX(upp.post_id) AS viewer_pin_id,
                    COUNT(DISTINCT c.id) AS comments_count
                FROM posts p
                JOIN users u ON u.id = p.author_id
                LEFT JOIN votes v ON v.post_id = p.id
                LEFT JOIN user_post_pins upp ON upp.post_id = p.id AND upp.user_id = ?
                LEFT JOIN comments c ON c.post_id = p.id AND c.status = 'visible'
                """ + where + """
                GROUP BY p.id, p.post_type, p.title, p.content, p.is_pinned, p.status, p.created_at, p.updated_at, u.id, u.full_name, u.role, u.avatar_url
                """ + orderBy + """
                LIMIT ? OFFSET ?
                """;

        String countSql = "SELECT COUNT(*) AS total FROM posts p " + where;

        long viewer = viewerUserId == null ? -1L : viewerUserId;
        List<Object> listParams = new ArrayList<>();
        listParams.add(viewer);
        listParams.add(viewer);
        listParams.addAll(params);
        listParams.add(safeLimit);
        listParams.add(offset);

        Future<RowSet<Row>> listFuture = pool.preparedQuery(listSql).execute(Tuple.from(listParams));
        Future<RowSet<Row>> countFuture = pool.preparedQuery(countSql).execute(Tuple.from(params));

        return Future.all(listFuture, countFuture).map(composite -> {
            RowSet<Row> listRows = composite.resultAt(0);
            RowSet<Row> countRows = composite.resultAt(1);
            int total = countRows.iterator().next().getInteger("total");

            JsonArray items = new JsonArray();
            for (Row row : listRows) {
                items.add(toFeedItem(row));
            }

            return new JsonObject()
                    .put("items", items)
                    .put("pagination", new JsonObject()
                            .put("page", safePage)
                            .put("limit", safeLimit)
                            .put("total", total));
        });
    }

    public Future<JsonObject> toggleViewerPin(long userId, long postId) {
        return pool.preparedQuery("SELECT id FROM posts WHERE id = ? AND status = 'published'")
                .execute(Tuple.of(postId))
                .compose(rows -> {
                    if (!rows.iterator().hasNext()) {
                        return Future.failedFuture(new PostNotFoundException());
                    }
                    return pool.preparedQuery(
                            "SELECT post_id FROM user_post_pins WHERE user_id = ? AND post_id = ?")
                            .execute(Tuple.of(userId, postId))
                            .compose(pinRows -> {
                                if (pinRows.iterator().hasNext()) {
                                    return pool.preparedQuery(
                                            "DELETE FROM user_post_pins WHERE user_id = ? AND post_id = ?")
                                            .execute(Tuple.of(userId, postId))
                                            .mapEmpty()
                                            .map(v -> new JsonObject().put("viewerPinned", false));
                                }
                                return pool.preparedQuery(
                                """
                                        INSERT INTO user_post_pins (user_id, post_id) VALUES (?, ?)
                                        """)
                                        .execute(Tuple.of(userId, postId))
                                        .mapEmpty()
                                        .map(v -> new JsonObject().put("viewerPinned", true));
                            });
                });
    }

    public Future<Boolean> updatePostPinned(long postId, boolean pinned) {
        String sql = """
                UPDATE posts SET is_pinned = ? WHERE id = ? AND status = 'published'
                """;
        return pool.preparedQuery(sql)
                .execute(Tuple.of(pinned, postId))
                .map(rows -> rows.rowCount() > 0);
    }

    private JsonObject toFeedItem(Row row) {
        String content = row.getString("content");
        String preview = content == null ? "" : (content.length() > 140 ? content.substring(0, 140) + "..." : content);

        return new JsonObject()
                .put("id", row.getLong("id"))
                .put("postType", row.getString("post_type"))
                .put("title", row.getString("title"))
                .put("contentPreview", preview)
                .put("isPinned", row.getBoolean("is_pinned"))
                .put("status", row.getString("status"))
                .put("createdAt", stringify(row.getValue("created_at")))
                .put("updatedAt", stringify(row.getValue("updated_at")))
                .put("author", new JsonObject()
                        .put("id", row.getLong("author_id"))
                        .put("fullName", row.getString("full_name"))
                        .put("role", row.getString("role"))
                        .put("avatarUrl", row.getString("avatar_url")))
                .put("stats", new JsonObject()
                        .put("score", row.getInteger("likes") - row.getInteger("dislikes"))
                        .put("likes", row.getInteger("likes"))
                        .put("dislikes", row.getInteger("dislikes"))
                        .put("commentsCount", row.getInteger("comments_count")))
                .put("viewerVote", row.getString("viewer_vote"))
                .put("viewerPinned", row.getValue("viewer_pin_id") != null);
    }

    private JsonObject toPostDetail(Row row) {
        return new JsonObject()
                .put("id", row.getLong("id"))
                .put("postType", row.getString("post_type"))
                .put("title", row.getString("title"))
                .put("content", row.getString("content"))
                .put("isPinned", row.getBoolean("is_pinned"))
                .put("status", row.getString("status"))
                .put("createdAt", stringify(row.getValue("created_at")))
                .put("author", new JsonObject()
                        .put("id", row.getLong("author_id"))
                        .put("fullName", row.getString("full_name"))
                        .put("role", row.getString("role"))
                        .put("avatarUrl", row.getString("avatar_url")))
                .put("stats", new JsonObject()
                        .put("score", row.getInteger("likes") - row.getInteger("dislikes"))
                        .put("likes", row.getInteger("likes"))
                        .put("dislikes", row.getInteger("dislikes"))
                        .put("commentsCount", row.getInteger("comments_count")))
                .put("viewerVote", row.getString("viewer_vote"))
                .put("viewerPinned", row.getValue("viewer_pin_id") != null);
    }

    private JsonObject toCreatedPost(Row row) {
        return new JsonObject()
                .put("id", row.getLong("id"))
                .put("postType", row.getString("post_type"))
                .put("title", row.getString("title"))
                .put("content", row.getString("content"))
                .put("isPinned", row.getBoolean("is_pinned"))
                .put("status", row.getString("status"))
                .put("createdAt", stringify(row.getValue("created_at")))
                .put("author", new JsonObject()
                        .put("id", row.getLong("author_id"))
                        .put("fullName", row.getString("full_name"))
                        .put("role", row.getString("role"))
                        .put("avatarUrl", row.getString("avatar_url")));
    }

    private String stringify(Object value) {
        return value == null ? null : value.toString();
    }

    public static final class PostNotFoundException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
