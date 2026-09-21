import { useEffect, useState } from 'react';
import axios from '../api/axiosConfig';
import { CalendarClock, HandCoins, Ban } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
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

function RentalsList() {
    const [reservations, setReservations] = useState([]);
    const [loans, setLoans] = useState([]);
    const [loading, setLoading] = useState(true);
    const [errorMsg, setErrorMsg] = useState('');
    const [successMsg, setSuccessMsg] = useState('');
    const [tab, setTab] = useState('AKTYWNA');
    const [busyId, setBusyId] = useState(null);

    const fetchMine = () => {
        setLoading(true);
        setErrorMsg('');
        Promise.all([
            axios.get('/api/reservations/mine'),
            axios.get('/api/loans/mine'),
        ])
            .then(([resRes, resLoans]) => {
                setReservations(Array.isArray(resRes.data) ? resRes.data : []);
                setLoans(Array.isArray(resLoans.data) ? resLoans.data : []);
                setLoading(false);
            })
            .catch(error => {
                console.error('Błąd pobierania wypożyczeń', error);
                setErrorMsg(extractErrorMessage(error, 'Nie udało się pobrać danych.'));
                setLoading(false);
            });
    };

    useEffect(() => {
        fetchMine();
    }, []);

    const visibleReservations = tab === 'ALL'
        ? reservations
        : reservations.filter(r => r.status === tab);
    const visibleLoans = tab === 'ALL'
        ? loans
        : loans.filter(l => (tab === 'AKTYWNA' ? l.actualReturnDate == null : l.actualReturnDate != null));

    const runAction = (key, fn) => {
        setBusyId(key);
        setErrorMsg('');
        setSuccessMsg('');
        fn()
            .then(() => fetchMine())
            .catch(error => {
                console.error('Błąd akcji', error);
                setErrorMsg(extractErrorMessage(error, 'Operacja nie powiodła się.'));
            })
            .finally(() => setBusyId(null));
    };

    const cancel = (id) => runAction(`cancel-${id}`, () => axios.patch(`/api/reservations/${id}/cancel`));
    const requestReturn = (id) => runAction(`return-${id}`,
        () => axios.patch(`/api/loans/${id}/request-return`)
            .then(() => setSuccessMsg('Zwrot zgłoszony. Opiekun potwierdzi odbiór sprzętu.')));

    return (
        <div className="mt-8 space-y-8">
            <PageHeader
                title="Moje wypożyczenia"
                description="Twoje rezerwacje i wypożyczenia w jednym miejscu."
                actions={
                    <>
                        <Button
                            size="sm"
                            variant={tab === 'AKTYWNA' ? 'default' : 'outline'}
                            onClick={() => setTab('AKTYWNA')}
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
                                <CalendarClock aria-hidden className="size-5 text-muted-foreground" />
                                Rezerwacje
                            </CardTitle>
                        </CardHeader>
                        <CardContent className="p-0">
                            {visibleReservations.length > 0 ? (
                                <Table>
                                    <TableHeader>
                                        <TableRow>
                                            <TableHead>ID</TableHead>
                                            <TableHead>Sprzęt</TableHead>
                                            <TableHead>S/N</TableHead>
                                            <TableHead>Start</TableHead>
                                            <TableHead>Koniec</TableHead>
                                            <TableHead>Status</TableHead>
                                            <TableHead className="text-right">Akcje</TableHead>
                                        </TableRow>
                                    </TableHeader>
                                    <TableBody>
                                        {visibleReservations.map((r) => (
                                            <TableRow key={r.id}>
                                                <TableCell className="text-muted-foreground">{r.id}</TableCell>
                                                <TableCell className="font-medium">{r.equipment?.deviceType || `Sprzęt ${r.equipment?.id ?? ''}`}</TableCell>
                                                <TableCell className="text-muted-foreground font-mono text-xs">{r.equipment?.serialNumber || '—'}</TableCell>
                                                <TableCell className="text-muted-foreground">{formatDateTime(r.startDate)}</TableCell>
                                                <TableCell className="text-muted-foreground">{formatDateTime(r.endDate)}</TableCell>
                                                <TableCell>
                                                    <Badge status={r.status}>{r.status}</Badge>
                                                </TableCell>
                                                <TableCell className="text-right">
                                                    {r.status === 'AKTYWNA' ? (
                                                        <Button
                                                            variant="destructive"
                                                            size="sm"
                                                            onClick={() => cancel(r.id)}
                                                            disabled={busyId === `cancel-${r.id}`}
                                                        >
                                                            <Ban aria-hidden />
                                                            {busyId === `cancel-${r.id}` ? 'Anulowanie...' : 'Anuluj'}
                                                        </Button>
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
                                    icon={CalendarClock}
                                    title="Brak rezerwacji w tej zakładce"
                                />
                            )}
                        </CardContent>
                    </Card>

                    <Card className="bg-white/5 border-white/10 shadow-xl">
                        <CardHeader>
                            <CardTitle className="flex items-center gap-2">
                                <HandCoins aria-hidden className="size-5 text-muted-foreground" />
                                Wypożyczenia
                            </CardTitle>
                        </CardHeader>
                        <CardContent className="p-0">
                            {visibleLoans.length > 0 ? (
                                <Table>
                                    <TableHeader>
                                        <TableRow>
                                            <TableHead>ID</TableHead>
                                            <TableHead>Sprzęt</TableHead>
                                            <TableHead>S/N</TableHead>
                                            <TableHead>Wydano</TableHead>
                                            <TableHead>Termin</TableHead>
                                            <TableHead>Zwrócono</TableHead>
                                            <TableHead className="text-right">Akcje</TableHead>
                                        </TableRow>
                                    </TableHeader>
                                    <TableBody>
                                        {visibleLoans.map((l) => (
                                            <TableRow key={l.id}>
                                                <TableCell className="text-muted-foreground">{l.id}</TableCell>
                                                <TableCell className="font-medium">{l.equipment?.deviceType || `Sprzęt ${l.equipment?.id ?? ''}`}</TableCell>
                                                <TableCell className="text-muted-foreground font-mono text-xs">{l.equipment?.serialNumber || '—'}</TableCell>
                                                <TableCell className="text-muted-foreground">{formatDateTime(l.borrowDate)}</TableCell>
                                                <TableCell className="text-muted-foreground">{formatDateTime(l.expectedReturnDate)}</TableCell>
                                                <TableCell className="text-muted-foreground">
                                                    {l.actualReturnDate
                                                        ? formatDateTime(l.actualReturnDate)
                                                        : (l.returnRequestedAt ? <Badge variant="secondary">czeka na odbiór</Badge> : '—')}
                                                </TableCell>
                                                <TableCell className="text-right">
                                                    {l.actualReturnDate == null && l.returnRequestedAt == null ? (
                                                        <Button
                                                            variant="link"
                                                            onClick={() => requestReturn(l.id)}
                                                            disabled={busyId === `return-${l.id}`}
                                                        >
                                                            {busyId === `return-${l.id}` ? 'Zgłaszanie...' : 'Zwróć'}
                                                        </Button>
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
                                />
                            )}
                        </CardContent>
                    </Card>
                </>
            )}
        </div>
    );
}

export default RentalsList;
