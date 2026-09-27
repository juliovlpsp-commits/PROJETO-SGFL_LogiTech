// src/Login.jsx
import { useState } from 'react';

export default function Login({ onLoginSuccess }) {
    const [email, setEmail] = useState('');
    const [senha, setSenha] = useState('');
    const [erro, setErro] = useState('');

    const handleSubmit = async (e) => {
        e.preventDefault();
        setErro('');

        try {
            const response = await fetch('http://localhost:8080/api/auth/login', { // ajuste a URL da sua rota de login se for diferente
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username: email, password: senha })
            });

            if (response.ok) {
                const data = await response.json();

                // ATENÇÃO AQUI: Extraia o token do objeto retornado pelo Spring Boot!
                // Se a sua DTO LoginResponse.java tiver o campo 'token', use data.token
                const jwtToken = data.token || data.jwt || data;

                if (jwtToken && typeof jwtToken === 'string') {
                    onLoginSuccess(jwtToken);
                } else {
                    setErro('Token não retornado corretamente pelo servidor.');
                }
            } else {
                setErro('Credenciais inválidas.');
            }
        } catch (err) {
            setErro('Erro ao conectar com o servidor.');
        }
    };

    return (
        <div style={styles.container}>
            <form onSubmit={handleSubmit} style={styles.form}>
                <h2>SGFL - Login</h2>
                {erro && <div style={styles.erro}>{erro}</div>}
                <input
                    type="email"
                    placeholder="Email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                />
                <input
                    type="password"
                    placeholder="Senha"
                    value={senha}
                    onChange={(e) => setSenha(e.target.value)}
                    required
                />
                <button type="submit">Entrar</button>
            </form>
        </div>
    );
}

const styles = {
    container: { display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh', backgroundColor: '#121212', color: '#fff' },
    form: { display: 'flex', flexDirection: 'column', gap: '1rem', padding: '2rem', backgroundColor: '#1e1e1e', borderRadius: '8px', minWidth: '300px' },
    erro: { color: '#ff6b6b', fontSize: '0.9rem' }
};