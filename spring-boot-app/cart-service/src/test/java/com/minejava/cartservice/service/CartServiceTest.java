package com.minejava.cartservice.service;

import com.minejava.cartservice.model.Cart;
import com.minejava.cartservice.model.CartItem;
import com.minejava.cartservice.repository.CartRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private CartService cartService;

    @Test
    void addItemCombinesQuantityForTheSameSku() {
        CartItem existing = new CartItem(null, "SKU-1", "Phone", 1, 100.0);
        Cart cart = new Cart("user-1", new ArrayList<>(List.of(existing)));
        when(cartRepository.findById("user-1")).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cart updated = cartService.addItem("user-1", new CartItem(null, "sku-1", "Phone", 2, 100.0));

        assertEquals(1, updated.getItems().size());
        assertEquals(3, updated.getItems().get(0).getQuantity());
        verify(cartRepository).save(cart);
    }

    @Test
    void getCartCreatesAnEmptyCartWhenUserDoesNotHaveOne() {
        when(cartRepository.findById("user-2")).thenReturn(Optional.empty());

        Cart cart = cartService.getCart("user-2");

        assertEquals("user-2", cart.getUserId());
        assertEquals(List.of(), cart.getItems());
    }

    @Test
    void clearCartDeletesTheUsersCart() {
        cartService.clearCart("user-3");

        verify(cartRepository).deleteById("user-3");
    }
}
