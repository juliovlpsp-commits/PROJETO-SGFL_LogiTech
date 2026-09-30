import { useState } from 'react';
import api from './api';
import { useTheme } from './useTheme';

export default function Login({ onLoginSuccess }) {
    const [email, setEmail] = useState('');
    const [senha, setSenha] = useState('');
    const [erro, setErro] = useState('');
    const [carregando, setCarregando] = useState(false);

    const { theme, mode, toggle } = useTheme();
    const styles = getStyles(theme);

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (carregando) {
            return;
        }

        setErro('');
        setCarregando(true);

        try {
            const response = await api.post('/auth/login', {
                username: email.trim(),
                password: senha
            });

            const jwtToken =
                response.data?.token ||
                response.data?.jwt ||
                response.data;

            if (
                typeof jwtToken !== 'string' ||
                jwtToken.trim() === ''
            ) {
                setErro(
                    'Token não retornado corretamente pelo servidor.'
                );
                return;
            }

            onLoginSuccess(jwtToken);

        } catch (error) {

            if (error.response?.status === 401) {
                setErro('Credenciais inválidas.');
            } else if (error.response?.status === 429) {
                setErro(
                    'Muitas tentativas de login. Aguarde alguns instantes e tente novamente.'
                );
            } else if (error.response?.data?.message) {
                setErro(error.response.data.message);
            } else {
                setErro(
                    'Não foi possível conectar ao servidor.'
                );
            }

        } finally {
            setCarregando(false);
        }
    };

    return (
        <div style={styles.page}>

            <button
                onClick={toggle}
                style={styles.themeToggle}
                aria-label="Alternar tema"
                type="button"
            >
                {mode === 'dark' ? <SunIcon /> : <MoonIcon />}
            </button>

            <div style={styles.card}>

                <div style={styles.brand}>
                    <span style={styles.mark}>
                        <span style={styles.markDot} />
                    </span>

                    <div>
                        <div style={styles.wordmark}>
                            SGFL
                        </div>

                        <div style={styles.subtitle}>
                            Gestão de Entregas
                        </div>
                    </div>
                </div>

                <form
                    onSubmit={handleSubmit}
                    style={styles.form}
                >

                    {erro && (
                        <div
                            style={styles.erro}
                            role="alert"
                        >
                            {erro}
                        </div>
                    )}

                    <label style={styles.label}>
                        Email

                        <input
                            type="email"
                            placeholder="voce@empresa.com"
                            value={email}
                            onChange={(e) =>
                                setEmail(e.target.value)
                            }
                            style={styles.input}
                            autoComplete="username"
                            required
                            disabled={carregando}
                        />
                    </label>

                    <label style={styles.label}>
                        Senha

                        <input
                            type="password"
                            placeholder="••••••••"
                            value={senha}
                            onChange={(e) =>
                                setSenha(e.target.value)
                            }
                            style={styles.input}
                            autoComplete="current-password"
                            required
                            disabled={carregando}
                        />
                    </label>

                    <button
                        type="submit"
                        style={{
                            ...styles.button,
                            opacity: carregando ? 0.7 : 1
                        }}
                        disabled={carregando}
                    >
                        {carregando
                            ? 'Entrando...'
                            : 'Entrar'}
                    </button>

                </form>
            </div>
        </div>
    );
}

function SunIcon() {
    return (
        <svg
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
        >
            <circle
                cx="12"
                cy="12"
                r="4"
            />

            <path
                d="M12 2v2M12 20v2M4.2 4.2l1.4 1.4M18.4 18.4l1.4 1.4M2 12h2M20 12h2M4.2 19.8l1.4-1.4M18.4 5.6l1.4-1.4"
            />
        </svg>
    );
}

function MoonIcon() {
    return (
        <svg
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
        >
            <path
                d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z"
            />
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
            fontFamily:
                "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif",
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
            fontFamily:
                "Georgia, 'Iowan Old Style', serif",
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
            borderLeft:
                `3px solid ${theme.statuses.PENDENTE.dot}`,
            color: theme.statuses.PENDENTE.ink,
            fontSize: '13px'
        }
    };
}