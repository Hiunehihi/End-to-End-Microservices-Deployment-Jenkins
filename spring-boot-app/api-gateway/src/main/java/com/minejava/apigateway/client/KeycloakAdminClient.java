package com.minejava.apigateway.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
public class KeycloakAdminClient {

    private final WebClient webClient;
    private final String realm;
    private final String registrationClientId;
    private final String registrationClientSecret;
    private final String frontendClientId;

    public KeycloakAdminClient(
            @Value("${keycloak.base-url}") String baseUrl,
            @Value("${keycloak.realm}") String realm,
            @Value("${keycloak.registration-client-id}") String registrationClientId,
            @Value("${keycloak.registration-client-secret}") String registrationClientSecret,
            @Value("${keycloak.frontend-client-id}") String frontendClientId) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
        this.realm = realm;
        this.registrationClientId = registrationClientId;
        this.registrationClientSecret = registrationClientSecret;
        this.frontendClientId = frontendClientId;
    }

    public Mono<Map<String, Object>> passwordLogin(String username, String password) {
        return webClient.post()
                .uri("/realms/{realm}/protocol/openid-connect/token", realm)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "password")
                        .with("client_id", frontendClientId)
                        .with("username", username)
                        .with("password", password))
                .retrieve()
                .onStatus(s -> s.value() == 401, r -> Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials")))
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});
    }

    public Mono<Map<String, Object>> refreshToken(String refreshToken) {
        return webClient.post()
                .uri("/realms/{realm}/protocol/openid-connect/token", realm)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "refresh_token")
                        .with("client_id", frontendClientId)
                        .with("refresh_token", refreshToken))
                .retrieve()
                .onStatus(s -> s.value() == 400 || s.value() == 401,
                        r -> Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token")))
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});
    }

    private Mono<String> serviceAccountToken() {
        return webClient.post()
                .uri("/realms/{realm}/protocol/openid-connect/token", realm)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "client_credentials")
                        .with("client_id", registrationClientId)
                        .with("client_secret", registrationClientSecret))
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                .map(m -> (String) m.get("access_token"));
    }

    public Mono<Void> registerUser(String username, String email, String password,
                                   String firstName, String lastName) {
        return serviceAccountToken().flatMap(token ->
            webClient.post()
                .uri("/admin/realms/{realm}/users", realm)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "username", username,
                        "email", email,
                        "firstName", firstName == null ? "" : firstName,
                        "lastName", lastName == null ? "" : lastName,
                        "enabled", true,
                        "emailVerified", true,
                        "credentials", List.of(Map.of(
                                "type", "password",
                                "value", password,
                                "temporary", false))
                ))
                .retrieve()
                .onStatus(s -> s.value() == 409,
                        r -> Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Username or email already exists")))
                .toBodilessEntity()
                .then()
        );
    }

    public Mono<Map<String, Object>> getUserByUsername(String username) {
        return serviceAccountToken().flatMap(token ->
            webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/admin/realms/{realm}/users")
                        .queryParam("username", username)
                        .queryParam("exact", true)
                        .build(realm))
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .flatMap(list -> list.isEmpty() ? Mono.empty() : Mono.just(list.get(0)))
        );
    }

    public Mono<Map<String, Object>> getUserById(String userId) {
        return serviceAccountToken().flatMap(token ->
            webClient.get()
                .uri("/admin/realms/{realm}/users/{id}", realm, userId)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
        );
    }

    public Mono<Void> updateUser(String userId, Map<String, Object> userPayload) {
        return serviceAccountToken().flatMap(token ->
            webClient.put()
                .uri("/admin/realms/{realm}/users/{id}", realm, userId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userPayload)
                .retrieve()
                .toBodilessEntity()
                .then()
        );
    }

    public Mono<List<Map<String, Object>>> listUsers() {
        return serviceAccountToken().flatMap(token ->
            webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/admin/realms/{realm}/users")
                        .queryParam("max", 200)
                        .build(realm))
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<List<Map<String, Object>>>() {})
        );
    }

    public Mono<Void> setUserEnabled(String userId, boolean enabled) {
        return serviceAccountToken().flatMap(token ->
            webClient.put()
                .uri("/admin/realms/{realm}/users/{id}", realm, userId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("enabled", enabled))
                .retrieve()
                .toBodilessEntity()
                .then()
        );
    }

    @SuppressWarnings("unchecked")
    public Mono<Void> assignRealmRole(String userId, String roleName) {
        return serviceAccountToken().flatMap(token ->
            webClient.get()
                .uri("/admin/realms/{realm}/roles/{role}", realm, roleName)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                .flatMap(role -> webClient.post()
                    .uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", realm, userId)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(List.of(role))
                    .retrieve()
                    .toBodilessEntity()
                    .then()
                )
        );
    }

    public Mono<Void> resetPassword(String userId, String newPassword) {
        return serviceAccountToken().flatMap(token ->
            webClient.put()
                .uri("/admin/realms/{realm}/users/{id}/reset-password", realm, userId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "type", "password",
                        "value", newPassword,
                        "temporary", false))
                .retrieve()
                .toBodilessEntity()
                .then()
        );
    }
}
