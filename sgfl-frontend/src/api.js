import axios from 'axios';

const api = axios.create({
    baseURL:
        import.meta.env.VITE_API_URL ||
        'http://localhost:8080/api',
    withCredentials: true
});

// Sessão do navegador usa cookie HttpOnly; credenciais inválidas no login
// não devem encerrar uma sessão já aberta em outra tela.
api.interceptors.response.use(
    (response) => response,
    (error) => {
        const status = error.response?.status;
        const url = error.config?.url || '';

        if (status === 401 && !url.includes('/auth/')) {
            window.dispatchEvent(new Event('sgfl:unauthorized'));
        }

        return Promise.reject(error);
    }
);

export default api;
