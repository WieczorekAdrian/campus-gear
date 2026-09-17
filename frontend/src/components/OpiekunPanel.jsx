import { useEffect, useState } from 'react';
import axios from '../api/axiosConfig';
import ReturnDialog from './ReturnDialog';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';

function extractErrorMessage(error, fallback) {
    return error?.response?.data?.detail || error?.response?.data?.message || fallback;
}

function OpiekunPanel() {
    const [reservations, setReservations] = useState([]);
    const [loans, setLoans] = useState([]);
    const [loading, setLoading] = useState(true);
    const [errorMsg, setErrorMsg] = useState('');
    const [successMsg, setSuccessMsg] = useState('');
    const [tab, setTab] = useState('ACTIVE');
    const [busyId, setBusyId] = useState(null);
    const [returnTarget, setReturnTarget] = useState(null);

    const fetchAll = () => {
        setLoading(true);
        setErrorMsg('');
        Promise.all([
            axios.get('/api/reservations'),
            axios.get('/api/loans', { params: { activeOnly: false } }),
        ])
            .then(([resRes, resLoans]) => {
                setReservations(Array.isArray(resRes.data) ? resRes.data : []);
                setLoans(Array.isArray(resLoans.data) ? resLoans.data : []);
                setLoading(false);
            })
            .catch(error => {
                console.error('Błąd pobierania panelu', error);
                setErrorMsg(extractErrorMessage(error, 'Nie udało się pobrać danych panelu.'));
                setLoading(false);
            });
    };

    useEffect(() => {
        fetchAll();
    }, []);

    const visibleReservations = tab === 'ALL'
        ? reservations
        : reservations.filter(r => r.status === 'AKTYWNA');
    // W zakładce Aktywne wypożyczenia pokazujemy TYLKO zgłoszone do odbioru.
    // Odbierz pojawia się dopiero jak użytkownik kliknie Zwróć.
    const visibleLoans = tab === 'ALL'
        ? loans
        : loans.filter(l => l.actualReturnDate == null && l.returnRequestedAt != null);

    const runAction = (key, fn, okMsg) => {
        setBusyId(key);
        setErrorMsg('');
        setSuccessMsg('');
        fn()
            .then(() => {
                if (okMsg) setSuccessMsg(okMsg);
                fetchAll();
            })
            .catch(error => {
                console.error('Błąd akcji opiekuna', error);
                setErrorMsg(extractErrorMessage(error, 'Operacja nie powiodła się.'));
            })
            .finally(() => setBusyId(null));
    };

    const issue = (id) => runAction(`issue-${id}`,
        () => axios.post('/api/loans', { reservationId: id }), 'Sprzęt wydany.');
    const cancel = (id) => runAction(`cancel-${id}`,
        () => axios.patch(`/api/reservations/${id}/cancel`));
    const confirmReturn = (damaged, damageDescription) => {
        if (!returnTarget) return;
        const { id } = returnTarget;
        runAction(`return-${id}`,
            () => axios.patch(`/api/loans/${id}/confirm-return`, { damaged, damageDescription })
                .then(() => {
                    setSuccessMsg(damaged ? 'Sprzęt zwrócony i zgłoszony do serwisu.' : 'Zwrot odebrany.');
                    setReturnTarget(null);
                }));
    };

    const actionBtn = (key, label, fn, variant = 'default') => (
        <Button
            size="sm"
            variant={variant}
            disabled={busyId === key}
            onClick={fn}
        >
            {busyId === key ? '...' : label}
        </Button>
    );

    return (
        <div className="max-w-7xl mx-auto mt-8 space-y-8">
            <div className="flex justify-between items-center">
                <h2 className="text-2xl font-bold tracking-tight text-foreground">Panel opiekuna</h2>
                <div className="flex gap-2">
                    <Button
                        size="sm"
                        variant={tab === 'ACTIVE' ? 'default' : 'outline'}
                        onClick={() => setTab('ACTIVE')}
                    >
                        Aktywne
                    </Button>
                    <Button
                        size="sm"
                        variant={tab === 'ALL' ? 'default' : 'outline'}
                        onClick={() => setTab('ALL')}
                    >
                        Historia
                    </Button>
                </div>
            </div>

            {errorMsg && (
                <div className="text-sm font-medium text-destructive text-center">{errorMsg}</div>
            )}
            {successMsg && (
                <div className="text-sm font-medium text-green-400 text-center">{successMsg}</div>
            )}

            {loading ? (
                <div className="text-center text-muted-foreground py-10">Ładowanie danych...</div>
            ) : (
                <>
                    <Card className="bg-white/5 border-white/10">
                        <CardHeader>
                            <CardTitle>Rezerwacje ({visibleReservations.length})</CardTitle>
                        </CardHeader>
                        <CardContent className="overflow-x-auto p-0">
                            <table className="w-full text-sm text-left text-foreground whitespace-nowrap">
                                <thead className="text-xs uppercase bg-black/20 text-muted-foreground border-b border-white/10">
                                <tr>
                                    <th scope="col" className="px-6 py-4 font-medium tracking-wider">ID</th>
                                    <th scope="col" className="px-6 py-4 font-medium tracking-wider">Użytkownik</th>
                                    <th scope="col" className="px-6 py-4 font-medium tracking-wider">Sprzęt</th>
                                    <th scope="col" className="px-6 py-4 font-medium tracking-wider">S/N</th>
                                    <th scope="col" className="px-6 py-4 font-medium tracking-wider">Termin</th>
                                    <th scope="col" className="px-6 py-4 font-medium tracking-wider">Status</th>
                                    <th scope="col" className="px-6 py-4 font-medium tracking-wider text-right">Akcje</th>
                                </tr>
                                </thead>
                                <tbody className="divide-y divide-white/5">
                                {visibleReservations.length > 0 ? (
                                    visibleReservations.map((r) => (
                                        <tr key={r.id} className="hover:bg-white/5 transition-colors">
                                            <td className="px-6 py-4 text-muted-foreground">{r.id}</td>
                                            <td className="px-6 py-4">{r.userEmail || '—'}</td>
                                            <td className="px-6 py-4 font-medium">{r.equipment?.deviceType || '—'}</td>
                                            <td className="px-6 py-4 text-muted-foreground font-mono text-xs">{r.equipment?.serialNumber || '—'}</td>
                                            <td className="px-6 py-4 text-muted-foreground">{r.startDate?.replace('T', ' ')} → {r.endDate?.replace('T', ' ')}</td>
                                            <td className="px-6 py-4"><Badge status={r.status}>{r.status}</Badge></td>
                                            <td className="px-6 py-4 text-right space-x-2">
                                                {r.status === 'AKTYWNA' ? (
                                                    <>
                                                        {actionBtn(`issue-${r.id}`, 'Wydaj', () => issue(r.id))}
                                                        {actionBtn(`cancel-${r.id}`, 'Anuluj', () => cancel(r.id), 'destructive')}
                                                    </>
                                                ) : (
                                                    <span className="text-muted-foreground">—</span>
                                                )}
                                            </td>
                                        </tr>
                                    ))
                                ) : (
                                    <tr>
                                        <td colSpan="7" className="px-6 py-8 text-center text-muted-foreground">
                                            Brak rezerwacji w tej zakładce.
                                        </td>
                                    </tr>
                                )}
                                </tbody>
                            </table>
                        </CardContent>
                    </Card>

                    <Card className="bg-white/5 border-white/10">
                        <CardHeader>
                            <CardTitle>Do odbioru ({visibleLoans.length})</CardTitle>
                        </CardHeader>
                        <CardContent className="overflow-x-auto p-0">
                            <table className="w-full text-sm text-left text-foreground whitespace-nowrap">
                                <thead className="text-xs uppercase bg-black/20 text-muted-foreground border-b border-white/10">
                                <tr>
                                    <th scope="col" className="px-6 py-4 font-medium tracking-wider">ID</th>
                                    <th scope="col" className="px-6 py-4 font-medium tracking-wider">Użytkownik</th>
                                    <th scope="col" className="px-6 py-4 font-medium tracking-wider">Sprzęt</th>
                                    <th scope="col" className="px-6 py-4 font-medium tracking-wider">S/N</th>
                                    <th scope="col" className="px-6 py-4 font-medium tracking-wider">Wydano / Termin</th>
                                    <th scope="col" className="px-6 py-4 font-medium tracking-wider">Zwrócono</th>
                                    <th scope="col" className="px-6 py-4 font-medium tracking-wider text-right">Akcje</th>
                                </tr>
                                </thead>
                                <tbody className="divide-y divide-white/5">
                                {visibleLoans.length > 0 ? (
                                    visibleLoans.map((l) => (
                                        <tr key={l.id} className="hover:bg-white/5 transition-colors">
                                            <td className="px-6 py-4 text-muted-foreground">{l.id}</td>
                                            <td className="px-6 py-4">{l.userEmail || '—'}</td>
                                            <td className="px-6 py-4 font-medium">{l.equipment?.deviceType || '—'}</td>
                                            <td className="px-6 py-4 text-muted-foreground font-mono text-xs">{l.equipment?.serialNumber || '—'}</td>
                                            <td className="px-6 py-4 text-muted-foreground">{l.borrowDate?.replace('T', ' ')} → {l.expectedReturnDate?.replace('T', ' ')}</td>
                                            <td className="px-6 py-4 text-muted-foreground">{l.actualReturnDate ? l.actualReturnDate.replace('T', ' ') : <Badge variant="secondary">wypożyczony</Badge>}</td>
                                            <td className="px-6 py-4 text-right">
                                                {l.actualReturnDate == null ? (
                                                    actionBtn(`return-${l.id}`, 'Odbierz zwrot', () => setReturnTarget({ id: l.id, label: `${l.equipment?.deviceType || ''} (${l.equipment?.serialNumber || ''}) — ${l.userEmail || ''}` }))
                                                ) : (
                                                    <span className="text-muted-foreground">—</span>
                                                )}
                                            </td>
                                        </tr>
                                    ))
                                ) : (
                                    <tr>
                                        <td colSpan="7" className="px-6 py-8 text-center text-muted-foreground">
                                            Brak wypożyczeń w tej zakładce.
                                        </td>
                                    </tr>
                                )}
                                </tbody>
                            </table>
                        </CardContent>
                    </Card>
                </>
            )}

            <ReturnDialog
                open={returnTarget != null}
                equipmentLabel={returnTarget?.label}
                busy={returnTarget != null && busyId === `return-${returnTarget.id}`}
                onClose={() => setReturnTarget(null)}
                onConfirm={confirmReturn}
            />
        </div>
    );
}

export default OpiekunPanel;
