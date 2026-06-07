import React, { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { CheckCircle, XCircle, ArrowLeft } from 'lucide-react';

const PaymentResult: React.FC = () => {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const [status, setStatus] = useState<'SUCCESS' | 'FAILED' | 'PENDING'>('PENDING');

    useEffect(() => {
        const responseCode = searchParams.get('vnp_ResponseCode');
        if (responseCode === '00') {
            setStatus('SUCCESS');
        } else {
            setStatus('FAILED');
        }
    }, [searchParams]);

    return (
        <div className="payment-result-container">
            {status === 'SUCCESS' ? (
                <div className="result-card success">
                    <CheckCircle size={64} color="#10b981" />
                    <h2>Thanh Toán Thành Công!</h2>
                    <p>Cảm ơn bạn đã mua sắm tại V-Shop. Đơn hàng của bạn đang được xử lý.</p>
                </div>
            ) : status === 'FAILED' ? (
                <div className="result-card error">
                    <XCircle size={64} color="#ef4444" />
                    <h2>Thanh Toán Thất Bại</h2>
                    <p>Có lỗi xảy ra trong quá trình thanh toán. Vui lòng thử lại sau.</p>
                </div>
            ) : (
                <div className="loader">Đang kiểm tra kết quả...</div>
            )}
            <button className="btn-buy" onClick={() => navigate('/')}>
                <ArrowLeft size={18} /> Quay lại cửa hàng
            </button>
        </div>
    );
};

export default PaymentResult;
