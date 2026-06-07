import React, { useEffect, useMemo, useState } from 'react';
import { productService, orderService } from '../services/api';
import { AdminOrder, Product } from '../types';
import { ArrowLeft, BarChart3, Boxes, CheckCircle2, Clock3, Loader2, PackagePlus, Save, Trash2, XCircle } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

const emptyProduct: Product = {
    skuCode: '',
    name: '',
    description: '',
    price: 0,
    imageUrl: '',
    quantity: 0
};

const AddProduct: React.FC = () => {
    const navigate = useNavigate();
    const [loading, setLoading] = useState(false);
    const [activeTab, setActiveTab] = useState<'dashboard' | 'products'>('dashboard');
    const [products, setProducts] = useState<Product[]>([]);
    const [orders, setOrders] = useState<AdminOrder[]>([]);
    const [product, setProduct] = useState<Product>(emptyProduct);

    const formatVnd = (value: number) => new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND'
    }).format(value || 0);

    useEffect(() => {
        fetchDashboardData();
    }, []);

    const fetchDashboardData = async () => {
        try {
            const [productResponse, orderResponse] = await Promise.all([
                productService.getAllProducts(),
                orderService.getAdminOrders()
            ]);
            setProducts(productResponse.data);
            setOrders(orderResponse.data);
        } catch (error) {
            console.error("Error fetching admin data:", error);
        }
    };

    const stats = useMemo(() => {
        const completed = orders.filter(order => order.status === 'COMPLETED');
        const pending = orders.filter(order => order.status === 'PENDING');
        const failed = orders.filter(order => order.status === 'FAILED');
        const revenue = completed.reduce((sum, order) => sum + (Number(order.totalAmount) || 0), 0);
        const lowStock = products.filter(item => (item.quantity || 0) > 0 && (item.quantity || 0) <= 5).length;
        return { completed, pending, failed, revenue, lowStock };
    }, [orders, products]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        try {
            await productService.createProduct(product);
            alert("Sản phẩm đã được tạo thành công!");
            setProduct(emptyProduct);
            fetchDashboardData();
        } catch (error) {
            console.error("Error creating product:", error);
            alert("Lỗi khi tạo sản phẩm.");
        } finally {
            setLoading(false);
        }
    };

    const handleDelete = async (id: string) => {
        if (!window.confirm("Bạn có chắc chắn muốn xóa sản phẩm này?")) return;
        try {
            await productService.deleteProduct(id);
            fetchDashboardData();
        } catch (error) {
            alert("Lỗi khi xóa sản phẩm.");
        }
    };

    const statusClass = (status: string) => status.toLowerCase();
    const statusLabel = (status: string) => {
        if (status === 'COMPLETED') return 'Đã thanh toán';
        if (status === 'PENDING') return 'Chờ thanh toán';
        if (status === 'FAILED') return 'Thất bại';
        return status;
    };

    return (
        <div className="admin-page">
            <div className="admin-header-panel">
                <button className="btn-icon-text" onClick={() => navigate('/')}>
                    <ArrowLeft size={18} /> Quay lại cửa hàng
                </button>
                <div>
                    <h2>Quản trị V-Shop</h2>
                    <p>Theo dõi doanh thu, đơn hàng và kho sản phẩm demo.</p>
                </div>
            </div>

            <div className="admin-tabs">
                <button className={activeTab === 'dashboard' ? 'active' : ''} onClick={() => setActiveTab('dashboard')}>
                    <BarChart3 size={18} /> Tổng quan
                </button>
                <button className={activeTab === 'products' ? 'active' : ''} onClick={() => setActiveTab('products')}>
                    <PackagePlus size={18} /> Sản phẩm
                </button>
            </div>

            {activeTab === 'dashboard' ? (
                <div className="admin-dashboard">
                    <div className="stats-grid">
                        <div className="stat-card revenue">
                            <span>Doanh thu</span>
                            <strong>{formatVnd(stats.revenue)}</strong>
                        </div>
                        <div className="stat-card">
                            <CheckCircle2 size={22} />
                            <span>Đã thanh toán</span>
                            <strong>{stats.completed.length}</strong>
                        </div>
                        <div className="stat-card">
                            <Clock3 size={22} />
                            <span>Chờ thanh toán</span>
                            <strong>{stats.pending.length}</strong>
                        </div>
                        <div className="stat-card">
                            <XCircle size={22} />
                            <span>Thất bại</span>
                            <strong>{stats.failed.length}</strong>
                        </div>
                        <div className="stat-card">
                            <Boxes size={22} />
                            <span>Sản phẩm</span>
                            <strong>{products.length}</strong>
                        </div>
                    </div>

                    <div className="admin-section">
                        <div className="section-heading">
                            <h3>Đơn hàng gần đây</h3>
                            <span>{orders.length} đơn hàng</span>
                        </div>
                        <div className="order-table">
                            {orders.length === 0 ? (
                                <p className="empty-state">Chưa có đơn hàng nào.</p>
                            ) : orders.map(order => (
                                <div className="order-row" key={order.orderNumber}>
                                    <div>
                                        <strong>{order.orderNumber}</strong>
                                        <p>{order.customerName} · {order.customerEmail}</p>
                                    </div>
                                    <span className={`order-status ${statusClass(order.status)}`}>{statusLabel(order.status)}</span>
                                    <span>{order.itemCount} sản phẩm</span>
                                    <strong>{formatVnd(Number(order.totalAmount))}</strong>
                                </div>
                            ))}
                        </div>
                    </div>

                    <div className="admin-section">
                        <div className="section-heading">
                            <h3>Cảnh báo kho</h3>
                            <span>{stats.lowStock} sản phẩm sắp hết</span>
                        </div>
                        <div className="stock-list">
                            {products.filter(item => (item.quantity || 0) <= 5).map(item => (
                                <div className="stock-row" key={item.skuCode}>
                                    <span>{item.name}</span>
                                    <strong>{item.quantity || 0}</strong>
                                </div>
                            ))}
                        </div>
                    </div>
                </div>
            ) : (
                <div className="admin-content">
                    <div className="form-section">
                        <h3>Thêm sản phẩm mới</h3>
                        <form onSubmit={handleSubmit} className="admin-form">
                            <div className="form-grid">
                                <div className="form-group">
                                    <label>Tên sản phẩm</label>
                                    <input required value={product.name} onChange={(e) => setProduct({...product, name: e.target.value})} placeholder="Ví dụ: iPhone 15 Pro" />
                                </div>
                                <div className="form-group">
                                    <label>Mã SKU</label>
                                    <input required value={product.skuCode} onChange={(e) => setProduct({...product, skuCode: e.target.value})} placeholder="IPHONE-15-PRO" />
                                </div>
                            </div>

                            <div className="form-grid">
                                <div className="form-group">
                                    <label>Giá bán (VND)</label>
                                    <input type="number" required min="0" step="1000" value={product.price || ''} onChange={(e) => setProduct({...product, price: e.target.value === '' ? 0 : Number(e.target.value)})} placeholder="28990000" />
                                </div>
                                <div className="form-group">
                                    <label>Số lượng kho</label>
                                    <input type="number" required min="0" value={product.quantity || ''} onChange={(e) => setProduct({...product, quantity: e.target.value === '' ? 0 : Number(e.target.value)})} placeholder="100" />
                                </div>
                            </div>

                            <div className="form-group">
                                <label>Ảnh local</label>
                                <input value={product.imageUrl} onChange={(e) => setProduct({...product, imageUrl: e.target.value})} placeholder="/assets/products/iphone-15-pro.svg" />
                            </div>

                            <div className="form-group">
                                <label>Mô tả</label>
                                <textarea required rows={3} value={product.description} onChange={(e) => setProduct({...product, description: e.target.value})} placeholder="Nhập mô tả sản phẩm..." />
                            </div>

                            <button type="submit" className="btn-submit-admin" disabled={loading}>
                                {loading ? <Loader2 className="spin" size={20} /> : <Save size={20} />}
                                Lưu sản phẩm
                            </button>
                        </form>
                    </div>

                    <div className="list-section">
                        <h3>Danh sách sản phẩm ({products.length})</h3>
                        <div className="admin-product-list">
                            {products.map((p) => (
                                <div key={p.id || p.skuCode} className="admin-product-item">
                                    <img src={p.imageUrl || '/assets/products/product-placeholder.svg'} alt={p.name} />
                                    <div className="item-details">
                                        <p className="name">{p.name}</p>
                                        <p className="sku">{p.skuCode}</p>
                                        <p className="price">{formatVnd(p.price)} · Còn {p.quantity || 0}</p>
                                    </div>
                                    {p.id && (
                                        <button className="btn-delete" onClick={() => handleDelete(p.id!)}>
                                            <Trash2 size={18} />
                                        </button>
                                    )}
                                </div>
                            ))}
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default AddProduct;
