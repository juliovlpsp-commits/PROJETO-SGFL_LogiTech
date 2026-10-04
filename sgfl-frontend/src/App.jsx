import { useEffect, useState } from 'react';
import Login from './Login';
import Dashboard from './Dashboard';
import GestaoComercial from './GestaoComercial';
import api from './api';
import RastreioPublico from './RastreioPublico';

export default function App() {
    const [autenticado, setAutenticado] = useState(false);
    const [inicializado, setInicializado] = useState(false);

    const handleLoginSuccess = () => {
        setAutenticado(true);
    };

    const handleLogout = async () => {
        try {
            await api.post('/auth/logout');
        } catch {
            // Fecha a sessão na interface mesmo se a API estiver indisponível.
        } finally {
            setAutenticado(false);
        }
    };

    useEffect(() => {
        api.get('/auth/csrf')
            .catch(() => {})
            .then(() => api.get('/auth/session'))
            .then(() => setAutenticado(true))
            .catch(() => setAutenticado(false))
            .finally(() => setInicializado(true));

        const aoExpirar = () => {
            setAutenticado(false);
        };

        window.addEventListener(
            'sgfl:unauthorized',
            aoExpirar
        );

        return () =>
            window.removeEventListener(
                'sgfl:unauthorized',
                aoExpirar
            );
    }, []);

    const caminho = window.location.pathname;
    if (caminho.startsWith('/rastreio')) {
        const codigo = decodeURIComponent(caminho.split('/')[2] || '');
        return <RastreioPublico codigoInicial={codigo} />;
    }

    return (
        <div>
            {!inicializado ? (
                <div className="sgfl-app-loading" role="status" aria-live="polite"
                    style={{ minHeight: '100vh', display: 'grid', placeItems: 'center' }}>
                    Carregando aplicação…
                </div>
            ) : !autenticado ? (
                <Login
                    onLoginSuccess={
                        handleLoginSuccess
                    }
                />
            ) : (
                <>
                    <Dashboard
                        onLogout={
                            handleLogout
                        }
                    />

                    <GestaoComercial />
                </>
            )}
        </div>
    );
}
