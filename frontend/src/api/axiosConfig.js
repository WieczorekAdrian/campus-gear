import axios from 'axios';

const instance = axios.create({
    // Tutaj ewentualnie baseURL, np. baseURL: 'http://localhost:8080'
});

// Dodajemy tzw. Interceptor, który przed każdym wysłaniem zapytania dokleja token
instance.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token');
        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// Odpowiedzi 401/403 (brak lub zły token) -> czyścimy sesję i wracamy na login.
// Działa tylko w przeglądarce; testy e2e (Playwright) i tak asertują statusy.
instance.interceptors.response.use(
    (response) => response,
    (error) => {
        const status = error?.response?.status;
        if ((status === 401 || status === 403) && typeof window !== 'undefined') {
            const url = error?.config?.url || '';
            const isAuthCall = url.includes('/api/auth/login') || url.includes('/api/auth/register');
            const isPublicEquipmentGet =
                error?.config?.method === 'get' && url.includes('/api/equipment');
            if (!isAuthCall && !isPublicEquipmentGet && window.location.pathname !== '/login') {
                localStorage.removeItem('token');
                localStorage.removeItem('role');
                localStorage.removeItem('firstName');
                window.location.assign('/login');
            }
        }
        return Promise.reject(error);
    }
);

export default instance;