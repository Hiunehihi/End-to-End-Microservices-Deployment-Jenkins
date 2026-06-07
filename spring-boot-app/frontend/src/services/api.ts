import axios from 'axios';
import {
    AdminOrder,
    InventoryItem,
    KeycloakUser,
    OrderRequest,
    OrderStats,
    Product
} from '../types';

const getApiBaseUrl = () => {
    const configured = process.env.REACT_APP_API_BASE_URL;
    if (configured) return configured;
    if (typeof window !== 'undefined' && window.location.hostname !== 'localhost') {
        return `${window.location.protocol}//${window.location.hostname}:30085`;
    }
    return 'http://localhost:30085';
};

const API_BASE_URL = getApiBaseUrl();

const api = axios.create({ baseURL: API_BASE_URL });

api.interceptors.request.use((config) => {
    const token = localStorage.getItem('token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

api.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error?.response?.status === 401) {
            const isAuthEndpoint = error.config?.url?.includes('/api/auth/');
            if (!isAuthEndpoint) {
                localStorage.removeItem('token');
                localStorage.removeItem('refreshToken');
                window.dispatchEvent(new Event('auth:logout'));
                if (window.location.pathname !== '/login') {
                    window.location.href = '/login';
                }
            }
        }
        return Promise.reject(error);
    }
);

export const productService = {
    getAllProducts: () => api.get<Product[]>('/api/product'),
    createProduct: (product: Product) => api.post('/api/product', product),
    updateProduct: (id: string, product: Product) => api.put<Product>(`/api/product/${id}`, product),
    deleteProduct: (id: string) => api.delete(`/api/product/${id}`)
};

export const orderService = {
    placeOrder: (order: OrderRequest) => api.post<string>('/api/order', order),
    getMyOrders: () => api.get<AdminOrder[]>('/api/order/me'),
    getAdminOrders: () => api.get<AdminOrder[]>('/api/order/admin/all'),
    updateOrderStatus: (orderNumber: string, status: string) =>
        api.patch<AdminOrder>(`/api/order/admin/${orderNumber}/status`, { status }),
    getStats: () => api.get<OrderStats>('/api/order/admin/stats')
};

export const cartService = {
    getCart: () => api.get('/api/cart'),
    addToCart: (item: any) => api.post('/api/cart/add', item),
    clearCart: () => api.delete('/api/cart')
};

export const paymentService = {
    createPayment: (paymentRequest: { orderId: string; amount: number; orderInfo: string; items?: any[] }) =>
        api.post<string>('/api/payment/create', paymentRequest),
    manualConfirm: (paymentRequest: { orderId: string; amount: number; orderInfo: string; items?: any[] }) =>
        api.post('/api/payment/manual-confirm', paymentRequest)
};

export const inventoryService = {
    listAll: () => api.get<InventoryItem[]>('/api/inventory/admin/all'),
    updateQuantity: (skuCode: string, quantity: number) =>
        api.put<InventoryItem>(`/api/inventory/admin/${skuCode}`, { quantity })
};

export const profileService = {
    me: () => api.get<KeycloakUser>('/api/profile/me'),
    update: (payload: Partial<KeycloakUser> & { address?: string; phone?: string }) =>
        api.put<KeycloakUser>('/api/profile/me', payload)
};

export const adminUserService = {
    list: () => api.get<KeycloakUser[]>('/api/admin/users'),
    setEnabled: (id: string, enabled: boolean) =>
        api.put(`/api/admin/users/${id}/enabled`, { enabled }),
    resetPassword: (id: string, password: string) =>
        api.post(`/api/admin/users/${id}/reset-password`, { password }),
    grantAdmin: (id: string) => api.post(`/api/admin/users/${id}/grant-admin`)
};

export default api;
