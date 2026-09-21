import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from '../api/axiosConfig';
import { LogOut, Mail, User as UserIcon, ShieldCheck } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { PageHeader } from '@/components/ui/page-header';
import { Skeleton } from '@/components/ui/skeleton';

function roleLabel(role) {
    if (role === 'ROLE_OPIEKUN') return 'Opiekun';
    if (role === 'ROLE_ADMIN') return 'Admin';
    return 'Student';
}

function Profile() {
    const [profile, setProfile] = useState(null);
    const [loading, setLoading] = useState(true);
    const [errorMsg, setErrorMsg] = useState('');
    const navigate = useNavigate();

    useEffect(() => {
        axios.get('/api/auth/me')
            .then(response => {
                setProfile(response.data);
                setLoading(false);
            })
            .catch(error => {
                console.error('Błąd pobierania profilu', error);
                setErrorMsg(error?.response?.data?.detail || 'Nie udało się pobrać profilu.');
                setLoading(false);
            });
    }, []);

    const logout = () => {
        localStorage.removeItem('token');
        localStorage.removeItem('role');
        localStorage.removeItem('firstName');
        navigate('/login');
    };

    return (
        <div className="mt-8 mx-auto max-w-xl space-y-6">
            <PageHeader title="Profil" description="Twoje dane konta w wypożyczalni." />

            {loading ? (
                <Card className="bg-white/5 border-white/10">
                    <CardContent className="p-6 space-y-3">
                        <Skeleton className="h-12 w-12 rounded-full" />
                        <Skeleton className="h-4 w-2/3" />
                        <Skeleton className="h-4 w-1/2" />
                    </CardContent>
                </Card>
            ) : errorMsg ? (
                <div className="text-center text-destructive">{errorMsg}</div>
            ) : (
                <Card className="bg-white/5 border-white/10 shadow-xl">
                    <CardContent className="p-6">
                        <div className="flex items-center gap-4">
                            <span className="flex size-14 items-center justify-center rounded-full bg-primary text-xl font-bold text-primary-foreground">
                                {(profile?.firstName?.[0] ?? '?').toUpperCase()}
                            </span>
                            <div className="space-y-1">
                                <CardTitle>{profile?.firstName} {profile?.lastName}</CardTitle>
                                <Badge variant="secondary">{roleLabel(profile?.role)}</Badge>
                            </div>
                        </div>
                        <dl className="mt-6 space-y-3 text-sm">
                            <div className="flex items-center gap-3 rounded-md border border-white/10 bg-white/5 px-3 py-2.5">
                                <Mail aria-hidden className="size-4 shrink-0 text-muted-foreground" />
                                <span className="text-muted-foreground">Email</span>
                                <span className="ml-auto font-medium">{profile?.email}</span>
                            </div>
                            <div className="flex items-center gap-3 rounded-md border border-white/10 bg-white/5 px-3 py-2.5">
                                <UserIcon aria-hidden className="size-4 shrink-0 text-muted-foreground" />
                                <span className="text-muted-foreground">Imię i nazwisko</span>
                                <span className="ml-auto font-medium">{profile?.firstName} {profile?.lastName}</span>
                            </div>
                            <div className="flex items-center gap-3 rounded-md border border-white/10 bg-white/5 px-3 py-2.5">
                                <ShieldCheck aria-hidden className="size-4 shrink-0 text-muted-foreground" />
                                <span className="text-muted-foreground">Rola</span>
                                <span className="ml-auto font-medium">{profile?.role}</span>
                            </div>
                        </dl>
                        <Button variant="outline" onClick={logout} className="mt-6 w-full sm:w-auto">
                            <LogOut aria-hidden />
                            Wyloguj
                        </Button>
                    </CardContent>
                </Card>
            )}
        </div>
    );
}

export default Profile;
