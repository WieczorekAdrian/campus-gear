import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from '../api/axiosConfig';

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

    if (loading) {
        return <div className="text-center text-muted-foreground mt-20">Ładowanie profilu...</div>;
    }

    if (errorMsg) {
        return <div className="text-center text-destructive mt-20">{errorMsg}</div>;
    }

    return (
        <div className="max-w-xl mx-auto mt-8 bg-white/5 border border-white/10 rounded-md p-6 backdrop-blur-sm">
            <h2 className="text-2xl font-bold tracking-tight mb-4">Profil</h2>
            <div className="space-y-2 text-sm">
                <div><span className="text-muted-foreground">Email: </span>{profile?.email}</div>
                <div><span className="text-muted-foreground">Imię: </span>{profile?.firstName}</div>
                <div><span className="text-muted-foreground">Nazwisko: </span>{profile?.lastName}</div>
                <div><span className="text-muted-foreground">Rola: </span>{profile?.role}</div>
            </div>
            <button
                onClick={logout}
                className="mt-6 px-4 py-2 rounded-md border border-white/10 text-muted-foreground hover:text-foreground"
            >
                Wyloguj
            </button>
        </div>
    );
}

export default Profile;
