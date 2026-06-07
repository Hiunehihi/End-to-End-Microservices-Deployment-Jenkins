package com.minejava.apigateway.controller;

import com.minejava.apigateway.client.KeycloakAdminClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final KeycloakAdminClient keycloak;

    public AuthController(KeycloakAdminClient keycloak) {
        this.keycloak = keycloak;
    }

    public record RegisterRequest(String username, String email, String password,
                                  String firstName, String lastName) {}

    public record LoginRequest(String username, String password) {}

    public record RefreshRequest(String refreshToken) {}

    @PostMapping("/register")
    public Mono<Map<String, Object>> register(@RequestBody RegisterRequest req) {
        return keycloak.registerUser(req.username(), req.email(), req.password(),
                        req.firstName(), req.lastName())
                .then(keycloak.passwordLogin(req.username(), req.password()));
    }

    @PostMapping("/login")
    public Mono<Map<String, Object>> login(@RequestBody LoginRequest req) {
        return keycloak.passwordLogin(req.username(), req.password());
    }

    @PostMapping("/refresh")
    public Mono<Map<String, Object>> refresh(@RequestBody RefreshRequest req) {
        return keycloak.refreshToken(req.refreshToken());
    }
}
