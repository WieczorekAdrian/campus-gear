import React, { useState } from 'react';
import axios from '../api/axiosConfig';
import { useNavigate, Link } from 'react-router-dom';

function Register() {
    // Stany dla wszystkich 4 pól, których wymaga Kacper
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [firstName, setFirstName] = useState('');
    const [lastName, setLastName] = useState('');

    // Stany na komunikaty
    const [errorMsg, setErrorMsg] = useState('');
    const [successMsg, setSuccessMsg] = useState('');

    const navigate = useNavigate();

    const handleRegister = (e) => {
        e.preventDefault();

        // Zbieramy dane do obiektu, dokładnie tak jak chciał backend
        const payload = { email, password, firstName, lastName };

        axios.post('/api/auth/register', payload)
            .then(response => {
                setSuccessMsg('Rejestracja udana! Za 2 sekundy przeniosę Cię do logowania...');
                setErrorMsg('');

                // Automatyczne przekierowanie do logowania po 2 sekundach
                setTimeout(() => {
                    navigate('/login');
                }, 2000);
            })
            .catch(error => {
                console.error("Błąd rejestracji", error);
                setErrorMsg('Wystąpił błąd. Upewnij się, że taki email już nie istnieje.');
                setSuccessMsg('');
            });
    };

    return (
        <div className="flex items-center justify-center min-h-screen bg-gray-100">
            <div className="bg-white p-8 rounded-lg shadow-md w-full max-w-md">
                <h2 className="text-2xl font-bold text-center text-gray-800 mb-6">Załóż konto</h2>

                {errorMsg && <div className="bg-red-100 text-red-700 p-3 rounded mb-4 text-sm text-center">{errorMsg}</div>}
                {successMsg && <div className="bg-green-100 text-green-700 p-3 rounded mb-4 text-sm text-center">{successMsg}</div>}

                <form onSubmit={handleRegister} className="space-y-4">
                    <div className="flex space-x-4">
                        <div className="w-1/2">
                            <label className="block text-sm font-medium text-gray-700">Imię</label>
                            <input type="text" required value={firstName} onChange={(e) => setFirstName(e.target.value)}
                                   className="mt-1 w-full p-2 border border-gray-300 rounded focus:ring focus:ring-blue-200" />
                        </div>
                        <div className="w-1/2">
                            <label className="block text-sm font-medium text-gray-700">Nazwisko</label>
                            <input type="text" required value={lastName} onChange={(e) => setLastName(e.target.value)}
                                   className="mt-1 w-full p-2 border border-gray-300 rounded focus:ring focus:ring-blue-200" />
                        </div>
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-700">Email</label>
                        <input type="email" required value={email} onChange={(e) => setEmail(e.target.value)}
                               className="mt-1 w-full p-2 border border-gray-300 rounded focus:ring focus:ring-blue-200" />
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-700">Hasło</label>
                        <input type="password" required value={password} onChange={(e) => setPassword(e.target.value)}
                               className="mt-1 w-full p-2 border border-gray-300 rounded focus:ring focus:ring-blue-200" />
                    </div>
                    <button type="submit" className="w-full bg-green-600 text-white font-bold py-2 px-4 rounded hover:bg-green-700 transition-colors">
                        Zarejestruj się
                    </button>
                </form>

                {/* Link powrotny do logowania */}
                <div className="mt-4 text-center">
                    <span className="text-sm text-gray-600">Masz już konto? </span>
                    <Link to="/login" className="text-sm text-blue-600 hover:underline">Zaloguj się</Link>
                </div>
            </div>
        </div>
    );
}

export default Register;