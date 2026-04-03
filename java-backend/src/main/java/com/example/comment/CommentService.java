package com.example.comment;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public class CommentService {

    private final CommentRepository repository;

    public CommentService() {
        this.repository = new CommentRepository();
    }

    public Future<JsonObject> listComments(long postId, Long viewerUserId) {
        return repository.postExists(postId).compose(exists -> {
            if (!exists) {
                return Future.failedFuture(new ApiException(404, "post not found"));
            }
            return repository.listCommentsTree(postId, viewerUserId);
        });
    }

    public Future<JsonObject> createComment(long postId, long userId, JsonObject body) {
        String content = normalize(body.getString("content"));
        Long parentCommentId = body.getLong("parentCommentId");

        if (content == null || content.length() < 2 || content.length() > 2000) {
            return Future.failedFuture(new ApiException(400, "content must be between 2 and 2000 characters"));
        }

        return repository.postExists(postId).compose(exists -> {
            if (!exists) {
                return Future.failedFuture(new ApiException(404, "post not found"));
            }
            if (parentCommentId == null) {
                return repository.createComment(postId, null, userId, content);
            }
            return repository.commentExistsOnPost(parentCommentId, postId).compose(parentExists -> {
                if (!parentExists) {
                    return Future.failedFuture(new ApiException(400, "parent comment not found on this post"));
                }
                return repository.createComment(postId, parentCommentId, userId, content);
            });
        });
    }

    public Future<JsonObject> voteComment(long commentId, long userId, JsonObject body) {
        String voteType = normalize(body.getString("voteType"));
        if (!"up".equals(voteType) && !"down".equals(voteType)) {
            return Future.failedFuture(new ApiException(400, "voteType must be one of: up, down"));
        }
        return repository.findUserCommentVote(commentId, userId).compose(currentVote -> {
            if (voteType.equals(currentVote)) {
                return repository.deleteCommentVote(commentId, userId)
                        .compose(v -> repository.getCommentVoteStats(commentId, userId));
            }
            return repository.upsertCommentVote(commentId, userId, voteType)
                    .compose(v -> repository.getCommentVoteStats(commentId, userId));
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
