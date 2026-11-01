package com.procureflow.catalog.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.procureflow.identity.domain.EmailAddress;
import com.procureflow.identity.domain.port.RoleRepository;
import com.procureflow.identity.domain.port.UserRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class DepartmentFlowIntegrationTest {

    @Value("${local.server.port}")
    int port;

    @Autowired UserRepository users;
    @Autowired RoleRepository roles;

    private final HttpClient client = HttpClient.newHttpClient();

    private HttpResponse<String> call(String method, String path, String json, String bearer) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json");
        if (bearer != null) b.header("Authorization", "Bearer " + bearer);
        b.method(method, json == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(json));
        return client.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String field(String json, String name) {
        var m = Pattern.compile("\"" + name + "\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        if (!m.find()) throw new AssertionError(name + " not found in " + json);
        return m.group(1);
    }

    private String tokenFor(String roleCode) throws Exception {
        String email = "d-" + UUID.randomUUID() + "@example.com";
        call("POST", "/api/v1/auth/register",
                "{\"email\":\"" + email + "\",\"password\":\"Strong123\",\"fullName\":\"Test\"}", null);
        if (roleCode != null) {
            var user = users.findByEmail(EmailAddress.of(email)).orElseThrow();
            user.assignRole(roles.findByCode(roleCode).orElseThrow());
            users.save(user);
        }
        return field(call("POST", "/api/v1/auth/login",
                "{\"email\":\"" + email + "\",\"password\":\"Strong123\"}", null).body(), "accessToken");
    }

    private static String newCode() {
        return "D" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    private static String body(String code) {
        return "{\"code\":\"" + code + "\",\"name\":\"Finance Dept\"}";
    }

    @Test
    void requesterCanReadButNotCreate() throws Exception {
        String requester = tokenFor(null);
        assertThat(call("GET", "/api/v1/departments", null, requester).statusCode()).isEqualTo(200);
        assertThat(call("POST", "/api/v1/departments", body(newCode()), requester).statusCode()).isEqualTo(403);
    }

    @Test
    void adminCreatesAndUpdatesDepartment() throws Exception {
        String admin = tokenFor("ADMIN");
        String code = newCode().toLowerCase();

        var created = call("POST", "/api/v1/departments", body(code), admin);
        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(created.body()).contains(code.toUpperCase());   // stored upper-case
        String id = field(created.body(), "id");

        var updated = call("PUT", "/api/v1/departments/" + id,
                "{\"name\":\"Finance and Accounts\",\"active\":false}", admin);
        assertThat(updated.statusCode()).isEqualTo(200);
        assertThat(updated.body()).contains("Finance and Accounts").contains("\"active\":false");
    }

    @Test
    void duplicateCodeIgnoringCaseIs409() throws Exception {
        String admin = tokenFor("ADMIN");
        String code = newCode();
        call("POST", "/api/v1/departments", body(code), admin);

        assertThat(call("POST", "/api/v1/departments", body(code.toLowerCase()), admin).statusCode())
                .isEqualTo(409);
    }

    @Test
    void badCodeIs400AndUnknownIdIs404() throws Exception {
        String admin = tokenFor("ADMIN");
        assertThat(call("POST", "/api/v1/departments", body("x y!"), admin).statusCode()).isEqualTo(400);
        assertThat(call("GET", "/api/v1/departments/" + UUID.randomUUID(), null, admin).statusCode())
                .isEqualTo(404);
    }
}