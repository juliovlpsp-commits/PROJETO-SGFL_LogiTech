// src/Login.jsx
import { useState } from 'react';
import { useTheme } from './useTheme';

export default function Login({ onLoginSuccess }) {
    const [email, setEmail] = useState('');
    const [senha, setSenha] = useState('');
    const [erro, setErro] = useState('');
    const { theme, mode, toggle } = useTheme();
    const styles = getStyles(theme);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setErro('');

        try {
            const response = await fetch('http://localhost:8080/api/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username: email, password: senha })
            });

            if (response.ok) {
                const data = await response.json();
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
        <div style={styles.page}>
            <button onClick={toggle} style={styles.themeToggle} aria-label="Alternar tema" type="button">
                {mode === 'dark' ? <SunIcon /> : <MoonIcon />}
            </button>

            <div style={styles.card}>
                <div style={styles.brand}>
                    <span style={styles.mark}>
                        <span style={styles.markDot} />
                    </span>
                    <div>
                        <div style={styles.wordmark}>SGFL</div>
                        <div style={styles.subtitle}>Gestão de Entregas</div>
                    </div>
                </div>

                <form onSubmit={handleSubmit} style={styles.form}>
                    {erro && <div style={styles.erro}>{erro}</div>}

                    <label style={styles.label}>
                        Email
                        <input
                            type="email"
                            placeholder="voce@empresa.com"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            style={styles.input}
                            required
                        />
                    </label>

                    <label style={styles.label}>
                        Senha
                        <input
                            type="password"
                            placeholder="••••••••"
                            value={senha}
                            onChange={(e) => setSenha(e.target.value)}
                            style={styles.input}
                            required
                        />
                    </label>

                    <button type="submit" style={styles.button}>Entrar</button>
                </form>
            </div>
        </div>
    );
}

function SunIcon() {
    return (
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
            <circle cx="12" cy="12" r="4" />
            <path d="M12 2v2M12 20v2M4.2 4.2l1.4 1.4M18.4 18.4l1.4 1.4M2 12h2M20 12h2M4.2 19.8l1.4-1.4M18.4 5.6l1.4-1.4" />
        </svg>
    );
}

function MoonIcon() {
    return (
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z" />
        </svg>
    );
}

function getStyles(theme) {
    return {
        page: {
            position: 'relative',
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            minHeight: '100vh',
            backgroundColor: theme.bg,
            fontFamily: "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif",
            padding: '24px',
            transition: 'background-color 0.15s ease'
        },
        themeToggle: {
            position: 'absolute',
            top: '20px',
            right: '20px',
            width: '36px',
            height: '36px',
            borderRadius: '8px',
            border: `1px solid ${theme.border}`,
            backgroundColor: theme.surface,
            color: theme.inkSoft,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            cursor: 'pointer'
        },
        card: {
            width: '100%',
            maxWidth: '380px',
            backgroundColor: theme.surface,
            border: `1px solid ${theme.border}`,
            borderRadius: '14px',
            padding: '36px 32px',
            boxShadow: '0 1px 2px rgba(0,0,0,0.04)'
        },
        brand: {
            display: 'flex',
            alignItems: 'center',
            gap: '14px',
            marginBottom: '32px'
        },
        mark: {
            width: '40px',
            height: '40px',
            borderRadius: '10px',
            backgroundColor: theme.accent,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexShrink: 0
        },
        markDot: {
            width: '10px',
            height: '10px',
            borderRadius: '50%',
            border: `2px solid ${theme.bg}`
        },
        wordmark: {
            fontFamily: "Georgia, 'Iowan Old Style', serif",
            fontSize: '22px',
            color: theme.ink,
            lineHeight: 1.1
        },
        subtitle: {
            fontSize: '13px',
            color: theme.inkSoft,
            marginTop: '2px'
        },
        form: {
            display: 'flex',
            flexDirection: 'column',
            gap: '18px'
        },
        label: {
            display: 'flex',
            flexDirection: 'column',
            gap: '6px',
            fontSize: '13px',
            color: theme.inkSoft,
            fontWeight: 500
        },
        input: {
            padding: '11px 12px',
            borderRadius: '8px',
            border: `1px solid ${theme.borderStrong}`,
            backgroundColor: theme.surfaceAlt,
            color: theme.ink,
            fontSize: '15px',
            outline: 'none'
        },
        button: {
            marginTop: '6px',
            padding: '12px',
            borderRadius: '8px',
            border: 'none',
            backgroundColor: theme.accent,
            color: theme.accentInk,
            fontSize: '15px',
            fontWeight: 600,
            cursor: 'pointer'
        },
        erro: {
            padding: '10px 12px',
            borderRadius: '8px',
            backgroundColor: theme.statuses.PENDENTE.bg,
            borderLeft: `3px solid ${theme.statuses.PENDENTE.dot}`,
            color: theme.statuses.PENDENTE.ink,
            fontSize: '13px'
        }
    };
}