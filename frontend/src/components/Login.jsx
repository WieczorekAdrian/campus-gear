import React, { useState } from 'react';
import axios from '../api/axiosConfig';
import { useNavigate, Link } from 'react-router-dom';
import myLogo from '../assets/logo.png';

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

function Login() {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [errorMsg, setErrorMsg] = useState('');

    const navigate = useNavigate();

    const handleLogin = (e) => {
        e.preventDefault();

        // 1. Uderzamy w endpoint logowania
        axios.post('/api/auth/login', { email, password })
            .then(response => {
                // 2. Zapisujemy token (wiemy z dokumentacji, że to na pewno przychodzi)
                localStorage.setItem('token', response.data.token);

                // 3. Natychmiast uderzamy w niezawodny endpoint z danymi użytkownika
                return axios.get('/api/auth/me');
            })
            .then(meResponse => {
                // 4. Pobieramy i zapisujemy rolę i imię z drugiego żądania
                localStorage.setItem('role', meResponse.data.role);
                localStorage.setItem('firstName', meResponse.data.firstName);

                // 5. Dopiero teraz przekierowujemy użytkownika na listę sprzętu
                navigate('/equipment');
            })
            .catch(error => {
                console.error("Błąd logowania", error);
                setErrorMsg('Nieprawidłowy email lub hasło.');
            });
    };

    return (
        <div className="flex min-h-svh flex-col items-center justify-center bg-background p-6 md:p-10">
            <div className="w-full max-w-sm space-y-8">

                {/* Sekcja Nagłówka (bez ramki) */}
                <div className="flex flex-col items-center space-y-2 text-center">
                    {/* ZMIEŃ NA TO: */}
                    <img
                        src={myLogo}
                        alt="Logo Campus Gear"
                        className="h-16 w-auto object-contain drop-shadow-md"
                    />
                    <h1 className="mt-4 text-2xl font-bold tracking-tight">
                        Witaj w Campus Gear
                    </h1>
                    <p className="text-sm text-muted-foreground">
                        Nie masz jeszcze konta?{" "}
                        <Link to="/register" className="underline underline-offset-4 hover:text-primary text-foreground">
                            Zarejestruj się
                        </Link>
                    </p>
                </div>

                {/* Sekcja Formularza */}
                <form onSubmit={handleLogin} className="space-y-6">
                    {errorMsg && (
                        <div className="text-sm font-medium text-destructive text-center">
                            {errorMsg}
                        </div>
                    )}

                    <div className="space-y-4">
                        <div className="space-y-2">
                            <Label htmlFor="email">Email</Label>
                            <Input
                                id="email"
                                type="email"
                                placeholder="m@example.com"
                                required
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                className="bg-white/5 border-white/10 focus:bg-white/10 transition-colors"
                            />
                        </div>
                        <div className="space-y-2">
                            <div className="flex items-center justify-between">
                                <Label htmlFor="password">Hasło</Label>
                                <Link to="#" className="text-sm underline-offset-4 hover:underline text-muted-foreground">
                                    Zapomniałeś hasła?
                                </Link>
                            </div>
                            <Input
                                id="password"
                                type="password"
                                required
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                className="bg-white/5 border-white/10 focus:bg-white/10 transition-colors"
                            />
                        </div>
                        <Button type="submit" className="w-full">
                            Zaloguj się
                        </Button>
                    </div>
                </form>
            </div>
        </div>
    );
}

export default Login;