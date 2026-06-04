import React, { useState } from 'react';
import axios from '../api/axiosConfig';
import { useNavigate, Link } from 'react-router-dom';

// Importy komponentów shadcn/ui
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

function Login() {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [errorMsg, setErrorMsg] = useState('');

    const navigate = useNavigate();

    const handleLogin = (e) => {
        e.preventDefault();

        axios.post('/api/auth/login', { email, password })
            .then(response => {
                localStorage.setItem('token', response.data.token);
                localStorage.setItem('role', response.data.role);

                navigate('/equipment');
            })
            .catch(error => {
                console.error("Błąd logowania", error);
                setErrorMsg('Nieprawidłowy email lub hasło.');
            });
    };

    return (
        <div className="flex items-center justify-center min-h-screen bg-gray-50 p-4">
            <Card className="w-full max-w-md shadow-lg">
                <CardHeader className="space-y-1">
                    <CardTitle className="text-2xl font-bold text-center">Logowanie</CardTitle>
                    <CardDescription className="text-center">
                        Wprowadź swoje dane, aby wejść do systemu Campus Gear
                    </CardDescription>
                </CardHeader>
                <CardContent>
                    {errorMsg && (
                        <div className="bg-red-50 text-red-600 p-3 rounded-md mb-4 text-sm text-center border border-red-200">
                            {errorMsg}
                        </div>
                    )}

                    <form onSubmit={handleLogin} className="space-y-4">
                        <div className="space-y-2">
                            <Label htmlFor="email">Adres email</Label>
                            <Input
                                id="email"
                                type="email"
                                placeholder="nazwa@campus.edu.pl"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                required
                            />
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="password">Hasło</Label>
                            <Input
                                id="password"
                                type="password"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                required
                            />
                        </div>

                        <Button type="submit" className="w-full bg-blue-600 hover:bg-blue-700">
                            Zaloguj się
                        </Button>
                    </form>

                    <div className="mt-6 text-center text-sm text-gray-500">
                        Nie masz jeszcze konta?{' '}
                        <Link to="/register" className="text-blue-600 font-medium hover:underline">
                            Zarejestruj się
                        </Link>
                    </div>
                </CardContent>
            </Card>
        </div>
    );
}

export default Login;