import { BrowserRouter as Router, Routes, Route, Navigate, Outlet, Link } from 'react-router-dom';

// Importy Twoich komponentów
import EquipmentList from './components/EquipmentList';
import RentalsList from './components/RentalsList';
import Profile from './components/Profile';
import Login from './components/Login';
import Register from './components/Register';

function RequireAuth({ children }) {
    const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;
    if (!token) {
        return <Navigate to="/login" replace />;
    }
    return children;
}

// 1. Główny układ aplikacji (Layout) dla zalogowanych
function MainLayout() {
    return (
        <div className="min-h-screen bg-background text-foreground flex flex-col">

            {/* Wycentrowany, "szklany" pasek nawigacji bez napisu Campus Gear */}
            <nav className="flex justify-center items-center space-x-8 p-4 bg-white/5 border-b border-white/10 backdrop-blur-md sticky top-0 z-50">
                <Link to="/equipment" className="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors">
                    Lista Sprzętu
                </Link>
                <Link to="/rentals" className="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors">
                    Moje Wypożyczenia
                </Link>
                <Link to="/profile" className="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors">
                    Profil
                </Link>
            </nav>

            {/* Miejsce, w którym wyświetlają się poszczególne podstrony */}
            <main className="p-6 flex-1">
                <Outlet />
            </main>
        </div>
    );
}

// 3. Główny komponent App spinający wszystko w całość
function App() {
    return (
        <Router>
            <Routes>
                {/* GRUPA 1: Trasy publiczne (Czyste ekrany, BEZ paska nawigacji) */}
                <Route path="/" element={<Navigate to="/login" />} />
                <Route path="/login" element={<Login />} />
                <Route path="/register" element={<Register />} />

                {/* GRUPA 2: Trasy chronione (Korzystające z MainLayout, Z paskiem nawigacji) */}
                <Route element={<RequireAuth><MainLayout /></RequireAuth>}>
                    <Route path="/equipment" element={<EquipmentList />} />
                    <Route path="/rentals" element={<RentalsList />} />
                    <Route path="/profile" element={<Profile />} />
                </Route>
            </Routes>
        </Router>
    );
}

export default App;
