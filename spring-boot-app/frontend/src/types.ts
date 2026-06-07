export interface Product {
    id?: string;
    skuCode: string;
    name: string;
    description: string;
    price: number;
    imageUrl?: string;
    quantity?: number;
}

export interface CartItem {
    productId: string;
    skuCode: string;
    name: string;
    quantity: number;
    price: number;
}

export interface Cart {
    userId: string;
    items: CartItem[];
}

export interface OrderLineItemsDto {
    id?: number;
    skuCode: string;
    price: number;
    quantity: number;
}

export interface OrderRequest {
    orderLineItemsDtoList: OrderLineItemsDto[];
}

export type OrderStatus =
    | 'PENDING'
    | 'COMPLETED'
    | 'FAILED'
    | 'SHIPPED'
    | 'DELIVERED'
    | 'CANCELLED';

export interface AdminOrder {
    orderNumber: string;
    status: OrderStatus | string;
    totalAmount: number;
    itemCount: number;
    customerName: string;
    customerEmail: string;
    userId?: string;
    createdAt?: string;
    items: Array<{
        skuCode: string;
        quantity: number;
        price: number;
    }>;
}

export interface OrderStats {
    totalOrders: number;
    totalRevenue: number;
    ordersByStatus: Record<string, number>;
    topProducts: Array<{ skuCode: string; totalQuantity: number; totalRevenue: number }>;
}

export interface InventoryItem {
    skuCode: string;
    quantity: number;
    isInStock: boolean;
}

export interface KeycloakUser {
    id: string;
    username: string;
    email?: string;
    firstName?: string;
    lastName?: string;
    enabled: boolean;
    attributes?: Record<string, string[]>;
}

export interface AuthTokenResponse {
    access_token: string;
    refresh_token: string;
    expires_in: number;
    refresh_expires_in: number;
    token_type: string;
    'not-before-policy'?: number;
    session_state?: string;
    scope?: string;
}
