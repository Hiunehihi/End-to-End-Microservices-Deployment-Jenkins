import React, { useEffect, useState } from 'react';
import { adminUserService, orderService } from '../../services/api';
import { OrderStats } from '../../types';
import { Loader2 } from 'lucide-react';

const formatVnd = (value: number) =>
    new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value || 0);

const Dashboard: React.FC = () => {
    const [stats, setStats] = useState<OrderStats | null>(null);
    const [userCount, setUserCount] = useState<number | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        (async () => {
            try {
                const [s, u] = await Promise.all([
                    orderService.getStats(),
                    adminUserService.list().catch(() => ({ data: [] }))
                ]);
                setStats(s.data);
                setUserCount(Array.isArray(u.data) ? u.data.length : null);
            } finally {
                setLoading(false);
            }
        })();
    }, []);

    if (loading) return <div className="loader"><Loader2 className="spin" /> Đang tải...</div>;
    if (!stats) return <p>Không tải được dữ liệu thống kê.</p>;

    const byStatus = stats.ordersByStatus || {};

    return (
        <div className="admin-page-content">
            <h2>Tổng quan</h2>
            <div className="stats-grid">
                <div className="stat-card revenue">
                    <span>Doanh thu</span>
                    <strong>{formatVnd(Number(stats.totalRevenue))}</strong>
                </div>
                <div className="stat-card">
                    <span>Tổng đơn</span>
                    <strong>{stats.totalOrders}</strong>
                </div>
                <div className="stat-card">
                    <span>Đã thanh toán</span>
                    <strong>{byStatus['COMPLETED'] || 0}</strong>
                </div>
                <div className="stat-card">
                    <span>Chờ thanh toán</span>
                    <strong>{byStatus['PENDING'] || 0}</strong>
                </div>
                <div className="stat-card">
                    <span>Đã giao</span>
                    <strong>{byStatus['DELIVERED'] || 0}</strong>
                </div>
                <div className="stat-card">
                    <span>Người dùng</span>
                    <strong>{userCount ?? '—'}</strong>
                </div>
            </div>

            <div className="admin-section">
                <h3>Top sản phẩm bán chạy</h3>
                <div className="order-table">
                    {stats.topProducts.length === 0 ? (
                        <p className="empty-state">Chưa có dữ liệu.</p>
                    ) : stats.topProducts.map((p) => (
                        <div className="order-row" key={p.skuCode}>
                            <strong>{p.skuCode}</strong>
                            <span>{p.totalQuantity} sản phẩm</span>
                            <strong>{formatVnd(Number(p.totalRevenue))}</strong>
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
};

export default Dashboard;
