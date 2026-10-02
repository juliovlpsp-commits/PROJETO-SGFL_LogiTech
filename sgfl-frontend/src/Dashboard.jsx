import { useCallback, useEffect, useState } from 'react';
import { useTheme } from './useTheme';
import CadastroRecursos from './CadastroRecursos';
import api from './api';

const TAMANHO_PAGINA = 10;

export default function Dashboard({ onLogout }) {

    const [entregas, setEntregas] = useState([]);
    const [veiculos, setVeiculos] = useState([]);
    const [motoristas, setMotoristas] = useState([]);

    const [pagina, setPagina] = useState(0);
    const [totalPaginas, setTotalPaginas] = useState(0);

    const [descricao, setDescricao] = useState('');
    const [enderecoOrigem, setEnderecoOrigem] = useState('');
    const [enderecoDestino, setEnderecoDestino] = useState('');
    const [pesoCargaKg, setPesoCargaKg] = useState('');

    const [mensagem, setMensagem] = useState('');
    const [tipoMensagem, setTipoMensagem] = useState('info');

    const [carregando, setCarregando] = useState(false);
    const [acaoEmAndamento, setAcaoEmAndamento] = useState(null);

    const [entregaAlocacao, setEntregaAlocacao] = useState(null);
    const [veiculoSelecionado, setVeiculoSelecionado] = useState('');
    const [motoristaSelecionado, setMotoristaSelecionado] = useState('');

    const [carregandoRecursos, setCarregandoRecursos] = useState(false);
    const [mostrarRecursos, setMostrarRecursos] = useState(false);

    const {
        theme: baseTheme,
        mode,
        toggle
    } = useTheme();

    const theme = mode === 'dark'
        ? {
            ...baseTheme,
            bg: '#14080C',
            surface: 'rgba(30, 13, 20, 0.84)',
            surfaceAlt: 'rgba(39, 18, 27, 0.76)',
            border: 'rgba(244, 233, 236, 0.13)',
            borderStrong: 'rgba(165, 69, 82, 0.48)',
            ink: '#F4E9EC',
            inkSoft: '#B79AA3',
            accent: '#A54552',
            accentInk: '#FFF1F4',
            danger: '#D86A78',
            backgroundImage:
                'radial-gradient(circle at 12% 8%, rgba(165, 69, 82, 0.16), transparent 27%),' +
                'radial-gradient(circle at 88% 22%, rgba(244, 233, 236, 0.045), transparent 25%),' +
                'radial-gradient(circle, rgba(244, 233, 236, 0.035) 0.7px, transparent 0.8px),' +
                'linear-gradient(135deg, #14080C 0%, #1B0A11 48%, #0E0508 100%)',
            backgroundSize:
                'auto, auto, 8px 8px, auto',
            statuses: {
                ...baseTheme.statuses,
                PENDENTE: {
                    bg: 'rgba(165, 69, 82, 0.12)',
                    ink: '#D99AA3',
                    dot: '#A54552'
                },
                EM_TRANSITO: {
                    bg: 'rgba(194, 128, 57, 0.16)',
                    ink: '#E6BD7B',
                    dot: '#D6A04B'
                },
                ENTREGUE: {
                    bg: 'rgba(79, 122, 88, 0.16)',
                    ink: '#9FC4A6',
                    dot: '#5D9466'
                },
                CANCELADA: {
                    bg: 'rgba(142, 120, 128, 0.14)',
                    ink: '#C9B3BA',
                    dot: '#8E7880'
                }
            }
        }
        : baseTheme;

    const styles = getStyles(theme);

    const request = useCallback(
        async (
            url,
            options = {}
        ) => {
            try {
                const response = await api.request({
                    url,
                    method: options.method || 'GET',
                    data: options.body,
                    headers: {
                        ...(options.body ? { 'Content-Type': 'application/json' } : {}),
                        ...(options.headers || {})
                    }
                });
                return response.data;
            } catch (error) {
                if (error.response) {
                    error.status = error.response.status;
                    error.data = error.response.data;
                    error.message = error.response.data?.message || error.message;
                } else {
                    error.code = 'NETWORK_ERROR';
                    error.message = 'Não foi possível conectar ao servidor.';
                }
                throw error;
            }
        },
        []
    );

    const mostrarMensagem = useCallback(
        (
            texto,
            tipo = 'info'
        ) => {

            setMensagem(texto);
            setTipoMensagem(tipo);
        },
        []
    );

    const tratarErro = useCallback(
        (
            error,
            mensagemPadrao
        ) => {

            console.error(
                'SGFL - erro na operação:',
                error
            );

            if (
                error?.code ===
                'NETWORK_ERROR'
            ) {

                mostrarMensagem(
                    'Não foi possível conectar ao backend. Verifique se o Spring Boot está rodando na porta 8080.',
                    'erro'
                );

                return;
            }

            if (
                error?.status === 401
            ) {

                mostrarMensagem(
                    'Sua sessão expirou. Faça login novamente.',
                    'erro'
                );

                onLogout();

                return;
            }

            if (
                error?.status === 403
            ) {

                mostrarMensagem(
                    'Você não tem permissão para realizar essa operação.',
                    'erro'
                );

                return;
            }

            if (
                error?.status === 400 ||
                error?.status === 409 ||
                error?.status === 404
            ) {

                mostrarMensagem(
                    error.message ||
                    mensagemPadrao,
                    'erro'
                );

                return;
            }

            mostrarMensagem(
                error?.message ||
                mensagemPadrao,
                'erro'
            );
        },
        [
            mostrarMensagem,
            onLogout
        ]
    );

    const carregarEntregas = useCallback(
        async (
            paginaAlvo = 0
        ) => {

            try {

                const data =
                    await request(
                        `/entregas?page=${paginaAlvo}&size=${TAMANHO_PAGINA}`
                    );

                setEntregas(
                    Array.isArray(
                        data?.content
                    )
                        ? data.content
                        : []
                );

                setPagina(
                    Number(
                        data?.number ??
                        paginaAlvo
                    )
                );

                setTotalPaginas(
                    Number(
                        data?.totalPages ??
                        0
                    )
                );

            } catch (error) {

                tratarErro(
                    error,
                    'Não foi possível carregar as entregas.'
                );
            }
        },
        [
            request,
            tratarErro
        ]
    );

    const carregarRecursos = useCallback(
        async () => {

            setCarregandoRecursos(true);

            try {

                const [
                    veiculosData,
                    motoristasData
                ] = await Promise.all([
                    request('/veiculos'),
                    request('/motoristas')
                ]);

                setVeiculos(
                    Array.isArray(
                        veiculosData
                    )
                        ? veiculosData
                        : []
                );

                setMotoristas(
                    Array.isArray(
                        motoristasData
                    )
                        ? motoristasData
                        : []
                );

            } catch (error) {

                tratarErro(
                    error,
                    'Não foi possível carregar veículos e motoristas.'
                );

            } finally {

                setCarregandoRecursos(
                    false
                );
            }
        },
        [
            request,
            tratarErro
        ]
    );

    useEffect(() => {

        carregarEntregas(0);
        carregarRecursos();

    }, [
        carregarEntregas,
        carregarRecursos
    ]);

    const handleCriarEntrega =
        async (
            event
        ) => {

            event.preventDefault();

            if (
                !descricao.trim()
            ) {

                mostrarMensagem(
                    'Informe a descrição da entrega.',
                    'erro'
                );

                return;
            }

            if (
                !enderecoDestino.trim()
            ) {

                mostrarMensagem(
                    'Informe o endereço de destino.',
                    'erro'
                );

                return;
            }

            const peso =
                Number(pesoCargaKg);

            if (
                !Number.isFinite(peso) ||
                peso <= 0
            ) {

                mostrarMensagem(
                    'Informe um peso maior que zero.',
                    'erro'
                );

                return;
            }

            setCarregando(true);
            setMensagem('');

            try {

                await request(
                    '/entregas',
                    {
                        method: 'POST',
                        body: JSON.stringify({
                            descricao:
                                descricao.trim(),

                            enderecoOrigem:
                                enderecoOrigem.trim() ||
                                null,

                            enderecoDestino:
                                enderecoDestino.trim(),

                            pesoCargaKg:
                            peso,

                            status:
                                'PENDENTE'
                        })
                    }
                );

                setDescricao('');
                setEnderecoOrigem('');
                setEnderecoDestino('');
                setPesoCargaKg('');

                mostrarMensagem(
                    'Entrega criada com sucesso.',
                    'sucesso'
                );

                await carregarEntregas(0);

            } catch (error) {

                tratarErro(
                    error,
                    'Não foi possível criar a entrega.'
                );

            } finally {

                setCarregando(false);
            }
        };

    const handleCancelar =
        async (
            id
        ) => {

            if (
                !window.confirm(
                    `Deseja cancelar a entrega #${id}?`
                )
            ) {
                return;
            }

            setAcaoEmAndamento(
                `cancelar-${id}`
            );

            setMensagem('');

            try {

                await request(
                    `/entregas/${id}/status`,
                    {
                        method: 'PATCH',
                        body: JSON.stringify({
                            status:
                                'CANCELADA'
                        })
                    }
                );

                await carregarEntregas(
                    pagina
                );

                mostrarMensagem(
                    `Entrega #${id} cancelada com sucesso.`,
                    'sucesso'
                );

            } catch (error) {

                tratarErro(
                    error,
                    'Não foi possível cancelar a entrega.'
                );

            } finally {

                setAcaoEmAndamento(
                    null
                );
            }
        };

    const handleFinalizar =
        async (
            id
        ) => {

            if (
                !window.confirm(
                    `Deseja finalizar a entrega #${id}?`
                )
            ) {
                return;
            }

            setAcaoEmAndamento(
                `finalizar-${id}`
            );

            setMensagem('');

            try {

                const data =
                    await request(
                        `/entregas/${id}/finalizar`,
                        {
                            method: 'PUT'
                        }
                    );

                /*
                 * Atualizamos a linha imediatamente.
                 * Depois recarregamos a página para manter
                 * o frontend sincronizado com o banco.
                 */
                setEntregas(
                    atual =>
                        atual.map(
                            item =>
                                item.id === id
                                    ? data
                                    : item
                        )
                );

                await carregarEntregas(
                    pagina
                );

                mostrarMensagem(
                    `Entrega #${id} finalizada com sucesso. Status: ENTREGUE.`,
                    'sucesso'
                );

            } catch (error) {

                tratarErro(
                    error,
                    'Não foi possível finalizar a entrega.'
                );

            } finally {

                setAcaoEmAndamento(
                    null
                );
            }
        };

    const handleExcluir =
        async (
            id
        ) => {

            if (
                !window.confirm(
                    `Deseja excluir a entrega #${id}? Esta ação não pode ser desfeita.`
                )
            ) {
                return;
            }

            setAcaoEmAndamento(
                `excluir-${id}`
            );

            setMensagem('');

            try {

                await request(
                    `/entregas/${id}`,
                    {
                        method: 'DELETE'
                    }
                );

                let paginaAtual =
                    pagina;

                /*
                 * Se excluímos a última linha de uma
                 * página que não é a primeira, voltamos
                 * uma página.
                 */
                if (
                    entregas.length === 1 &&
                    pagina > 0
                ) {
                    paginaAtual =
                        pagina - 1;
                }

                await carregarEntregas(
                    paginaAtual
                );

                mostrarMensagem(
                    `Entrega #${id} excluída com sucesso.`,
                    'sucesso'
                );

            } catch (error) {

                tratarErro(
                    error,
                    'Não foi possível excluir a entrega.'
                );

            } finally {

                setAcaoEmAndamento(
                    null
                );
            }
        };

    const abrirAlocacao =
        async (
            entrega
        ) => {

            if (
                entrega.status !==
                'PENDENTE'
            ) {
                mostrarMensagem(
                    'Somente entregas PENDENTES podem ser alocadas.',
                    'erro'
                );

                return;
            }

            setEntregaAlocacao(
                entrega
            );

            setVeiculoSelecionado('');
            setMotoristaSelecionado('');

            setMensagem('');

            await carregarRecursos();
        };

    const fecharAlocacao =
        () => {

            if (
                acaoEmAndamento !==
                null
            ) {
                return;
            }

            setEntregaAlocacao(
                null
            );

            setVeiculoSelecionado('');
            setMotoristaSelecionado('');
        };

    const handleAlocar =
        async (
            event
        ) => {

            event.preventDefault();

            if (
                !entregaAlocacao
            ) {
                return;
            }

            if (
                !veiculoSelecionado
            ) {

                mostrarMensagem(
                    'Selecione um veículo.',
                    'erro'
                );

                return;
            }

            if (
                !motoristaSelecionado
            ) {

                mostrarMensagem(
                    'Selecione um motorista.',
                    'erro'
                );

                return;
            }

            const id =
                entregaAlocacao.id;

            setAcaoEmAndamento(
                `alocar-${id}`
            );

            setMensagem('');

            try {

                await request(
                    `/entregas/${id}/alocar?veiculoId=${Number(
                        veiculoSelecionado
                    )}&motoristaId=${Number(
                        motoristaSelecionado
                    )}`,
                    {
                        method: 'PUT'
                    }
                );

                setEntregaAlocacao(
                    null
                );

                setVeiculoSelecionado('');
                setMotoristaSelecionado('');

                await carregarEntregas(
                    pagina
                );

                mostrarMensagem(
                    `Entrega #${id} alocada com sucesso. Status: EM_TRANSITO.`,
                    'sucesso'
                );

            } catch (error) {

                tratarErro(
                    error,
                    'Não foi possível alocar a entrega.'
                );

            } finally {

                setAcaoEmAndamento(
                    null
                );
            }
        };

    const renderAcoes =
        (
            item
        ) => {

            const emAndamento =
                acaoEmAndamento !==
                null;

            const cancelar =
                acaoEmAndamento ===
                `cancelar-${item.id}`;

            const finalizar =
                acaoEmAndamento ===
                `finalizar-${item.id}`;

            const excluir =
                acaoEmAndamento ===
                `excluir-${item.id}`;

            const alocar =
                acaoEmAndamento ===
                `alocar-${item.id}`;

            return (
                <div
                    style={
                        styles.actions
                    }
                >

                    {item.status ===
                        'PENDENTE' && (
                            <button
                                type="button"
                                disabled={
                                    emAndamento
                                }
                                onClick={() =>
                                    abrirAlocacao(
                                        item
                                    )
                                }
                                style={
                                    emAndamento
                                        ? styles.btnDisabled
                                        : styles.btnPrimary
                                }
                            >
                                {alocar
                                    ? 'Alocando...'
                                    : 'Alocar'}
                            </button>
                        )}

                    {(
                        item.status ===
                        'PENDENTE' ||
                        item.status ===
                        'EM_TRANSITO'
                    ) && (
                        <button
                            type="button"
                            disabled={
                                emAndamento
                            }
                            onClick={() =>
                                handleCancelar(
                                    item.id
                                )
                            }
                            style={
                                emAndamento
                                    ? styles.btnDisabled
                                    : styles.btnCancel
                            }
                        >
                            {cancelar
                                ? 'Cancelando...'
                                : 'Cancelar'}
                        </button>
                    )}

                    {item.status ===
                        'EM_TRANSITO' && (
                            <button
                                type="button"
                                disabled={
                                    emAndamento
                                }
                                onClick={() =>
                                    handleFinalizar(
                                        item.id
                                    )
                                }
                                style={
                                    emAndamento
                                        ? styles.btnDisabled
                                        : styles.btnFinalize
                                }
                            >
                                {finalizar
                                    ? 'Finalizando...'
                                    : 'Finalizar'}
                            </button>
                        )}

                    <button
                        type="button"
                        disabled={
                            emAndamento
                        }
                        onClick={() =>
                            handleExcluir(
                                item.id
                            )
                        }
                        style={
                            emAndamento
                                ? styles.btnDisabled
                                : styles.btnDelete
                        }
                    >
                        {excluir
                            ? 'Excluindo...'
                            : 'Excluir'}
                    </button>

                </div>
            );
        };

    return (
        <div
            className={
                mode === 'dark'
                    ? 'sgfl-page sgfl-page-dark'
                    : 'sgfl-page'
            }
            style={
                styles.page
            }
        >

            {mode === 'dark' && (
                <>
                    <div className="sgfl-ambient sgfl-ambient-a" />
                    <div className="sgfl-ambient sgfl-ambient-b" />
                    <div className="sgfl-ambient sgfl-ambient-c" />
                    <div className="sgfl-grid-overlay" />
                </>
            )}

            <div
                style={
                    styles.shell
                }
            >

                <header
                    style={
                        styles.header
                    }
                >

                    <div
                        style={
                            styles.brand
                        }
                    >

                        <div
                            style={
                                styles.brandIcon
                            }
                        >
                            <BoxIcon />
                        </div>

                        <div>

                            <div
                                style={
                                    styles.wordmark
                                }
                            >
                                SGFL
                            </div>

                            <div
                                style={
                                    styles.subtitle
                                }
                            >
                                Gestão de Entregas
                            </div>

                        </div>

                    </div>

                    <div
                        style={
                            styles.headerActions
                        }
                    >

                        <button
                            type="button"
                            onClick={() =>
                                setMostrarRecursos(
                                    true
                                )
                            }
                            style={
                                styles.btnResources
                            }
                        >
                            <SettingsIcon />
                            Gerenciar recursos
                        </button>

                        <button
                            type="button"
                            onClick={toggle}
                            style={
                                styles.themeToggle
                            }
                            aria-label="Alternar tema"
                        >
                            {mode ===
                            'dark'
                                ? <SunIcon />
                                : <MoonIcon />}
                        </button>

                        <button
                            type="button"
                            onClick={
                                onLogout
                            }
                            style={
                                styles.btnLogout
                            }
                        >
                            Sair
                        </button>

                    </div>

                </header>

                {mensagem && (
                    <div
                        style={
                            tipoMensagem ===
                            'erro'
                                ? styles.alertaErro
                                : styles.alertaSucesso
                        }
                    >
                        {mensagem}
                    </div>
                )}

                <section
                    style={
                        styles.summaryGrid
                    }
                >

                    <SummaryCard
                        theme={theme}
                        icon={
                            <UserIcon />
                        }
                        label="Motoristas"
                        value={
                            motoristas.length
                        }
                        description="cadastrados"
                    />

                    <SummaryCard
                        theme={theme}
                        icon={
                            <TruckIcon />
                        }
                        label="Veículos"
                        value={
                            veiculos.length
                        }
                        description="cadastrados"
                    />

                    <SummaryCard
                        theme={theme}
                        icon={
                            <PackageIcon />
                        }
                        label="Entregas"
                        value={
                            entregas.length
                        }
                        description="nesta página"
                    />

                </section>

                <div
                    style={
                        styles.grid
                    }
                >

                    <section
                        style={
                            styles.card
                        }
                    >

                        <h3
                            style={
                                styles.cardTitle
                            }
                        >
                            Registrar nova entrega
                        </h3>

                        <form
                            onSubmit={
                                handleCriarEntrega
                            }
                            style={
                                styles.form
                            }
                        >

                            <label
                                style={
                                    styles.label
                                }
                            >
                                Descrição

                                <input
                                    type="text"
                                    value={
                                        descricao
                                    }
                                    onChange={
                                        event =>
                                            setDescricao(
                                                event.target.value
                                            )
                                    }
                                    placeholder="Ex.: Encomenda de equipamentos"
                                    style={
                                        styles.input
                                    }
                                    disabled={
                                        carregando
                                    }
                                />
                            </label>

                            <label
                                style={
                                    styles.label
                                }
                            >
                                Origem

                                <input
                                    type="text"
                                    value={
                                        enderecoOrigem
                                    }
                                    onChange={
                                        event =>
                                            setEnderecoOrigem(
                                                event.target.value
                                            )
                                    }
                                    placeholder="Centro de distribuição"
                                    style={
                                        styles.input
                                    }
                                    disabled={
                                        carregando
                                    }
                                />
                            </label>

                            <label
                                style={
                                    styles.label
                                }
                            >
                                Destino

                                <input
                                    type="text"
                                    value={
                                        enderecoDestino
                                    }
                                    onChange={
                                        event =>
                                            setEnderecoDestino(
                                                event.target.value
                                            )
                                    }
                                    placeholder="Av. Central, 500"
                                    style={
                                        styles.input
                                    }
                                    disabled={
                                        carregando
                                    }
                                />
                            </label>

                            <label
                                style={
                                    styles.label
                                }
                            >
                                Peso da carga

                                <div
                                    style={
                                        styles.inputWithSuffix
                                    }
                                >
                                    <input
                                        type="number"
                                        min="0.01"
                                        step="0.01"
                                        value={
                                            pesoCargaKg
                                        }
                                        onChange={
                                            event =>
                                                setPesoCargaKg(
                                                    event.target.value
                                                )
                                        }
                                        placeholder="2500"
                                        style={
                                            styles.inputNumber
                                        }
                                        disabled={
                                            carregando
                                        }
                                    />

                                    <span
                                        style={
                                            styles.suffix
                                        }
                                    >
                                        kg
                                    </span>
                                </div>
                            </label>

                            <div
                                style={
                                    styles.statusInfo
                                }
                            >

                                <div
                                    style={
                                        styles.statusInfoTop
                                    }
                                >
                                    <span>
                                        Status inicial
                                    </span>

                                    <StatusBadge
                                        status="PENDENTE"
                                        theme={
                                            theme
                                        }
                                    />
                                </div>

                                <span
                                    style={
                                        styles.statusInfoText
                                    }
                                >
                                    Toda entrega nova começa
                                    pendente. A alocação coloca
                                    a entrega em trânsito.
                                </span>

                            </div>

                            <button
                                type="submit"
                                disabled={
                                    carregando
                                }
                                style={
                                    carregando
                                        ? styles.btnDisabledLarge
                                        : styles.btnSubmit
                                }
                            >
                                {carregando
                                    ? 'Salvando...'
                                    : 'Registrar entrega'}
                            </button>

                        </form>

                    </section>

                    <section
                        style={
                            styles.card
                        }
                    >

                        <div
                            style={
                                styles.tableHeader
                            }
                        >

                            <div>
                                <h3
                                    style={
                                        styles.cardTitle
                                    }
                                >
                                    Entregas
                                </h3>

                                <span
                                    style={
                                        styles.tableSubtitle
                                    }
                                >
                                    Controle o ciclo operacional
                                    de cada entrega.
                                </span>
                            </div>

                            <button
                                type="button"
                                onClick={() =>
                                    carregarEntregas(
                                        pagina
                                    )
                                }
                                disabled={
                                    acaoEmAndamento !==
                                    null
                                }
                                style={
                                    styles.refreshButton
                                }
                            >
                                <RefreshIcon />
                                Atualizar
                            </button>

                        </div>

                        {entregas.length ===
                        0 ? (

                            <div
                                style={
                                    styles.emptyState
                                }
                            >
                                <PackageIcon />

                                <strong>
                                    Nenhuma entrega encontrada
                                </strong>

                                <span>
                                    Crie uma nova entrega para
                                    começar.
                                </span>
                            </div>

                        ) : (

                            <>
                                <div
                                    className="sgfl-table-wrap"
                                    style={
                                        styles.tableWrap
                                    }
                                >
                                    <table
                                        style={
                                            styles.table
                                        }
                                    >
                                        <thead>

                                        <tr>

                                            <th
                                                style={
                                                    styles.th
                                                }
                                            >
                                                ID
                                            </th>

                                            <th
                                                style={
                                                    styles.th
                                                }
                                            >
                                                Descrição
                                            </th>

                                            <th
                                                style={
                                                    styles.th
                                                }
                                            >
                                                Destino
                                            </th>

                                            <th
                                                style={
                                                    styles.th
                                                }
                                            >
                                                Peso
                                            </th>

                                            <th
                                                style={
                                                    styles.th
                                                }
                                            >
                                                Status
                                            </th>

                                            <th
                                                style={
                                                    styles.th
                                                }
                                            >
                                                Ações
                                            </th>

                                        </tr>

                                        </thead>

                                        <tbody>

                                        {entregas.map(
                                            item => (

                                                <tr
                                                    key={
                                                        item.id
                                                    }
                                                    style={
                                                        styles.tr
                                                    }
                                                >

                                                    <td
                                                        style={
                                                            styles.tdId
                                                        }
                                                    >
                                                        #
                                                        {
                                                            item.id
                                                        }
                                                    </td>

                                                    <td
                                                        style={
                                                            styles.td
                                                        }
                                                    >
                                                        <div
                                                            style={
                                                                styles.deliveryTitle
                                                            }
                                                        >
                                                            {
                                                                item.descricao ||
                                                                'Sem descrição'
                                                            }
                                                        </div>
                                                    </td>

                                                    <td
                                                        style={
                                                            styles.td
                                                        }
                                                    >
                                                        {
                                                            item.enderecoDestino ||
                                                            '-'
                                                        }
                                                    </td>

                                                    <td
                                                        style={
                                                            styles.td
                                                        }
                                                    >
                                                        {
                                                            formatarPeso(
                                                                item.pesoCargaKg
                                                            )
                                                        }
                                                    </td>

                                                    <td
                                                        style={
                                                            styles.td
                                                        }
                                                    >
                                                        <StatusBadge
                                                            status={
                                                                item.status
                                                            }
                                                            theme={
                                                                theme
                                                            }
                                                        />
                                                    </td>

                                                    <td
                                                        style={
                                                            styles.td
                                                        }
                                                    >
                                                        {renderAcoes(
                                                            item
                                                        )}
                                                    </td>

                                                </tr>

                                            )
                                        )}

                                        </tbody>

                                    </table>
                                </div>

                                {totalPaginas >
                                    1 && (
                                        <div
                                            style={
                                                styles.pagination
                                            }
                                        >

                                            <button
                                                type="button"
                                                disabled={
                                                    pagina ===
                                                    0 ||
                                                    acaoEmAndamento !==
                                                    null
                                                }
                                                onClick={() =>
                                                    carregarEntregas(
                                                        pagina -
                                                        1
                                                    )
                                                }
                                                style={
                                                    styles.pageBtn
                                                }
                                            >
                                                Anterior
                                            </button>

                                            <span
                                                style={
                                                    styles.pageInfo
                                                }
                                            >
                                            Página{' '}
                                                {
                                                    pagina +
                                                    1
                                                }{' '}
                                                de{' '}
                                                {
                                                    totalPaginas
                                                }
                                        </span>

                                            <button
                                                type="button"
                                                disabled={
                                                    pagina +
                                                    1 >=
                                                    totalPaginas ||
                                                    acaoEmAndamento !==
                                                    null
                                                }
                                                onClick={() =>
                                                    carregarEntregas(
                                                        pagina +
                                                        1
                                                    )
                                                }
                                                style={
                                                    styles.pageBtn
                                                }
                                            >
                                                Próxima
                                            </button>

                                        </div>
                                    )}

                            </>
                        )}

                    </section>

                </div>

            </div>

            {entregaAlocacao && (
                <div
                    style={
                        styles.modalOverlay
                    }
                >

                    <div
                        style={
                            styles.modal
                        }
                    >

                        <div
                            style={
                                styles.modalHeader
                            }
                        >

                            <div>
                                <h3
                                    style={
                                        styles.modalTitle
                                    }
                                >
                                    Alocar entrega #
                                    {
                                        entregaAlocacao.id
                                    }
                                </h3>

                                <p
                                    style={
                                        styles.modalSubtitle
                                    }
                                >
                                    Selecione o veículo e o
                                    motorista.
                                </p>
                            </div>

                            <button
                                type="button"
                                onClick={
                                    fecharAlocacao
                                }
                                style={
                                    styles.modalClose
                                }
                            >
                                ×
                            </button>

                        </div>

                        <form
                            onSubmit={
                                handleAlocar
                            }
                            style={
                                styles.form
                            }
                        >

                            <label
                                style={
                                    styles.label
                                }
                            >
                                Veículo

                                <select
                                    value={
                                        veiculoSelecionado
                                    }
                                    onChange={
                                        event =>
                                            setVeiculoSelecionado(
                                                event.target.value
                                            )
                                    }
                                    style={
                                        styles.input
                                    }
                                    disabled={
                                        carregandoRecursos ||
                                        acaoEmAndamento !==
                                        null
                                    }
                                >

                                    <option value="">
                                        {carregandoRecursos
                                            ? 'Carregando veículos...'
                                            : 'Selecione um veículo'}
                                    </option>

                                    {veiculos.map(
                                        veiculo => (
                                            <option
                                                key={
                                                    veiculo.id
                                                }
                                                value={
                                                    veiculo.id
                                                }
                                            >
                                                {
                                                    veiculo.modelo
                                                }
                                                {' — '}
                                                {
                                                    veiculo.placa
                                                }
                                                {' — '}
                                                {
                                                    formatarPeso(
                                                        veiculo.capacidadeCargaKg
                                                    )
                                                }
                                            </option>
                                        )
                                    )}

                                </select>
                            </label>

                            <label
                                style={
                                    styles.label
                                }
                            >
                                Motorista

                                <select
                                    value={
                                        motoristaSelecionado
                                    }
                                    onChange={
                                        event =>
                                            setMotoristaSelecionado(
                                                event.target.value
                                            )
                                    }
                                    style={
                                        styles.input
                                    }
                                    disabled={
                                        carregandoRecursos ||
                                        acaoEmAndamento !==
                                        null
                                    }
                                >

                                    <option value="">
                                        {carregandoRecursos
                                            ? 'Carregando motoristas...'
                                            : 'Selecione um motorista'}
                                    </option>

                                    {motoristas.map(
                                        motorista => (
                                            <option
                                                key={
                                                    motorista.id
                                                }
                                                value={
                                                    motorista.id
                                                }
                                            >
                                                {
                                                    motorista.nome
                                                }
                                                {' — CNH '}
                                                {
                                                    motorista.tipoCNH
                                                }
                                            </option>
                                        )
                                    )}

                                </select>
                            </label>

                            <div
                                style={
                                    styles.modalActions
                                }
                            >

                                <button
                                    type="button"
                                    onClick={
                                        fecharAlocacao
                                    }
                                    style={
                                        styles.btnSecondary
                                    }
                                >
                                    Voltar
                                </button>

                                <button
                                    type="submit"
                                    disabled={
                                        !veiculoSelecionado ||
                                        !motoristaSelecionado ||
                                        acaoEmAndamento !==
                                        null
                                    }
                                    style={
                                        !veiculoSelecionado ||
                                        !motoristaSelecionado ||
                                        acaoEmAndamento !==
                                        null
                                            ? styles.btnDisabled
                                            : styles.btnPrimary
                                    }
                                >
                                    Confirmar alocação
                                </button>

                            </div>

                        </form>

                    </div>

                </div>
            )}

            {mostrarRecursos && (
                <CadastroRecursos
                    theme={
                        theme
                    }
                    veiculos={
                        veiculos
                    }
                    motoristas={
                        motoristas
                    }
                    onClose={() =>
                        setMostrarRecursos(
                            false
                        )
                    }
                    onAtualizar={
                        carregarRecursos
                    }
                />
            )}

            <style>
                {`
                    .sgfl-page-dark {
                        isolation: isolate;
                    }

                    .sgfl-page-dark .sgfl-ambient {
                        position: absolute;
                        border-radius: 999px;
                        pointer-events: none;
                        z-index: 0;
                        filter: blur(46px);
                        opacity: 0.56;
                        mix-blend-mode: screen;
                    }

                    .sgfl-page-dark .sgfl-ambient-a {
                        width: 390px;
                        height: 390px;
                        top: -120px;
                        left: -100px;
                        background: radial-gradient(circle, rgba(165, 69, 82, 0.34) 0%, rgba(165, 69, 82, 0) 72%);
                    }

                    .sgfl-page-dark .sgfl-ambient-b {
                        width: 420px;
                        height: 420px;
                        top: 25%;
                        right: -175px;
                        background: radial-gradient(circle, rgba(188, 140, 129, 0.14) 0%, rgba(188, 140, 129, 0) 72%);
                    }

                    .sgfl-page-dark .sgfl-ambient-c {
                        width: 360px;
                        height: 360px;
                        bottom: -145px;
                        right: 20%;
                        background: radial-gradient(circle, rgba(165, 69, 82, 0.18) 0%, rgba(165, 69, 82, 0) 74%);
                    }

                    .sgfl-page-dark .sgfl-grid-overlay {
                        position: absolute;
                        inset: 0;
                        z-index: 0;
                        pointer-events: none;
                        opacity: 0.76;
                        background-image:
                            radial-gradient(circle, rgba(244,233,236,0.055) 0.7px, transparent 0.8px),
                            linear-gradient(rgba(244,233,236,0.016) 1px, transparent 1px),
                            linear-gradient(90deg, rgba(244,233,236,0.016) 1px, transparent 1px);
                        background-size: 8px 8px, 54px 54px, 54px 54px;
                        mask-image: linear-gradient(to bottom, rgba(0,0,0,0.72), transparent 92%);
                        -webkit-mask-image: linear-gradient(to bottom, rgba(0,0,0,0.72), transparent 92%);
                    }

                    .sgfl-page-dark button {
                        position: relative;
                        overflow: hidden;
                        transition:
                            transform 180ms ease,
                            filter 180ms ease,
                            box-shadow 180ms ease,
                            border-color 180ms ease;
                    }

                    .sgfl-page-dark button::after {
                        content: '';
                        position: absolute;
                        inset: 0;
                        pointer-events: none;
                        background: linear-gradient(
                            115deg,
                            transparent 12%,
                            rgba(255,255,255,0.09) 46%,
                            transparent 72%
                        );
                        opacity: 0.42;
                        transform: translateX(-115%);
                        transition: transform 460ms ease;
                    }

                    .sgfl-page-dark button:not(:disabled):hover {
                        transform: translateY(-1px);
                        filter: brightness(1.06);
                        box-shadow:
                            0 12px 34px rgba(85, 26, 36, 0.20),
                            inset 0 1px 0 rgba(255,255,255,0.10);
                    }

                    .sgfl-page-dark button:not(:disabled):hover::after {
                        transform: translateX(115%);
                    }

                    .sgfl-page-dark button:not(:disabled):active {
                        transform: translateY(0) scale(0.985);
                    }

                    .sgfl-page-dark input,
                    .sgfl-page-dark textarea,
                    .sgfl-page-dark select {
                        background-image:
                            linear-gradient(
                                135deg,
                                rgba(60, 22, 34, 0.34),
                                rgba(17, 11, 10, 0.16)
                            );
                        backdrop-filter: blur(18px) saturate(115%);
                        -webkit-backdrop-filter: blur(18px) saturate(115%);
                        box-shadow: inset 0 1px 0 rgba(255,255,255,0.035);
                    }

                    .sgfl-page-dark .sgfl-blur-btn {
                        background-image:
                            linear-gradient(
                                135deg,
                                rgba(165,69,82,0.15),
                                rgba(244,233,236,0.03)
                            );
                        backdrop-filter: blur(22px) saturate(120%);
                        -webkit-backdrop-filter: blur(22px) saturate(120%);
                        box-shadow:
                            inset 0 1px 0 rgba(255,255,255,0.09),
                            0 10px 30px rgba(0,0,0,0.20);
                    }

                    .sgfl-page-dark .sgfl-blur-btn:hover {
                        border-color: rgba(165,69,82,0.56) !important;
                    }

                    .sgfl-table-wrap {
                        width: 100%;
                        overflow-x: auto;
                    }

                    @media (max-width: 1000px) {
                        .sgfl-grid {
                            grid-template-columns: 1fr !important;
                        }
                    }

                    @media (max-width: 760px) {
                        .sgfl-summary {
                            grid-template-columns: 1fr !important;
                        }
                    }
                `}
            </style>

        </div>
    );
}

