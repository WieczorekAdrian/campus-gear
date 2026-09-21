import { useState } from 'react';
import axios from '../api/axiosConfig';
import { useNavigate, Link } from 'react-router-dom';
import { UserPlus } from 'lucide-react';

// Pamiętaj o imporcie swojego logo!
import myLogo from '../assets/logo.png';

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent } from "@/components/ui/card";

function Register() {
    const [firstName, setFirstName] = useState('');
    const [lastName, setLastName] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');

    const [errorMsg, setErrorMsg] = useState('');
    const [successMsg, setSuccessMsg] = useState('');
    const [submitting, setSubmitting] = useState(false);

    const navigate = useNavigate();

    const handleRegister = (e) => {
        e.preventDefault();
        setSubmitting(true);
        const payload = { email, password, firstName, lastName };

        axios.post('/api/auth/register', payload)
            .then(response => {
                setSuccessMsg('Konto utworzone! Przekierowanie do logowania...');
                setErrorMsg('');
                setTimeout(() => navigate('/login'), 2000);
            })
            .catch(error => {
                console.error("Błąd rejestracji", error);
                setErrorMsg('Wystąpił błąd. Email może być już zajęty.');
                setSuccessMsg('');
            })
            .finally(() => setSubmitting(false));
    };

    return (
        <div className="flex min-h-svh flex-col items-center justify-center bg-background p-6 md:p-10">
            <div className="w-full max-w-sm space-y-6">

                {/* Nagłówek z Logo */}
                <div className="flex flex-col items-center space-y-2 text-center">
                    <img src={myLogo} alt="Logo Campus Gear" className="h-16 w-auto object-contain drop-shadow-md" />
                    <h1 className="mt-4 text-2xl font-bold tracking-tight">
                        Załóż konto
                    </h1>
                    <p className="text-sm text-muted-foreground">
                        Masz już konto?{" "}
                        <Link to="/login" className="underline underline-offset-4 hover:text-primary text-foreground">
                            Zaloguj się
                        </Link>
                    </p>
                </div>

                <Card className="bg-white/5 border-white/10 shadow-xl">
                    <CardContent className="p-6">
                        {/* Formularz */}
                        <form onSubmit={handleRegister} className="space-y-6">
                            {errorMsg && <div className="text-sm font-medium text-destructive text-center">{errorMsg}</div>}
                            {successMsg && <div className="text-sm font-medium text-green-500 text-center">{successMsg}</div>}

                            <div className="space-y-4">
                                <div className="grid grid-cols-2 gap-4">
                                    <div className="space-y-2">
                                        <Label htmlFor="firstName">Imię</Label>
                                        <Input id="firstName" required value={firstName} onChange={(e) => setFirstName(e.target.value)}
                                               className="bg-white/5 border-white/10 focus:bg-white/10 transition-colors" />
                                    </div>
                                    <div className="space-y-2">
                                        <Label htmlFor="lastName">Nazwisko</Label>
                                        <Input id="lastName" required value={lastName} onChange={(e) => setLastName(e.target.value)}
                                               className="bg-white/5 border-white/10 focus:bg-white/10 transition-colors" />
                                    </div>
                                </div>
                                <div className="space-y-2">
                                    <Label htmlFor="email">Email</Label>
                                    <Input id="email" type="email" required value={email} onChange={(e) => setEmail(e.target.value)}
                                           className="bg-white/5 border-white/10 focus:bg-white/10 transition-colors" />
                                </div>
                                <div className="space-y-2">
                                    <Label htmlFor="password">Hasło</Label>
                                    <Input id="password" type="password" required value={password} onChange={(e) => setPassword(e.target.value)}
                                           className="bg-white/5 border-white/10 focus:bg-white/10 transition-colors" />
                                </div>

                                <Button type="submit" className="w-full" disabled={submitting}>
                                    <UserPlus aria-hidden />
                                    {submitting ? 'Tworzenie konta...' : 'Zarejestruj się'}
                                </Button>
                            </div>
                        </form>
                    </CardContent>
                </Card>
            </div>
        </div>
    );
}

export default Register;
