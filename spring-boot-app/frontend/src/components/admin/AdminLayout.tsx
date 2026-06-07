import React from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { ArrowLeft, BarChart3, Boxes, ClipboardList, LogOut, PackagePlus, Users } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

const AdminLayout: React.FC = () => {
    const navigate = useNavigate();
    const { user, logout } = useAuth();

    const handleLogout = () => {
        logout();
        navigate('/login', { replace: true });
    };

    return (
        <div className="admin-shell">
            <aside className="admin-sidebar">
                <div className="admin-brand">V-Shop Admin</div>
                <nav>
                    <NavLink to="/admin" end><BarChart3 size={18} /> Tổng quan</NavLink>
                    <NavLink to="/admin/products"><PackagePlus size={18} /> Sản phẩm</NavLink>
                    <NavLink to="/admin/orders"><ClipboardList size={18} /> Đơn hàng</NavLink>
                    <NavLink to="/admin/inventory"><Boxes size={18} /> Tồn kho</NavLink>
                    <NavLink to="/admin/users"><Users size={18} /> Người dùng</NavLink>
                </nav>
                <div className="admin-sidebar-footer">
                    <span>{user?.username}</span>
                    <button className="btn-icon-text" onClick={() => navigate('/')}>
                        <ArrowLeft size={16} /> Cửa hàng
                    </button>
                    <button className="btn-icon-text" onClick={handleLogout}>
                        <LogOut size={16} /> Đăng xuất
                    </button>
                </div>
            </aside>
            <main className="admin-main">
                <Outlet />
            </main>
        </div>
    );
};

export default AdminLayout;
