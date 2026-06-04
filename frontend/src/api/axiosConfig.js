import axios from 'axios';

// Ustawiamy "przechwytywacz" (interceptor) dla każdego wychodzącego zapytania
axios.interceptors.request.use(
    (config) => {
        // Wyciągamy token z pamięci przeglądarki
        const token = localStorage.getItem('token');

        // Jeśli token istnieje, doklejamy go do nagłówka
        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

export default axios;