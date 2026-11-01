package com.procureflow.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GoogleRedirectIntegrationTest {

    @Value("${local.server.port}")
    int port;

    @Test
    void startingGoogleLoginRedirectsToGoogle() throws Exception {
        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
        HttpRequest req = HttpRequest.newBuilder(
                URI.create("http://localhost:" + port + "/oauth2/authorization/google")).GET().build();

        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());

        assertThat(res.statusCode()).isEqualTo(302);
        assertThat(res.headers().firstValue("Location").orElse(""))
                .contains("accounts.google.com").contains("code_challenge");
    }
}