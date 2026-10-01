import { useEffect, useState } from 'react';
import Login from './Login';
import Dashboard from './Dashboard';
import GestaoComercial from './GestaoComercial';

export default function App() {
    const [token, setToken] = useState(() => {
        const savedToken =
            localStorage.getItem('token');

        return (
            savedToken &&
            savedToken !== 'undefined' &&
            savedToken !== 'null'
        )
            ? savedToken
            : null;
    });

    const handleLoginSuccess = (
        newToken
    ) => {
        localStorage.setItem(
            'token',
            newToken
        );

        setToken(newToken);
    };

    const handleLogout = () => {
        localStorage.removeItem(
            'token'
        );

        setToken(null);
    };

    useEffect(() => {
        const aoExpirar = () => {
            setToken(null);
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
            {!token ? (
                <Login
                    onLoginSuccess={
                        handleLoginSuccess
                    }
                />
            ) : (
                <>
                    <Dashboard
                        token={token}
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