import axios from 'axios';
import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { cartService, orderService, paymentService, productService } from './api';

jest.mock('axios', () => {
    const mockJest = require('@jest/globals').jest;
    return {
        create: mockJest.fn(() => ({
            get: mockJest.fn(),
            post: mockJest.fn(),
            put: mockJest.fn(),
            delete: mockJest.fn(),
            interceptors: {
                request: { use: mockJest.fn() },
                response: { use: mockJest.fn() }
            }
        }))
    };
});

const client = (axios.create as any).mock.results[0].value;

describe('frontend API services', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('uses the public product route to load the catalog', () => {
        productService.getAllProducts();

        expect(client.get).toHaveBeenCalledWith('/api/product');
    });

    it('posts orders and manual payments to their backend routes', () => {
        const order = { orderLineItemsDtoList: [] };
        const payment = { orderId: 'order-1', amount: 1000, orderInfo: 'payment' };

        orderService.placeOrder(order);
        paymentService.manualConfirm(payment);

        expect(client.post).toHaveBeenCalledWith('/api/order', order);
        expect(client.post).toHaveBeenCalledWith('/api/payment/manual-confirm', payment);
    });

    it('clears the authenticated cart through its delete endpoint', () => {
        cartService.clearCart();

        expect(client.delete).toHaveBeenCalledWith('/api/cart');
    });
});
