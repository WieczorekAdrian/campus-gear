import { useEffect, useState } from 'react';
import axios from '../api/axiosConfig';
import { Input } from "@/components/ui/input";

const STATUS_OPTIONS = ['', 'DOSTEPNY', 'ZAREZERWOWANY', 'WYPOZYCZONY', 'SERWISOWANY', 'ZNISZCZONY'];

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

    const [reservingId, setReservingId] = useState(null);
    const [startDate, setStartDate] = useState(() => toLocalInputValue(new Date(Date.now() + 24 * 3600 * 1000)));
    const [endDate, setEndDate] = useState(() => toLocalInputValue(new Date(Date.now() + 3 * 24 * 3600 * 1000)));
    const [formError, setFormError] = useState('');
    const [formSuccess, setFormSuccess] = useState('');
    const [submitting, setSubmitting] = useState(false);

    const fetchEquipment = (status) => {
        setLoading(true);
        setErrorMsg('');
        const url = status ? `/api/equipment/search?status=${encodeURIComponent(status)}` : '/api/equipment';
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
        fetchEquipment('');
    }, []);

    useEffect(() => {
        fetchEquipment(selectedStatus);
    }, [selectedStatus]);

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
        <div className="max-w-7xl mx-auto mt-8">
            <div className="flex justify-between items-center mb-6">
                <h2 className="text-2xl font-bold tracking-tight text-foreground">Dostępny sprzęt</h2>
            </div>

            {errorMsg && (
                <div className="mb-4 text-sm font-medium text-destructive text-center">{errorMsg}</div>
            )}

            <div className="flex flex-col sm:flex-row gap-4 mb-6 bg-white/5 p-4 rounded-md border border-white/10 backdrop-blur-sm">
                <div className="flex-1">
                    <Input
                        placeholder="Szukaj po typie, specyfikacji lub numerze seryjnym..."
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        className="bg-white/5 border-white/10 text-foreground"
                    />
                </div>
                <div className="sm:w-64">
                    <select
                        value={selectedStatus}
                        onChange={(e) => setSelectedStatus(e.target.value)}
                        className="w-full h-10 px-3 py-2 rounded-md bg-transparent border border-white/10 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-primary/50 transition-colors"
                        style={{ backgroundColor: '#1a1a1a' }}
                    >
                        <option value="">Wszystkie statusy</option>
                        {STATUS_OPTIONS.filter(Boolean).map(st => (
                            <option key={st} value={st}>{st}</option>
                        ))}
                    </select>
                </div>
            </div>

            {reservingId && (
                <div className="mb-6 bg-white/5 p-4 rounded-md border border-white/10 backdrop-blur-sm">
                    <h3 className="font-semibold mb-3">Rezerwacja sprzętu ID {reservingId}</h3>
                    {formError && <div className="text-sm text-destructive mb-2">{formError}</div>}
                    {formSuccess && <div className="text-sm text-green-400 mb-2">{formSuccess}</div>}
                    <div className="flex flex-col sm:flex-row gap-4">
                        <label className="flex-1 text-sm">
                            Start
                            <input
                                type="datetime-local"
                                value={startDate}
                                onChange={(e) => setStartDate(e.target.value)}
                                className="mt-1 w-full h-10 px-3 rounded-md bg-white/5 border border-white/10"
                            />
                        </label>
                        <label className="flex-1 text-sm">
                            Koniec
                            <input
                                type="datetime-local"
                                value={endDate}
                                onChange={(e) => setEndDate(e.target.value)}
                                className="mt-1 w-full h-10 px-3 rounded-md bg-white/5 border border-white/10"
                            />
                        </label>
                    </div>
                    <div className="mt-3 flex gap-3">
                        <button
                            onClick={submitReservation}
                            disabled={submitting}
                            className="bg-primary text-primary-foreground px-4 py-2 rounded-md font-medium disabled:opacity-50"
                        >
                            {submitting ? 'Rezerwuję...' : 'Potwierdź rezerwację'}
                        </button>
                        <button
                            onClick={() => setReservingId(null)}
                            className="px-4 py-2 rounded-md border border-white/10 text-muted-foreground"
                        >
                            Zamknij
                        </button>
                    </div>
                </div>
            )}

            {loading ? (
                <div className="text-center text-muted-foreground py-10">Ładowanie danych...</div>
            ) : (
                <div className="rounded-md border border-white/10 overflow-x-auto bg-white/5 backdrop-blur-sm shadow-xl">
                    <table className="w-full text-sm text-left text-foreground whitespace-nowrap">
                        <thead className="text-xs uppercase bg-black/20 text-muted-foreground border-b border-white/10">
                        <tr>
                            <th scope="col" className="px-6 py-4 font-medium tracking-wider">ID</th>
                            <th scope="col" className="px-6 py-4 font-medium tracking-wider">S/N</th>
                            <th scope="col" className="px-6 py-4 font-medium tracking-wider">Typ</th>
                            <th scope="col" className="px-6 py-4 font-medium tracking-wider">Specyfikacja</th>
                            <th scope="col" className="px-6 py-4 font-medium tracking-wider">Lokalizacja</th>
                            <th scope="col" className="px-6 py-4 font-medium tracking-wider">Status</th>
                            <th scope="col" className="px-6 py-4 font-medium tracking-wider text-right">Akcje</th>
                        </tr>
                        </thead>
                        <tbody className="divide-y divide-white/5">
                        {filteredEquipment.length > 0 ? (
                            filteredEquipment.map((item) => (
                                <tr key={item.id} className="hover:bg-white/5 transition-colors">
                                    <td className="px-6 py-4 text-muted-foreground">{item.id}</td>
                                    <td className="px-6 py-4 text-muted-foreground font-mono text-xs">{item.serialNumber || 'Brak'}</td>
                                    <td className="px-6 py-4 font-medium">{item.deviceType || '—'}</td>
                                    <td className="px-6 py-4 text-muted-foreground">{item.technicalSpecification || '—'}</td>
                                    <td className="px-6 py-4 text-muted-foreground">{item.location || 'Magazyn główny'}</td>
                                    <td className="px-6 py-4">
                                            <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-green-500/10 text-green-400 border border-green-500/20">
                                                {item.status || '—'}
                                            </span>
                                    </td>
                                    <td className="px-6 py-4 text-right">
                                        <button
                                            onClick={() => openReserve(item)}
                                            className="text-primary hover:text-primary/80 font-medium transition-colors"
                                        >
                                            Wypożycz
                                        </button>
                                    </td>
                                </tr>
                            ))
                        ) : (
                            <tr>
                                <td colSpan="7" className="px-6 py-8 text-center text-muted-foreground">
                                    Nie znaleziono sprzętu spełniającego kryteria.
                                </td>
                            </tr>
                        )}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    );
}

export default EquipmentList;
