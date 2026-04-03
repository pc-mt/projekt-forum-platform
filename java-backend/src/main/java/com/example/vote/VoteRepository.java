package com.example.vote;

import com.example.database.DatabaseClient;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

public class VoteRepository {

    private final JDBCPool pool;

    public VoteRepository() {
        this.pool = DatabaseClient.getInstance();
    }

    public Future<Boolean> postExists(long postId) {
        return pool.preparedQuery("SELECT id FROM posts WHERE id = ? AND status = 'published'")
                .execute(Tuple.of(postId))
                .map(rows -> rows.iterator().hasNext());
    }

    public Future<Void> upsertPostVote(long postId, long userId, String voteType) {
        String sql = """
                INSERT INTO votes (post_id, user_id, vote_type)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE vote_type = VALUES(vote_type)
                """;
        return pool.preparedQuery(sql).execute(Tuple.of(postId, userId, voteType)).mapEmpty();
    }

    public Future<JsonObject> getPostVoteStats(long postId, long viewerUserId) {
        String sql = """
                SELECT
                    COALESCE(SUM(CASE WHEN vote_type='up' THEN 1 WHEN vote_type='down' THEN -1 ELSE 0 END), 0) AS score,
                    MAX(CASE WHEN user_id = ? THEN vote_type ELSE NULL END) AS viewer_vote
                FROM votes
                WHERE post_id = ?
                """;
        return pool.preparedQuery(sql).execute(Tuple.of(viewerUserId, postId)).map(rows -> {
            Row row = rows.iterator().next();
            return new JsonObject()
                    .put("score", row.getInteger("score"))
                    .put("viewerVote", row.getString("viewer_vote"));
        });
    }
}
