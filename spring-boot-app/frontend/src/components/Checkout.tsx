import React, { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { paymentService, cartService } from '../services/api';
import { CheckCircle, CreditCard, Landmark, Loader2, ArrowLeft } from 'lucide-react';
import { useCart } from '../context/CartContext';

const Checkout: React.FC = () => {
    const { refreshCartCount } = useCart();
    const location = useLocation();
    const navigate = useNavigate();
    const { orderId, totalAmount, items } = location.state || {};
    const [method, setMethod] = useState<'VNPAY' | 'BANK' | null>(null);
    const [loading, setLoading] = useState(false);
    const formatVnd = (value: number) => new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND'
    }).format(value);

    if (!orderId) {
        return <div className="loader">Không tìm thấy thông tin đơn hàng.</div>;
    }

    const handleVNPay = async () => {
        setLoading(true);
        try {
            const paymentRequest = {
                orderId: orderId,
                amount: totalAmount,
                orderInfo: `Thanh toan don hang ${orderId}`,
                items: items.map((i: any) => ({ skuCode: i.skuCode, quantity: i.quantity, price: i.price }))
            };
            const response = await paymentService.createPayment(paymentRequest);
            
            // Xóa giỏ hàng trước khi redirect
            await cartService.clearCart();
            refreshCartCount();
            
            window.location.href = response.data;
        } catch (error: any) {
            alert(error?.response?.data?.message || "Lỗi khi kết nối VNPay.");
            setLoading(false);
        }
    };

    const handleManualConfirm = async () => {
        setLoading(true);
        try {
            const paymentRequest = {
                orderId: orderId,
                amount: totalAmount,
                orderInfo: "Manual Bank Transfer",
                items: items.map((i: any) => ({ skuCode: i.skuCode, quantity: i.quantity, price: i.price }))
            };
            await paymentService.manualConfirm(paymentRequest);
            
            // Xóa giỏ hàng sau khi xác nhận thành công
            await cartService.clearCart();
            refreshCartCount();
            
            navigate('/payment-result?vnp_ResponseCode=00');
        } catch (error: any) {
            console.error("Confirm error:", error);
            alert(error?.response?.data?.message || "Lỗi xác nhận thanh toán.");
            setLoading(false);
        }
    };

    return (
        <div className="checkout-page">
            <div className="checkout-card">
                <button className="btn-icon-text" onClick={() => navigate('/cart')}>
                    <ArrowLeft size={18} /> Quay lại giỏ hàng
                </button>
                <h2 className="section-title">Chọn Phương Thức Thanh Toán</h2>
                <div className="order-summary-small">
                    <p>Mã đơn hàng: <strong>{orderId}</strong></p>
                    <p>Tổng tiền: <strong className="price">{formatVnd(totalAmount)}</strong></p>
                </div>

                <div className="payment-methods">
                    <div 
                        className={`method-item ${method === 'VNPAY' ? 'active' : ''}`}
                        onClick={() => setMethod('VNPAY')}
                    >
                        <CreditCard size={32} />
                        <div>
                            <h4>Ví VNPay / Thẻ ATM</h4>
                            <p>Thanh toán qua cổng VNPay (Sandbox)</p>
                        </div>
                    </div>

                    <div 
                        className={`method-item ${method === 'BANK' ? 'active' : ''}`}
                        onClick={() => setMethod('BANK')}
                    >
                        <Landmark size={32} />
                        <div>
                            <h4>Chuyển khoản VietQR</h4>
                            <p>Quét mã QR để thanh toán nhanh</p>
                        </div>
                    </div>
                </div>

                {method === 'BANK' && (
                    <div className="qr-section">
                        <h4>Quét mã VietQR để thanh toán</h4>
                        <div className="qr-code">
                            <img
                                src={`https://img.vietqr.io/image/970422-123456789-compact2.jpg?amount=${totalAmount}&addInfo=THANH TOAN ${orderId}`}
                                alt="VietQR"
                            />
                        </div>
                        <p className="qr-hint">Nội dung: <strong>THANH TOAN {orderId}</strong></p>
                        <button className="btn-submit-admin" onClick={handleManualConfirm} disabled={loading}>
                            {loading ? <Loader2 className="spin" size={20} /> : <CheckCircle size={20} />}
                            Tôi đã chuyển khoản thành công
                        </button>
                    </div>
                )}

                {method === 'VNPAY' && (
                    <div className="vnpay-action">
                        <button className="btn-submit-admin" onClick={handleVNPay} disabled={loading}>
                            {loading ? <Loader2 className="spin" size={20} /> : "Tiến hành tới VNPay"}
                        </button>
                    </div>
                )}
            </div>
        </div>
    );
};

export default Checkout;
