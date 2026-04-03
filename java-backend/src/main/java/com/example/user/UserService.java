package com.example.user;

import java.util.Locale;
import java.util.regex.Pattern;

import com.example.auth.JwtUtils;
import com.example.auth.PasswordUtils;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public class UserService {

    private static final Pattern EMAIL_PATTERN = Pattern
            .compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    private final UserRepository repository;
    private final JwtUtils jwtUtils;

    public UserService() {
        this.repository = new UserRepository();
        this.jwtUtils = new JwtUtils();
    }

    public Future<JsonObject> register(JsonObject body) {
        String fullName = clean(body.getString("fullName"));
        String email = normalizeEmail(body.getString("email"));
        String password = body.getString("password");

        String validationError = validateRegisterInput(fullName, email, password);
        if (validationError != null) {
            return Future.failedFuture(new ApiException(400, validationError));
        }

        return repository.findByEmail(email).compose(existing -> {
            if (existing != null) {
                return Future.failedFuture(new ApiException(409, "email already exists"));
            }

            String hash = PasswordUtils.hash(password);
            return repository.createUser(fullName, email, hash, "user")
                    .compose(user -> buildAuthResponse(user, 0, true));
        });
    }

    public Future<JsonObject> login(JsonObject body) {
        String email = normalizeEmail(body.getString("email"));
        String password = body.getString("password");

        if (email == null || email.isBlank()) {
            return Future.failedFuture(new ApiException(400, "email is required"));
        }
        if (password == null || password.isBlank()) {
            return Future.failedFuture(new ApiException(400, "password is required"));
        }

        return repository.findByEmail(email).compose(user -> {
            if (user == null || !PasswordUtils.verify(password, user.getString("passwordHash"))) {
                return Future.failedFuture(new ApiException(401, "invalid credentials"));
            }
            return buildAuthResponse(user, 0, true);
        });
    }

    public Future<JsonObject> me(long userId) {
        return repository.findById(userId).compose(user -> {
            if (user == null) {
                return Future.failedFuture(new ApiException(401, "unauthorized"));
            }
            return Future.succeededFuture(new JsonObject().put("user", sanitizeUser(user)));
        });
    }

    public Future<JsonObject> profile(long userId) {
        return repository.findById(userId).compose(user -> {
            if (user == null) {
                return Future.failedFuture(new ApiException(401, "unauthorized"));
            }

            return repository.fetchProfileStats(userId)
                    .compose(stats -> repository.fetchRecentPosts(userId, 5)
                            .map(recentPosts -> new JsonObject()
                                    .put("user", sanitizeUser(user))
                                    .put("stats", stats)
                                    .put("recentPosts", recentPosts)));
        });
    }

    public long extractUserIdFromToken(String token) {
        return jwtUtils.extractUserId(token);
    }

    private Future<JsonObject> buildAuthResponse(JsonObject user, int postsLimit, boolean includeStats) {
        String token = jwtUtils.generateToken(
                user.getLong("id"),
                user.getString("email"),
                user.getString("role"));

        if (!includeStats) {
            return Future.succeededFuture(new JsonObject()
                    .put("token", token)
                    .put("user", sanitizeUser(user)));
        }

        return repository.fetchProfileStats(user.getLong("id"))
                .map(stats -> new JsonObject()
                        .put("token", token)
                        .put("user", sanitizeUser(user))
                        .put("stats", stats));
    }

    private JsonObject sanitizeUser(JsonObject user) {
        return new JsonObject()
                .put("id", user.getLong("id"))
                .put("fullName", user.getString("fullName"))
                .put("email", user.getString("email"))
                .put("role", user.getString("role"))
                .put("avatarUrl", user.getString("avatarUrl"))
                .put("memberSince", user.getString("memberSince"));
    }

    private String validateRegisterInput(String fullName, String email, String password) {
        if (fullName == null || fullName.isBlank()) {
            return "fullName is required";
        }
        if (fullName.length() < 2 || fullName.length() > 120) {
            return "fullName must be between 2 and 120 characters";
        }
        if (email == null || email.isBlank()) {
            return "email is required";
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return "invalid email format";
        }
        if (password == null || password.isBlank()) {
            return "password is required";
        }
        if (password.length() < 8) {
            return "password must be at least 8 characters";
        }
        if (password.length() > 72) {
            return "password must be at most 72 characters";
        }
        return null;
    }

    private String normalizeEmail(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
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

