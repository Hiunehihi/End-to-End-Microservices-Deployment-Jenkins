import React, { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Loader2, LogIn } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

const Login: React.FC = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const { login } = useAuth();
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState(false);

    const redirectAfterLogin = (roles: string[]) => {
        const fromState = (location.state as any)?.from?.pathname as string | undefined;
        if (fromState && fromState !== '/login') {
            navigate(fromState, { replace: true });
            return;
        }
        if (roles.includes('ADMIN')) {
            navigate('/admin', { replace: true });
        } else {
            navigate('/', { replace: true });
        }
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError(null);
        setLoading(true);
        try {
            const user = await login(username.trim(), password);
            redirectAfterLogin(user.roles);
        } catch (err: any) {
            setError(err?.response?.data?.error_description || 'Sai tên đăng nhập hoặc mật khẩu.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="auth-page">
            <form className="auth-card" onSubmit={handleSubmit}>
                <h2>Đăng nhập</h2>
                <p className="auth-subtitle">Chào mừng trở lại V-Shop.</p>

                <div className="form-group">
                    <label>Tên đăng nhập</label>
                    <input value={username} onChange={(e) => setUsername(e.target.value)} required />
                </div>
                <div className="form-group">
                    <label>Mật khẩu</label>
                    <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
                </div>

                {error && <div className="auth-error">{error}</div>}

                <button type="submit" className="btn-submit-admin" disabled={loading}>
                    {loading ? <Loader2 className="spin" size={20} /> : <LogIn size={20} />}
                    Đăng nhập
                </button>

                <p className="auth-footer">
                    Chưa có tài khoản? <Link to="/register">Đăng ký ngay</Link>
                </p>
            </form>
        </div>
    );
};

export default Login;
