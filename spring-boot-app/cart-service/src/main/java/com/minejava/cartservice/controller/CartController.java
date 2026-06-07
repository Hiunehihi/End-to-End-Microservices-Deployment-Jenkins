package com.minejava.cartservice.controller;

import com.minejava.cartservice.model.Cart;
import com.minejava.cartservice.model.CartItem;
import com.minejava.cartservice.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Slf4j
public class CartController {
    private final CartService cartService;

    @GetMapping
    public Cart getMyCart(@RequestHeader(value = "X-User-Id", required = false) String userId) {
        return cartService.getCart(requireUser(userId));
    }

    @PostMapping("/add")
    public Cart addItem(@RequestHeader(value = "X-User-Id", required = false) String userId,
                        @RequestBody CartItem item) {
        String uid = requireUser(userId);
        log.info("Adding item to cart for user {} sku {}", uid, item.getSkuCode());
        return cartService.addItem(uid, item);
    }

    @DeleteMapping
    public void clearMyCart(@RequestHeader(value = "X-User-Id", required = false) String userId) {
        cartService.clearCart(requireUser(userId));
    }

    private String requireUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing user context");
        }
        return userId;
    }
}
