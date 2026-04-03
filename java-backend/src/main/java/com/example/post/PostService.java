package com.example.post;

import java.util.Set;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public class PostService {

    private static final Set<String> ALLOWED_TYPES = Set.of("news", "idea", "discussion");
    private static final Set<String> ALLOWED_SORTS = Set.of("popular", "recent", "comments");

    private final PostRepository repository;

    public PostService() {
        this.repository = new PostRepository();
    }

    public Future<JsonObject> listPosts(String type, String sort, int page, int limit, Long viewerUserId) {
        String normalizedType = type == null || type.isBlank() ? null : type.trim().toLowerCase();
        String normalizedSort = sort == null || sort.isBlank() ? "recent" : sort.trim().toLowerCase();

        if (normalizedType != null && !ALLOWED_TYPES.contains(normalizedType)) {
            return Future.failedFuture(new ApiException(400, "invalid query parameter: type"));
        }
        if (!ALLOWED_SORTS.contains(normalizedSort)) {
            return Future.failedFuture(new ApiException(400, "invalid query parameter: sort"));
        }

        return repository.listPosts(normalizedType, normalizedSort, page, limit, viewerUserId);
    }

    public Future<JsonObject> createPost(long authorId, JsonObject body) {
        String postType = normalize(body.getString("postType"));
        String title = normalize(body.getString("title"));
        String content = normalize(body.getString("content"));

        if (postType == null || !ALLOWED_TYPES.contains(postType)) {
            return Future.failedFuture(new ApiException(400, "postType must be one of: news, idea, discussion"));
        }
        if (title == null || title.length() < 3 || title.length() > 200) {
            return Future.failedFuture(new ApiException(400, "title must be between 3 and 200 characters"));
        }
        if (content == null || content.length() < 10 || content.length() > 10000) {
            return Future.failedFuture(new ApiException(400, "content must be between 10 and 10000 characters"));
        }

        return repository.createPost(authorId, postType, title, content);
    }

    public Future<JsonObject> getPostDetail(long postId, Long viewerUserId) {
        return repository.findPostById(postId, viewerUserId).compose(post -> {
            if (post == null) {
                return Future.failedFuture(new ApiException(404, "post not found"));
            }
            return Future.succeededFuture(post);
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
