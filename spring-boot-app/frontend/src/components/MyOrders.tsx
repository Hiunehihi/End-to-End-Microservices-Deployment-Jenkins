import React, { useEffect, useState } from 'react';
import { Loader2 } from 'lucide-react';
import { orderService } from '../services/api';
import { AdminOrder } from '../types';

const formatVnd = (value: number) =>
    new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value || 0);

const statusLabel = (status: string) => {
    switch (status) {
        case 'PENDING': return 'Chờ thanh toán';
        case 'COMPLETED': return 'Đã thanh toán';
        case 'FAILED': return 'Thất bại';
        case 'SHIPPED': return 'Đang giao';
        case 'DELIVERED': return 'Đã giao';
        case 'CANCELLED': return 'Đã hủy';
        default: return status;
    }
};

const MyOrders: React.FC = () => {
    const [orders, setOrders] = useState<AdminOrder[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        (async () => {
            try {
                const res = await orderService.getMyOrders();
                setOrders(res.data);
            } finally {
                setLoading(false);
            }
        })();
    }, []);

    if (loading) return <div className="loader"><Loader2 className="spin" /> Đang tải đơn...</div>;

    return (
        <div className="cart-container">
            <h2 className="section-title">Đơn hàng của tôi</h2>
            {orders.length === 0 ? (
                <p className="empty-state">Bạn chưa có đơn hàng nào.</p>
            ) : (
                <div className="order-table">
                    {orders.map(o => (
                        <div className="order-row" key={o.orderNumber}>
                            <div>
                                <strong>{o.orderNumber}</strong>
                                {o.createdAt && <p style={{ fontSize: 12, opacity: 0.7 }}>{new Date(o.createdAt).toLocaleString('vi-VN')}</p>}
                            </div>
                            <span className={`order-status ${o.status.toLowerCase()}`}>{statusLabel(o.status)}</span>
                            <span>{o.itemCount} sản phẩm</span>
                            <strong>{formatVnd(Number(o.totalAmount))}</strong>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

export default MyOrders;
