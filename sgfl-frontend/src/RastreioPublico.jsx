import { useState } from 'react';
import api from './api';
import { useTheme } from './useTheme';

export default function RastreioPublico({ codigoInicial = '' }) {
    const [codigo, setCodigo] = useState(codigoInicial);
    const [entrega, setEntrega] = useState(null);
    const [erro, setErro] = useState('');
    const [carregando, setCarregando] = useState(false);

    // Variante global (vinho/neutro) para o portal acompanhar o tema.
    const { variante } = useTheme();

    const consultar = async (event) => {
        event.preventDefault();
        setCarregando(true);
        setErro('');
        setEntrega(null);
        try {
            const response = await api.get(`/rastreio/${codigo.trim().toUpperCase()}`);
            setEntrega(response.data);
        } catch (error) {
            setErro(error.response?.data?.message || 'Código de rastreio não encontrado.');
        } finally {
            setCarregando(false);
        }
    };

    const status = entrega?.status || '';
    const etapas = [
        ['PENDENTE', 'Pedido registrado'],
        ['EM_TRANSITO', 'Em trânsito'],
        ['ENTREGUE', 'Entrega concluída']
    ];

    return (
        <main className={`sgfl-rastreio-page sgfl-tema-${variante}`}>
            <div className="sgfl-rastreio-glow" />
            <section className="sgfl-rastreio-shell">
                <div className="sgfl-rastreio-brand">SGFL</div>
                <div className="sgfl-rastreio-kicker">PORTAL DO CLIENTE / RASTREIO</div>
                <h1>Acompanhe sua <em>entrega.</em></h1>
                <p>Informe o código de rastreio para consultar o status operacional da entrega.</p>

                <form className="sgfl-rastreio-form" onSubmit={consultar}>
                    <input
                        value={codigo}
                        onChange={(event) => setCodigo(event.target.value)}
                        placeholder="Ex.: SGFL-1A2B3C4D5E6F"
                        aria-label="Código de rastreio"
                        required
                    />
                    <button type="submit" disabled={carregando}>
                        {carregando ? 'Consultando...' : 'Consultar'}
                    </button>
                </form>

                {erro && <div className="sgfl-rastreio-error" role="alert">{erro}</div>}

                {entrega && (
                    <section className="sgfl-rastreio-result">
                        <div className="sgfl-rastreio-result-top">
                            <div>
                                <span>CÓDIGO</span>
                                <strong>{entrega.codigoRastreio}</strong>
                            </div>
                            <div className="sgfl-rastreio-status">{status.replace('_', ' ')}</div>
                        </div>

                        <div className="sgfl-rastreio-line">
                            {etapas.map(([value, label], index) => {
                                const atualIndex = status === 'CANCELADA' ? -1 : status === 'PENDENTE' ? 0 : status === 'EM_TRANSITO' ? 1 : status === 'ENTREGUE' ? 2 : -1;
                                return (
                                    <div className={`sgfl-rastreio-step ${index <= atualIndex ? 'active' : ''}`} key={value}>
                                        <span />
                                        <strong>{label}</strong>
                                        {index < etapas.length - 1 && <i />}
                                    </div>
                                );
                            })}
                        </div>

                        <div className="sgfl-rastreio-info">
                            <div><span>Origem</span><strong>{entrega.origem || '—'}</strong></div>
                            <div><span>Destino</span><strong>{entrega.destino}</strong></div>
                            <div><span>Veículo</span><strong>{entrega.veiculo || 'A definir'}</strong></div>
                            <div><span>Motorista</span><strong>{entrega.motorista || 'A definir'}</strong></div>
                        </div>
                    </section>
                )}
            </section>
        </main>
    );
}
