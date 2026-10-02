import { useEffect, useState } from 'react';
import Login from './Login';
import Dashboard from './Dashboard';
import GestaoComercial from './GestaoComercial';
import api from './api';

export default function App() {
    const [autenticado, setAutenticado] = useState(false);

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
        api.get('/auth/session')
            .then(() => setAutenticado(true))
            .catch(() => setAutenticado(false));

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

    return (
        <div>
            {!autenticado ? (
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
