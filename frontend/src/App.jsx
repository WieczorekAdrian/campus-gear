import { BrowserRouter as Router, Routes, Route, Navigate, Outlet, NavLink, Link } from 'react-router-dom';

// Importy Twoich komponentów
import EquipmentList from './components/EquipmentList';
import RentalsList from './components/RentalsList';
import OpiekunPanel from './components/OpiekunPanel';
import Profile from './components/Profile';
import Login from './components/Login';
import Register from './components/Register';
import myLogo from './assets/logo.png';

function RequireAuth({ children }) {
    const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;
    if (!token) {
        return <Navigate to="/login" replace />;
    }
    return children;
}

function isOpiekun() {
    if (typeof window === 'undefined') return false;
    const role = localStorage.getItem('role');
    return role === 'ROLE_OPIEKUN' || role === 'ROLE_ADMIN';
}

function RequireOpiekun({ children }) {
    const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;
    if (!token) {
        return <Navigate to="/login" replace />;
    }
    if (!isOpiekun()) {
        return <Navigate to="/equipment" replace />;
    }
    return children;
}

function NavItem({ to, children }) {
    return (
        <NavLink
            to={to}
            className={({ isActive }) =>
                `rounded-full px-4 py-2 text-sm font-medium transition-colors ${
                    isActive
                        ? 'bg-white/10 text-foreground'
                        : 'text-muted-foreground hover:text-foreground hover:bg-white/5'
                }`
            }
        >
            {children}
        </NavLink>
    );
}

function UserChip() {
    const firstName = typeof window !== 'undefined' ? localStorage.getItem('firstName') : null;
    const role = typeof window !== 'undefined' ? localStorage.getItem('role') : null;
    return (
        <Link
            to="/profile"
            title="Profil"
            className="flex items-center gap-2.5 rounded-full border border-white/10 bg-white/5 py-1 pl-1 pr-3 transition-colors hover:bg-white/10"
        >
            <span className="flex size-7 items-center justify-center rounded-full bg-primary text-xs font-bold text-primary-foreground">
                {(firstName?.[0] ?? '?').toUpperCase()}
            </span>
            <span className="hidden sm:block text-left leading-tight">
                <span className="block text-xs font-medium text-foreground">{firstName ?? 'Konto'}</span>
                <span className="block text-[11px] text-muted-foreground">
                    {role === 'ROLE_OPIEKUN' ? 'Opiekun' : role === 'ROLE_ADMIN' ? 'Admin' : 'Student'}
                </span>
            </span>
        </Link>
    );
}

// 1. Główny układ aplikacji (Layout) dla zalogowanych
function MainLayout() {
    const showPanel = isOpiekun();
    return (
        <div className="min-h-screen bg-background text-foreground flex flex-col">

            <nav className="flex items-center justify-between gap-4 px-4 sm:px-6 py-3 bg-white/5 border-b border-white/10 backdrop-blur-md sticky top-0 z-50">
                <Link to="/equipment" className="flex items-center gap-2.5 shrink-0">
                    <img src={myLogo} alt="Logo Campus Gear" className="h-8 w-auto object-contain" />
                    <span className="hidden md:block text-sm font-bold tracking-tight">Campus Gear</span>
                </Link>

                <div className="flex items-center gap-1">
                    <NavItem to="/equipment">Lista Sprzętu</NavItem>
                    <NavItem to="/rentals">Moje Wypożyczenia</NavItem>
                    {showPanel && (
                        <NavItem to="/panel">Panel opiekuna</NavItem>
                    )}
                </div>

                <UserChip />
            </nav>

            {/* Miejsce, w którym wyświetlają się poszczególne podstrony */}
            <main className="p-6 flex-1 w-full max-w-7xl mx-auto">
                <Outlet />
            </main>

            <footer className="border-t border-white/10 py-4 text-center text-xs text-muted-foreground">
                Campus Gear — wypożyczalnia sprzętu uczelnianego
            </footer>
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
                    <Route path="/panel" element={<RequireOpiekun><OpiekunPanel /></RequireOpiekun>} />
                </Route>
            </Routes>
        </Router>
    );
}

export default App;
