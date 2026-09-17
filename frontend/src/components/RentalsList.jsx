import { useEffect, useState } from 'react';
import axios from '../api/axiosConfig';

function extractErrorMessage(error, fallback) {
    return error?.response?.data?.detail || error?.response?.data?.message || fallback;
}

function RentalsList() {
    const [reservations, setReservations] = useState([]);
    const [loading, setLoading] = useState(true);
    const [errorMsg, setErrorMsg] = useState('');
    const [tab, setTab] = useState('AKTYWNA');
    const [cancellingId, setCancellingId] = useState(null);

    const fetchMine = () => {
        setLoading(true);
        setErrorMsg('');
        axios.get('/api/reservations/mine')
            .then(response => {
                setReservations(Array.isArray(response.data) ? response.data : []);
                setLoading(false);
            })
            .catch(error => {
                console.error('Błąd pobierania rezerwacji', error);
                setErrorMsg(extractErrorMessage(error, 'Nie udało się pobrać rezerwacji.'));
                setLoading(false);
            });
    };

    useEffect(() => {
        fetchMine();
    }, []);

    const visible = tab === 'ALL' ? reservations : reservations.filter(r => r.status === tab);

    const cancel = (id) => {
        setCancellingId(id);
        setErrorMsg('');
        axios.patch(`/api/reservations/${id}/cancel`)
            .then(() => fetchMine())
            .catch(error => {
                console.error('Błąd anulowania', error);
                setErrorMsg(extractErrorMessage(error, 'Nie udało się anulować rezerwacji.'));
            })
            .finally(() => setCancellingId(null));
    };

    return (
        <div className="max-w-7xl mx-auto mt-8">
            <div className="flex justify-between items-center mb-6">
                <h2 className="text-2xl font-bold tracking-tight text-foreground">Moje wypożyczenia</h2>
                <div className="flex gap-2">
                    <button
                        onClick={() => setTab('AKTYWNA')}
                        className={`px-4 py-2 rounded-md text-sm font-medium ${tab === 'AKTYWNA' ? 'bg-primary text-primary-foreground' : 'border border-white/10 text-muted-foreground'}`}
                    >
                        Aktywne
                    </button>
                    <button
                        onClick={() => setTab('ALL')}
                        className={`px-4 py-2 rounded-md text-sm font-medium ${tab === 'ALL' ? 'bg-primary text-primary-foreground' : 'border border-white/10 text-muted-foreground'}`}
                    >
                        Historia
                    </button>
                </div>
            </div>

            {errorMsg && (
                <div className="mb-4 text-sm font-medium text-destructive text-center">{errorMsg}</div>
            )}

            {loading ? (
                <div className="text-center text-muted-foreground py-10">Ładowanie danych...</div>
            ) : (
                <div className="rounded-md border border-white/10 overflow-x-auto bg-white/5 backdrop-blur-sm shadow-xl">
                    <table className="w-full text-sm text-left text-foreground whitespace-nowrap">
                        <thead className="text-xs uppercase bg-black/20 text-muted-foreground border-b border-white/10">
                        <tr>
                            <th scope="col" className="px-6 py-4 font-medium tracking-wider">ID</th>
                            <th scope="col" className="px-6 py-4 font-medium tracking-wider">Sprzęt</th>
                            <th scope="col" className="px-6 py-4 font-medium tracking-wider">S/N</th>
                            <th scope="col" className="px-6 py-4 font-medium tracking-wider">Start</th>
                            <th scope="col" className="px-6 py-4 font-medium tracking-wider">Koniec</th>
                            <th scope="col" className="px-6 py-4 font-medium tracking-wider">Status</th>
                            <th scope="col" className="px-6 py-4 font-medium tracking-wider text-right">Akcje</th>
                        </tr>
                        </thead>
                        <tbody className="divide-y divide-white/5">
                        {visible.length > 0 ? (
                            visible.map((r) => (
                                <tr key={r.id} className="hover:bg-white/5 transition-colors">
                                    <td className="px-6 py-4 text-muted-foreground">{r.id}</td>
                                    <td className="px-6 py-4 font-medium">{r.equipment?.deviceType || `Sprzęt ${r.equipment?.id ?? ''}`}</td>
                                    <td className="px-6 py-4 text-muted-foreground font-mono text-xs">{r.equipment?.serialNumber || '—'}</td>
                                    <td className="px-6 py-4 text-muted-foreground">{r.startDate?.replace('T', ' ')}</td>
                                    <td className="px-6 py-4 text-muted-foreground">{r.endDate?.replace('T', ' ')}</td>
                                    <td className="px-6 py-4">
                                        <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-white/10 border border-white/10">
                                            {r.status}
                                        </span>
                                    </td>
                                    <td className="px-6 py-4 text-right">
                                        {r.status === 'AKTYWNA' ? (
                                            <button
                                                onClick={() => cancel(r.id)}
                                                disabled={cancellingId === r.id}
                                                className="text-destructive hover:opacity-80 font-medium disabled:opacity-50"
                                            >
                                                {cancellingId === r.id ? 'Anulowanie...' : 'Anuluj'}
                                            </button>
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
                </div>
            )}
        </div>
    );
}

export default RentalsList;
