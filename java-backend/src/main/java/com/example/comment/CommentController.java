package com.example.comment;

import com.example.auth.JwtUtils;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class CommentController {

    private final CommentService commentService;
    private final JwtUtils jwtUtils;

    public CommentController() {
        this.commentService = new CommentService();
        this.jwtUtils = new JwtUtils();
    }

    public void handleListComments(RoutingContext ctx) {
        long postId = parseId(ctx.pathParam("postId"));
        if (postId <= 0) {
            json(ctx, 400, new JsonObject().put("error", "invalid post id"));
            return;
        }
        Long viewerId = extractUserIdOptional(ctx);
        commentService.listComments(postId, viewerId)
                .onSuccess(result -> json(ctx, 200, result))
                .onFailure(err -> handleFailure(ctx, err));
    }

    public void handleCreateComment(RoutingContext ctx) {
        long postId = parseId(ctx.pathParam("postId"));
        if (postId <= 0) {
            json(ctx, 400, new JsonObject().put("error", "invalid post id"));
            return;
        }
        Long userId = extractUserIdRequired(ctx);
        if (userId == null) {
            return;
        }
        JsonObject body = ctx.body() == null ? new JsonObject() : ctx.body().asJsonObject();
        commentService.createComment(postId, userId, body)
                .onSuccess(result -> json(ctx, 201, result))
                .onFailure(err -> handleFailure(ctx, err));
    }

    public void handleVoteComment(RoutingContext ctx) {
        long commentId = parseId(ctx.pathParam("commentId"));
        if (commentId <= 0) {
            json(ctx, 400, new JsonObject().put("error", "invalid comment id"));
            return;
        }
        Long userId = extractUserIdRequired(ctx);
        if (userId == null) {
            return;
        }
        JsonObject body = ctx.body() == null ? new JsonObject() : ctx.body().asJsonObject();
        commentService.voteComment(commentId, userId, body)
                .onSuccess(result -> json(ctx, 200, result))
                .onFailure(err -> handleFailure(ctx, err));
    }

    private long parseId(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (Exception e) {
            return -1;
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

    private Long extractUserIdRequired(RoutingContext ctx) {
        String auth = ctx.request().getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            json(ctx, 401, new JsonObject().put("error", "unauthorized"));
            return null;
        }
        try {
            return Long.parseLong(jwtUtils.verifyToken(auth.substring("Bearer ".length()).trim()).getSubject());
        } catch (Exception e) {
            json(ctx, 401, new JsonObject().put("error", "unauthorized"));
            return null;
        }
    }

    private void handleFailure(RoutingContext ctx, Throwable err) {
        if (err instanceof CommentService.ApiException apiException) {
            json(ctx, apiException.getStatusCode(), new JsonObject().put("error", apiException.getMessage()));
            return;
        }
        json(ctx, 500, new JsonObject().put("error", "internal server error"));
    }

    private void json(RoutingContext ctx, int status, JsonObject body) {
        ctx.response().setStatusCode(status).putHeader("Content-Type", "application/json").end(body.encode());
    }
}
