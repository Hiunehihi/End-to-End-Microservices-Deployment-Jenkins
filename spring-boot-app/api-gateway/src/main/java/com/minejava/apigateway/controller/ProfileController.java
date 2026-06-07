package com.minejava.apigateway.controller;

import com.minejava.apigateway.client.KeycloakAdminClient;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final KeycloakAdminClient keycloak;

    public ProfileController(KeycloakAdminClient keycloak) {
        this.keycloak = keycloak;
    }

    public record ProfileUpdate(String firstName, String lastName, String email,
                                String address, String phone) {}

    @GetMapping("/me")
    public Mono<Map<String, Object>> me(@AuthenticationPrincipal Jwt jwt) {
        return keycloak.getUserById(jwt.getSubject());
    }

    @PutMapping("/me")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> update(@AuthenticationPrincipal Jwt jwt,
                                            @RequestBody ProfileUpdate body) {
        String userId = jwt.getSubject();
        return keycloak.getUserById(userId).flatMap(current -> {
            Map<String, Object> payload = new HashMap<>(current);
            if (body.firstName() != null) payload.put("firstName", body.firstName());
            if (body.lastName() != null) payload.put("lastName", body.lastName());
            if (body.email() != null) payload.put("email", body.email());

            Map<String, Object> rawAttrs = (Map<String, Object>) payload.getOrDefault("attributes", new HashMap<>());
            Map<String, Object> attributes = new HashMap<>(rawAttrs);
            if (body.address() != null) attributes.put("address", List.of(body.address()));
            if (body.phone() != null) attributes.put("phone", List.of(body.phone()));
            payload.put("attributes", attributes);

            return keycloak.updateUser(userId, payload).then(keycloak.getUserById(userId));
        });
    }
}
