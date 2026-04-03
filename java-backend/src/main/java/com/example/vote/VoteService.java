package com.example.vote;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public class VoteService {

    private final VoteRepository repository;

    public VoteService() {
        this.repository = new VoteRepository();
    }

    public Future<JsonObject> votePost(long postId, long userId, JsonObject body) {
        String voteType = normalize(body.getString("voteType"));
        if (!"up".equals(voteType) && !"down".equals(voteType)) {
            return Future.failedFuture(new ApiException(400, "voteType must be one of: up, down"));
        }
        return repository.postExists(postId).compose(exists -> {
            if (!exists) {
                return Future.failedFuture(new ApiException(404, "post not found"));
            }
            return repository.upsertPostVote(postId, userId, voteType)
                    .compose(v -> repository.getPostVoteStats(postId, userId));
        });
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        return v.isEmpty() ? null : v;
    }

    public static class ApiException extends RuntimeException {
        private final int statusCode;

        public ApiException(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }
}
