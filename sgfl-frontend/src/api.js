import axios from 'axios';

const api = axios.create({
    baseURL:
        import.meta.env.VITE_API_URL ||
        'http://localhost:8080/api'
});

api.interceptors.request.use((config) => {
    const token = localStorage.getItem('token');

    if (token) {
        config.headers.Authorization =
            `Bearer ${token}`;
    }

    return config;
});

// Token expirado/invalido: limpa a sessao e avisa o App para voltar ao login.
// Ignora as rotas de autenticacao, onde 401 significa apenas "credenciais erradas".
api.interceptors.response.use(
    (response) => response,
    (error) => {
        const status = error.response?.status;
        const url = error.config?.url || '';
        const temToken = !!localStorage.getItem('token');

        if (status === 401 && temToken && !url.includes('/auth/')) {
            localStorage.removeItem('token');
            window.dispatchEvent(new Event('sgfl:unauthorized'));
        }

        return Promise.reject(error);
    }
);

export default api;
