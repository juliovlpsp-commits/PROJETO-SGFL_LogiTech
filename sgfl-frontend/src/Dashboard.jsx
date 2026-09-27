import { useState, useEffect } from 'react';
import { useTheme } from './useTheme';

export default function Dashboard({ token, onLogout }) {
    const [entregas, setEntregas] = useState([]);
    const [descricao, setDescricao] = useState('');
    const [enderecoDestino, setEnderecoDestino] = useState('');
    const [status, setStatus] = useState('PENDENTE');
    const [mensagem, setMensagem] = useState('');
    const [carregando, setCarregando] = useState(false);
    const { theme, mode, toggle } = useTheme();
    const styles = getStyles(theme);

    const carregarEntregas = async () => {
        try {
            const response = await fetch('http://localhost:8080/api/entregas', {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });
            if (response.ok) {
                const data = await response.json();
                setEntregas(data);
            } else {
                setMensagem('Erro ao carregar lista de entregas.');
            }
        } catch (err) {
            setMensagem('Não foi possível conectar ao servidor backend.');
        }
    };

    useEffect(() => {
        carregarEntregas();
    }, []);

    // Criar nova entrega
    const handleCriarEntrega = async (e) => {
        e.preventDefault();
        if (!descricao || !enderecoDestino) {
            setMensagem('Preencha a descrição e o endereço de destino!');
            return;
        }

        setCarregando(true);
        setMensagem('');

        try {
            const response = await fetch('http://localhost:8080/api/entregas', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({
                    descricao,
                    enderecoDestino,
                    status
                })
            });

            if (response.ok) {
                setMensagem('Entrega criada com sucesso!');
                setDescricao('');
                setEnderecoDestino('');
                setStatus('PENDENTE');
                carregarEntregas();
            } else {
                setMensagem('Erro ao criar entrega.');
            }
        } catch (err) {
            setMensagem('Erro de conexão ao criar entrega.');
        } finally {
            setCarregando(false);
        }
    };

    const handleAtualizarStatus = async (id, novoStatus) => {
        try {
            const response = await fetch(`http://localhost:8080/api/entregas/${id}/status`, {
                method: 'PATCH',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({ status: novoStatus })
            });

            if (response.ok) {
                carregarEntregas();
            } else {
                setMensagem('Erro ao atualizar o estado da entrega.');
            }
        } catch (err) {
            setMensagem('Erro de conexão ao atualizar entrega.');
        }
    };

    return (
        <div style={styles.page}>
            <div style={styles.shell}>
                <header style={styles.header}>
                    <div style={styles.brand}>
                        <span style={styles.mark}><span style={styles.markDot} /></span>
                        <div>
                            <div style={styles.wordmark}>SGFL</div>
                            <div style={styles.subtitle}>Gestão de Entregas</div>
                        </div>
                    </div>
                    <div style={styles.headerActions}>
                        <button onClick={toggle} style={styles.themeToggle} aria-label="Alternar tema" type="button">
                            {mode === 'dark' ? <SunIcon /> : <MoonIcon />}
                        </button>
                        <button onClick={onLogout} style={styles.btnLogout}>Sair</button>
                    </div>
                </header>

                {mensagem && <div style={styles.alerta}>{mensagem}</div>}

                <div style={styles.grid}>
                    {/* Formulário de Nova Entrega */}
                    <div style={styles.card}>
                        <h3 style={styles.cardTitle}>Registar nova entrega</h3>
                        <form onSubmit={handleCriarEntrega} style={styles.form}>
                            <label style={styles.label}>
                                Descrição do pedido
                                <input
                                    type="text"
                                    value={descricao}
                                    onChange={(e) => setDescricao(e.target.value)}
                                    placeholder="Ex: Encomenda #1092 - Periféricos"
                                    style={styles.input}
                                />
                            </label>

                            <label style={styles.label}>
                                Endereço de destino
                                <input
                                    type="text"
                                    value={enderecoDestino}
                                    onChange={(e) => setEnderecoDestino(e.target.value)}
                                    placeholder="Ex: Av. Central, 500 - Lisboa"
                                    style={styles.input}
                                />
                            </label>

                            <label style={styles.label}>
                                Estado inicial
                                <select
                                    value={status}
                                    onChange={(e) => setStatus(e.target.value)}
                                    style={styles.input}
                                >
                                    <option value="PENDENTE">Pendente</option>
                                    <option value="EM_TRANSITO">Em trânsito</option>
                                    <option value="ENTREGUE">Entregue</option>
                                </select>
                            </label>

                            <button type="submit" disabled={carregando} style={styles.btnSubmit}>
                                {carregando ? 'A guardar…' : 'Registar entrega'}
                            </button>
                        </form>
                    </div>

                    {/* Tabela de Entregas */}
                    <div style={styles.card}>
                        <h3 style={styles.cardTitle}>Entregas registadas</h3>
                        {entregas.length === 0 ? (
                            <p style={styles.empty}>Ainda não há entregas por aqui. Comece pelo formulário ao lado.</p>
                        ) : (
                            <table style={styles.table}>
                                <thead>
                                <tr>
                                    <th style={styles.th}>ID</th>
                                    <th style={styles.th}>Descrição</th>
                                    <th style={styles.th}>Destino</th>
                                    <th style={styles.th}>Estado</th>
                                    <th style={styles.th}>Ações</th>
                                </tr>
                                </thead>
                                <tbody>
                                {entregas.map((item) => (
                                    <tr key={item.id} style={styles.tr}>
                                        <td style={styles.td}>#{item.id}</td>
                                        <td style={styles.td}>{item.descricao}</td>
                                        <td style={styles.td}>{item.enderecoDestino}</td>
                                        <td style={styles.td}>
                                            <StatusBadge status={item.status} theme={theme} />
                                        </td>
                                        <td style={styles.td}>
                                            <select
                                                value={item.status}
                                                onChange={(e) => handleAtualizarStatus(item.id, e.target.value)}
                                                style={styles.selectStatus}
                                            >
                                                <option value="PENDENTE">Pendente</option>
                                                <option value="EM_TRANSITO">Em trânsito</option>
                                                <option value="ENTREGUE">Entregue</option>
                                            </select>
                                        </td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
}

function StatusBadge({ status, theme }) {
    const tone = theme.statuses[status] || theme.statuses.PENDENTE;
    const label = getStatusLabel(status);
    return (
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', padding: '4px 10px', borderRadius: '999px', fontSize: '12px', fontWeight: 600, backgroundColor: tone.bg, color: tone.ink }}>
            <span style={{ width: '6px', height: '6px', borderRadius: '50%', backgroundColor: tone.dot }} />
            {label}
        </span>
    );
}

function getStatusLabel(status) {
    if (status === 'ENTREGUE') return 'Entregue';
    if (status === 'EM_TRANSITO') return 'Em trânsito';
    if (status === 'PENDENTE') return 'Pendente';
    return status;
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
            minHeight: '100vh',
            backgroundColor: theme.bg,
            fontFamily: "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif",
            color: theme.ink,
            padding: '32px 24px',
            transition: 'background-color 0.15s ease'
        },
        shell: {
            width: '100%',
            margin: '0 auto'
        },
        header: {
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginBottom: '28px'
        },
        brand: {
            display: 'flex',
            alignItems: 'center',
            gap: '14px'
        },
        headerActions: {
            display: 'flex',
            alignItems: 'center',
            gap: '10px'
        },
        mark: {
            width: '36px',
            height: '36px',
            borderRadius: '9px',
            backgroundColor: theme.accent,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexShrink: 0
        },
        markDot: {
            width: '9px',
            height: '9px',
            borderRadius: '50%',
            border: `2px solid ${theme.bg}`
        },
        wordmark: {
            fontFamily: "Georgia, 'Iowan Old Style', serif",
            fontSize: '19px',
            lineHeight: 1.1
        },
        subtitle: {
            fontSize: '12px',
            color: theme.inkSoft,
            marginTop: '2px'
        },
        themeToggle: {
            width: '34px',
            height: '34px',
            borderRadius: '8px',
            border: `1px solid ${theme.border}`,
            backgroundColor: theme.surface,
            color: theme.inkSoft,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            cursor: 'pointer'
        },
        btnLogout: {
            padding: '8px 16px',
            backgroundColor: 'transparent',
            color: theme.statuses.PENDENTE.ink,
            border: `1px solid ${theme.border}`,
            borderRadius: '8px',
            cursor: 'pointer',
            fontSize: '13px',
            fontWeight: 600
        },
        alerta: {
            padding: '12px 16px',
            backgroundColor: theme.surface,
            border: `1px solid ${theme.border}`,
            borderLeft: `3px solid ${theme.accent}`,
            marginBottom: '20px',
            borderRadius: '8px',
            fontSize: '14px'
        },
        grid: {
            display: 'grid',
            gridTemplateColumns: '340px 1fr',
            gap: '20px',
            alignItems: 'start'
        },
        card: {
            backgroundColor: theme.surface,
            border: `1px solid ${theme.border}`,
            padding: '24px',
            borderRadius: '12px'
        },
        cardTitle: {
            fontFamily: "Georgia, 'Iowan Old Style', serif",
            fontSize: '17px',
            fontWeight: 400,
            margin: '0 0 18px 0'
        },
        form: { display: 'flex', flexDirection: 'column', gap: '16px' },
        label: {
            display: 'flex',
            flexDirection: 'column',
            gap: '6px',
            fontSize: '13px',
            color: theme.inkSoft,
            fontWeight: 500
        },
        input: {
            padding: '10px 12px',
            borderRadius: '8px',
            border: `1px solid ${theme.borderStrong}`,
            backgroundColor: theme.surfaceAlt,
            color: theme.ink,
            fontSize: '14px'
        },
        btnSubmit: {
            marginTop: '4px',
            padding: '11px',
            backgroundColor: theme.accent,
            color: theme.accentInk,
            border: 'none',
            borderRadius: '8px',
            cursor: 'pointer',
            fontWeight: 600,
            fontSize: '14px'
        },
        empty: { color: theme.inkSoft, fontSize: '14px', lineHeight: 1.5 },
        table: { width: '100%', borderCollapse: 'collapse', marginTop: '4px' },
        th: {
            textAlign: 'left',
            borderBottom: `1px solid ${theme.border}`,
            padding: '8px 10px',
            color: theme.inkSoft,
            fontSize: '12px',
            fontWeight: 600
        },
        tr: { borderBottom: `1px solid ${theme.border}` },
        td: { padding: '12px 10px', fontSize: '14px' },
        selectStatus: {
            backgroundColor: theme.surfaceAlt,
            color: theme.ink,
            border: `1px solid ${theme.borderStrong}`,
            padding: '6px 8px',
            borderRadius: '8px',
            fontSize: '13px'
        }
    };
}