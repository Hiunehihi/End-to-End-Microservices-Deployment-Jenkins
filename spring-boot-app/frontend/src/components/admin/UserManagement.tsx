import React, { useEffect, useState } from 'react';
import { Loader2, ShieldCheck, ToggleLeft, ToggleRight } from 'lucide-react';
import { adminUserService } from '../../services/api';
import { KeycloakUser } from '../../types';

const UserManagement: React.FC = () => {
    const [users, setUsers] = useState<KeycloakUser[]>([]);
    const [loading, setLoading] = useState(true);
    const [busy, setBusy] = useState<string | null>(null);

    const load = async () => {
        setLoading(true);
        try {
            const res = await adminUserService.list();
            setUsers(res.data);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { load(); }, []);

    const toggleEnabled = async (user: KeycloakUser) => {
        setBusy(user.id);
        try {
            await adminUserService.setEnabled(user.id, !user.enabled);
            await load();
        } finally { setBusy(null); }
    };

    const grantAdmin = async (user: KeycloakUser) => {
        if (!window.confirm(`Cấp quyền ADMIN cho ${user.username}?`)) return;
        setBusy(user.id);
        try {
            await adminUserService.grantAdmin(user.id);
            alert('Đã cấp quyền ADMIN.');
        } finally { setBusy(null); }
    };

    const resetPassword = async (user: KeycloakUser) => {
        const pwd = window.prompt(`Mật khẩu mới cho ${user.username}:`);
        if (!pwd) return;
        setBusy(user.id);
        try {
            await adminUserService.resetPassword(user.id, pwd);
            alert('Đã đặt lại mật khẩu.');
        } finally { setBusy(null); }
    };

    if (loading) return <div className="loader"><Loader2 className="spin" /> Đang tải...</div>;

    return (
        <div className="admin-page-content">
            <h2>Quản lý người dùng</h2>
            <div className="order-table">
                {users.map(u => (
                    <div className="order-row" key={u.id}>
                        <div>
                            <strong>{u.username}</strong>
                            <p>{u.email}</p>
                            <p style={{ fontSize: 12, opacity: 0.7 }}>{u.firstName} {u.lastName}</p>
                        </div>
                        <span style={{ color: u.enabled ? '#16a34a' : '#dc2626' }}>
                            {u.enabled ? 'Hoạt động' : 'Đã khóa'}
                        </span>
                        <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                            <button className="btn-icon-text" disabled={busy === u.id} onClick={() => toggleEnabled(u)}>
                                {u.enabled ? <ToggleRight size={16} /> : <ToggleLeft size={16} />}
                                {u.enabled ? 'Khóa' : 'Mở'}
                            </button>
                            <button className="btn-icon-text" disabled={busy === u.id} onClick={() => grantAdmin(u)}>
                                <ShieldCheck size={16} /> Admin
                            </button>
                            <button className="btn-icon-text" disabled={busy === u.id} onClick={() => resetPassword(u)}>
                                Reset MK
                            </button>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
};

export default UserManagement;
