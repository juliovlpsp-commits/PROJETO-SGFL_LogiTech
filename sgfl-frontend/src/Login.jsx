import { useRef, useState } from 'react';
import api from './api';
import { useTheme } from './useTheme';

export default function Login({ onLoginSuccess }) {
    const [email, setEmail] = useState('');
    const [senha, setSenha] = useState('');
    const [erro, setErro] = useState('');
    const [carregando, setCarregando] = useState(false);

    const loginSectionRef = useRef(null);
    const { mode, toggle } = useTheme();

    const dark = mode === 'dark';

    const handleSubmit = async (event) => {
        event.preventDefault();

        if (carregando) {
            return;
        }

        setErro('');
        setCarregando(true);

        try {
            await api.post('/auth/login', {
                username: email.trim(),
                password: senha
            });

            onLoginSuccess();
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

    const irParaLogin = () => {
        loginSectionRef.current?.scrollIntoView({
            behavior: 'smooth',
            block: 'start'
        });
    };

    const styles = getStyles(dark);

    return (
        <main
            className={`sgfl-login ${dark ? 'is-dark' : 'is-light'}`}
            style={styles.page}
        >
            <button
                type="button"
                onClick={toggle}
                style={styles.themeToggle}
                aria-label="Alternar tema"
            >
                {dark ? <SunIcon /> : <MoonIcon />}
            </button>

            <div style={styles.progressRail}>
                <span style={styles.progressLabel}>
                    SGFL
                </span>
                <span style={styles.progressLine} />
                <span style={styles.progressNumber}>
                    01
                </span>
            </div>

            <section
                className="sgfl-login-panel sgfl-login-welcome"
                style={styles.welcome}
            >
                <div style={{ ...styles.orbit, ...styles.orbitOne }} />
                <div style={{ ...styles.orbit, ...styles.orbitTwo }} />
                <div style={styles.grain} />

                <div style={styles.welcomeInner}>
                    <div className="sgfl-kicker" style={styles.kicker}>
                        SISTEMA DE GESTÃO E FULFILLMENT LOGÍSTICO
                    </div>

                    <div className="sgfl-brand-row" style={styles.brandRow}>
                        <Crest size={46} />

                        <div>
                            <div style={styles.wordmark}>
                                SGFL
                            </div>
                            <div style={styles.brandSub}>
                                Gestão de Entregas
                            </div>
                        </div>
                    </div>

                    <h1 className="sgfl-hero-title" style={styles.heroTitle}>
                        A operação começa
                        <br />
                        <em>antes</em> da estrada.
                    </h1>

                    <p className="sgfl-hero-copy" style={styles.heroCopy}>
                        Um espaço único para organizar entregas,
                        acompanhar recursos, controlar pedidos e
                        manter o fluxo logístico sob controle.
                    </p>

                    <div style={styles.editorialRule}>
                        <span />
                        <span />
                    </div>

                    <div className="sgfl-hero-meta" style={styles.heroMeta}>
                        <span>
                            CLIENTES
                        </span>
                        <span>
                            PRODUTOS
                        </span>
                        <span>
                            ESTOQUE
                        </span>
                        <span>
                            ENTREGAS
                        </span>
                    </div>

                    <button
                        type="button"
                        onClick={irParaLogin}
                        className="sgfl-scroll-cta"
                        style={styles.scrollCta}
                    >
                        <span>
                            Acessar o sistema
                        </span>
                        <ArrowDownIcon />
                    </button>
                </div>

                <div style={styles.cornerNote}>
                    SGFL / 2026
                </div>
            </section>

            <section
                ref={loginSectionRef}
                className="sgfl-login-panel sgfl-login-form-panel"
                style={styles.loginPanel}
            >
                <div style={styles.loginPanelInner}>
                    <div style={styles.sectionNumber}>
                        02 / ACESSO
                    </div>

                    <div className="sgfl-login-grid" style={styles.loginGrid}>
                        <div style={styles.loginEditorial}>
                            <div style={styles.miniMark}>
                                SGFL
                            </div>

                            <h2 style={styles.loginTitle}>
                                Bem-vindo
                                <br />
                                <em>de volta.</em>
                            </h2>

                            <p style={styles.loginCopy}>
                                Entre para acessar o painel operacional
                                e continuar de onde parou.
                            </p>

                            <div style={styles.sideNote}>
                                <span style={styles.sideNoteLine} />
                                <span>
                                    Acesso seguro
                                    <br />
                                    Ambiente operacional
                                </span>
                            </div>
                        </div>

                        <div
                            className="sgfl-login-card"
                            style={styles.card}
                        >
                            <div style={styles.crestWrap}>
                                <Crest size={34} />
                            </div>

                            <div style={styles.cardTop}>
                                <span>
                                    AUTENTICAÇÃO
                                </span>

                                <span>
                                    SGFL-01
                                </span>
                            </div>

                            <form
                                onSubmit={handleSubmit}
                                style={styles.form}
                            >
                                <div style={styles.formIntro}>
                                    <h3 style={styles.formTitle}>
                                        Entrar no sistema
                                    </h3>

                                    <p style={styles.formSubtitle}>
                                        Use suas credenciais de acesso.
                                    </p>
                                </div>

                                {erro && (
                                    <div
                                        style={styles.error}
                                        role="alert"
                                    >
                                        <span style={styles.errorDot} />
                                        {erro}
                                    </div>
                                )}

                                <label style={styles.field}>
                                    <span style={styles.label}>
                                        E-mail
                                    </span>

                                    <div
                                        className="sgfl-login-field"
                                        style={styles.inputShell}
                                    >
                                        <MailIcon />
                                        <input
                                            type="email"
                                            placeholder="voce@empresa.com"
                                            value={email}
                                            onChange={(event) =>
                                                setEmail(
                                                    event.target.value
                                                )
                                            }
                                            style={styles.input}
                                            autoComplete="username"
                                            required
                                            disabled={carregando}
                                        />
                                    </div>
                                </label>

                                <label style={styles.field}>
                                    <span style={styles.label}>
                                        Senha
                                    </span>

                                    <div
                                        className="sgfl-login-field"
                                        style={styles.inputShell}
                                    >
                                        <LockIcon />
                                        <input
                                            type="password"
                                            placeholder="Sua senha"
                                            value={senha}
                                            onChange={(event) =>
                                                setSenha(
                                                    event.target.value
                                                )
                                            }
                                            style={styles.input}
                                            autoComplete="current-password"
                                            required
                                            disabled={carregando}
                                        />
                                    </div>
                                </label>

                                <button
                                    type="submit"
                                    className="sgfl-login-submit"
                                    style={{
                                        ...styles.submit,
                                        opacity: carregando ? 0.72 : 1
                                    }}
                                    disabled={carregando}
                                >
                                    <span>
                                        {carregando
                                            ? 'Autenticando...'
                                            : 'Entrar'}
                                    </span>

                                    <ArrowRightIcon />
                                </button>
                            </form>

                            <div style={styles.cardBottom}>
                                <span>
                                    SGFL
                                </span>
                                <span>
                                    Gestão de Entregas
                                </span>
                            </div>
                        </div>
                    </div>
                </div>
            </section>

            <style>
                {`
                    .sgfl-login {
                        scroll-snap-type: y mandatory;
                        scroll-behavior: smooth;
                        overflow-y: auto;
                    }

                    .sgfl-login-panel {
                        scroll-snap-align: start;
                        scroll-snap-stop: always;
                    }

                    .sgfl-login-welcome {
                        isolation: isolate;
                    }

                    .sgfl-login-welcome::before {
                        content: '';
                        position: absolute;
                        inset: 0;
                        pointer-events: none;
                        z-index: -1;
                        background:
                            linear-gradient(
                                90deg,
                                transparent 0 48%,
                                rgba(115, 31, 44, 0.12) 48.1%,
                                transparent 48.2%
                            ),
                            linear-gradient(
                                0deg,
                                transparent 0 78%,
                                rgba(115, 31, 44, 0.08) 78.1%,
                                transparent 78.2%
                            );
                    }

                    .sgfl-login.is-dark .sgfl-login-welcome::before {
                        background:
                            linear-gradient(
                                90deg,
                                transparent 0 48%,
                                rgba(244, 190, 202, 0.07) 48.1%,
                                transparent 48.2%
                            ),
                            linear-gradient(
                                0deg,
                                transparent 0 78%,
                                rgba(244, 190, 202, 0.05) 78.1%,
                                transparent 78.2%
                            );
                    }

                    .sgfl-login .sgfl-login-field:focus-within {
                        transform: translateY(-1px);
                        border-color: ${dark ? '#C85A6E' : '#7A1F2B'} !important;
                        box-shadow: 0 0 0 3px ${dark ? 'rgba(165,69,82,0.24)' : 'rgba(122,31,43,0.14)'};
                    }

                    /* Faixa vinho no topo do cartão (recortada pelas bordas arredondadas) */
                    .sgfl-login .sgfl-login-card::before {
                        content: '';
                        position: absolute;
                        top: 0;
                        left: 0;
                        right: 0;
                        height: 4px;
                        background: ${dark ? '#A54552' : '#7A1F2B'};
                        pointer-events: none;
                    }

                    .sgfl-login .sgfl-login-field:focus-within svg {
                        opacity: 1;
                        transform: translateX(2px);
                    }

                    .sgfl-login .sgfl-login-field input::placeholder {
                        color: ${dark ? 'rgba(244,233,236,0.34)' : 'rgba(42,18,25,0.34)'};
                    }

                    .sgfl-login .sgfl-scroll-cta:hover {
                        transform: translateY(-2px);
                        letter-spacing: 0.06em;
                    }

                    .sgfl-login .sgfl-scroll-cta:hover svg {
                        transform: translateY(3px);
                    }

                    .sgfl-login .sgfl-login-submit:hover:not(:disabled) {
                        transform: translateY(-2px);
                        box-shadow:
                            0 18px 38px ${dark
                    ? 'rgba(88, 22, 31, 0.34)'
                    : 'rgba(78, 24, 34, 0.16)'},
                            inset 0 1px 0 rgba(255,255,255,0.18);
                    }

                    .sgfl-login .sgfl-login-submit:hover:not(:disabled) svg {
                        transform: translateX(4px);
                    }

                    .sgfl-login .sgfl-login-submit:active:not(:disabled) {
                        transform: translateY(0) scale(0.985);
                    }

                    @keyframes sgflFadeUp {
                        from {
                            opacity: 0;
                            transform: translateY(22px);
                        }
                        to {
                            opacity: 1;
                            transform: translateY(0);
                        }
                    }

                    @keyframes sgflDrift {
                        from {
                            transform: translate3d(0, 0, 0) rotate(0deg);
                        }
                        to {
                            transform: translate3d(28px, -18px, 0) rotate(3deg);
                        }
                    }

                    @keyframes sgflPulse {
                        0%, 100% {
                            opacity: 0.46;
                        }
                        50% {
                            opacity: 0.82;
                        }
                    }

                    .sgfl-login-welcome .sgfl-kicker,
                    .sgfl-login-welcome .sgfl-brand-row,
                    .sgfl-login-welcome .sgfl-hero-title,
                    .sgfl-login-welcome .sgfl-hero-copy,
                    .sgfl-login-welcome .sgfl-hero-meta,
                    .sgfl-login-welcome .sgfl-scroll-cta {
                        animation: sgflFadeUp 0.85s both;
                    }

                    .sgfl-login-welcome .sgfl-brand-row {
                        animation-delay: 0.08s;
                    }

                    .sgfl-login-welcome .sgfl-hero-title {
                        animation-delay: 0.16s;
                    }

                    .sgfl-login-welcome .sgfl-hero-copy {
                        animation-delay: 0.25s;
                    }

                    .sgfl-login-welcome .sgfl-hero-meta {
                        animation-delay: 0.34s;
                    }

                    .sgfl-login-welcome .sgfl-scroll-cta {
                        animation-delay: 0.46s;
                    }

                    @media (max-width: 900px) {
                        .sgfl-login-panel {
                            min-height: 100svh !important;
                            height: auto !important;
                        }

                        .sgfl-login-welcome {
                            padding: 86px 58px 80px !important;
                        }

                        .sgfl-login-form-panel {
                            padding: 80px 34px !important;
                        }

                        .sgfl-login-welcome h1 {
                            font-size: clamp(48px, 11vw, 84px) !important;
                        }

                        .sgfl-login-grid {
                            grid-template-columns: 1fr !important;
                            gap: 42px !important;
                        }
                    }

                    @media (max-width: 640px) {
                        .sgfl-login-welcome {
                            padding: 78px 28px 68px !important;
                        }

                        .sgfl-login-form-panel {
                            padding: 70px 20px 50px !important;
                        }

                        .sgfl-login-progress {
                            display: none !important;
                        }
                    }

                    @media (prefers-reduced-motion: reduce) {
                        .sgfl-login,
                        .sgfl-login * {
                            scroll-behavior: auto !important;
                            animation-duration: 0.01ms !important;
                            animation-iteration-count: 1 !important;
                            transition-duration: 0.01ms !important;
                        }
                    }
                `}
            </style>
        </main>
    );
}

function getStyles(dark) {
    const palette = dark
        ? {
            ink: '#F4E9EC',
            soft: '#B79AA3',
            line: 'rgba(244,233,236,0.18)',
            lineStrong: 'rgba(244,233,236,0.32)',
            paper: '#1A0A10',
            paperSoft: 'rgba(255,255,255,0.055)',
            maroon: '#A54552',
            maroonDeep: '#6E202D',
            cream: '#FBEFF2',
            shadow: 'rgba(0,0,0,0.42)'
        }
        : {
            ink: '#2A1219',
            soft: '#7A5A64',
            line: 'rgba(42,18,25,0.14)',
            lineStrong: 'rgba(42,18,25,0.26)',
            paper: '#F7ECEF',
            paperSoft: 'rgba(255,255,255,0.66)',
            maroon: '#7A1F2B',
            maroonDeep: '#55131D',
            cream: '#FFF1F4',
            shadow: 'rgba(60,22,34,0.10)'
        };

    return {
        page: {
            position: 'relative',
            minHeight: '100svh',
            height: '100svh',
            overflowY: 'auto',
            backgroundColor: palette.paper,
            color: palette.ink,
            fontFamily:
                "Inter, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif"
        },

        themeToggle: {
            position: 'fixed',
            top: '18px',
            right: '20px',
            zIndex: 40,
            width: '40px',
            height: '40px',
            borderRadius: '50%',
            border: `1px solid ${palette.lineStrong}`,
            backgroundColor: dark
                ? 'rgba(24,20,18,0.74)'
                : 'rgba(255,250,244,0.80)',
            backdropFilter: 'blur(16px)',
            WebkitBackdropFilter: 'blur(16px)',
            color: palette.ink,
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            cursor: 'pointer',
            boxShadow: `0 10px 26px ${palette.shadow}`
        },

        progressRail: {
            position: 'fixed',
            left: '22px',
            top: '50%',
            zIndex: 30,
            transform: 'translateY(-50%)',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            gap: '9px',
            color: palette.soft,
            fontSize: '8px',
            letterSpacing: '0.16em',
            fontWeight: 800,
            textTransform: 'uppercase'
        },

        progressLabel: {
            color: palette.maroon,
            writingMode: 'vertical-rl',
            transform: 'rotate(180deg)'
        },

        progressLine: {
            width: '1px',
            height: '94px',
            backgroundColor: palette.lineStrong
        },

        progressNumber: {
            color: palette.soft
        },

        welcome: {
            position: 'relative',
            minHeight: '100svh',
            height: '100svh',
            display: 'flex',
            alignItems: 'center',
            overflow: 'hidden',
            padding: '96px 10vw 72px 11vw',
            borderBottom: `1px solid ${palette.line}`
        },

        welcomeInner: {
            position: 'relative',
            zIndex: 5,
            maxWidth: '1120px'
        },

        kicker: {
            fontSize: '10px',
            lineHeight: 1.3,
            fontWeight: 800,
            letterSpacing: '0.17em',
            color: palette.maroon,
            textTransform: 'uppercase',
            marginBottom: '24px'
        },

        brandRow: {
            display: 'flex',
            alignItems: 'center',
            gap: '13px',
            marginBottom: '58px'
        },

        mark: {
            width: '46px',
            height: '46px',
            display: 'grid',
            gridTemplateColumns: 'repeat(3, 1fr)',
            gap: '4px',
            alignItems: 'end'
        },

        markDot: {
            width: '6px',
            height: '6px',
            borderRadius: '50%',
            backgroundColor: palette.maroon
        },

        wordmark: {
            fontFamily:
                "Georgia, 'Times New Roman', serif",
            fontSize: '23px',
            fontWeight: 700,
            letterSpacing: '0.02em',
            lineHeight: 1
        },

        brandSub: {
            marginTop: '4px',
            color: palette.soft,
            fontSize: '10px',
            letterSpacing: '0.08em',
            textTransform: 'uppercase'
        },

        heroTitle: {
            margin: 0,
            maxWidth: '980px',
            fontFamily:
                "Georgia, 'Times New Roman', serif",
            fontSize: 'clamp(52px, 7.6vw, 112px)',
            lineHeight: 0.94,
            fontWeight: 400,
            letterSpacing: '-0.045em'
        },

        heroCopy: {
            maxWidth: '640px',
            margin: '34px 0 0',
            color: palette.soft,
            fontSize: '15px',
            lineHeight: 1.75
        },

        editorialRule: {
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            marginTop: '34px'
        },

        editorialRuleSpan: {
            height: '1px',
            backgroundColor: palette.lineStrong
        },

        heroMeta: {
            display: 'flex',
            flexWrap: 'wrap',
            gap: '10px 24px',
            marginTop: '28px',
            color: palette.soft,
            fontSize: '8px',
            fontWeight: 800,
            letterSpacing: '0.13em'
        },

        scrollCta: {
            marginTop: '42px',
            display: 'inline-flex',
            alignItems: 'center',
            gap: '13px',
            padding: '12px 0',
            border: 'none',
            background: 'transparent',
            color: palette.ink,
            fontSize: '11px',
            fontWeight: 800,
            letterSpacing: '0.04em',
            textTransform: 'uppercase',
            cursor: 'pointer',
            transition: 'transform 180ms ease, letter-spacing 180ms ease'
        },

        cornerNote: {
            position: 'absolute',
            right: '34px',
            bottom: '24px',
            zIndex: 5,
            color: palette.soft,
            fontSize: '8px',
            letterSpacing: '0.15em'
        },

        orbit: {
            position: 'absolute',
            zIndex: 0,
            borderRadius: '50%',
            border: `1px solid ${palette.lineStrong}`,
            animation: 'sgflPulse 4.2s ease-in-out infinite'
        },

        orbitOne: {
            width: '38vw',
            height: '38vw',
            right: '-15vw',
            top: '-9vw'
        },

        orbitTwo: {
            width: '26vw',
            height: '26vw',
            right: '8vw',
            bottom: '-14vw',
            opacity: 0.55,
            animationDelay: '0.8s'
        },

        grain: {
            position: 'absolute',
            inset: 0,
            pointerEvents: 'none',
            opacity: dark ? 0.12 : 0.2,
            backgroundImage:
                `radial-gradient(circle at 10% 20%, ${palette.maroon} 0 1px, transparent 1px)`,
            backgroundSize: '6px 6px'
        },

        loginPanel: {
            minHeight: '100svh',
            height: '100svh',
            display: 'flex',
            alignItems: 'center',
            padding: '56px 6vw 52px 6vw',
            backgroundColor: dark
                ? '#14080C'
                : '#F1E1E6',
            backgroundImage: dark
                ? 'radial-gradient(circle at 78% 30%, rgba(165, 69, 82, 0.16), transparent 42%),' +
                'radial-gradient(circle, rgba(244, 233, 236, 0.035) 0.7px, transparent 0.8px)'
                : 'radial-gradient(circle at 78% 30%, rgba(165, 69, 82, 0.12), transparent 42%)',
            backgroundSize: dark ? 'auto, 8px 8px' : 'auto'
        },

        loginPanelInner: {
            width: '100%',
            maxWidth: '1060px',
            margin: '0 auto'
        },

        sectionNumber: {
            color: palette.maroon,
            fontSize: '9px',
            fontWeight: 800,
            letterSpacing: '0.16em',
            marginBottom: '22px'
        },

        loginGrid: {
            display: 'grid',
            gridTemplateColumns:
                'minmax(240px, 1fr) minmax(340px, 430px)',
            justifyContent: 'center',
            alignItems: 'center',
            gap: '6vw'
        },

        loginEditorial: {
            alignSelf: 'center'
        },

        miniMark: {
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '7px 9px',
            borderRadius: '8px',
            border: `1px solid ${palette.lineStrong}`,
            color: palette.maroon,
            fontSize: '9px',
            letterSpacing: '0.13em',
            fontWeight: 800
        },

        loginTitle: {
            margin: '28px 0 18px',
            fontFamily:
                "Georgia, 'Times New Roman', serif",
            fontSize: 'clamp(44px, 5vw, 78px)',
            lineHeight: 0.96,
            fontWeight: 400,
            letterSpacing: '-0.04em'
        },

        loginCopy: {
            maxWidth: '410px',
            margin: 0,
            color: palette.soft,
            fontSize: '14px',
            lineHeight: 1.75
        },

        sideNote: {
            display: 'flex',
            alignItems: 'flex-start',
            gap: '13px',
            marginTop: '48px',
            color: palette.soft,
            fontSize: '9px',
            lineHeight: 1.55,
            textTransform: 'uppercase',
            letterSpacing: '0.12em'
        },

        sideNoteLine: {
            width: '32px',
            height: '1px',
            backgroundColor: palette.maroon,
            marginTop: '6px'
        },

        card: {
            position: 'relative',
            width: '100%',
            padding: '30px 28px 20px',
            overflow: 'hidden',
            border: `1px solid ${dark ? 'rgba(165,69,82,0.50)' : 'rgba(122,31,43,0.30)'}`,
            borderRadius: '18px',
            backgroundColor: dark
                ? 'rgba(30,13,20,0.90)'
                : 'rgba(255,247,249,0.92)',
            backdropFilter: 'blur(22px)',
            WebkitBackdropFilter: 'blur(22px)',
            boxShadow:
                `0 28px 80px ${palette.shadow}, 0 0 0 1px rgba(0,0,0,0.25), inset 0 1px 0 rgba(255,255,255,0.08)`
        },

        crestWrap: {
            display: 'flex',
            justifyContent: 'center',
            marginBottom: '14px'
        },

        cardTop: {
            display: 'flex',
            justifyContent: 'space-between',
            gap: '14px',
            paddingBottom: '12px',
            borderBottom: `1px solid ${palette.line}`,
            color: palette.soft,
            fontSize: '8px',
            fontWeight: 800,
            letterSpacing: '0.14em'
        },

        form: {
            display: 'flex',
            flexDirection: 'column',
            gap: '14px',
            paddingTop: '20px'
        },

        formIntro: {
            marginBottom: '3px'
        },

        formTitle: {
            margin: 0,
            fontFamily:
                "Georgia, 'Times New Roman', serif",
            fontSize: '26px',
            fontWeight: 400,
            lineHeight: 1.1
        },

        formSubtitle: {
            margin: '7px 0 0',
            color: palette.soft,
            fontSize: '11px'
        },

        error: {
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            padding: '10px 11px',
            borderRadius: '10px',
            border: `1px solid ${dark ? 'rgba(180,79,92,0.32)' : 'rgba(122,31,43,0.2)'}`,
            backgroundColor: dark
                ? 'rgba(122,31,43,0.18)'
                : 'rgba(122,31,43,0.07)',
            color: dark ? '#F1B0B8' : palette.maroonDeep,
            fontSize: '11px',
            lineHeight: 1.4
        },

        errorDot: {
            width: '5px',
            height: '5px',
            flexShrink: 0,
            borderRadius: '50%',
            backgroundColor: palette.maroon
        },

        field: {
            display: 'flex',
            flexDirection: 'column',
            gap: '7px'
        },

        label: {
            color: palette.soft,
            fontSize: '9px',
            fontWeight: 800,
            letterSpacing: '0.12em',
            textTransform: 'uppercase'
        },

        inputShell: {
            display: 'flex',
            alignItems: 'center',
            gap: '10px',
            minHeight: '44px',
            padding: '0 14px',
            border: `1px solid ${dark ? 'rgba(165,69,82,0.45)' : 'rgba(122,31,43,0.30)'}`,
            borderRadius: '12px',
            backgroundColor: dark
                ? 'rgba(165,69,82,0.06)'
                : 'rgba(255,255,255,0.70)',
            transition:
                'transform 180ms ease, border-color 180ms ease, box-shadow 180ms ease'
        },

        input: {
            width: '100%',
            minWidth: 0,
            border: 'none',
            outline: 'none',
            background: 'transparent',
            color: palette.ink,
            fontSize: '14px',
            fontFamily:
                "-apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif"
        },

        submit: {
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: '12px',
            marginTop: '6px',
            minHeight: '46px',
            padding: '0 16px 0 18px',
            border: `1px solid ${dark ? '#B4546A' : palette.maroonDeep}`,
            borderRadius: '12px',
            backgroundColor: dark ? '#862234' : palette.maroonDeep,
            color: palette.cream,
            fontSize: '11px',
            fontWeight: 800,
            letterSpacing: '0.11em',
            textTransform: 'uppercase',
            cursor: 'pointer',
            boxShadow:
                `0 12px 25px ${dark ? 'rgba(83,18,28,0.24)' : 'rgba(83,18,28,0.12)'}, inset 0 0 0 3px ${dark ? 'rgba(20,8,12,0.25)' : 'rgba(255,255,255,0.10)'}`,
            transition:
                'transform 180ms ease, box-shadow 180ms ease, filter 180ms ease'
        },

        cardBottom: {
            display: 'flex',
            justifyContent: 'space-between',
            gap: '16px',
            marginTop: '18px',
            paddingTop: '12px',
            borderTop: `1px solid ${palette.line}`,
            color: palette.soft,
            fontSize: '8px',
            letterSpacing: '0.11em',
            textTransform: 'uppercase'
        }
    };
}

/**
 * Brasão do SGFL (original): escudo vinho com uma caixa/volume ao centro.
 * Mesmo desenho do favicon, para a identidade ficar consistente.
 */
function Crest({ size = 46 }) {
    return (
        <svg
            width={size}
            height={Math.round(size * 1.16)}
            viewBox="0 0 48 56"
            fill="none"
            role="img"
            aria-label="SGFL"
        >
            <defs>
                <linearGradient
                    id="sgflCrestGradient"
                    x1="0"
                    y1="0"
                    x2="1"
                    y2="1"
                >
                    <stop offset="0" stopColor="#B84D5E" />
                    <stop offset="0.55" stopColor="#8A2536" />
                    <stop offset="1" stopColor="#5E1A27" />
                </linearGradient>
            </defs>

            <path
                d="M24 2.5 43.5 8.5V28C43.5 41.5 34.5 50.5 24 54 13.5 50.5 4.5 41.5 4.5 28V8.5L24 2.5Z"
                fill="url(#sgflCrestGradient)"
                stroke="#F4D9DF"
                strokeWidth="1.2"
                strokeLinejoin="round"
            />

            <path
                d="M24 7 40 11.8V28C40 39.6 32.6 47 24 50.2 15.4 47 8 39.6 8 28V11.8L24 7Z"
                stroke="#F4D9DF"
                strokeOpacity="0.45"
                strokeWidth="0.8"
            />

            <g
                stroke="#FBEFF2"
                strokeWidth="1.8"
                strokeLinejoin="round"
                strokeLinecap="round"
            >
                <path d="M24 17 34 22.5V33.5L24 39 14 33.5V22.5L24 17Z" />
                <path d="M14 22.5 24 28 34 22.5M24 28V39" />
            </g>
        </svg>
    );
}

function ArrowDownIcon() {
    return (
        <svg
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinecap="round"
            strokeLinejoin="round"
        >
            <path d="M12 4v15" />
            <path d="m6 13 6 6 6-6" />
        </svg>
    );
}

function ArrowRightIcon() {
    return (
        <svg
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinecap="round"
            strokeLinejoin="round"
        >
            <path d="M5 12h14" />
            <path d="m13 6 6 6-6 6" />
        </svg>
    );
}

function MailIcon() {
    return (
        <svg
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden="true"
        >
            <rect
                x="3"
                y="5"
                width="18"
                height="14"
                rx="2"
            />
            <path d="m3 7 9 6 9-6" />
        </svg>
    );
}

function LockIcon() {
    return (
        <svg
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden="true"
        >
            <rect
                x="5"
                y="10"
                width="14"
                height="10"
                rx="2"
            />
            <path d="M8 10V7a4 4 0 0 1 8 0v3" />
        </svg>
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
            strokeWidth="1.8"
            strokeLinecap="round"
        >
            <circle cx="12" cy="12" r="4" />
            <path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" />
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
            strokeWidth="1.8"
            strokeLinecap="round"
            strokeLinejoin="round"
        >
            <path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8Z" />
        </svg>
    );
}
