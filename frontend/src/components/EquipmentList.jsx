import { useEffect, useState } from 'react';
import axios from '../api/axiosConfig';
import { Search, PackagePlus, CalendarClock, X } from 'lucide-react';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Label } from '@/components/ui/label';
import { PageHeader } from '@/components/ui/page-header';
import { Select } from '@/components/ui/select';
import { EmptyState } from '@/components/ui/empty-state';
import { TableSkeleton } from '@/components/ui/skeleton';
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/table';

const STATUS_OPTIONS = ['DOSTEPNY', 'ZAREZERWOWANY', 'WYPOZYCZONY', 'SERWISOWANY', 'ZNISZCZONY'];

function toLocalInputValue(date) {
    const pad = (n) => String(n).padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function extractErrorMessage(error, fallback) {
    return error?.response?.data?.detail || error?.response?.data?.message || fallback;
}

function EquipmentList() {
    const [equipment, setEquipment] = useState([]);
    const [loading, setLoading] = useState(true);
    const [errorMsg, setErrorMsg] = useState('');

    const [searchTerm, setSearchTerm] = useState('');
    const [selectedStatus, setSelectedStatus] = useState('');
    const [selectedLocation, setSelectedLocation] = useState('');
    const [locationInput, setLocationInput] = useState('');

    const [reservingId, setReservingId] = useState(null);
    const [startDate, setStartDate] = useState(() => toLocalInputValue(new Date(Date.now() + 24 * 3600 * 1000)));
    const [endDate, setEndDate] = useState(() => toLocalInputValue(new Date(Date.now() + 3 * 24 * 3600 * 1000)));
    const [formError, setFormError] = useState('');
    const [formSuccess, setFormSuccess] = useState('');
    const [submitting, setSubmitting] = useState(false);

    const fetchEquipment = (status, location) => {
        setLoading(true);
        setErrorMsg('');
        const params = new URLSearchParams();
        if (status) params.append('status', status);
        if (location) params.append('location', location);
        const query = params.toString();
        const url = query ? `/api/equipment/search?${query}` : '/api/equipment';
        axios.get(url)
            .then(response => {
                setEquipment(Array.isArray(response.data) ? response.data : []);
                setLoading(false);
            })
            .catch(error => {
                console.error("Błąd pobierania sprzętu", error);
                setErrorMsg(extractErrorMessage(error, 'Nie udało się pobrać sprzętu.'));
                setLoading(false);
            });
    };

    useEffect(() => {
        fetchEquipment(selectedStatus, selectedLocation);
    }, [selectedStatus, selectedLocation]);

    // Debounce wpisywania lokalizacji, żeby nie strzelać requestem na każdą literę
    useEffect(() => {
        const timer = setTimeout(() => setSelectedLocation(locationInput), 400);
        return () => clearTimeout(timer);
    }, [locationInput]);

    const filteredEquipment = equipment.filter(item => {
        const searchLower = searchTerm.trim().toLowerCase();
        if (!searchLower) return true;
        return (
            (item.deviceType && item.deviceType.toLowerCase().includes(searchLower)) ||
            (item.serialNumber && item.serialNumber.toLowerCase().includes(searchLower)) ||
            (item.technicalSpecification && item.technicalSpecification.toLowerCase().includes(searchLower)) ||
            (item.location && item.location.toLowerCase().includes(searchLower))
        );
    });

    const openReserve = (item) => {
        setReservingId(item.id);
        setFormError('');
        setFormSuccess('');
    };

    const submitReservation = () => {
        if (!reservingId) return;
        setSubmitting(true);
        setFormError('');
        setFormSuccess('');
        axios.post('/api/reservations', {
            equipmentId: reservingId,
            startDate: `${startDate}:00`,
            endDate: `${endDate}:00`,
        })
            .then(() => {
                setFormSuccess('Zarezerwowano pomyślnie. Zobacz zakładkę Moje Wypożyczenia.');
            })
            .catch(error => {
                console.error("Błąd rezerwacji", error);
                setFormError(extractErrorMessage(error, 'Nie udało się zarezerwować.'));
            })
            .finally(() => setSubmitting(false));
    };

    return (
        <div className="mt-8 space-y-6">
            <PageHeader
                title="Dostępny sprzęt"
                description="Przeglądaj, filtruj i rezerwuj sprzęt z wypożyczalni."
            />

            {errorMsg && (
                <div className="text-sm font-medium text-destructive text-center">{errorMsg}</div>
            )}

            <Card className="bg-white/5 border-white/10">
                <CardContent className="flex flex-col sm:flex-row gap-3 p-4">
                    <div className="relative flex-1">
                        <Search aria-hidden className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                        <Input
                            placeholder="Szukaj po typie, specyfikacji lub numerze seryjnym..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            className="bg-white/5 border-white/10 pl-9 text-foreground"
                        />
                    </div>
                    <div className="sm:w-56 space-y-2">
                        <Label htmlFor="statusFilter">Status</Label>
                        <Select
                            id="statusFilter"
                            value={selectedStatus}
                            onChange={(e) => setSelectedStatus(e.target.value)}
                        >
                            <option value="">Wszystkie statusy</option>
                            {STATUS_OPTIONS.map(st => (
                                <option key={st} value={st}>{st}</option>
                            ))}
                        </Select>
                    </div>
                    <div className="sm:w-64 space-y-2">
                        <Label htmlFor="locationFilter">Lokalizacja</Label>
                        <Input
                            id="locationFilter"
                            placeholder="Filtruj po lokalizacji..."
                            value={locationInput}
                            onChange={(e) => setLocationInput(e.target.value)}
                            className="bg-white/5 border-white/10 text-foreground"
                        />
                    </div>
                </CardContent>
            </Card>

            {reservingId && (
                <Card className="bg-white/5 border-white/10">
                    <CardHeader>
                        <CardTitle className="flex items-center gap-2">
                            <CalendarClock aria-hidden className="size-5 text-muted-foreground" />
                            Rezerwacja sprzętu ID {reservingId}
                        </CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-4">
                        {formError && <div className="text-sm text-destructive">{formError}</div>}
                        {formSuccess && <div className="text-sm text-green-400">{formSuccess}</div>}
                        <div className="flex flex-col sm:flex-row gap-4">
                            <div className="flex-1 space-y-2">
                                <Label htmlFor="reserve-start">Start</Label>
                                <Input
                                    id="reserve-start"
                                    type="datetime-local"
                                    value={startDate}
                                    onChange={(e) => setStartDate(e.target.value)}
                                    className="bg-white/5 border-white/10"
                                />
                            </div>
                            <div className="flex-1 space-y-2">
                                <Label htmlFor="reserve-end">Koniec</Label>
                                <Input
                                    id="reserve-end"
                                    type="datetime-local"
                                    value={endDate}
                                    onChange={(e) => setEndDate(e.target.value)}
                                    className="bg-white/5 border-white/10"
                                />
                            </div>
                        </div>
                        <div className="flex gap-2">
                            <Button onClick={submitReservation} disabled={submitting}>
                                <PackagePlus aria-hidden />
                                {submitting ? 'Rezerwuję...' : 'Potwierdź rezerwację'}
                            </Button>
                            <Button variant="outline" onClick={() => setReservingId(null)}>
                                <X aria-hidden />
                                Zamknij
                            </Button>
                        </div>
                    </CardContent>
                </Card>
            )}

            <Card className="bg-white/5 border-white/10 shadow-xl">
                <CardContent className="p-0">
                    {loading ? (
                        <TableSkeleton rows={6} cols={7} />
                    ) : filteredEquipment.length > 0 ? (
                        <Table>
                            <TableHeader>
                                <TableRow>
                                    <TableHead>ID</TableHead>
                                    <TableHead>S/N</TableHead>
                                    <TableHead>Typ</TableHead>
                                    <TableHead>Specyfikacja</TableHead>
                                    <TableHead>Lokalizacja</TableHead>
                                    <TableHead>Status</TableHead>
                                    <TableHead className="text-right">Akcje</TableHead>
                                </TableRow>
                            </TableHeader>
                            <TableBody>
                                {filteredEquipment.map((item) => (
                                    <TableRow key={item.id}>
                                        <TableCell className="text-muted-foreground">{item.id}</TableCell>
                                        <TableCell className="text-muted-foreground font-mono text-xs">{item.serialNumber || 'Brak'}</TableCell>
                                        <TableCell className="font-medium">{item.deviceType || '—'}</TableCell>
                                        <TableCell className="text-muted-foreground">{item.technicalSpecification || '—'}</TableCell>
                                        <TableCell className="text-muted-foreground">{item.location || 'Magazyn główny'}</TableCell>
                                        <TableCell>
                                            <Badge status={item.status}>{item.status || '—'}</Badge>
                                        </TableCell>
                                        <TableCell className="text-right">
                                            <Button
                                                variant="link"
                                                onClick={() => openReserve(item)}
                                            >
                                                Wypożycz
                                            </Button>
                                        </TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    ) : (
                        <EmptyState
                            title="Nie znaleziono sprzętu"
                            description="Spróbuj zmienić frazę lub status."
                        />
                    )}
                </CardContent>
            </Card>
        </div>
    );
}

export default EquipmentList;
