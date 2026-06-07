package com.minejava.cartservice.service;

import com.minejava.cartservice.model.Cart;
import com.minejava.cartservice.model.CartItem;
import com.minejava.cartservice.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartRepository cartRepository;

    public Cart getCart(String userId) {
        return cartRepository.findById(userId).orElse(new Cart(userId, new ArrayList<>()));
    }

    public Cart addItem(String userId, CartItem item) {
        Cart cart = getCart(userId);
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(i -> sameItem(i, item))
                .findFirst();

        if (existingItem.isPresent()) {
            existingItem.get().setQuantity(existingItem.get().getQuantity() + item.getQuantity());
        } else {
            cart.getItems().add(item);
        }
        return cartRepository.save(cart);
    }

    private boolean sameItem(CartItem current, CartItem next) {
        if (current.getProductId() != null && next.getProductId() != null) {
            return current.getProductId().equals(next.getProductId());
        }
        if (current.getSkuCode() != null && next.getSkuCode() != null) {
            return current.getSkuCode().equalsIgnoreCase(next.getSkuCode());
        }
        return current.getName() != null && current.getName().equals(next.getName());
    }

    public void clearCart(String userId) {
        cartRepository.deleteById(userId);
    }
}
