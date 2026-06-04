import React, { useEffect, useState } from 'react';
import axios from '../api/axiosConfig';
import { Input } from "@/components/ui/input";

function EquipmentList() {
    const [equipment, setEquipment] = useState([]);
    const [loading, setLoading] = useState(true);

    // Stany do filtrowania
    const [searchTerm, setSearchTerm] = useState('');
    const [selectedCategory, setSelectedCategory] = useState('');

    useEffect(() => {
        axios.get('/api/equipment')
            .then(response => {
                setEquipment(response.data);
                setLoading(false);
            })
            .catch(error => {
                console.error("Błąd pobierania sprzętu", error);
                setLoading(false);
            });
    }, []);

    // Logika filtrowania sprzętu
    const filteredEquipment = equipment.filter(item => {
        // Zabezpieczenie przed nullami i zamiana na małe litery
        const searchLower = searchTerm.toLowerCase();
        const matchesSearch =
            (item.name && item.name.toLowerCase().includes(searchLower)) ||
            (item.serialNumber && item.serialNumber.toLowerCase().includes(searchLower));

        const matchesCategory = selectedCategory === '' || item.category === selectedCategory;

        return matchesSearch && matchesCategory;
    });

    // Unikalne kategorie do listy rozwijanej (wyciągnięte z pobranego sprzętu)
    const categories = [...new Set(equipment.map(item => item.category).filter(Boolean))];

    return (
        <div className="max-w-7xl mx-auto mt-8">
            <div className="flex justify-between items-center mb-6">
                <h2 className="text-2xl font-bold tracking-tight text-foreground">Dostępny sprzęt</h2>

                {/* TYMCZASOWY PRZYCISK DO TESTÓW */}
                <button
                    onClick={() => {
                        const testData = {
                            deviceType: "Aparat Sony A7 III",                // Zamiast name
                            technicalSpecification: "Kategoria: Foto/Wideo", // Zamiast category
                            serialNumber: "SN-987654321",
                            location: "Magazyn Główny - Regał 2",
                            status: "DOSTEPNY"
                        };

                        axios.post('/api/equipment', testData)
                            .then(() => {
                                alert("Udało się dodać sprzęt! Odśwież stronę.");
                                window.location.reload(); // Automatyczne odświeżenie strony po dodaniu
                            })
                            .catch(err => {
                                console.error(err);
                                alert("Błąd! Sprawdź konsolę (F12)");
                            });
                    }}
                    className="bg-green-600 hover:bg-green-700 text-white px-4 py-2 rounded-md font-medium transition-colors"
                >
                    + Dodaj testowy sprzęt
                </button>
            </div>

            {/* Pasek filtrowania i wyszukiwania */}
            <div className="flex flex-col sm:flex-row gap-4 mb-6 bg-white/5 p-4 rounded-md border border-white/10 backdrop-blur-sm">
                <div className="flex-1">
                    <Input
                        placeholder="Szukaj po nazwie lub numerze seryjnym..."
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        className="bg-white/5 border-white/10 text-foreground"
                    />
                </div>
                <div className="sm:w-64">
                    <select
                        value={selectedCategory}
                        onChange={(e) => setSelectedCategory(e.target.value)}
                        className="w-full h-10 px-3 py-2 rounded-md bg-transparent border border-white/10 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-primary/50 transition-colors"
                        style={{ backgroundColor: '#1a1a1a' }} // Tło dla rozwiniętej listy (natywne selecty w HTML bywają oporne na stylizację w Dark Mode)
                    >
                        <option value="">Wszystkie kategorie</option>
                        {categories.map(cat => (
                            <option key={cat} value={cat}>{cat}</option>
                        ))}
                    </select>
                </div>
            </div>

            {loading ? (
                <div className="text-center text-muted-foreground py-10">Ładowanie danych...</div>
            ) : (
                <div className="rounded-md border border-white/10 overflow-x-auto bg-white/5 backdrop-blur-sm shadow-xl">
                    <table className="w-full text-sm text-left text-foreground whitespace-nowrap">
                        <thead className="text-xs uppercase bg-black/20 text-muted-foreground border-b border-white/10">
                        <tr>
                            <th scope="col" className="px-6 py-4 font-medium tracking-wider">ID</th>
                            <th scope="col" className="px-6 py-4 font-medium tracking-wider">S/N</th>
                            <th scope="col" className="px-6 py-4 font-medium tracking-wider">Kategoria</th>
                            <th scope="col" className="px-6 py-4 font-medium tracking-wider">Nazwa Sprzętu</th>
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
                                    <td className="px-6 py-4">
                                            <span className="px-2 py-1 bg-white/10 rounded-md text-xs">
                                                {item.category || 'Inne'}
                                            </span>
                                    </td>
                                    <td className="px-6 py-4 font-medium">{item.name}</td>
                                    <td className="px-6 py-4 text-muted-foreground">{item.location || 'Magazyn główny'}</td>
                                    <td className="px-6 py-4">
                                            <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-green-500/10 text-green-400 border border-green-500/20">
                                                {item.status || 'Dostępny'}
                                            </span>
                                    </td>
                                    <td className="px-6 py-4 text-right">
                                        <button className="text-primary hover:text-primary/80 font-medium transition-colors">
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