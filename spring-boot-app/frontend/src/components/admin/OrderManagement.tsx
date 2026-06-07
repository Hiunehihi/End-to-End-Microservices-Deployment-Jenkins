import React, { useEffect, useMemo, useState } from 'react';
import { Loader2 } from 'lucide-react';
import { orderService } from '../../services/api';
import { AdminOrder } from '../../types';

const STATUSES = ['PENDING', 'COMPLETED', 'FAILED', 'SHIPPED', 'DELIVERED', 'CANCELLED'];

const formatVnd = (value: number) =>
    new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value || 0);

const OrderManagement: React.FC = () => {
    const [orders, setOrders] = useState<AdminOrder[]>([]);
    const [filter, setFilter] = useState<string>('ALL');
    const [loading, setLoading] = useState(true);
    const [updating, setUpdating] = useState<string | null>(null);

    const load = async () => {
        setLoading(true);
        try {
            const res = await orderService.getAdminOrders();
            setOrders(res.data);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { load(); }, []);

    const filtered = useMemo(() => filter === 'ALL'
        ? orders
        : orders.filter(o => o.status === filter), [orders, filter]);

    const updateStatus = async (orderNumber: string, status: string) => {
        setUpdating(orderNumber);
        try {
            await orderService.updateOrderStatus(orderNumber, status);
            await load();
        } finally {
            setUpdating(null);
        }
    };

    if (loading) return <div className="loader"><Loader2 className="spin" /> Đang tải đơn hàng...</div>;

    return (
        <div className="admin-page-content">
            <h2>Quản lý đơn hàng</h2>

            <div style={{ display: 'flex', gap: 8, margin: '16px 0', flexWrap: 'wrap' }}>
                {['ALL', ...STATUSES].map(s => (
                    <button key={s}
                        className={`btn-icon-text ${filter === s ? 'active' : ''}`}
                        onClick={() => setFilter(s)}>
                        {s}
                    </button>
                ))}
            </div>

            <div className="order-table">
                {filtered.length === 0 ? (
                    <p className="empty-state">Không có đơn nào.</p>
                ) : filtered.map(o => (
                    <div className="order-row" key={o.orderNumber}>
                        <div>
                            <strong>{o.orderNumber}</strong>
                            <p>{o.customerName} · {o.customerEmail}</p>
                            {o.createdAt && <p style={{ fontSize: 12, opacity: 0.7 }}>{new Date(o.createdAt).toLocaleString('vi-VN')}</p>}
                        </div>
                        <span>{o.itemCount} sp</span>
                        <strong>{formatVnd(Number(o.totalAmount))}</strong>
                        <select
                            value={o.status}
                            disabled={updating === o.orderNumber}
                            onChange={(e) => updateStatus(o.orderNumber, e.target.value)}
                        >
                            {STATUSES.map(s => <option key={s} value={s}>{s}</option>)}
                        </select>
                    </div>
                ))}
            </div>
        </div>
    );
};

export default OrderManagement;
