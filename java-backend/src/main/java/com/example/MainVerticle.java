package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.database.DatabaseClient;
import com.example.http.HttpServerVerticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class MainVerticle extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

    @Override
    public void start(Promise<Void> startPromise) {
        setupJDBCPool();

        HttpServerVerticle http = new HttpServerVerticle();
        vertx.deployVerticle(http, ar -> {
            if (ar.succeeded()) {
                logger.info("HTTP verticle deployed.");
                startPromise.complete();
            } else {
                logger.error("HTTP verticle failed: {}", ar.cause().getMessage());
                startPromise.fail(ar.cause());
            }
        });
    }

    private void setupJDBCPool() {
        JsonObject config = new JsonObject()
                .put("DB_HOST", System.getenv("DB_HOST") != null ? System.getenv("DB_HOST") : "mariadb")
                .put("DB_PORT", System.getenv("DB_PORT") != null ? Integer.parseInt(System.getenv("DB_PORT")) : 3306)
                .put("DB_NAME", System.getenv("DB_NAME") != null ? System.getenv("DB_NAME") : "chatplatform")
                .put("DB_USER", System.getenv("DB_USER") != null ? System.getenv("DB_USER") : "chatuser")
                .put("DB_PASSWORD", System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : "change_me");

        DatabaseClient.initialize(vertx, config);
    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new MainVerticle(), res -> {
            if (res.succeeded()) {
                logger.info("MainVerticle deployment succeeded");
            } else {
                logger.error("MainVerticle deployment failed: {}", res.cause().getMessage());
            }
        });
    }
}
