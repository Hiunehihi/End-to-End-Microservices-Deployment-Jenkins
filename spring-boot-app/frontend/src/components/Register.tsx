import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Loader2, UserPlus } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

const Register: React.FC = () => {
    const navigate = useNavigate();
    const { register } = useAuth();
    const [form, setForm] = useState({
        username: '',
        email: '',
        password: '',
        firstName: '',
        lastName: ''
    });
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState(false);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError(null);
        setLoading(true);
        try {
            await register({
                username: form.username.trim(),
                email: form.email.trim(),
                password: form.password,
                firstName: form.firstName.trim(),
                lastName: form.lastName.trim()
            });
            navigate('/', { replace: true });
        } catch (err: any) {
            const status = err?.response?.status;
            if (status === 409) {
                setError('Tên đăng nhập hoặc email đã tồn tại.');
            } else {
                setError(err?.response?.data?.error_description || 'Đăng ký không thành công.');
            }
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="auth-page">
            <form className="auth-card" onSubmit={handleSubmit}>
                <h2>Đăng ký tài khoản</h2>
                <p className="auth-subtitle">Tạo tài khoản khách hàng V-Shop.</p>

                <div className="form-grid">
                    <div className="form-group">
                        <label>Họ</label>
                        <input value={form.firstName} onChange={(e) => setForm({ ...form, firstName: e.target.value })} />
                    </div>
                    <div className="form-group">
                        <label>Tên</label>
                        <input value={form.lastName} onChange={(e) => setForm({ ...form, lastName: e.target.value })} />
                    </div>
                </div>
                <div className="form-group">
                    <label>Tên đăng nhập</label>
                    <input required value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} />
                </div>
                <div className="form-group">
                    <label>Email</label>
                    <input required type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
                </div>
                <div className="form-group">
                    <label>Mật khẩu</label>
                    <input required type="password" minLength={6} value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
                </div>

                {error && <div className="auth-error">{error}</div>}

                <button type="submit" className="btn-submit-admin" disabled={loading}>
                    {loading ? <Loader2 className="spin" size={20} /> : <UserPlus size={20} />}
                    Đăng ký
                </button>

                <p className="auth-footer">
                    Đã có tài khoản? <Link to="/login">Đăng nhập</Link>
                </p>
            </form>
        </div>
    );
};

export default Register;
