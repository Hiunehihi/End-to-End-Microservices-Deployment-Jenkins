import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ClipboardList, LayoutDashboard, LogIn, LogOut, Search, ShoppingBag, UserCircle, UserPlus } from 'lucide-react';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';

const Navbar: React.FC = () => {
    const { cartCount } = useCart();
    const { user, isAuthenticated, isAdmin, logout } = useAuth();
    const navigate = useNavigate();

    const handleLogout = () => {
        logout();
        navigate('/login');
    };

    return (
        <nav className="navbar">
            <div className="navbar-brand">
                <Link to="/">
                    <ShoppingBag className="logo-icon" size={28} />
                    <span className="logo-text">V-Shop CI/CD Demo</span>
                </Link>
            </div>

            <div className="search-bar">
                <Search size={18} className="search-icon" />
                <input type="text" placeholder="Tìm kiếm sản phẩm..." />
            </div>

            <ul className="navbar-links">
                <li><Link to="/" title="Cửa hàng">Cửa hàng</Link></li>

                {isAuthenticated && (
                    <li className="cart-link">
                        <Link to="/cart">
                            <div className="cart-icon-wrapper">
                                <ShoppingBag size={22} />
                                {cartCount > 0 && <span className="cart-count">{cartCount}</span>}
                            </div>
                        </Link>
                    </li>
                )}

                {isAuthenticated && (
                    <li><Link to="/my-orders" title="Đơn của tôi"><ClipboardList size={20} /></Link></li>
                )}

                {isAdmin && (
                    <li><Link to="/admin" title="Quản trị viên (administrator)" className="admin-access"><LayoutDashboard size={20} /></Link></li>
                )}

                {isAuthenticated ? (
                    <>
                        <li><Link to="/profile" title="Hồ sơ"><UserCircle size={20} /> {user?.username}</Link></li>
                        <li>
                            <button className="btn-icon-text" onClick={handleLogout}>
                                <LogOut size={18} /> Đăng xuất
                            </button>
                        </li>
                    </>
                ) : (
                    <>
                        <li><Link to="/login"><LogIn size={18} /> Đăng nhập</Link></li>
                        <li><Link to="/register"><UserPlus size={18} /> Đăng ký</Link></li>
                    </>
                )}
            </ul>
        </nav>
    );
};

export default Navbar;
