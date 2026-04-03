package com.example.http;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.comment.CommentController;
import com.example.post.PostController;
import com.example.user.UserController;
import com.example.vote.VoteController;

import java.util.Set;

public class HttpServerVerticle extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(HttpServerVerticle.class);

    /** Exposed for future route registration from MainVerticle. */
    public Router router;

    @Override
    public void start() {
        router = Router.router(vertx);
        UserController userController = new UserController();
        PostController postController = new PostController();
        VoteController voteController = new VoteController();
        CommentController commentController = new CommentController();

        Set<String> allowedHeaders = Set.of(
                "x-requested-with",
                "Access-Control-Allow-Origin",
                "origin",
                "Content-Type",
                "accept",
                "Authorization"
        );
        // CorsHandler must run before BodyHandler so OPTIONS preflight works. Only addRelativeOrigin(".*") keeps
        // "allow any origin"; adding another pattern (e.g. ^null$) leaves restricted mode and rejects http origins → 403.
        router.route().handler(CorsHandler.create()
                .addRelativeOrigin(".*")
                .allowedHeaders(allowedHeaders)
                .allowedMethods(Set.of(io.vertx.core.http.HttpMethod.GET, io.vertx.core.http.HttpMethod.POST,
                        io.vertx.core.http.HttpMethod.PUT, io.vertx.core.http.HttpMethod.PATCH,
                        io.vertx.core.http.HttpMethod.DELETE, io.vertx.core.http.HttpMethod.OPTIONS)));
        router.route().handler(BodyHandler.create());

        router.get("/api/health").handler(ctx -> {
            JsonObject body = new JsonObject()
                    .put("status", "ok")
                    .put("service", "chat-platform-backend");
            ctx.response()
                    .putHeader("Content-Type", "application/json")
                    .end(body.encode());
        });
        router.post("/api/auth/register").handler(userController::handleRegister);
        router.post("/api/auth/login").handler(userController::handleLogin);
        router.get("/api/auth/me").handler(userController::handleMe);
        router.get("/api/profile/me").handler(userController::handleProfileMe);
        router.get("/api/posts").handler(postController::handleListPosts);
        router.patch("/api/posts/:id/pinned").handler(postController::handlePatchPostPinned);
        router.post("/api/posts/:id/my-pin/toggle").handler(postController::handlePostToggleMyPin);
        router.get("/api/posts/:id").handler(postController::handleGetPostDetail);
        router.post("/api/posts").handler(postController::handleCreatePost);
        router.post("/api/posts/:postId/vote").handler(voteController::handleVotePost);
        router.get("/api/posts/:postId/comments").handler(commentController::handleListComments);
        router.post("/api/posts/:postId/comments").handler(commentController::handleCreateComment);
        router.post("/api/comments/:commentId/vote").handler(commentController::handleVoteComment);

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(8080, http -> {
                    if (http.succeeded()) {
                        logger.info("HTTP server started on port 8080");
                    } else {
                        logger.error("Failed to start HTTP server: {}", http.cause().getMessage());
                    }
                });
    }
}
