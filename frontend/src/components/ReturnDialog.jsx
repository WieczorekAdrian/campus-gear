import { useState } from 'react';
import { TriangleAlert } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

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
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm">
            <Card className="w-full max-w-md bg-card shadow-2xl">
                <CardHeader>
                    <CardTitle>Zwrot sprzętu</CardTitle>
                    <p className="text-sm text-muted-foreground">{equipmentLabel}</p>
                </CardHeader>
                <CardContent className="space-y-4">
                    <label className="flex items-center gap-3 rounded-md border border-white/10 bg-white/5 p-3 text-sm cursor-pointer transition-colors hover:bg-white/10">
                        <input
                            type="checkbox"
                            checked={damaged}
                            onChange={(e) => setDamaged(e.target.checked)}
                            className="size-4 accent-current"
                        />
                        <TriangleAlert aria-hidden className="size-4 text-muted-foreground" />
                        Sprzęt jest uszkodzony
                    </label>

                    {damaged && (
                        <div className="space-y-2">
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

                    <div className="flex justify-end gap-2">
                        <Button variant="outline" onClick={close} disabled={busy}>
                            Anuluj
                        </Button>
                        <Button onClick={confirm} disabled={busy}>
                            {busy ? 'Zapisywanie...' : 'Potwierdź zwrot'}
                        </Button>
                    </div>
                </CardContent>
            </Card>
        </div>
    );
}

export default ReturnDialog;
