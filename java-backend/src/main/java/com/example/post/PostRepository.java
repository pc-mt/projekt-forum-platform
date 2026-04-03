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

    public Future<JsonObject> findPostById(long postId) {
        String sql = """
                SELECT
                    p.id, p.post_type, p.title, p.content, p.is_pinned, p.status, p.created_at,
                    u.id AS author_id, u.full_name, u.role, u.avatar_url,
                    COALESCE(SUM(CASE WHEN v.vote_type='up' THEN 1 WHEN v.vote_type='down' THEN -1 ELSE 0 END), 0) AS score,
                    COUNT(DISTINCT c.id) AS comments_count
                FROM posts p
                JOIN users u ON u.id = p.author_id
                LEFT JOIN votes v ON v.post_id = p.id
                LEFT JOIN comments c ON c.post_id = p.id AND c.status = 'visible'
                WHERE p.id = ? AND p.status = 'published'
                GROUP BY p.id, p.post_type, p.title, p.content, p.is_pinned, p.status, p.created_at, u.id, u.full_name, u.role, u.avatar_url
                """;
        return pool.preparedQuery(sql)
                .execute(Tuple.of(postId))
                .map(rows -> rows.iterator().hasNext() ? toPostDetail(rows.iterator().next()) : null);
    }

    public Future<JsonObject> listPosts(String type, String sort, int page, int limit) {
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
        if ("popular".equals(sort)) {
            orderBy = " ORDER BY score DESC, p.created_at DESC ";
        } else if ("comments".equals(sort)) {
            orderBy = " ORDER BY comments_count DESC, p.created_at DESC ";
        } else {
            orderBy = " ORDER BY p.created_at DESC ";
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
                    COALESCE(SUM(CASE WHEN v.vote_type='up' THEN 1 WHEN v.vote_type='down' THEN -1 ELSE 0 END), 0) AS score,
                    COUNT(DISTINCT c.id) AS comments_count
                FROM posts p
                JOIN users u ON u.id = p.author_id
                LEFT JOIN votes v ON v.post_id = p.id
                LEFT JOIN comments c ON c.post_id = p.id AND c.status = 'visible'
                """ + where + """
                GROUP BY p.id, p.post_type, p.title, p.content, p.is_pinned, p.status, p.created_at, p.updated_at, u.id, u.full_name, u.role, u.avatar_url
                """ + orderBy + """
                LIMIT ? OFFSET ?
                """;

        String countSql = "SELECT COUNT(*) AS total FROM posts p " + where;

        List<Object> listParams = new ArrayList<>(params);
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
                        .put("score", row.getInteger("score"))
                        .put("commentsCount", row.getInteger("comments_count")))
                .put("viewerVote", (Object) null);
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
                        .put("score", row.getInteger("score"))
                        .put("commentsCount", row.getInteger("comments_count")))
                .put("viewerVote", (Object) null);
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
}
