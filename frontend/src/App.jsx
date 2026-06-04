import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import EquipmentList from './components/EquipmentList';
import Login from './components/Login';
import Register from './components/Register'; // NOWY IMPORT

function App() {
    return (
        <Router>
            <div className="min-h-screen bg-gray-50">
                <nav className="bg-blue-600 p-4 shadow-md text-white flex justify-between items-center">
                    <h1 className="text-xl font-bold">Campus Gear</h1>
                </nav>

                <Routes>
                    <Route path="/" element={<Navigate to="/login" />} />
                    <Route path="/login" element={<Login />} />
                    <Route path="/register" element={<Register />} /> {/* NOWA ŚCIEŻKA */}
                    <Route path="/equipment" element={<EquipmentList />} />
                </Routes>
            </div>
        </Router>
    );
}

export default App;