function SummaryCard({
                         icon,
                         label,
                         value,
                         description,
                         theme
                     }) {
    return (
        <div
            style={{
                display: 'flex',
                alignItems: 'center',
                gap: '13px',
                padding: '15px 16px',
                border:
                    `1px solid ${theme.border}`,
                borderRadius: '12px',
                backgroundColor: theme.surface
            }}
        >

            <div
                style={{
                    width: '40px',
                    height: '40px',
                    borderRadius: '10px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    backgroundColor:
                    theme.surfaceAlt,
                    color:
                    theme.accent
                }}
            >
                {icon}
            </div>

            <div>

                <div
                    style={{
                        fontSize: '12px',
                        color: theme.inkSoft,
                        fontWeight: 600
                    }}
                >
                    {label}
                </div>

                <div>
                    <strong
                        style={{
                            fontSize: '24px',
                            lineHeight: 1.1
                        }}
                    >
                        {value}
                    </strong>

                    <span
                        style={{
                            marginLeft: '5px',
                            color: theme.inkSoft,
                            fontSize: '11px'
                        }}
                    >
                        {description}
                    </span>
                </div>

            </div>

        </div>
    );
}

function StatusBadge({
                         status,
                         theme
                     }) {

    const tone =
        theme.statuses[status] ||
        theme.statuses.PENDENTE;

    let label =
        status;

    if (status === 'PENDENTE') {
        label = 'Pendente';
    } else if (status === 'EM_TRANSITO') {
        label = 'Em trânsito';
    } else if (status === 'ENTREGUE') {
        label = 'Entregue';
    } else if (status === 'CANCELADA') {
        label = 'Cancelada';
    }

    return (
        <span
            style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '6px',
                padding: '5px 10px',
                borderRadius: '999px',
                backgroundColor: tone.bg,
                color: tone.ink,
                fontSize: '11px',
                fontWeight: 700,
                whiteSpace: 'nowrap'
            }}
        >

            <span
                style={{
                    width: '6px',
                    height: '6px',
                    borderRadius: '50%',
                    backgroundColor: tone.dot
                }}
            />

            {label}

        </span>
    );
}

