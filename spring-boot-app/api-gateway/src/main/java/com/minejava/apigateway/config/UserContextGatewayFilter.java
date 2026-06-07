package com.minejava.apigateway.config;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
public class UserContextGatewayFilter implements GlobalFilter, Ordered {

    @Override
    @SuppressWarnings("unchecked")
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getPrincipal()
                .cast(JwtAuthenticationToken.class)
                .flatMap(auth -> {
                    Jwt jwt = auth.getToken();
                    String userId = jwt.getSubject();
                    String username = jwt.getClaimAsString("preferred_username");
                    String email = jwt.getClaimAsString("email");
                    Map<String, Object> realmAccess = jwt.getClaim("realm_access");
                    String roles = "";
                    if (realmAccess != null && realmAccess.get("roles") instanceof List<?> list) {
                        roles = String.join(",", list.stream().map(Object::toString).toList());
                    }

                    ServerHttpRequest mutated = exchange.getRequest().mutate()
                            .header("X-User-Id", userId == null ? "" : userId)
                            .header("X-User-Username", username == null ? "" : username)
                            .header("X-User-Email", email == null ? "" : email)
                            .header("X-User-Roles", roles)
                            .build();
                    return chain.filter(exchange.mutate().request(mutated).build());
                })
                .switchIfEmpty(chain.filter(exchange));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
