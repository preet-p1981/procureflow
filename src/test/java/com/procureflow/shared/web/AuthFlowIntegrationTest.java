package com.procureflow.shared.web;


import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    @Value("${local.server.port}")
    int port;

    private final HttpClient client = HttpClient.newHttpClient();

    private HttpResponse<String> post(String path, String json) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private static String registerJson(String email, String password) {
        return """
               {"email":"%s","password":"%s","fullName":"Test User"}""".formatted(email, password);
    }

    private static String loginJson(String email, String password) {
        return """
               {"email":"%s","password":"%s"}""".formatted(email, password);
    }

    @Test
    void registerThenLoginReturnsToken() throws Exception {
        String email = "it-" + UUID.randomUUID() + "@example.com";

        assertThat(post("/api/v1/auth/register", registerJson(email, "Strong123")).statusCode())
                .isEqualTo(201);

        HttpResponse<String> login = post("/api/v1/auth/login", loginJson(email, "Strong123"));
        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(login.body()).contains("accessToken").contains("Bearer");
    }

    @Test
    void wrongPasswordIs401AndDuplicateEmailIs409() throws Exception {
        String email = "it-" + UUID.randomUUID() + "@example.com";
        post("/api/v1/auth/register", registerJson(email, "Strong123"));

        assertThat(post("/api/v1/auth/login", loginJson(email, "Wrong123")).statusCode())
                .isEqualTo(401);
        assertThat(post("/api/v1/auth/register", registerJson(email, "Strong123")).statusCode())
                .isEqualTo(409);
    }

    @Test
    void weakPasswordIs4xx() throws Exception {
        String email = "it-" + UUID.randomUUID() + "@example.com";
        assertThat(post("/api/v1/auth/register", registerJson(email, "weak")).statusCode())
                .isBetween(400, 422);
    }
    private static String field(String json, String name) {
        var m = java.util.regex.Pattern.compile("\"" + name + "\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        if (!m.find()) throw new AssertionError(name + " not found in " + json);
        return m.group(1);
    }

    private static String refreshJson(String token) {
        return """
           {"refreshToken":"%s"}""".formatted(token);
    }

    @Test
    void refreshRotatesToken() throws Exception {
        String email = "it-" + UUID.randomUUID() + "@example.com";
        post("/api/v1/auth/register", registerJson(email, "Strong123"));
        String first = field(post("/api/v1/auth/login", loginJson(email, "Strong123")).body(), "refreshToken");

        HttpResponse<String> res = post("/api/v1/auth/refresh", refreshJson(first));

        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(field(res.body(), "refreshToken")).isNotEqualTo(first);
    }

    @Test
    void reusingOldRefreshTokenRevokesWholeFamily() throws Exception {
        String email = "it-" + UUID.randomUUID() + "@example.com";
        post("/api/v1/auth/register", registerJson(email, "Strong123"));
        String first = field(post("/api/v1/auth/login", loginJson(email, "Strong123")).body(), "refreshToken");
        String second = field(post("/api/v1/auth/refresh", refreshJson(first)).body(), "refreshToken");

        assertThat(post("/api/v1/auth/refresh", refreshJson(first)).statusCode()).isEqualTo(401); // reuse
        assertThat(post("/api/v1/auth/refresh", refreshJson(second)).statusCode()).isEqualTo(401); // family dead
    }

    @Test
    void garbageRefreshTokenIs401() throws Exception {
        assertThat(post("/api/v1/auth/refresh", refreshJson("not-a-real-token")).statusCode()).isEqualTo(401);
    }
    private HttpResponse<String> get(String path, String bearer) throws Exception {
        var b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET();
        if (bearer != null) b.header("Authorization", "Bearer " + bearer);
        return client.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void meWithoutTokenIs401ProblemJson() throws Exception {
        HttpResponse<String> res = get("/api/v1/users/me", null);
        assertThat(res.statusCode()).isEqualTo(401);
        assertThat(res.headers().firstValue("Content-Type").orElse("")).contains("problem+json");
    }

    @Test
    void meWithTokenReturnsIdentity() throws Exception {
        String email = "it-" + UUID.randomUUID() + "@example.com";
        post("/api/v1/auth/register", registerJson(email, "Strong123"));
        String access = field(post("/api/v1/auth/login", loginJson(email, "Strong123")).body(), "accessToken");

        HttpResponse<String> res = get("/api/v1/users/me", access);

        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(res.body()).contains(email).contains("ROLE_REQUESTER");
    }

//    @Test
//    void requesterCannotCallAdminEndpoint() throws Exception {
//        String email = "it-" + UUID.randomUUID() + "@example.com";
//        post("/api/v1/auth/register", registerJson(email, "Strong123"));
//        String access = field(post("/api/v1/auth/login", loginJson(email, "Strong123")).body(), "accessToken");
//
//        HttpResponse<String> res = get("/api/v1/users/admin-check", access);
//
//        assertThat(res.statusCode()).isEqualTo(403);
//        assertThat(res.headers().firstValue("Content-Type").orElse("")).contains("problem+json");
//    }
@org.springframework.beans.factory.annotation.Autowired
com.procureflow.identity.domain.port.UserRepository userRepo;
    @org.springframework.beans.factory.annotation.Autowired
    com.procureflow.identity.domain.port.RoleRepository roleRepo;

    private HttpResponse<String> patch(String path, String json, String bearer) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + bearer)
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private String registerAndLogin(String email) throws Exception {
        post("/api/v1/auth/register", registerJson(email, "Strong123"));
        return field(post("/api/v1/auth/login", loginJson(email, "Strong123")).body(), "accessToken");
    }

    private String adminToken() throws Exception {
        String email = "admin-" + UUID.randomUUID() + "@example.com";
        post("/api/v1/auth/register", registerJson(email, "Strong123"));
        var user = userRepo.findByEmail(com.procureflow.identity.domain.EmailAddress.of(email)).orElseThrow();
        user.assignRole(roleRepo.findByCode("ADMIN").orElseThrow());
        userRepo.save(user);
        // log in AFTER promotion so the JWT carries the admin authorities
        return field(post("/api/v1/auth/login", loginJson(email, "Strong123")).body(), "accessToken");
    }

    @Test
    void requesterCannotChangeRoles() throws Exception {
        String access = registerAndLogin("it-" + UUID.randomUUID() + "@example.com");
        HttpResponse<String> res = patch("/api/v1/users/" + UUID.randomUUID() + "/roles",
                "{\"assign\":[\"AUDITOR\"]}", access);
        assertThat(res.statusCode()).isEqualTo(403);
    }

    @Test
    void adminCanAssignRole() throws Exception {
        String targetEmail = "it-" + UUID.randomUUID() + "@example.com";
        post("/api/v1/auth/register", registerJson(targetEmail, "Strong123"));
        String targetId = userRepo.findByEmail(com.procureflow.identity.domain.EmailAddress.of(targetEmail))
                .orElseThrow().id().value().toString();

        HttpResponse<String> res = patch("/api/v1/users/" + targetId + "/roles",
                "{\"assign\":[\"AUDITOR\"]}", adminToken());

        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(res.body()).contains("AUDITOR").contains("REQUESTER");
    }

    @Test
    void unknownRoleCodeIsRejected() throws Exception {
        String targetEmail = "it-" + UUID.randomUUID() + "@example.com";
        post("/api/v1/auth/register", registerJson(targetEmail, "Strong123"));
        String targetId = userRepo.findByEmail(com.procureflow.identity.domain.EmailAddress.of(targetEmail))
                .orElseThrow().id().value().toString();

        HttpResponse<String> res = patch("/api/v1/users/" + targetId + "/roles",
                "{\"assign\":[\"NOPE\"]}", adminToken());

        assertThat(res.statusCode()).isBetween(400, 422);
    }
}