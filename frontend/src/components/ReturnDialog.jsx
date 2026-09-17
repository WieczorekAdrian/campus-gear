import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

function ReturnDialog({ open, equipmentLabel, busy, onClose, onConfirm }) {
    const [damaged, setDamaged] = useState(false);
    const [description, setDescription] = useState('');

    if (!open) return null;

    const close = () => {
        setDamaged(false);
        setDescription('');
        onClose();
    };

    const confirm = () => {
        onConfirm(damaged, description);
        setDamaged(false);
        setDescription('');
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
            <div className="w-full max-w-md rounded-md border border-white/10 bg-background p-6 shadow-xl">
                <h3 className="text-lg font-semibold mb-1">Zwrot sprzętu</h3>
                <p className="text-sm text-muted-foreground mb-4">{equipmentLabel}</p>

                <label className="flex items-center gap-3 text-sm cursor-pointer">
                    <input
                        type="checkbox"
                        checked={damaged}
                        onChange={(e) => setDamaged(e.target.checked)}
                        className="size-4 accent-current"
                    />
                    Sprzęt jest uszkodzony
                </label>

                {damaged && (
                    <div className="mt-4 space-y-2">
                        <Label htmlFor="damageDescription">Opis uszkodzenia</Label>
                        <Input
                            id="damageDescription"
                            placeholder="np. pęknięta obudowa, nie działa przycisk..."
                            value={description}
                            onChange={(e) => setDescription(e.target.value)}
                            className="bg-white/5 border-white/10"
                        />
                    </div>
                )}

                <div className="mt-6 flex justify-end gap-3">
                    <Button variant="outline" onClick={close} disabled={busy}>
                        Anuluj
                    </Button>
                    <Button onClick={confirm} disabled={busy}>
                        {busy ? 'Zapisywanie...' : 'Potwierdź zwrot'}
                    </Button>
                </div>
            </div>
        </div>
    );
}

export default ReturnDialog;
