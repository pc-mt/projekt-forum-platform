package com.example.user;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class UserController {

    private final UserService userService;

    public UserController() {
        this.userService = new UserService();
    }

    public void handleRegister(RoutingContext ctx) {
        JsonObject body = readBody(ctx);
        userService.register(body)
                .onSuccess(result -> json(ctx, 201, result))
                .onFailure(err -> handleFailure(ctx, err));
    }

    public void handleLogin(RoutingContext ctx) {
        JsonObject body = readBody(ctx);
        userService.login(body)
                .onSuccess(result -> json(ctx, 200, result))
                .onFailure(err -> handleFailure(ctx, err));
    }

    public void handleMe(RoutingContext ctx) {
        Long userId = extractAuthenticatedUserId(ctx);
        if (userId == null) {
            return;
        }
        userService.me(userId)
                .onSuccess(result -> json(ctx, 200, result))
                .onFailure(err -> handleFailure(ctx, err));
    }

    public void handleProfileMe(RoutingContext ctx) {
        Long userId = extractAuthenticatedUserId(ctx);
        if (userId == null) {
            return;
        }
        userService.profile(userId)
                .onSuccess(result -> json(ctx, 200, result))
                .onFailure(err -> handleFailure(ctx, err));
    }

    private JsonObject readBody(RoutingContext ctx) {
        JsonObject body = ctx.body() == null ? null : ctx.body().asJsonObject();
        return body == null ? new JsonObject() : body;
    }

    private Long extractAuthenticatedUserId(RoutingContext ctx) {
        String auth = ctx.request().getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            json(ctx, 401, new JsonObject().put("error", "missing token"));
            return null;
        }
        String token = auth.substring("Bearer ".length()).trim();
        try {
            return userService.extractUserIdFromToken(token);
        } catch (Exception e) {
            json(ctx, 401, new JsonObject().put("error", "invalid token"));
            return null;
        }
    }

    private void handleFailure(RoutingContext ctx, Throwable err) {
        if (err instanceof UserService.ApiException apiException) {
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

