package com.example.post;

import com.example.auth.JwtUtils;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class PostController {

    private final PostService postService;
    private final JwtUtils jwtUtils;

    public PostController() {
        this.postService = new PostService();
        this.jwtUtils = new JwtUtils();
    }

    public void handleListPosts(RoutingContext ctx) {
        String type = ctx.request().getParam("type");
        String sort = ctx.request().getParam("sort");
        int page = parseInt(ctx.request().getParam("page"), 1);
        int limit = parseInt(ctx.request().getParam("limit"), 20);
        Long viewerUserId = extractUserIdOptional(ctx);

        postService.listPosts(type, sort, page, limit, viewerUserId)
                .onSuccess(result -> json(ctx, 200, result))
                .onFailure(err -> handleFailure(ctx, err));
    }

    public void handleCreatePost(RoutingContext ctx) {
        Long userId = extractUserId(ctx);
        if (userId == null) {
            return;
        }
        JsonObject body = ctx.body() == null ? new JsonObject() : ctx.body().asJsonObject();
        postService.createPost(userId, body)
                .onSuccess(result -> json(ctx, 201, result))
                .onFailure(err -> handleFailure(ctx, err));
    }

    public void handleGetPostDetail(RoutingContext ctx) {
        String idParam = ctx.pathParam("id");
        long postId;
        try {
            postId = Long.parseLong(idParam);
        } catch (Exception e) {
            json(ctx, 400, new JsonObject().put("error", "invalid post id"));
            return;
        }

        postService.getPostDetail(postId, extractUserIdOptional(ctx))
                .onSuccess(result -> json(ctx, 200, result))
                .onFailure(err -> handleFailure(ctx, err));
    }

    private Long extractUserId(RoutingContext ctx) {
        String auth = ctx.request().getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            json(ctx, 401, new JsonObject().put("error", "unauthorized"));
            return null;
        }
        String token = auth.substring("Bearer ".length()).trim();
        try {
            return Long.parseLong(jwtUtils.verifyToken(token).getSubject());
        } catch (Exception e) {
            json(ctx, 401, new JsonObject().put("error", "unauthorized"));
            return null;
        }
    }

    private Long extractUserIdOptional(RoutingContext ctx) {
        String auth = ctx.request().getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return null;
        }
        try {
            return Long.parseLong(jwtUtils.verifyToken(auth.substring("Bearer ".length()).trim()).getSubject());
        } catch (Exception e) {
            return null;
        }
    }

    private int parseInt(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void handleFailure(RoutingContext ctx, Throwable err) {
        if (err instanceof PostService.ApiException apiException) {
            json(ctx, apiException.getStatusCode(), new JsonObject().put("error", apiException.getMessage()));
            return;
        }
        json(ctx, 500, new JsonObject().put("error", "internal server error"));
    }

    private void json(RoutingContext ctx, int status, JsonObject body) {
        ctx.response()
                .setStatusCode(status)
                .putHeader("Content-Type", "application/json")
                .end(body.encode());
    }
}
