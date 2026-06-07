import React, { useEffect, useState } from 'react';
import { Loader2, Save } from 'lucide-react';
import { profileService } from '../services/api';

interface ProfileForm {
    firstName: string;
    lastName: string;
    email: string;
    address: string;
    phone: string;
}

const empty: ProfileForm = { firstName: '', lastName: '', email: '', address: '', phone: '' };

const Profile: React.FC = () => {
    const [form, setForm] = useState<ProfileForm>(empty);
    const [username, setUsername] = useState('');
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);

    useEffect(() => {
        (async () => {
            try {
                const res = await profileService.me();
                const u = res.data as any;
                setUsername(u.username || '');
                setForm({
                    firstName: u.firstName || '',
                    lastName: u.lastName || '',
                    email: u.email || '',
                    address: u.attributes?.address?.[0] || '',
                    phone: u.attributes?.phone?.[0] || ''
                });
            } finally {
                setLoading(false);
            }
        })();
    }, []);

    const submit = async (e: React.FormEvent) => {
        e.preventDefault();
        setSaving(true);
        try {
            await profileService.update(form);
            alert('Đã cập nhật hồ sơ.');
        } catch {
            alert('Cập nhật thất bại.');
        } finally {
            setSaving(false);
        }
    };

    if (loading) return <div className="loader"><Loader2 className="spin" /> Đang tải hồ sơ...</div>;

    return (
        <div className="auth-page">
            <form className="auth-card" onSubmit={submit}>
                <h2>Hồ sơ của tôi</h2>
                <p className="auth-subtitle">Tên đăng nhập: <strong>{username}</strong></p>

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
                    <label>Email</label>
                    <input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
                </div>
                <div className="form-group">
                    <label>Số điện thoại</label>
                    <input value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
                </div>
                <div className="form-group">
                    <label>Địa chỉ giao hàng</label>
                    <textarea rows={2} value={form.address} onChange={(e) => setForm({ ...form, address: e.target.value })} />
                </div>

                <button type="submit" className="btn-submit-admin" disabled={saving}>
                    {saving ? <Loader2 className="spin" size={20} /> : <Save size={20} />} Lưu
                </button>
            </form>
        </div>
    );
};

export default Profile;
