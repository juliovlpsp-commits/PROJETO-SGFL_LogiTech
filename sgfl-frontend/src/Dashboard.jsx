import { useState, useEffect } from 'react';

export default function Dashboard({ token, onLogout }) {
    const [entregas, setEntregas] = useState([]);
    const [descricao, setDescricao] = useState('');
    const [enderecoDestino, setEnderecoDestino] = useState('');
    const [status, setStatus] = useState('PENDENTE');
    const [mensagem, setMensagem] = useState('');
    const [carregando, setCarregando] = useState(false);

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
        <div style={styles.container}>
            {/* Cabeçalho */}
            <header style={styles.header}>
                <h2>SGFL - Gestão de Entregas</h2>
                <button onClick={onLogout} style={styles.btnLogout}>Sair</button>
            </header>

            {mensagem && <div style={styles.alerta}>{mensagem}</div>}

            <div style={styles.grid}>
                {/* Formulário de Nova Entrega */}
                <div style={styles.card}>
                    <h3>Registar Nova Entrega</h3>
                    <form onSubmit={handleCriarEntrega} style={styles.form}>
                        <div style={styles.inputGroup}>
                            <label>Descrição do Pedido:</label>
                            <input
                                type="text"
                                value={descricao}
                                onChange={(e) => setDescricao(e.target.value)}
                                placeholder="Ex: Encomenda #1092 - Periféricos"
                                style={styles.input}
                            />
                        </div>

                        <div style={styles.inputGroup}>
                            <label>Endereço de Destino:</label>
                            <input
                                type="text"
                                value={enderecoDestino}
                                onChange={(e) => setEnderecoDestino(e.target.value)}
                                placeholder="Ex: Av. Central, 500 - Lisboa"
                                style={styles.input}
                            />
                        </div>

                        <div style={styles.inputGroup}>
                            <label>Estado Inicial:</label>
                            <select
                                value={status}
                                onChange={(e) => setStatus(e.target.value)}
                                style={styles.input}
                            >
                                <option value="PENDENTE">PENDENTE</option>
                                <option value="EM_TRANSTIO">EM TRÂNSITO</option>
                                <option value="ENTREGUE">ENTREGUE</option>
                            </select>
                        </div>

                        <button type="submit" disabled={carregando} style={styles.btnSubmit}>
                            {carregando ? 'A guardar...' : 'Registar Entrega'}
                        </button>
                    </form>
                </div>

                {/* Tabela de Entregas */}
                <div style={styles.card}>
                    <h3>Entregas Registadas</h3>
                    {entregas.length === 0 ? (
                        <p style={{ color: '#888' }}>Nenhuma entrega registada até ao momento.</p>
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
                      <span style={getBadgeStyle(item.status)}>
                        {item.status}
                      </span>
                                    </td>
                                    <td style={styles.td}>
                                        <select
                                            value={item.status}
                                            onChange={(e) => handleAtualizarStatus(item.id, e.target.value)}
                                            style={styles.selectStatus}
                                        >
                                            <option value="PENDENTE">PENDENTE</option>
                                            <option value="EM_TRANSTIO">EM TRÂNSITO</option>
                                            <option value="ENTREGUE">ENTREGUE</option>
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
    );
}

function getBadgeStyle(status) {
    const base = {
        padding: '4px 8px',
        borderRadius: '4px',
        fontSize: '12px',
        fontWeight: 'bold',
        color: '#fff'
    };
    if (status === 'ENTREGUE') return { ...base, backgroundColor: '#28a745' };
    if (status === 'EM_TRANSTIO' || status === 'EM_TRÂNSITO') return { ...base, backgroundColor: '#ffc107', color: '#000' };
    return { ...base, backgroundColor: '#dc3545' };
}

const styles = {
    container: { padding: '20px', fontFamily: 'sans-serif', backgroundColor: '#121212', minHeight: '100vh', color: '#fff' },
    header: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px', borderBottom: '1px solid #333', paddingBottom: '10px' },
    btnLogout: { padding: '8px 16px', backgroundColor: '#e53e3e', color: '#fff', border: 'none', borderRadius: '4px', cursor: 'pointer' },
    alerta: { padding: '10px', backgroundColor: '#2b303b', borderLeft: '4px solid #3182ce', marginBottom: '20px', borderRadius: '4px' },
    grid: { display: 'grid', gridTemplateColumns: '1fr 2fr', gap: '20px' },
    card: { backgroundColor: '#1e1e1e', padding: '20px', borderRadius: '8px', boxShadow: '0 2px 8px rgba(0,0,0,0.5)' },
    form: { display: 'flex', flexDirection: 'column', gap: '15px' },
    inputGroup: { display: 'flex', flexDirection: 'column', gap: '5px' },
    input: { padding: '10px', borderRadius: '4px', border: '1px solid #444', backgroundColor: '#2a2a2a', color: '#fff' },
    btnSubmit: { padding: '10px', backgroundColor: '#2b6cb0', color: '#fff', border: 'none', borderRadius: '4px', cursor: 'pointer', fontWeight: 'bold' },
    table: { width: '100%', borderCollapse: 'collapse', marginTop: '10px' },
    th: { textAlign: 'left', borderBottom: '2px solid #444', padding: '8px', color: '#aaa' },
    tr: { borderBottom: '1px solid #333' },
    td: { padding: '10px 8px' },
    selectStatus: { backgroundColor: '#2a2a2a', color: '#fff', border: '1px solid #444', padding: '4px', borderRadius: '4px' }
};