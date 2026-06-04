import React, { useState, useEffect } from 'react';
import axios from '../api/axiosConfig';

function EquipmentList() {
    const [equipment, setEquipment] = useState([]);

    useEffect(() => {
        axios.get('/api/equipment')
            .then(response => {
                setEquipment(response.data);
            })
            .catch(error => {
                console.error("Wystąpił błąd podczas pobierania sprzętu!", error);
            });
    }, []);

    return (
        // p-6 daje margines wewnątrz (padding), max-w-4xl ogranicza szerokość, mx-auto środkuje na ekranie
        <div className="p-6 max-w-4xl mx-auto">
            {/* text-2xl powiększa tekst, font-bold pogrubia, mb-6 robi odstęp od dołu */}
            <h2 className="text-2xl font-bold mb-6 text-gray-800">Lista Dostępnego Sprzętu</h2>

            {/* Ładna, nowoczesna tabelka dzięki klasom Tailwinda */}
            <div className="overflow-x-auto bg-white shadow-md rounded-lg">
                <table className="min-w-full border-collapse">
                    <thead className="bg-gray-100 border-b-2 border-gray-200">
                    <tr>
                        <th className="p-4 text-left font-semibold text-gray-600">ID</th>
                        <th className="p-4 text-left font-semibold text-gray-600">Typ sprzętu</th>
                        <th className="p-4 text-left font-semibold text-gray-600">Numer seryjny</th>
                        <th className="p-4 text-left font-semibold text-gray-600">Status</th>
                    </tr>
                    </thead>
                    <tbody>
                    {equipment.map(item => (
                        <tr key={item.id} className="border-b border-gray-100 hover:bg-gray-50 transition-colors">
                            <td className="p-4 text-gray-800">{item.id}</td>
                            <td className="p-4 text-gray-800 font-medium">{item.deviceType}</td>
                            <td className="p-4 text-gray-600 text-sm">{item.serialNumber}</td>
                            <td className="p-4">
                                {/* Kolorowy badge dla statusu */}
                                <span className="bg-blue-100 text-blue-800 text-xs font-semibold px-2 py-1 rounded-full">
                                        {item.status}
                                    </span>
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
}

export default EquipmentList;