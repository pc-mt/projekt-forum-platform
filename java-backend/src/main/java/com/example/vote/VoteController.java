package com.example.vote;

import com.example.auth.JwtUtils;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class VoteController {

    private final VoteService voteService;
    private final JwtUtils jwtUtils;

    public VoteController() {
        this.voteService = new VoteService();
        this.jwtUtils = new JwtUtils();
    }

    public void handleVotePost(RoutingContext ctx) {
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
        voteService.votePost(postId, userId, body)
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
        if (err instanceof VoteService.ApiException apiException) {
            json(ctx, apiException.getStatusCode(), new JsonObject().put("error", apiException.getMessage()));
            return;
        }
        json(ctx, 500, new JsonObject().put("error", "internal server error"));
    }

    private void json(RoutingContext ctx, int status, JsonObject body) {
        ctx.response().setStatusCode(status).putHeader("Content-Type", "application/json").end(body.encode());
    }
}
