import React, { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { cartService, productService } from '../services/api';
import { useAuth } from './AuthContext';

interface CartContextType {
    cartCount: number;
    refreshCartCount: () => void;
}

const CartContext = createContext<CartContextType | undefined>(undefined);

export const CartProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const { isAuthenticated } = useAuth();
    const [cartCount, setCartCount] = useState(0);

    const refreshCartCount = useCallback(async () => {
        if (!isAuthenticated) {
            setCartCount(0);
            return;
        }
        try {
            const cartRes = await cartService.getCart();
            const cartItems = cartRes.data.items || [];

            const productRes = await productService.getAllProducts();
            const validProductSkus = productRes.data.map(p => p.skuCode);

            const validItems = cartItems.filter((item: any) => validProductSkus.includes(item.skuCode));
            const count = validItems.reduce((acc: number, item: any) => acc + item.quantity, 0);
            setCartCount(count);
        } catch (error) {
            console.error('Failed to fetch cart count:', error);
        }
    }, [isAuthenticated]);

    useEffect(() => {
        refreshCartCount();
    }, [refreshCartCount]);

    return (
        <CartContext.Provider value={{ cartCount, refreshCartCount }}>
            {children}
        </CartContext.Provider>
    );
};

export const useCart = () => {
    const context = useContext(CartContext);
    if (!context) throw new Error('useCart must be used within a CartProvider');
    return context;
};
