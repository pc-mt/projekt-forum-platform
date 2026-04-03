package com.example.comment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.database.DatabaseClient;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

public class CommentRepository {

    private final JDBCPool pool;

    public CommentRepository() {
        this.pool = DatabaseClient.getInstance();
    }

    public Future<Boolean> postExists(long postId) {
        return pool.preparedQuery("SELECT id FROM posts WHERE id = ? AND status = 'published'")
                .execute(Tuple.of(postId))
                .map(rows -> rows.iterator().hasNext());
    }

    public Future<Boolean> commentExistsOnPost(long commentId, long postId) {
        return pool.preparedQuery("SELECT id FROM comments WHERE id = ? AND post_id = ? AND status='visible'")
                .execute(Tuple.of(commentId, postId))
                .map(rows -> rows.iterator().hasNext());
    }

    public Future<JsonObject> createComment(long postId, Long parentCommentId, long authorId, String content) {
        String sql = """
                INSERT INTO comments (post_id, parent_comment_id, author_id, content, status)
                VALUES (?, ?, ?, ?, 'visible')
                """;
        return pool.preparedQuery(sql)
                .execute(Tuple.of(postId, parentCommentId, authorId, content))
                .compose(r -> findLatestByAuthorOnPost(authorId, postId));
    }

    public Future<JsonObject> findLatestByAuthorOnPost(long authorId, long postId) {
        String sql = """
                SELECT
                    c.id, c.post_id, c.parent_comment_id, c.content, c.created_at,
                    u.id AS author_id, u.full_name, u.role, u.avatar_url
                FROM comments c
                JOIN users u ON u.id = c.author_id
                WHERE c.author_id = ? AND c.post_id = ?
                ORDER BY c.id DESC
                LIMIT 1
                """;
        return pool.preparedQuery(sql)
                .execute(Tuple.of(authorId, postId))
                .map(rows -> rows.iterator().hasNext() ? toComment(rows.iterator().next()) : null);
    }

    public Future<JsonObject> listCommentsTree(long postId, Long viewerUserId) {
        String sql = """
                SELECT
                    c.id, c.post_id, c.parent_comment_id, c.content, c.created_at,
                    u.id AS author_id, u.full_name, u.role, u.avatar_url,
                    COALESCE(SUM(CASE WHEN cv.vote_type='up' THEN 1 WHEN cv.vote_type='down' THEN -1 ELSE 0 END), 0) AS score,
                    MAX(CASE WHEN cv.user_id = ? THEN cv.vote_type ELSE NULL END) AS viewer_vote
                FROM comments c
                JOIN users u ON u.id = c.author_id
                LEFT JOIN comment_votes cv ON cv.comment_id = c.id
                WHERE c.post_id = ? AND c.status = 'visible'
                GROUP BY c.id, c.post_id, c.parent_comment_id, c.content, c.created_at, u.id, u.full_name, u.role, u.avatar_url
                ORDER BY c.created_at ASC
                """;

        long viewer = viewerUserId == null ? -1L : viewerUserId;
        return pool.preparedQuery(sql).execute(Tuple.of(viewer, postId)).map(rows -> {
            Map<Long, JsonObject> all = new HashMap<>();
            List<JsonObject> ordered = new ArrayList<>();
            for (Row row : rows) {
                JsonObject c = toCommentWithVote(row);
                c.put("replies", new JsonArray());
                ordered.add(c);
                all.put(c.getLong("id"), c);
            }
            JsonArray roots = new JsonArray();
            for (JsonObject c : ordered) {
                Long parentId = c.getLong("parentCommentId");
                if (parentId == null) {
                    roots.add(c);
                } else {
                    JsonObject parent = all.get(parentId);
                    if (parent != null) {
                        parent.getJsonArray("replies").add(c);
                    } else {
                        roots.add(c);
                    }
                }
            }
            return new JsonObject().put("items", roots);
        });
    }

    public Future<Void> upsertCommentVote(long commentId, long userId, String voteType) {
        String sql = """
                INSERT INTO comment_votes (comment_id, user_id, vote_type)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE vote_type = VALUES(vote_type)
                """;
        return pool.preparedQuery(sql).execute(Tuple.of(commentId, userId, voteType)).mapEmpty();
    }

    public Future<JsonObject> getCommentVoteStats(long commentId, long viewerUserId) {
        String sql = """
                SELECT
                    COALESCE(SUM(CASE WHEN cv.vote_type='up' THEN 1 WHEN cv.vote_type='down' THEN -1 ELSE 0 END), 0) AS score,
                    MAX(CASE WHEN cv.user_id = ? THEN cv.vote_type ELSE NULL END) AS viewer_vote
                FROM comment_votes cv
                WHERE cv.comment_id = ?
                """;
        return pool.preparedQuery(sql).execute(Tuple.of(viewerUserId, commentId)).map(rows -> {
            Row row = rows.iterator().next();
            return new JsonObject()
                    .put("score", row.getInteger("score"))
                    .put("viewerVote", row.getString("viewer_vote"));
        });
    }

    private JsonObject toComment(Row row) {
        return new JsonObject()
                .put("id", row.getLong("id"))
                .put("postId", row.getLong("post_id"))
                .put("parentCommentId", row.getLong("parent_comment_id"))
                .put("content", row.getString("content"))
                .put("createdAt", row.getValue("created_at") == null ? null : row.getValue("created_at").toString())
                .put("author", new JsonObject()
                        .put("id", row.getLong("author_id"))
                        .put("fullName", row.getString("full_name"))
                        .put("role", row.getString("role"))
                        .put("avatarUrl", row.getString("avatar_url")));
    }

    private JsonObject toCommentWithVote(Row row) {
        return toComment(row)
                .put("stats", new JsonObject().put("score", row.getInteger("score")))
                .put("viewerVote", row.getString("viewer_vote"));
    }
}