function formatarPeso(valor) {

    if (
        valor === null ||
        valor === undefined ||
        Number.isNaN(
            Number(valor)
        )
    ) {
        return '-';
    }

    return `${Number(valor).toLocaleString(
        'pt-BR',
        {
            maximumFractionDigits: 2
        }
    )} kg`;
}

function BoxIcon() {
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
            <path d="m3 7 9-4 9 4-9 4-9-4Z" />
            <path d="M3 7v10l9 4 9-4V7" />
            <path d="M12 11v10" />
        </svg>
    );
}

function UserIcon() {
    return (
        <svg
            width="19"
            height="19"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinecap="round"
            strokeLinejoin="round"
        >
            <circle
                cx="12"
                cy="8"
                r="4"
            />

            <path
                d="M4 21c0-4 3.6-7 8-7s8 3 8 7"
            />
        </svg>
    );
}

function TruckIcon() {
    return (
        <svg
            width="20"
            height="20"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinecap="round"
            strokeLinejoin="round"
        >
            <path d="M3 6h11v11H3z" />
            <path d="M14 9h4l3 3v5h-7z" />
            <circle
                cx="7"
                cy="19"
                r="2"
            />
            <circle
                cx="18"
                cy="19"
                r="2"
            />
        </svg>
    );
}

