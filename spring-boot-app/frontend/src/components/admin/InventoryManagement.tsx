import React, { useEffect, useState } from 'react';
import { Loader2, Save } from 'lucide-react';
import { inventoryService } from '../../services/api';
import { InventoryItem } from '../../types';

const InventoryManagement: React.FC = () => {
    const [items, setItems] = useState<InventoryItem[]>([]);
    const [edits, setEdits] = useState<Record<string, number>>({});
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState<string | null>(null);

    const load = async () => {
        setLoading(true);
        try {
            const res = await inventoryService.listAll();
            setItems(res.data);
            setEdits({});
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { load(); }, []);

    const save = async (skuCode: string) => {
        const next = edits[skuCode];
        if (next == null) return;
        setSaving(skuCode);
        try {
            await inventoryService.updateQuantity(skuCode, next);
            await load();
        } finally {
            setSaving(null);
        }
    };

    if (loading) return <div className="loader"><Loader2 className="spin" /> Đang tải kho...</div>;

    return (
        <div className="admin-page-content">
            <h2>Quản lý tồn kho</h2>
            <div className="order-table">
                {items.map(it => (
                    <div className="order-row" key={it.skuCode}>
                        <strong>{it.skuCode}</strong>
                        <span>Hiện có: {it.quantity}</span>
                        <input
                            type="number"
                            min={0}
                            defaultValue={it.quantity}
                            onChange={(e) => setEdits(prev => ({ ...prev, [it.skuCode]: Number(e.target.value) }))}
                            style={{ width: 100 }}
                        />
                        <button
                            className="btn-icon-text"
                            disabled={saving === it.skuCode || edits[it.skuCode] == null || edits[it.skuCode] === it.quantity}
                            onClick={() => save(it.skuCode)}
                        >
                            {saving === it.skuCode ? <Loader2 className="spin" size={16} /> : <Save size={16} />} Lưu
                        </button>
                    </div>
                ))}
            </div>
        </div>
    );
};

export default InventoryManagement;
