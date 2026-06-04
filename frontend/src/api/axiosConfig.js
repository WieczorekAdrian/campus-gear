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

export default instance;