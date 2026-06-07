import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { productService, cartService } from '../services/api';
import { Product } from '../types';
import { ShoppingBag, Loader2, ShoppingCart } from 'lucide-react';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';

const ProductList: React.FC = () => {
    const { refreshCartCount } = useCart();
    const { isAuthenticated } = useAuth();
    const navigate = useNavigate();
    const [products, setProducts] = useState<Product[]>([]);
    const [loading, setLoading] = useState(true);
    const [addingId, setAddingId] = useState<string | null>(null);
    const formatVnd = (value: number) => new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND'
    }).format(value);

    useEffect(() => {
        const fetchProducts = async () => {
            try {
                const response = await productService.getAllProducts();
                setProducts(response.data);
            } catch (error) {
                console.error("Error fetching products:", error);
            } finally {
                setLoading(false);
            }
        };

        fetchProducts();
    }, []);

    const handleAddToCart = async (product: Product) => {
        if (!isAuthenticated) {
            navigate('/login');
            return;
        }
        setAddingId(product.id || product.name);
        try {
            await cartService.addToCart({
                productId: product.id || product.name,
                skuCode: product.skuCode,
                name: product.name,
                price: product.price,
                quantity: 1
            });
            refreshCartCount();
            alert("Đã thêm vào giỏ hàng!");
        } catch (error) {
            console.error("Cart error:", error);
            alert("Không thể thêm vào giỏ hàng.");
        } finally {
            setAddingId(null);
        }
    };

    if (loading) return <div className="loader"><Loader2 className="spin" /> Đang tải sản phẩm...</div>;

    return (
        <div className="shop-section">
            <h2 className="section-title">Sản Phẩm Mới Nhất</h2>
            <div className="product-grid">
                {products.length === 0 ? (
                    <p className="no-products">Hiện chưa có sản phẩm nào. Vui lòng quay lại sau!</p>
                ) : (
                    products.map((product, index) => (
                        <div key={product.id || index} className="product-card">
                            <div className="product-image">
                                {product.imageUrl ? (
                                    <img
                                        src={product.imageUrl}
                                        alt={product.name}
                                        className="img-fluid"
                                        onError={(event) => {
                                            event.currentTarget.style.display = 'none';
                                        }}
                                    />
                                ) : (
                                    <div className="img-placeholder">
                                        <ShoppingBag size={48} opacity={0.2} />
                                    </div>
                                )}
                                <span className="tag">New</span>
                            </div>
                            <div className="product-info">
                                <h3 className="product-name">{product.name}</h3>
                                <p className="product-desc">{product.description}</p>
                                <div className="stock-info">
                                    {product.quantity! > 0 ? (
                                        <span className="stock-status in-stock">Còn lại: {product.quantity}</span>
                                    ) : (
                                        <span className="stock-status out-of-stock">Hết hàng</span>
                                    )}
                                </div>
                                <div className="product-footer">
                                    <span className="price">{formatVnd(product.price)}</span>
                                    <button 
                                        className="btn-buy" 
                                        onClick={() => handleAddToCart(product)}
                                        disabled={addingId === (product.id || product.name) || (product.quantity !== undefined && product.quantity <= 0)}
                                    >
                                        <ShoppingCart size={16} />
                                        {addingId === (product.id || product.name) ? <Loader2 className="spin" size={16} /> : 
                                         (product.quantity !== undefined && product.quantity <= 0) ? "Hết hàng" : "Thêm giỏ hàng"}
                                    </button>
                                </div>
                            </div>
                        </div>
                    ))
                )}
            </div>
        </div>
    );
};

export default ProductList;
