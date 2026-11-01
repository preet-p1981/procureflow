package com.procureflow.vendor.web;

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
class VendorFlowIntegrationTest {

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

    /** Registers a user, optionally grants a role, then logs in so the JWT carries the new authorities. */
    private String tokenFor(String roleCode) throws Exception {
        String email = "v-" + UUID.randomUUID() + "@example.com";
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

    private static String vendorJson(String taxId) {
        return "{\"legalName\":\"Acme Pvt Ltd\",\"taxId\":\"" + taxId + "\",\"contactEmail\":\"sales@acme.com\"}";
    }

    private static String newTaxId() {
        return "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase();
    }

    @Test
    void requesterCannotRegisterVendor() throws Exception {
        var res = call("POST", "/api/v1/vendors", vendorJson(newTaxId()), tokenFor(null));
        assertThat(res.statusCode()).isEqualTo(403);
    }

    @Test
    void adminRegistersThenApprovesVendor() throws Exception {
        String admin = tokenFor("ADMIN");

        var created = call("POST", "/api/v1/vendors", vendorJson(newTaxId()), admin);
        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(created.body()).contains("PENDING");
        String id = field(created.body(), "id");

        var approved = call("POST", "/api/v1/vendors/" + id + "/approve", null, admin);
        assertThat(approved.statusCode()).isEqualTo(200);
        assertThat(approved.body()).contains("APPROVED");

        var fetched = call("GET", "/api/v1/vendors/" + id, null, admin);
        assertThat(fetched.statusCode()).isEqualTo(200);
        assertThat(fetched.body()).contains("APPROVED");
    }

    @Test
    void approvingTwiceIsBusinessRuleError() throws Exception {
        String admin = tokenFor("ADMIN");
        String id = field(call("POST", "/api/v1/vendors", vendorJson(newTaxId()), admin).body(), "id");
        call("POST", "/api/v1/vendors/" + id + "/approve", null, admin);

        var again = call("POST", "/api/v1/vendors/" + id + "/approve", null, admin);

        assertThat(again.statusCode()).isEqualTo(422);
        assertThat(again.body()).contains("VENDOR_INVALID_TRANSITION");
    }

    @Test
    void rejectNeedsReasonAndRejectedIsFinal() throws Exception {
        String admin = tokenFor("ADMIN");
        String id = field(call("POST", "/api/v1/vendors", vendorJson(newTaxId()), admin).body(), "id");

        assertThat(call("POST", "/api/v1/vendors/" + id + "/reject", "{\"reason\":\"\"}", admin).statusCode())
                .isEqualTo(400);
        assertThat(call("POST", "/api/v1/vendors/" + id + "/reject", "{\"reason\":\"Documents missing\"}", admin)
                .statusCode()).isEqualTo(200);
        assertThat(call("POST", "/api/v1/vendors/" + id + "/approve", null, admin).statusCode())
                .isEqualTo(422);
    }

    @Test
    void duplicateTaxIdIs409() throws Exception {
        String admin = tokenFor("ADMIN");
        String tax = newTaxId();
        call("POST", "/api/v1/vendors", vendorJson(tax), admin);

        assertThat(call("POST", "/api/v1/vendors", vendorJson(tax), admin).statusCode()).isEqualTo(409);
    }

    @Test
    void auditorCanReadButNotManage() throws Exception {
        String admin = tokenFor("ADMIN");
        String id = field(call("POST", "/api/v1/vendors", vendorJson(newTaxId()), admin).body(), "id");
        String auditor = tokenFor("AUDITOR");

        assertThat(call("GET", "/api/v1/vendors/" + id, null, auditor).statusCode()).isEqualTo(200);
        assertThat(call("POST", "/api/v1/vendors/" + id + "/approve", null, auditor).statusCode())
                .isEqualTo(403);
    }
}