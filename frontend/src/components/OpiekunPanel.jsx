import { useEffect, useState } from 'react';
import axios from '../api/axiosConfig';
import { ClipboardList, HandCoins, Inbox, Wrench } from 'lucide-react';
import ReturnDialog from './ReturnDialog';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { PageHeader } from '@/components/ui/page-header';
import { EmptyState } from '@/components/ui/empty-state';
import { TableSkeleton } from '@/components/ui/skeleton';
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/table';

function extractErrorMessage(error, fallback) {
    return error?.response?.data?.detail || error?.response?.data?.message || fallback;
}

function formatDateTime(value) {
    if (!value) return '—';
    return value.replace('T', ' ');
}

function OpiekunPanel() {
    const [reservations, setReservations] = useState([]);
    const [loans, setLoans] = useState([]);
    const [defects, setDefects] = useState([]);
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
            axios.get('/api/defects'),
        ])
            .then(([resRes, resLoans, resDefects]) => {
                setReservations(Array.isArray(resRes.data) ? resRes.data : []);
                setLoans(Array.isArray(resLoans.data) ? resLoans.data : []);
                setDefects(Array.isArray(resDefects.data) ? resDefects.data : []);
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
    const visibleDefects = tab === 'ALL'
        ? defects
        : defects.filter(d => d.status !== 'NAPRAWIONA');

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
    const defectStatus = (id, status, okMsg) => runAction(`defect-${id}-${status}`,
        () => axios.patch(`/api/defects/${id}/status`, { status }), okMsg);
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
        <div className="mt-8 space-y-8">
            <PageHeader
                title="Panel opiekuna"
                description="Wydawaj sprzęt, anuluj rezerwacje i odbieraj zwroty."
                actions={
                    <>
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
                    </>
                }
            />

            {errorMsg && (
                <div className="text-sm font-medium text-destructive text-center">{errorMsg}</div>
            )}
            {successMsg && (
                <div className="text-sm font-medium text-green-400 text-center">{successMsg}</div>
            )}

            {loading ? (
                <Card className="bg-white/5 border-white/10 shadow-xl">
                    <CardContent className="p-0">
                        <TableSkeleton rows={4} cols={7} />
                    </CardContent>
                </Card>
            ) : (
                <>
                    <Card className="bg-white/5 border-white/10 shadow-xl">
                        <CardHeader>
                            <CardTitle className="flex items-center gap-2">
                                <ClipboardList aria-hidden className="size-5 text-muted-foreground" />
                                Rezerwacje ({visibleReservations.length})
                            </CardTitle>
                        </CardHeader>
                        <CardContent className="p-0">
                            {visibleReservations.length > 0 ? (
                                <Table>
                                    <TableHeader>
                                        <TableRow>
                                            <TableHead>ID</TableHead>
                                            <TableHead>Użytkownik</TableHead>
                                            <TableHead>Sprzęt</TableHead>
                                            <TableHead>S/N</TableHead>
                                            <TableHead>Termin</TableHead>
                                            <TableHead>Status</TableHead>
                                            <TableHead className="text-right">Akcje</TableHead>
                                        </TableRow>
                                    </TableHeader>
                                    <TableBody>
                                        {visibleReservations.map((r) => (
                                            <TableRow key={r.id}>
                                                <TableCell className="text-muted-foreground">{r.id}</TableCell>
                                                <TableCell>{r.userEmail || '—'}</TableCell>
                                                <TableCell className="font-medium">{r.equipment?.deviceType || '—'}</TableCell>
                                                <TableCell className="text-muted-foreground font-mono text-xs">{r.equipment?.serialNumber || '—'}</TableCell>
                                                <TableCell className="text-muted-foreground">{formatDateTime(r.startDate)} → {formatDateTime(r.endDate)}</TableCell>
                                                <TableCell><Badge status={r.status}>{r.status}</Badge></TableCell>
                                                <TableCell className="text-right">
                                                    <div className="flex justify-end gap-2">
                                                        {r.status === 'AKTYWNA' ? (
                                                            <>
                                                                {actionBtn(`issue-${r.id}`, 'Wydaj', () => issue(r.id))}
                                                                {actionBtn(`cancel-${r.id}`, 'Anuluj', () => cancel(r.id), 'destructive')}
                                                            </>
                                                        ) : (
                                                            <span className="text-muted-foreground">—</span>
                                                        )}
                                                    </div>
                                                </TableCell>
                                            </TableRow>
                                        ))}
                                    </TableBody>
                                </Table>
                            ) : (
                                <EmptyState
                                    icon={ClipboardList}
                                    title="Brak rezerwacji w tej zakładce"
                                />
                            )}
                        </CardContent>
                    </Card>

                    <Card className="bg-white/5 border-white/10 shadow-xl">
                        <CardHeader>
                            <CardTitle className="flex items-center gap-2">
                                <Inbox aria-hidden className="size-5 text-muted-foreground" />
                                Do odbioru ({visibleLoans.length})
                            </CardTitle>
                        </CardHeader>
                        <CardContent className="p-0">
                            {visibleLoans.length > 0 ? (
                                <Table>
                                    <TableHeader>
                                        <TableRow>
                                            <TableHead>ID</TableHead>
                                            <TableHead>Użytkownik</TableHead>
                                            <TableHead>Sprzęt</TableHead>
                                            <TableHead>S/N</TableHead>
                                            <TableHead>Wydano / Termin</TableHead>
                                            <TableHead>Zwrócono</TableHead>
                                            <TableHead className="text-right">Akcje</TableHead>
                                        </TableRow>
                                    </TableHeader>
                                    <TableBody>
                                        {visibleLoans.map((l) => (
                                            <TableRow key={l.id}>
                                                <TableCell className="text-muted-foreground">{l.id}</TableCell>
                                                <TableCell>{l.userEmail || '—'}</TableCell>
                                                <TableCell className="font-medium">{l.equipment?.deviceType || '—'}</TableCell>
                                                <TableCell className="text-muted-foreground font-mono text-xs">{l.equipment?.serialNumber || '—'}</TableCell>
                                                <TableCell className="text-muted-foreground">{formatDateTime(l.borrowDate)} → {formatDateTime(l.expectedReturnDate)}</TableCell>
                                                <TableCell className="text-muted-foreground">{l.actualReturnDate ? formatDateTime(l.actualReturnDate) : <Badge variant="secondary">wypożyczony</Badge>}</TableCell>
                                                <TableCell className="text-right">
                                                    {l.actualReturnDate == null ? (
                                                        actionBtn(`return-${l.id}`, 'Odbierz zwrot', () => setReturnTarget({ id: l.id, label: `${l.equipment?.deviceType || ''} (${l.equipment?.serialNumber || ''}) — ${l.userEmail || ''}` }))
                                                    ) : (
                                                        <span className="text-muted-foreground">—</span>
                                                    )}
                                                </TableCell>
                                            </TableRow>
                                        ))}
                                    </TableBody>
                                </Table>
                            ) : (
                                <EmptyState
                                    icon={HandCoins}
                                    title="Brak wypożyczeń w tej zakładce"
                                    description="Zwroty zgłoszone przez użytkowników pojawią się tutaj."
                                />
                            )}
                        </CardContent>
                    </Card>

                    <Card className="bg-white/5 border-white/10 shadow-xl">
                        <CardHeader>
                            <CardTitle className="flex items-center gap-2">
                                <Wrench aria-hidden className="size-5 text-muted-foreground" />
                                Usterki ({visibleDefects.length})
                            </CardTitle>
                        </CardHeader>
                        <CardContent className="p-0">
                            {visibleDefects.length > 0 ? (
                                <Table>
                                    <TableHeader>
                                        <TableRow>
                                            <TableHead>ID</TableHead>
                                            <TableHead>Opis</TableHead>
                                            <TableHead>Sprzęt</TableHead>
                                            <TableHead>S/N</TableHead>
                                            <TableHead>Zgłosił</TableHead>
                                            <TableHead>Status</TableHead>
                                            <TableHead className="text-right">Akcje</TableHead>
                                        </TableRow>
                                    </TableHeader>
                                    <TableBody>
                                        {visibleDefects.map((d) => (
                                            <TableRow key={d.id}>
                                                <TableCell className="text-muted-foreground">{d.id}</TableCell>
                                                <TableCell className="font-medium whitespace-normal min-w-48">{d.description || '—'}</TableCell>
                                                <TableCell>{d.equipment?.deviceType || '—'}</TableCell>
                                                <TableCell className="text-muted-foreground font-mono text-xs">{d.equipment?.serialNumber || '—'}</TableCell>
                                                <TableCell className="text-muted-foreground">{d.reporterEmail || '—'}</TableCell>
                                                <TableCell><Badge status={d.status}>{d.status}</Badge></TableCell>
                                                <TableCell className="text-right">
                                                    <div className="flex justify-end gap-2">
                                                        {d.status === 'ZGLOSZONA' && (
                                                            actionBtn(`defect-${d.id}-repair`, 'Weź do naprawy',
                                                                () => defectStatus(d.id, 'W_NAPRAWIE', 'Przekazano do naprawy.'))
                                                        )}
                                                        {d.status === 'W_NAPRAWIE' && (
                                                            actionBtn(`defect-${d.id}-done`, 'Oznacz jako naprawione',
                                                                () => defectStatus(d.id, 'NAPRAWIONA', 'Sprzęt wrócił do obiegu.'))
                                                        )}
                                                        {d.status === 'NAPRAWIONA' && (
                                                            <span className="text-muted-foreground">—</span>
                                                        )}
                                                    </div>
                                                </TableCell>
                                            </TableRow>
                                        ))}
                                    </TableBody>
                                </Table>
                            ) : (
                                <EmptyState
                                    icon={Wrench}
                                    title="Brak usterek w tej zakładce"
                                />
                            )}
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
