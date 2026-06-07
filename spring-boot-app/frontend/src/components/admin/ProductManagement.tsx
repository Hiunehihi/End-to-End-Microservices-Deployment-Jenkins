import React, { useEffect, useState } from 'react';
import { Loader2, PackagePlus, Save, Trash2, X } from 'lucide-react';
import { productService } from '../../services/api';
import { Product } from '../../types';

const empty: Product = {
    skuCode: '',
    name: '',
    description: '',
    price: 0,
    imageUrl: '',
    quantity: 0
};

const formatVnd = (value: number) =>
    new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value || 0);

const ProductManagement: React.FC = () => {
    const [products, setProducts] = useState<Product[]>([]);
    const [editing, setEditing] = useState<Product | null>(null);
    const [form, setForm] = useState<Product>(empty);
    const [loading, setLoading] = useState(false);
    const [fetching, setFetching] = useState(true);

    const load = async () => {
        setFetching(true);
        try {
            const res = await productService.getAllProducts();
            setProducts(res.data);
        } finally {
            setFetching(false);
        }
    };

    useEffect(() => { load(); }, []);

    const startEdit = (p: Product) => {
        setEditing(p);
        setForm({ ...p, quantity: p.quantity ?? 0 });
    };

    const cancelEdit = () => {
        setEditing(null);
        setForm(empty);
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        try {
            if (editing && editing.id) {
                await productService.updateProduct(editing.id, form);
            } else {
                await productService.createProduct(form);
            }
            cancelEdit();
            await load();
        } catch (err) {
            alert('Lưu sản phẩm thất bại.');
        } finally {
            setLoading(false);
        }
    };

    const handleDelete = async (id: string) => {
        if (!window.confirm('Xóa sản phẩm này?')) return;
        await productService.deleteProduct(id);
        await load();
    };

    return (
        <div className="admin-page-content">
            <h2>Quản lý sản phẩm</h2>

            <div className="admin-content">
                <div className="form-section">
                    <h3>{editing ? 'Cập nhật sản phẩm' : 'Thêm sản phẩm'}</h3>
                    <form onSubmit={handleSubmit} className="admin-form">
                        <div className="form-grid">
                            <div className="form-group">
                                <label>Tên sản phẩm</label>
                                <input required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
                            </div>
                            <div className="form-group">
                                <label>Mã SKU</label>
                                <input required value={form.skuCode} onChange={(e) => setForm({ ...form, skuCode: e.target.value })} />
                            </div>
                        </div>
                        <div className="form-grid">
                            <div className="form-group">
                                <label>Giá (VND)</label>
                                <input type="number" min={0} required value={form.price || ''}
                                    onChange={(e) => setForm({ ...form, price: e.target.value === '' ? 0 : Number(e.target.value) })} />
                            </div>
                            <div className="form-group">
                                <label>Tồn kho</label>
                                <input type="number" min={0} value={form.quantity || ''}
                                    onChange={(e) => setForm({ ...form, quantity: e.target.value === '' ? 0 : Number(e.target.value) })} />
                            </div>
                        </div>
                        <div className="form-group">
                            <label>Ảnh (URL)</label>
                            <input value={form.imageUrl || ''} onChange={(e) => setForm({ ...form, imageUrl: e.target.value })} />
                        </div>
                        <div className="form-group">
                            <label>Mô tả</label>
                            <textarea required rows={3} value={form.description}
                                onChange={(e) => setForm({ ...form, description: e.target.value })} />
                        </div>
                        <div style={{ display: 'flex', gap: '8px' }}>
                            <button type="submit" className="btn-submit-admin" disabled={loading}>
                                {loading ? <Loader2 className="spin" size={20} /> : editing ? <Save size={20} /> : <PackagePlus size={20} />}
                                {editing ? 'Cập nhật' : 'Tạo mới'}
                            </button>
                            {editing && (
                                <button type="button" className="btn-icon-text" onClick={cancelEdit}>
                                    <X size={16} /> Hủy
                                </button>
                            )}
                        </div>
                    </form>
                </div>

                <div className="list-section">
                    <h3>Danh sách ({products.length})</h3>
                    {fetching && <p>Đang tải...</p>}
                    <div className="admin-product-list">
                        {products.map((p) => (
                            <div key={p.id || p.skuCode} className="admin-product-item">
                                <img src={p.imageUrl || '/assets/products/product-placeholder.svg'} alt={p.name} />
                                <div className="item-details">
                                    <p className="name">{p.name}</p>
                                    <p className="sku">{p.skuCode}</p>
                                    <p className="price">{formatVnd(p.price)} · Còn {p.quantity || 0}</p>
                                </div>
                                <div style={{ display: 'flex', gap: '6px' }}>
                                    <button className="btn-icon-text" onClick={() => startEdit(p)}>Sửa</button>
                                    {p.id && (
                                        <button className="btn-delete" onClick={() => handleDelete(p.id!)}>
                                            <Trash2 size={18} />
                                        </button>
                                    )}
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default ProductManagement;
