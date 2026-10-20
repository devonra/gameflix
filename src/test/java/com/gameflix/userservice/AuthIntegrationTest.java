package com.gameflix.userservice;

import com.gameflix.userservice.model.User;
import com.gameflix.userservice.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Runs against the real MySQL database. Each test uses a random @test.gameflix email and cleans up after itself. */
@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    private static final String DOMAIN = "@test.gameflix";
    private static final String PASSWORD = "Throwaway#Pass123";

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;

    private String email;

    @BeforeEach
    void setUp() { email = "user-" + UUID.randomUUID() + DOMAIN; }

    @AfterEach
    void cleanUp() { users.deleteByEmailEndingWith(DOMAIN); }

    private ResultActions register(String email, String password) throws Exception {
        String body = String.format("{\"email\":\"%s\",\"password\":\"%s\",\"username\":\"tester\"}", email, password);
        return mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions login(String email, String password) throws Exception {
        String body = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, password);
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String loginAndGetToken() throws Exception {
        register(email, PASSWORD).andExpect(status().isCreated());
        String json = login(email, PASSWORD).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.token");
    }

    /** Changes role USER -> ADMIN in the payload but keeps the original signature. */
    private String tamper(String token) {
        String[] p = token.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(p[1]), StandardCharsets.UTF_8);
        String forged = payload.replaceAll("\"role\"\\s*:\\s*\"USER\"", "\"role\":\"ADMIN\"");
        assertNotEquals(payload, forged, "tamper helper did not change the payload");
        String forgedB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(forged.getBytes(StandardCharsets.UTF_8));
        return p[0] + "." + forgedB64 + "." + p[2];
    }

    // 1. successful registration (and the stored value is a hash)
    @Test
    void registerSucceeds_andStoresHashNotPassword() throws Exception {
        register(email, PASSWORD)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.password").doesNotExist());

        User stored = users.findByEmail(email).orElseThrow();
        assertNotEquals(PASSWORD, stored.getPassword());
        assertTrue(stored.getPassword().startsWith("{bcrypt}$2a$12$"), "expected a delegating bcrypt hash, strength 12");
        assertEquals(68, stored.getPassword().length());
    }

    // 2. duplicate email rejected
    @Test
    void duplicateEmailIsRejectedWith409() throws Exception {
        register(email, PASSWORD).andExpect(status().isCreated());
        register(email, PASSWORD).andExpect(status().isConflict());
    }

    // extra: invalid input
    @Test
    void weakPasswordIsRejectedWith400() throws Exception {
        register(email, "short").andExpect(status().isBadRequest());
    }

    // 3. successful login returns a token
    @Test
    void loginSucceedsAndReturnsToken() throws Exception {
        register(email, PASSWORD).andExpect(status().isCreated());
        login(email, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    // 4. wrong password -> 401, and the message is identical to the unknown-email message
    @Test
    void wrongPasswordAndUnknownEmailGiveSameGeneric401() throws Exception {
        register(email, PASSWORD).andExpect(status().isCreated());
        login(email, "Wrong#Password999")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
        login("nobody-" + UUID.randomUUID() + DOMAIN, PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    // 5. protected call with a valid token
    @Test
    void meWithValidTokenReturnsProfile() throws Exception {
        String token = loginAndGetToken();
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    // 6a. protected call with a missing token
    @Test
    void meWithoutTokenIs401() throws Exception {
        mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
    }

    // 6b. protected call with a tampered token
    @Test
    void meWithTamperedTokenIs401() throws Exception {
        String forged = tamper(loginAndGetToken());
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized());
    }
}