function PackageIcon() {
    return (
        <svg
            width="20"
            height="20"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinecap="round"
            strokeLinejoin="round"
        >
            <path d="m4 7 8-4 8 4-8 4-8-4Z" />
            <path d="M4 7v10l8 4 8-4V7" />
            <path d="M12 11v10" />
        </svg>
    );
}

function SettingsIcon() {
    return (
        <svg
            width="15"
            height="15"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinecap="round"
            strokeLinejoin="round"
        >
            <circle
                cx="12"
                cy="12"
                r="3"
            />

            <path
                d="M19.4 15a1.7 1.7 0 0 0 .3 1.9l.1.1-1.8 1.8-.1-.1a1.7 1.7 0 0 0-1.9-.3 1.7 1.7 0 0 0-1 1.6v.2h-2.6V20a1.7 1.7 0 0 0-1-1.6 1.7 1.7 0 0 0-1.9.3l-.1.1-1.8-1.8.1-.1a1.7 1.7 0 0 0 .3-1.9 1.7 1.7 0 0 0-1.6-1H6v-2.6h.2a1.7 1.7 0 0 0 1.6-1 1.7 1.7 0 0 0-.3-1.9l-.1-.1 1.8-1.8.1.1a1.7 1.7 0 0 0 1.9.3 1.7 1.7 0 0 0 1-1.6V5h2.6v.2a1.7 1.7 0 0 0 1 1.6 1.7 1.7 0 0 0 1.9-.3l.1-.1 1.8 1.8-.1.1a1.7 1.7 0 0 0-.3 1.9 1.7 1.7 0 0 0 1.6 1h.2v2.6h-.2a1.7 1.7 0 0 0-1.6 1Z"
            />
        </svg>
    );
}

