package com.minejava.apigateway.controller;

import com.minejava.apigateway.client.KeycloakAdminClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final KeycloakAdminClient keycloak;

    public AdminUserController(KeycloakAdminClient keycloak) {
        this.keycloak = keycloak;
    }

    public record EnabledRequest(boolean enabled) {}
    public record PasswordResetRequest(String password) {}

    @GetMapping
    public Flux<Map<String, Object>> list() {
        return keycloak.listUsers().flatMapMany(Flux::fromIterable);
    }

    @PutMapping("/{id}/enabled")
    public Mono<Void> setEnabled(@PathVariable String id, @RequestBody EnabledRequest body) {
        return keycloak.setUserEnabled(id, body.enabled());
    }

    @PostMapping("/{id}/reset-password")
    public Mono<Void> resetPassword(@PathVariable String id, @RequestBody PasswordResetRequest body) {
        return keycloak.resetPassword(id, body.password());
    }

    @PostMapping("/{id}/grant-admin")
    public Mono<Void> grantAdmin(@PathVariable String id) {
        return keycloak.assignRealmRole(id, "ADMIN");
    }
}