function RefreshIcon() {
    return (
        <svg
            width="14"
            height="14"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
        >
            <path
                d="M20 11a8.1 8.1 0 0 0-14.9-3L3 11"
            />

            <path
                d="M3 4v7h7"
            />

            <path
                d="M4 13a8.1 8.1 0 0 0 14.9 3L21 13"
            />

            <path
                d="M21 20v-7h-7"
            />
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
            <circle
                cx="12"
                cy="12"
                r="4"
            />

            <path
                d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"
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
            strokeWidth="1.8"
            strokeLinecap="round"
            strokeLinejoin="round"
        >
            <path
                d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8Z"
            />
        </svg>
    );
}

function getStyles(theme) {

    return {
        page: {
            minHeight: '100vh',
            position: 'relative',
            overflow: 'hidden',
            backgroundColor: theme.bg,
            backgroundImage: theme.backgroundImage,
            backgroundSize: theme.backgroundSize || 'auto',
            color: theme.ink,
            fontFamily:
                "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif",
            padding: '28px 24px'
        },

        shell: {
            width: '100%',
            maxWidth: '1500px',
            margin: '0 auto',
            position: 'relative',
            zIndex: 2
        },

        header: {
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            gap: '20px',
            marginBottom: '20px'
        },

        brand: {
            display: 'flex',
            alignItems: 'center',
            gap: '11px'
        },

        brandIcon: {
            width: '38px',
            height: '38px',
            borderRadius: '14px',
            backgroundImage: 'linear-gradient(135deg, #A54552 0%, #7A2D38 52%, #6E202D 100%)',
            backgroundColor: theme.accent,
            color: theme.accentInk,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
        },

        wordmark: {
            fontFamily:
                "Georgia, 'Iowan Old Style', serif",
            fontSize: '20px',
            lineHeight: 1.1
        },

        subtitle: {
            fontSize: '12px',
            color: theme.inkSoft
        },

        headerActions: {
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            flexWrap: 'wrap',
            justifyContent: 'flex-end'
        },

        btnResources: {
            display: 'inline-flex',
            alignItems: 'center',
            gap: '7px',
            padding: '9px 13px',
            borderRadius: '12px',
            border:
                `1px solid ${theme.border}`,
            backgroundColor: theme.surface,
            backgroundImage: 'linear-gradient(135deg, rgba(165, 69, 82, 0.24), rgba(110, 32, 45, 0.10))',
            backdropFilter: 'blur(24px) saturate(165%)',
            WebkitBackdropFilter: 'blur(24px) saturate(165%)',
            color: theme.ink,
            fontSize: '12px',
            fontWeight: 700,
            cursor: 'pointer',
            boxShadow: 'inset 0 1px 0 rgba(255,255,255,0.10), 0 10px 30px rgba(0, 0, 0, 0.22)'
        },

        themeToggle: {
            width: '36px',
            height: '36px',
            borderRadius: '12px',
            border:
                `1px solid ${theme.border}`,
            backgroundColor: theme.surface,
            backgroundImage: 'linear-gradient(135deg, rgba(165, 69, 82, 0.20), rgba(110, 32, 45, 0.08))',
            backdropFilter: 'blur(24px) saturate(165%)',
            WebkitBackdropFilter: 'blur(24px) saturate(165%)',
            color: theme.ink,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            cursor: 'pointer'
        },

        btnLogout: {
            padding: '9px 13px',
            borderRadius: '12px',
            border:
                `1px solid ${theme.border}`,
            backgroundColor: 'rgba(165, 69, 82, 0.10)',
            backgroundImage: 'linear-gradient(135deg, rgba(190, 70, 95, 0.20), rgba(110, 32, 45, 0.10))',
            backdropFilter: 'blur(24px) saturate(165%)',
            WebkitBackdropFilter: 'blur(24px) saturate(165%)',
            color: theme.danger,
            fontSize: '12px',
            fontWeight: 700,
            cursor: 'pointer',
            boxShadow: 'inset 0 1px 0 rgba(255,255,255,0.08), 0 10px 28px rgba(0, 0, 0, 0.20)'
        },

        alertaErro: {
            marginBottom: '16px',
            padding: '12px 14px',
            borderRadius: '9px',
            border:
                `1px solid ${theme.border}`,
            borderLeft:
                `4px solid ${theme.danger}`,
            backgroundColor:
            theme.statuses.PENDENTE.bg,
            color:
            theme.statuses.PENDENTE.ink,
            fontSize: '13px'
        },

        alertaSucesso: {
            marginBottom: '16px',
            padding: '12px 14px',
            borderRadius: '9px',
            border:
                `1px solid ${theme.border}`,
            borderLeft:
                `4px solid ${theme.statuses.ENTREGUE.dot}`,
            backgroundColor:
            theme.statuses.ENTREGUE.bg,
            color:
            theme.statuses.ENTREGUE.ink,
            fontSize: '13px'
        },

        summaryGrid: {
            display: 'grid',
            gridTemplateColumns:
                'repeat(3, minmax(0, 1fr))',
            gap: '12px',
            marginBottom: '18px'
        },

        grid: {
            display: 'grid',
            gridTemplateColumns:
                '330px minmax(0, 1fr)',
            gap: '18px',
            alignItems: 'start'
        },

        card: {
            backgroundColor:
            theme.surface,
            backgroundImage: 'linear-gradient(145deg, rgba(60, 22, 34, 0.42), rgba(20, 8, 12, 0.24))',
            backdropFilter: 'blur(22px) saturate(150%)',
            WebkitBackdropFilter: 'blur(22px) saturate(150%)',
            border:
                `1px solid ${theme.border}`,
            borderRadius: '18px',
            padding: '20px'
        },

        cardTitle: {
            margin: 0,
            fontFamily:
                "Georgia, 'Iowan Old Style', serif",
            fontSize: '18px',
            fontWeight: 400
        },

        form: {
            display: 'flex',
            flexDirection: 'column',
            gap: '14px'
        },

        label: {
            display: 'flex',
            flexDirection: 'column',
            gap: '6px',
            color: theme.inkSoft,
            fontSize: '12px',
            fontWeight: 600
        },

        input: {
            width: '100%',
            boxSizing: 'border-box',
            padding: '10px 11px',
            borderRadius: '8px',
            border:
                `1px solid ${theme.borderStrong}`,
            backgroundColor:
            theme.surfaceAlt,
            color:
            theme.ink,
            fontSize: '14px'
        },

        inputWithSuffix: {
            display: 'flex',
            alignItems: 'stretch'
        },

        inputNumber: {
            flex: 1,
            minWidth: 0,
            boxSizing: 'border-box',
            padding: '10px 11px',
            borderRadius: '8px 0 0 8px',
            border:
                `1px solid ${theme.borderStrong}`,
            borderRight: 'none',
            backgroundColor:
            theme.surfaceAlt,
            color:
            theme.ink,
            fontSize: '14px'
        },

        suffix: {
            display: 'flex',
            alignItems: 'center',
            padding: '0 11px',
            borderRadius: '0 8px 8px 0',
            border:
                `1px solid ${theme.borderStrong}`,
            backgroundColor:
            theme.surfaceAlt,
            color:
            theme.inkSoft,
            fontSize: '13px'
        },

        statusInfo: {
            padding: '11px 12px',
            borderRadius: '8px',
            border:
                `1px solid ${theme.border}`,
            backgroundColor:
            theme.surfaceAlt
        },

        statusInfoTop: {
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            gap: '10px',
            marginBottom: '6px',
            fontSize: '12px',
            fontWeight: 700
        },

        statusInfoText: {
            color: theme.inkSoft,
            fontSize: '11px',
            lineHeight: 1.45
        },

        btnSubmit: {
            width: '100%',
            padding: '11px',
            border: '1px solid rgba(200, 90, 110, 0.60)',
            borderRadius: '12px',
            backgroundColor: theme.accent,
            backgroundImage: 'linear-gradient(135deg, #A54552 0%, #8D3F4B 52%, #6E202D 100%)',
            backdropFilter: 'blur(26px) saturate(170%)',
            WebkitBackdropFilter: 'blur(26px) saturate(170%)',
            color: theme.accentInk,
            fontSize: '13px',
            fontWeight: 700,
            cursor: 'pointer'
        },

        btnDisabledLarge: {
            width: '100%',
            padding: '11px',
            border: 'none',
            borderRadius: '8px',
            backgroundColor: theme.borderStrong,
            color: theme.inkSoft,
            fontSize: '13px',
            fontWeight: 700,
            cursor: 'not-allowed'
        },

        tableHeader: {
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'flex-end',
            gap: '14px',
            marginBottom: '17px'
        },

        tableSubtitle: {
            display: 'block',
            marginTop: '4px',
            color: theme.inkSoft,
            fontSize: '11px'
        },

        refreshButton: {
            display: 'inline-flex',
            alignItems: 'center',
            gap: '6px',
            padding: '8px 10px',
            borderRadius: '12px',
            border:
                `1px solid ${theme.border}`,
            backgroundColor:
            theme.surfaceAlt,
            color:
            theme.ink,
            fontSize: '11px',
            fontWeight: 700,
            cursor: 'pointer'
        },

        tableWrap: {
            width: '100%'
        },

        table: {
            width: '100%',
            borderCollapse: 'collapse',
            minWidth: '850px'
        },

        th: {
            textAlign: 'left',
            padding: '9px 8px',
            borderBottom:
                `1px solid ${theme.border}`,
            color: theme.inkSoft,
            fontSize: '10px',
            textTransform: 'uppercase',
            letterSpacing: '0.04em'
        },

        tr: {
            borderBottom:
                `1px solid ${theme.border}`
        },

        td: {
            padding: '12px 8px',
            fontSize: '13px',
            verticalAlign: 'middle'
        },

        tdId: {
            padding: '12px 8px',
            fontSize: '13px',
            fontWeight: 700,
            verticalAlign: 'middle'
        },

        deliveryTitle: {
            fontWeight: 600
        },

        actions: {
            display: 'flex',
            alignItems: 'center',
            flexWrap: 'wrap',
            gap: '5px'
        },

        btnPrimary: {
            padding: '7px 10px',
            border: '1px solid rgba(200, 90, 110, 0.60)',
            borderRadius: '10px',
            backgroundColor: theme.accent,
            backgroundImage: 'linear-gradient(135deg, rgba(178, 58, 84, 0.96), rgba(122, 31, 43, 0.92))',
            backdropFilter: 'blur(22px) saturate(170%)',
            WebkitBackdropFilter: 'blur(22px) saturate(170%)',
            color: theme.accentInk,
            fontSize: '11px',
            fontWeight: 700,
            cursor: 'pointer'
        },

        btnCancel: {
            padding: '7px 10px',
            borderRadius: '10px',
            border:
                `1px solid rgba(165, 69, 82, 0.38)`,
            backgroundColor: 'rgba(165, 69, 82, 0.07)',
            backgroundImage: 'linear-gradient(135deg, rgba(190, 70, 95, 0.16), rgba(110, 32, 45, 0.08))',
            backdropFilter: 'blur(22px) saturate(170%)',
            WebkitBackdropFilter: 'blur(22px) saturate(170%)',
            color: theme.danger,
            fontSize: '11px',
            fontWeight: 700,
            cursor: 'pointer'
        },

        btnFinalize: {
            padding: '7px 10px',
            border: '1px solid rgba(230, 189, 123, 0.55)',
            borderRadius: '10px',
            backgroundColor:
            theme.statuses.EM_TRANSITO.dot,
            backgroundImage: 'linear-gradient(135deg, rgba(176, 120, 48, 0.85), rgba(122, 76, 30, 0.88))',
            backdropFilter: 'blur(22px) saturate(170%)',
            WebkitBackdropFilter: 'blur(22px) saturate(170%)',
            color: '#FFFFFF',
            fontSize: '11px',
            fontWeight: 700,
            cursor: 'pointer'
        },

        btnDelete: {
            padding: '7px 10px',
            borderRadius: '10px',
            border:
                `1px solid rgba(165, 69, 82, 0.38)`,
            backgroundColor: 'rgba(165, 69, 82, 0.07)',
            backgroundImage: 'linear-gradient(135deg, rgba(165, 69, 82, 0.12), rgba(110, 32, 45, 0.10))',
            backdropFilter: 'blur(22px) saturate(170%)',
            WebkitBackdropFilter: 'blur(22px) saturate(170%)',
            color: theme.danger,
            fontSize: '11px',
            fontWeight: 700,
            cursor: 'pointer'
        },

        btnDisabled: {
            padding: '7px 10px',
            borderRadius: '8px',
            border:
                `1px solid ${theme.border}`,
            backgroundColor:
                'rgba(150, 110, 120, 0.14)',
            backgroundImage: 'linear-gradient(135deg, rgba(255,255,255,0.04), rgba(110, 50, 65, 0.10))',
            backdropFilter: 'blur(16px)',
            WebkitBackdropFilter: 'blur(16px)',
            color: theme.inkSoft,
            fontSize: '11px',
            fontWeight: 700,
            cursor: 'not-allowed'
        },

        emptyState: {
            minHeight: '270px',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '7px',
            color: theme.inkSoft
        },

        pagination: {
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            gap: '12px',
            marginTop: '18px'
        },

        pageBtn: {
            padding: '8px 12px',
            borderRadius: '10px',
            border:
                `1px solid ${theme.border}`,
            backgroundColor:
            theme.surfaceAlt,
            color: theme.ink,
            fontSize: '12px',
            fontWeight: 600,
            cursor: 'pointer'
        },

        pageInfo: {
            color: theme.inkSoft,
            fontSize: '12px'
        },

        modalOverlay: {
            position: 'fixed',
            inset: 0,
            backgroundColor:
                'rgba(0, 0, 0, 0.58)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '20px',
            zIndex: 3000
        },

        modal: {
            width: '100%',
            maxWidth: '500px',
            backgroundColor:
            theme.surface,
            backgroundImage: 'linear-gradient(145deg, rgba(46, 18, 28, 0.90), rgba(20, 8, 12, 0.86))',
            backdropFilter: 'blur(28px) saturate(155%)',
            WebkitBackdropFilter: 'blur(28px) saturate(155%)',
            border:
                `1px solid ${theme.border}`,
            borderRadius: '14px',
            padding: '23px',
            boxShadow:
                '0 25px 60px rgba(0,0,0,0.25)'
        },

        modalHeader: {
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'flex-start',
            gap: '15px',
            marginBottom: '20px'
        },

        modalTitle: {
            margin: 0,
            fontFamily:
                "Georgia, 'Iowan Old Style', serif",
            fontSize: '19px',
            fontWeight: 400
        },

        modalSubtitle: {
            margin: '6px 0 0',
            color: theme.inkSoft,
            fontSize: '12px'
        },

        modalClose: {
            width: '33px',
            height: '33px',
            borderRadius: '12px',
            border:
                `1px solid ${theme.borderStrong}`,
            backgroundColor: 'rgba(46, 18, 28, 0.60)',
            backgroundImage: 'linear-gradient(135deg, rgba(165, 69, 82, 0.20), rgba(110, 32, 45, 0.10))',
            backdropFilter: 'blur(20px) saturate(170%)',
            WebkitBackdropFilter: 'blur(20px) saturate(170%)',
            color: theme.ink,
            fontSize: '21px',
            cursor: 'pointer'
        },

        modalActions: {
            display: 'flex',
            justifyContent: 'flex-end',
            gap: '8px',
            marginTop: '5px'
        },

        btnSecondary: {
            padding: '9px 13px',
            borderRadius: '9px',
            border:
                `1px solid ${theme.borderStrong}`,
            backgroundColor: 'rgba(46, 18, 28, 0.58)',
            backgroundImage: 'linear-gradient(135deg, rgba(165, 69, 82, 0.16), rgba(110, 32, 45, 0.07))',
            backdropFilter: 'blur(20px) saturate(165%)',
            WebkitBackdropFilter: 'blur(20px) saturate(165%)',
            color: theme.ink,
            fontSize: '12px',
            fontWeight: 600,
            cursor: 'pointer'
        }
    };
}
