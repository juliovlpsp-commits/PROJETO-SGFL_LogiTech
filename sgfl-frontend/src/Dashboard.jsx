import { useCallback, useEffect, useRef, useState } from 'react';
import { useTheme } from './useTheme';
import CadastroRecursos from './CadastroRecursos';
import useEntregaList from './useEntregaList';
import api from './api';
import getStyles from './dashboardStyles';
import {
    BoxIcon,
    MoonIcon,
    PackageIcon,
    RefreshIcon,
    ScrollArrowIcon,
    SearchIcon,
    SettingsIcon,
    StatusBadge,
    SummaryCard,
    SunIcon,
    TruckIcon,
    UserIcon
} from './dashboardComponents';
import { formatarPeso } from './deliveryFormatting';
import AssinaturaCanvas from './AssinaturaCanvas';
import MapaEntrega from './MapaEntrega';
import GestaoComercial from './GestaoComercial';
import { enfileirar, lerFila, reenviarFila } from './offlineQueue';

export default function Dashboard({ onLogout }) {

    const [veiculos, setVeiculos] = useState([]);
    const [motoristas, setMotoristas] = useState([]);
    const [totalVeiculos, setTotalVeiculos] = useState(0);
    const [totalMotoristas, setTotalMotoristas] = useState(0);
    const [opcoesVeiculo, setOpcoesVeiculo] = useState([]);
    const [opcoesMotorista, setOpcoesMotorista] = useState([]);
    const [buscaVeiculo, setBuscaVeiculo] = useState('');
    const [buscaMotorista, setBuscaMotorista] = useState('');

    const [buscaEntrega, setBuscaEntrega] = useState('');
    const [filtroStatusEntrega, setFiltroStatusEntrega] = useState('');
    const requisicaoRecursosRef = useRef(0);
    const workspaceRef = useRef(null);
    const workspaceContentRef = useRef(null);
    const [painelEntregasAberto, setPainelEntregasAberto] = useState(false);
    const [transicaoPainel, setTransicaoPainel] = useState('');
    const transicaoTimeoutRef = useRef(null);
    const [workspaceTemRolagem, setWorkspaceTemRolagem] = useState(false);
    const [workspaceNoFim, setWorkspaceNoFim] = useState(false);

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

    const [kpisOperacionais, setKpisOperacionais] = useState(null);
    const [alertasOperacionais, setAlertasOperacionais] = useState([]);
    const [entregaRastreio, setEntregaRastreio] = useState(null);
    const [timelineRastreio, setTimelineRastreio] = useState([]);
    const [carregandoRastreio, setCarregandoRastreio] = useState(false);

    // Custos da entrega
    const [entregaCustos, setEntregaCustos] = useState(null);
    const [listaCustos, setListaCustos] = useState([]);
    const [carregandoCustos, setCarregandoCustos] = useState(false);
    const [custoTipo, setCustoTipo] = useState('COMBUSTIVEL');
    const [custoDescricao, setCustoDescricao] = useState('');
    const [custoValor, setCustoValor] = useState('');
    const [salvandoCusto, setSalvandoCusto] = useState(false);

    // Comprovante de entrega
    const [entregaComprovante, setEntregaComprovante] = useState(null);
    const [nomeRecebedor, setNomeRecebedor] = useState('');
    const [observacaoComprovante, setObservacaoComprovante] = useState('');
    const [fotoComprovante, setFotoComprovante] = useState(null);
    const [assinaturaComprovante, setAssinaturaComprovante] = useState('');
    const [salvandoComprovante, setSalvandoComprovante] = useState(false);

    // Mapa / geocodificação
    const [entregaMapa, setEntregaMapa] = useState(null);
    const [coordenadasMapa, setCoordenadasMapa] = useState(null);
    const [rotaMapa, setRotaMapa] = useState(null);
    const [etaHistorico, setEtaHistorico] = useState([]);
    const [geocodificandoMapa, setGeocodificandoMapa] = useState(false);
    const [mensagemMapa, setMensagemMapa] = useState('');

    const {
        theme: baseTheme,
        mode,
        toggle,
        variante
    } = useTheme();

    const theme = baseTheme;

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

                    // Sem resposta do servidor: mutações (POST/PUT/PATCH/DELETE)
                    // vão para a fila offline e voltam quando a rede retornar.
                    const metodo = options.method || 'GET';

                    if (metodo !== 'GET') {
                        enfileirar({
                            url,
                            method: metodo,
                            body: options.body ?? null
                        });
                        error.message =
                            'Sem conexão — ação guardada e será enviada quando a rede voltar.';
                    }
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

    const { entregas, pagina, totalPaginas, carregarEntregas } = useEntregaList({
        request,
        onError: tratarErro,
        search: buscaEntrega,
        status: filtroStatusEntrega
    });

    const carregarRecursos = useCallback(
        async () => {

            setCarregandoRecursos(true);

            try {

                const [
                    veiculosData,
                    motoristasData
                ] = await Promise.all([
                    request('/veiculos?page=0&size=100'),
                    request('/motoristas?page=0&size=100')
                ]);

                const veiculosDaPagina = Array.isArray(veiculosData?.content) ? veiculosData.content : [];
                const motoristasDaPagina = Array.isArray(motoristasData?.content) ? motoristasData.content : [];
                setVeiculos(veiculosDaPagina);
                setMotoristas(motoristasDaPagina);
                setOpcoesVeiculo(veiculosDaPagina);
                setOpcoesMotorista(motoristasDaPagina);
                setTotalVeiculos(Number(veiculosData?.totalElements ?? 0));
                setTotalMotoristas(Number(motoristasData?.totalElements ?? 0));

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
        if (!entregaAlocacao) return undefined;

        const requisicaoAtual = ++requisicaoRecursosRef.current;
        const temporizador = window.setTimeout(async () => {
            try {
                const parametrosVeiculo = new URLSearchParams({ page: '0', size: '100' });
                const parametrosMotorista = new URLSearchParams({ page: '0', size: '100' });
                if (buscaVeiculo.trim()) parametrosVeiculo.set('q', buscaVeiculo.trim());
                if (buscaMotorista.trim()) parametrosMotorista.set('q', buscaMotorista.trim());
                const [veiculosData, motoristasData] = await Promise.all([
                    request(`/veiculos?${parametrosVeiculo}`),
                    request(`/motoristas?${parametrosMotorista}`)
                ]);
                if (requisicaoAtual !== requisicaoRecursosRef.current) return;
                setOpcoesVeiculo(Array.isArray(veiculosData?.content) ? veiculosData.content : []);
                setOpcoesMotorista(Array.isArray(motoristasData?.content) ? motoristasData.content : []);
            } catch (error) {
                if (requisicaoAtual === requisicaoRecursosRef.current) {
                    tratarErro(error, 'Não foi possível buscar veículos e motoristas.');
                }
            } finally {
                if (requisicaoAtual === requisicaoRecursosRef.current) setCarregandoRecursos(false);
            }
        }, 220);

        return () => window.clearTimeout(temporizador);
    }, [entregaAlocacao, buscaVeiculo, buscaMotorista, request, tratarErro]);

    const atualizarIndicadorRolagem = useCallback(() => {
        const workspace = workspaceRef.current;
        if (!workspace) return;

        const temRolagem = workspace.scrollHeight > workspace.clientHeight + 4;
        const chegouAoFim = workspace.scrollTop + workspace.clientHeight >= workspace.scrollHeight - 16;

        setWorkspaceTemRolagem(temRolagem);
        setWorkspaceNoFim(chegouAoFim);
    }, []);

    useEffect(() => {
        const timeout = window.setTimeout(carregarRecursos, 0);
        return () => window.clearTimeout(timeout);
    }, [carregarRecursos]);
    const carregarIndicadoresOperacionais = useCallback(
        async () => {
            try {
                const [kpis, alertas] = await Promise.all([
                    request('/operacional/kpis'),
                    request('/operacional/alertas')
                ]);

                setKpisOperacionais(kpis || null);
                setAlertasOperacionais(Array.isArray(alertas) ? alertas : []);
            } catch (error) {
                // Os indicadores são auxiliares: a operação principal continua disponível
                // mesmo que o endpoint opcional esteja temporariamente indisponível.
                if (error?.status !== 404) {
                    console.error('SGFL - erro ao carregar indicadores operacionais:', error);
                }
            }
        },
        [request]
    );

    useEffect(() => {
        const timeout = window.setTimeout(carregarIndicadoresOperacionais, 0);
        return () => window.clearTimeout(timeout);
    }, [carregarIndicadoresOperacionais, entregas]);

    // Reenvia ações que ficaram na fila offline (app aberto de novo ou
    // a rede voltando).
    useEffect(() => {
        const reenviarPendencias = async () => {
            if (lerFila().length === 0) return;

            const pendentes = await reenviarFila(request);

            if (pendentes > 0) {
                mostrarMensagem(
                    `${pendentes} ação(ões) aguardando conexão para serem enviadas.`,
                    'info'
                );
            } else {
                mostrarMensagem('Ações offline reenviadas com sucesso.', 'sucesso');
                await carregarEntregas();
            }
        };

        reenviarPendencias();
        window.addEventListener('online', reenviarPendencias);

        return () => window.removeEventListener('online', reenviarPendencias);
    }, [request, mostrarMensagem, carregarEntregas]);

    const abrirRastreio = async (item) => {
        setEntregaRastreio(item);
        setTimelineRastreio([]);
        setCarregandoRastreio(true);

        try {
            const eventos = await request(`/entregas/${item.id}/timeline`);
            setTimelineRastreio(Array.isArray(eventos) ? eventos : []);
        } catch (error) {
            tratarErro(error, 'Não foi possível carregar o histórico da entrega.');
        } finally {
            setCarregandoRastreio(false);
        }
    };

    // ------------------------------------------------------------------
    // Custos da entrega
    // ------------------------------------------------------------------
    const abrirCustos = async (item) => {
        setEntregaCustos(item);
        setListaCustos([]);
        setCustoTipo('COMBUSTIVEL');
        setCustoDescricao('');
        setCustoValor('');
        setCarregandoCustos(true);

        try {
            const custos = await request(`/operacional/entregas/${item.id}/custos`);
            setListaCustos(Array.isArray(custos) ? custos : []);
        } catch (error) {
            tratarErro(error, 'Não foi possível carregar os custos da entrega.');
        } finally {
            setCarregandoCustos(false);
        }
    };

    const handleAdicionarCusto = async (event) => {
        event.preventDefault();

        if (!custoValor || Number(custoValor) <= 0) {
            mostrarMensagem('Informe um valor maior que zero para o custo.', 'erro');
            return;
        }

        setSalvandoCusto(true);

        try {
            await request(`/operacional/entregas/${entregaCustos.id}/custos`, {
                method: 'POST',
                body: {
                    tipo: custoTipo,
                    descricao: custoDescricao.trim() || null,
                    valor: Number(custoValor)
                }
            });

            const custos = await request(`/operacional/entregas/${entregaCustos.id}/custos`);
            setListaCustos(Array.isArray(custos) ? custos : []);
            setCustoDescricao('');
            setCustoValor('');
            mostrarMensagem('Custo registrado com sucesso.', 'sucesso');
        } catch (error) {
            tratarErro(error, 'Não foi possível registrar o custo.');
        } finally {
            setSalvandoCusto(false);
        }
    };

    // ------------------------------------------------------------------
    // Comprovante de entrega (foto + assinatura)
    // ------------------------------------------------------------------
    const abrirComprovante = async (item) => {
        setEntregaComprovante(item);
        setNomeRecebedor('');
        setObservacaoComprovante('');
        setFotoComprovante(null);
        setAssinaturaComprovante('');
    };

    const handleSelecionarFoto = (event) => {
        const arquivo = event.target.files?.[0] || null;
        setFotoComprovante(arquivo);
    };

    const handleSalvarComprovante = async (event) => {
        event.preventDefault();

        if (!nomeRecebedor.trim()) {
            mostrarMensagem('Informe o nome de quem recebeu a entrega.', 'erro');
            return;
        }

        setSalvandoComprovante(true);

        try {
            const form = new FormData();

            form.append(
                'dados',
                new Blob(
                    [
                        JSON.stringify({
                            nomeRecebedor: nomeRecebedor.trim(),
                            assinatura: assinaturaComprovante || null,
                            observacao: observacaoComprovante.trim() || null
                        })
                    ],
                    { type: 'application/json' }
                )
            );

            if (fotoComprovante) {
                form.append('foto', fotoComprovante);
            }

            await api.post(`/entregas/${entregaComprovante.id}/comprovante`, form);

            mostrarMensagem('Comprovante registrado com sucesso.', 'sucesso');
            setEntregaComprovante(null);
            await carregarEntregas();
        } catch (error) {
            tratarErro(error, 'Não foi possível registrar o comprovante.');
        } finally {
            setSalvandoComprovante(false);
        }
    };

    // ------------------------------------------------------------------
    // Mapa, geocodificação e rota
    // ------------------------------------------------------------------
    const abrirMapa = async (item) => {
        setEntregaMapa(item);
        setRotaMapa(null);
        setEtaHistorico([]);
        setMensagemMapa('');

        const coordenadas = {
            latitudeOrigem: item.latitudeOrigem,
            longitudeOrigem: item.longitudeOrigem,
            latitudeDestino: item.latitudeDestino,
            longitudeDestino: item.longitudeDestino
        };

        setCoordenadasMapa(coordenadas);

        const possuiAsDuas =
            coordenadas.latitudeOrigem != null &&
            coordenadas.longitudeOrigem != null &&
            coordenadas.latitudeDestino != null &&
            coordenadas.longitudeDestino != null;

        if (possuiAsDuas) {
            await carregarRota(item.id);
        } else {
            setMensagemMapa(
                'Esta entrega ainda não tem coordenadas. Use "Geocodificar endereços" para preencher.'
            );
        }
    };

    const carregarRota = async (entregaId) => {
        try {
            const estimativa = await request(`/entregas/${entregaId}/rota`);
            setRotaMapa(estimativa);
        } catch (error) {
            tratarErro(error, 'Não foi possível calcular a rota.');
        }

        try {
            const historico = await request(`/entregas/${entregaId}/rota/historico`);
            setEtaHistorico(Array.isArray(historico) ? historico : []);
        } catch {
            // Histórico é complementar: falha não pode quebrar o mapa.
        }
    };

    const geocodificarEnderecos = async () => {
        if (!entregaMapa) return;

        setGeocodificandoMapa(true);
        setMensagemMapa('');

        try {
            const coordenadas = { ...coordenadasMapa };

            if (!coordenadas.latitudeOrigem || !coordenadas.longitudeOrigem) {
                const origem = await request(
                    `/geocodificacao?endereco=${encodeURIComponent(entregaMapa.enderecoOrigem || '')}`
                );
                coordenadas.latitudeOrigem = origem.latitude;
                coordenadas.longitudeOrigem = origem.longitude;
            }

            if (!coordenadas.latitudeDestino || !coordenadas.longitudeDestino) {
                const destino = await request(
                    `/geocodificacao?endereco=${encodeURIComponent(entregaMapa.enderecoDestino || '')}`
                );
                coordenadas.latitudeDestino = destino.latitude;
                coordenadas.longitudeDestino = destino.longitude;
            }

            await request(`/entregas/${entregaMapa.id}/coordenadas`, {
                method: 'PUT',
                body: {
                    latitudeOrigem: coordenadas.latitudeOrigem,
                    longitudeOrigem: coordenadas.longitudeOrigem,
                    latitudeDestino: coordenadas.latitudeDestino,
                    longitudeDestino: coordenadas.longitudeDestino
                }
            });

            setCoordenadasMapa(coordenadas);
            setMensagemMapa('Coordenadas geocodificadas e salvas.');
            await carregarRota(entregaMapa.id);
        } catch (error) {
            tratarErro(error, 'Não foi possível geocodificar os endereços.');
            setMensagemMapa('Falha na geocodificação — verifique os endereços ou tente mais tarde.');
        } finally {
            setGeocodificandoMapa(false);
        }
    };

    useEffect(() => {
        const workspace = workspaceRef.current;
        const conteudo = workspaceContentRef.current;
        if (!workspace || !conteudo) return undefined;

        atualizarIndicadorRolagem();
        window.addEventListener('resize', atualizarIndicadorRolagem);

        const observer = typeof ResizeObserver === 'undefined'
            ? null
            : new ResizeObserver(atualizarIndicadorRolagem);
        observer?.observe(workspace);
        observer?.observe(conteudo);

        return () => {
            window.removeEventListener('resize', atualizarIndicadorRolagem);
            observer?.disconnect();
        };
    }, [atualizarIndicadorRolagem, entregas, totalPaginas, mensagem]);

    useEffect(() => () => {
        window.clearTimeout(transicaoTimeoutRef.current);
    }, []);

    const transicionarPainel = (abrir) => {
        if (transicaoTimeoutRef.current) return;

        const movimentoReduzido = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
        const duracaoSaida = movimentoReduzido ? 0 : 220;
        const duracaoEntrada = movimentoReduzido ? 0 : 620;

        setTransicaoPainel(abrir ? 'saindo-capa' : 'saindo-painel');
        transicaoTimeoutRef.current = window.setTimeout(() => {
            if (workspaceRef.current) {
                workspaceRef.current.scrollTop = 0;
            }

            setPainelEntregasAberto(abrir);
            setWorkspaceNoFim(false);
            setTransicaoPainel(abrir ? 'entrando-painel' : 'entrando-capa');

            transicaoTimeoutRef.current = window.setTimeout(() => {
                setTransicaoPainel('');
                transicaoTimeoutRef.current = null;
            }, duracaoEntrada);
        }, duracaoSaida);
    };

    const acessarEntregas = () => transicionarPainel(true);
    const voltarParaInicio = () => transicionarPainel(false);

    const rolarWorkspace = () => {
        const workspace = workspaceRef.current;
        if (!workspace) return;

        workspace.scrollTo({
            top: workspaceNoFim
                ? 0
                : workspace.scrollTop + workspace.clientHeight * 0.78,
            behavior: 'smooth'
        });
    };

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

                await request(
                    `/entregas/${id}/finalizar`,
                    {
                        method: 'PUT'
                    }
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
                        onClick={() => abrirRastreio(item)}
                        style={
                            styles.btnSecondary
                        }
                    >
                        Rastrear
                    </button>

                    <button
                        type="button"
                        disabled={
                            emAndamento
                        }
                        onClick={() => abrirCustos(item)}
                        style={
                            styles.btnSecondary
                        }
                    >
                        Custos
                    </button>

                    <button
                        type="button"
                        disabled={
                            emAndamento
                        }
                        onClick={() => abrirMapa(item)}
                        style={
                            styles.btnSecondary
                        }
                    >
                        Mapa
                    </button>

                    {item.status === 'EM_TRANSITO' && (
                        <button
                            type="button"
                            disabled={
                                emAndamento
                            }
                            onClick={() => abrirComprovante(item)}
                            style={
                                styles.btnSecondary
                            }
                        >
                            Comprovante
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

                <main
                    ref={workspaceRef}
                    className="sgfl-dashboard-scroll"
                    onScroll={atualizarIndicadorRolagem}
                    style={styles.workspace}
                >
                    <div
                        ref={workspaceContentRef}
                        style={styles.workspaceContent}
                    >
                        {!painelEntregasAberto ? (
                        <section
                            className={`sgfl-delivery-cover sgfl-delivery-cover-gate ${transicaoPainel === 'saindo-capa' ? 'sgfl-panel-transition-exit' : transicaoPainel === 'entrando-capa' ? 'sgfl-panel-transition-enter sgfl-panel-transition-enter-cover' : ''}`}
                            style={{ ...styles.deliveryCover, ...styles.deliveryCoverGate }}
                        >
                            <div style={styles.deliveryCoverCopy}>
                                <span style={styles.deliveryCoverEyebrow}>
                                    SGFL / CONTROLE DE ENTREGAS
                                </span>
                                <h1 style={styles.deliveryCoverTitle}>
                                    Entregas em <em>movimento.</em>
                                </h1>
                                <p style={styles.deliveryCoverText}>
                                    Acompanhe cada etapa da operação, encontre uma entrega e mantenha os recursos no caminho certo.
                                </p>
                                <button
                                    type="button"
                                    onClick={acessarEntregas}
                                    disabled={Boolean(transicaoPainel)}
                                    style={styles.deliveryCoverButton}
                                >
                                    <span>Acessar entregas</span>
                                    <ScrollArrowIcon />
                                </button>
                            </div>

                            <div className="sgfl-delivery-cover-art" style={styles.deliveryCoverArt} aria-hidden="true">
                                <div style={styles.deliveryCoverOrbit}>
                                    <div style={styles.deliveryCoverPackage}>
                                        <PackageIcon />
                                    </div>
                                    <div className="sgfl-delivery-orbit-spinner">
                                        <span style={styles.deliveryCoverNode} />
                                    </div>
                                </div>
                                <span style={styles.deliveryCoverCaption}>
                                    OPERAÇÃO / 01
                                </span>
                            </div>
                        </section>
                        ) : (
                        <div
                            className={`sgfl-panel-transition-content ${transicaoPainel === 'saindo-painel' ? 'sgfl-panel-transition-exit' : transicaoPainel === 'entrando-painel' ? 'sgfl-panel-transition-enter' : ''}`}
                            style={styles.panelTransitionContent}
                        >
                        <div style={styles.workspaceTitleBar}>
                            <div>
                                <span style={styles.deliveryCoverEyebrow}>SGFL / OPERAÇÃO</span>
                                <h1 style={styles.workspaceTitle}>Painel de entregas</h1>
                            </div>
                            <button
                                type="button"
                                onClick={voltarParaInicio}
                                disabled={Boolean(transicaoPainel)}
                                style={styles.workspaceBackButton}
                            >
                                Voltar ao início
                            </button>
                        </div>

                <section
                    className="sgfl-summary"
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
                            totalMotoristas
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
                            totalVeiculos
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

                {kpisOperacionais && (
                    <section
                        aria-label="Indicadores operacionais"
                        style={{
                            display: 'grid',
                            gridTemplateColumns: 'repeat(5, minmax(0, 1fr))',
                            gap: '10px',
                            marginBottom: '16px'
                        }}
                    >
                        {[
                            ['Hoje', kpisOperacionais.entregasHoje ?? 0],
                            ['Atrasadas', kpisOperacionais.atrasadas ?? 0],
                            ['Concluídas', kpisOperacionais.concluidasHoje ?? 0],
                            ['Peso', `${Number(kpisOperacionais.pesoTransportadoKg ?? 0).toLocaleString('pt-BR')} kg`],
                            ['Veículos livres', kpisOperacionais.veiculosDisponiveis ?? 0]
                        ].map(([label, value]) => (
                            <div
                                key={label}
                                style={{
                                    padding: '12px 14px',
                                    border: `1px solid ${theme.border}`,
                                    borderRadius: '14px',
                                    backgroundColor: theme.surfaceAlt,
                                    backdropFilter: 'blur(14px)',
                                    WebkitBackdropFilter: 'blur(14px)'
                                }}
                            >
                                <div style={{
                                    color: theme.inkSoft,
                                    fontSize: '9px',
                                    letterSpacing: '0.10em',
                                    textTransform: 'uppercase',
                                    fontWeight: 800
                                }}>
                                    {label}
                                </div>
                                <strong style={{
                                    display: 'block',
                                    marginTop: '4px',
                                    fontSize: '17px'
                                }}>
                                    {value}
                                </strong>
                            </div>
                        ))}
                    </section>
                )}

                {alertasOperacionais.length > 0 && (
                    <section
                        aria-label="Alertas operacionais"
                        style={{
                            marginBottom: '16px',
                            padding: '14px',
                            border: '1px solid rgba(165, 69, 82, 0.22)',
                            borderRadius: '16px',
                            backgroundColor: 'rgba(165, 69, 82, 0.08)',
                            backdropFilter: 'blur(14px)',
                            WebkitBackdropFilter: 'blur(14px)'
                        }}
                    >
                        <div style={{
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'space-between',
                            gap: '10px',
                            marginBottom: '10px'
                        }}>
                            <strong style={{ fontSize: '12px' }}>Alertas operacionais</strong>
                            <span style={{ color: theme.inkSoft, fontSize: '10px' }}>
                                {alertasOperacionais.length} alerta(s)
                            </span>
                        </div>
                        <div style={{ display: 'grid', gap: '8px' }}>
                            {alertasOperacionais.slice(0, 5).map(alerta => (
                                <div key={alerta.id} style={{
                                    display: 'grid',
                                    gridTemplateColumns: '8px 1fr',
                                    gap: '9px',
                                    alignItems: 'start'
                                }}>
                                    <span style={{
                                        width: '7px',
                                        height: '7px',
                                        marginTop: '4px',
                                        borderRadius: '50%',
                                        backgroundColor: alerta.severidade === 'CRITICO' ? theme.danger : theme.accent
                                    }} />
                                    <div>
                                        <strong style={{ fontSize: '11px' }}>{alerta.titulo}</strong>
                                        <div style={{ color: theme.inkSoft, fontSize: '10px', lineHeight: 1.5 }}>
                                            {alerta.descricao}
                                        </div>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </section>
                )}

                <div
                    className="sgfl-grid"
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

                            <div style={styles.deliveryToolbar}>
                                <label style={styles.deliverySearch}>
                                    <SearchIcon />
                                    <input
                                        type="search"
                                        value={buscaEntrega}
                                        onChange={event => setBuscaEntrega(event.target.value)}
                                        placeholder="ID, produto, endereço, motorista..."
                                        aria-label="Buscar entregas por ID, descrição, endereço, motorista ou veículo"
                                        style={styles.deliverySearchInput}
                                    />
                                </label>

                                <select
                                    value={filtroStatusEntrega}
                                    onChange={event => setFiltroStatusEntrega(event.target.value)}
                                    aria-label="Filtrar entregas por status"
                                    style={styles.deliveryStatusFilter}
                                >
                                    <option value="">Todos os status</option>
                                    <option value="PENDENTE">Pendente</option>
                                    <option value="EM_TRANSITO">Em trânsito</option>
                                    <option value="ENTREGUE">Entregue</option>
                                    <option value="CANCELADA">Cancelada</option>
                                </select>

                                <button
                                    type="button"
                                    onClick={() => carregarEntregas(pagina)}
                                    disabled={acaoEmAndamento !== null}
                                    style={styles.refreshButton}
                                >
                                    <RefreshIcon />
                                    Atualizar
                                </button>
                            </div>

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
                                    {buscaEntrega.trim() || filtroStatusEntrega
                                        ? 'Tente ajustar a busca ou o status selecionado.'
                                        : 'Crie uma nova entrega para começar.'}
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
                        )}

                    </div>

                </main>

                    {workspaceTemRolagem && (
                        <button
                            type="button"
                            className="sgfl-scroll-cue"
                            onClick={rolarWorkspace}
                            style={styles.scrollCue}
                            aria-label={workspaceNoFim ? 'Voltar ao início do painel' : 'Rolar para baixo'}
                            title={workspaceNoFim ? 'Voltar ao início' : 'Rolar para baixo'}
                        >
                            <ScrollArrowIcon direction={workspaceNoFim ? 'up' : 'down'} />
                            <span>{workspaceNoFim ? 'Topo' : 'Ver mais'}</span>
                        </button>
                    )}

            </div>

            {entregaRastreio && (
                <div style={styles.modalOverlay}>
                    <div style={{ ...styles.modal, maxWidth: '760px' }}>
                        <div style={styles.modalHeader}>
                            <div>
                                <h3 style={styles.modalTitle}>
                                    Rastreamento da entrega #{entregaRastreio.id}
                                </h3>
                                <p style={styles.modalSubtitle}>
                                    Linha do tempo das ações registradas no sistema.
                                </p>
                            </div>
                            <button
                                type="button"
                                onClick={() => setEntregaRastreio(null)}
                                style={styles.modalClose}
                            >
                                ×
                            </button>
                        </div>

                        <div style={{
                            margin: '12px 0 18px',
                            padding: '11px 13px',
                            border: `1px solid ${theme.border}`,
                            borderRadius: '12px',
                            backgroundColor: theme.surfaceAlt
                        }}>
                            <div style={{ color: theme.inkSoft, fontSize: '9px', letterSpacing: '0.10em', textTransform: 'uppercase' }}>
                                Código de rastreio
                            </div>
                            <strong style={{ display: 'block', marginTop: '4px', letterSpacing: '0.06em' }}>
                                {entregaRastreio.codigoRastreio || 'Gerado ao registrar a entrega'}
                            </strong>
                        </div>

                        {carregandoRastreio ? (
                            <div style={styles.emptyState}>Carregando histórico...</div>
                        ) : timelineRastreio.length === 0 ? (
                            <div style={styles.emptyState}>Nenhum evento registrado para esta entrega.</div>
                        ) : (
                            <div style={{ display: 'grid', gap: '11px', maxHeight: '420px', overflowY: 'auto', paddingRight: '5px' }}>
                                {timelineRastreio.map((evento, index) => (
                                    <div key={evento.id ?? index} style={{ display: 'grid', gridTemplateColumns: '12px 1fr', gap: '11px' }}>
                                        <div style={{ position: 'relative' }}>
                                            <span style={{
                                                display: 'block',
                                                width: '9px',
                                                height: '9px',
                                                marginTop: '3px',
                                                borderRadius: '50%',
                                                backgroundColor: theme.accent,
                                                boxShadow: `0 0 0 4px ${theme.accent}20`
                                            }} />
                                            {index < timelineRastreio.length - 1 && (
                                                <span style={{
                                                    position: 'absolute',
                                                    left: '4px',
                                                    top: '14px',
                                                    bottom: '-13px',
                                                    width: '1px',
                                                    backgroundColor: theme.border
                                                }} />
                                            )}
                                        </div>
                                        <div style={{ paddingBottom: '6px' }}>
                                            <div style={{ display: 'flex', justifyContent: 'space-between', gap: '10px' }}>
                                                <strong style={{ fontSize: '11px' }}>{evento.tipo}</strong>
                                                <span style={{ color: theme.inkSoft, fontSize: '9px' }}>
                                                    {evento.ocorridoEm ? new Date(evento.ocorridoEm).toLocaleString('pt-BR') : ''}
                                                </span>
                                            </div>
                                            <div style={{ color: theme.inkSoft, fontSize: '10px', marginTop: '3px' }}>
                                                {evento.observacao || 'Atualização registrada.'}
                                            </div>
                                            {evento.responsavel && (
                                                <div style={{ color: theme.inkSoft, fontSize: '9px', marginTop: '4px' }}>
                                                    Responsável: {evento.responsavel}
                                                </div>
                                            )}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                </div>
            )}

            {entregaCustos && (
                <div style={styles.modalOverlay}>
                    <div style={{ ...styles.modal, maxWidth: '640px' }}>
                        <div style={styles.modalHeader}>
                            <div>
                                <h3 style={styles.modalTitle}>
                                    Custos da entrega #{entregaCustos.id}
                                </h3>
                                <p style={styles.modalSubtitle}>
                                    Combustível, pedágio, manutenção e demais despesas desta entrega.
                                </p>
                            </div>
                            <button
                                type="button"
                                onClick={() => setEntregaCustos(null)}
                                style={styles.modalClose}
                            >
                                ×
                            </button>
                        </div>

                        <div
                            style={{
                                display: 'flex',
                                justifyContent: 'space-between',
                                gap: '10px',
                                padding: '10px 12px',
                                borderRadius: '10px',
                                background: theme.surfaceAlt,
                                border: `1px solid ${theme.border}`,
                                marginBottom: '14px',
                                fontSize: '12px'
                            }}
                        >
                            <span style={{ color: theme.inkSoft }}>Total registrado</span>
                            <strong>
                                {Number(
                                    listaCustos.reduce(
                                        (soma, custo) => soma + Number(custo.valor || 0),
                                        0
                                    )
                                ).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })}
                            </strong>
                        </div>

                        {carregandoCustos ? (
                            <div style={styles.emptyState}>Carregando custos…</div>
                        ) : listaCustos.length === 0 ? (
                            <div style={styles.emptyState}>
                                Nenhum custo registrado para esta entrega.
                            </div>
                        ) : (
                            <div style={{ display: 'grid', gap: '8px', marginBottom: '16px' }}>
                                {listaCustos.map((custo) => (
                                    <div
                                        key={custo.id}
                                        style={{
                                            display: 'flex',
                                            justifyContent: 'space-between',
                                            gap: '10px',
                                            alignItems: 'center',
                                            padding: '8px 12px',
                                            borderRadius: '10px',
                                            background: theme.surfaceAlt,
                                            border: `1px solid ${theme.border}`,
                                            fontSize: '11px'
                                        }}
                                    >
                                        <div>
                                            <strong>{custo.tipo}</strong>
                                            {custo.descricao && (
                                                <div
                                                    style={{
                                                        color: theme.inkSoft,
                                                        fontSize: '10px',
                                                        marginTop: '2px'
                                                    }}
                                                >
                                                    {custo.descricao}
                                                </div>
                                            )}
                                        </div>
                                        <div style={{ textAlign: 'right' }}>
                                            <strong>
                                                {Number(custo.valor).toLocaleString('pt-BR', {
                                                    style: 'currency',
                                                    currency: 'BRL'
                                                })}
                                            </strong>
                                            <div style={{ color: theme.inkSoft, fontSize: '9px' }}>
                                                {custo.criadoEm
                                                    ? new Date(custo.criadoEm).toLocaleString('pt-BR')
                                                    : ''}
                                            </div>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}

                        <form onSubmit={handleAdicionarCusto} style={styles.form}>
                            <div
                                style={{
                                    display: 'grid',
                                    gridTemplateColumns: '1fr 1fr',
                                    gap: '10px'
                                }}
                            >
                                <label style={styles.label}>
                                    Tipo
                                    <select
                                        value={custoTipo}
                                        onChange={(e) => setCustoTipo(e.target.value)}
                                        style={styles.input}
                                    >
                                        <option value="COMBUSTIVEL">Combustível</option>
                                        <option value="PEDAGIO">Pedágio</option>
                                        <option value="MANUTENCAO">Manutenção</option>
                                        <option value="OUTRO">Outro</option>
                                    </select>
                                </label>
                                <label style={styles.label}>
                                    Valor (R$)
                                    <input
                                        type="number"
                                        min="0.01"
                                        step="0.01"
                                        value={custoValor}
                                        onChange={(e) => setCustoValor(e.target.value)}
                                        placeholder="0,00"
                                        style={styles.input}
                                    />
                                </label>
                            </div>
                            <label style={styles.label}>
                                Descrição (opcional)
                                <input
                                    type="text"
                                    value={custoDescricao}
                                    onChange={(e) => setCustoDescricao(e.target.value)}
                                    placeholder="Ex.: abastecimento na OS340"
                                    style={styles.input}
                                />
                            </label>
                            <div style={styles.modalActions}>
                                <button
                                    type="button"
                                    onClick={() => setEntregaCustos(null)}
                                    style={styles.btnCancel}
                                >
                                    Fechar
                                </button>
                                <button
                                    type="submit"
                                    disabled={salvandoCusto}
                                    style={salvandoCusto ? styles.btnDisabled : styles.btnPrimary}
                                >
                                    {salvandoCusto ? 'Salvando…' : 'Adicionar custo'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {entregaComprovante && (
                <div style={styles.modalOverlay}>
                    <div style={{ ...styles.modal, maxWidth: '620px' }}>
                        <div style={styles.modalHeader}>
                            <div>
                                <h3 style={styles.modalTitle}>
                                    Comprovante da entrega #{entregaComprovante.id}
                                </h3>
                                <p style={styles.modalSubtitle}>
                                    Foto, assinatura e horário de quem recebeu.
                                </p>
                            </div>
                            <button
                                type="button"
                                onClick={() => setEntregaComprovante(null)}
                                style={styles.modalClose}
                            >
                                ×
                            </button>
                        </div>

                        <form onSubmit={handleSalvarComprovante} style={styles.form}>
                            <label style={styles.label}>
                                Quem recebeu *
                                <input
                                    type="text"
                                    value={nomeRecebedor}
                                    onChange={(e) => setNomeRecebedor(e.target.value)}
                                    placeholder="Nome completo de quem assina"
                                    style={styles.input}
                                />
                            </label>

                            <label style={styles.label}>
                                Foto do comprovante (opcional)
                                <input
                                    type="file"
                                    accept="image/*"
                                    capture="environment"
                                    onChange={handleSelecionarFoto}
                                    style={{ ...styles.input, padding: '8px' }}
                                />
                            </label>

                            {fotoComprovante && (
                                <div style={{ fontSize: '10px', color: theme.inkSoft }}>
                                    Arquivo selecionado: {fotoComprovante.name}
                                </div>
                            )}

                            <div style={styles.label}>Assinatura de quem recebeu</div>
                            <AssinaturaCanvas
                                onChange={setAssinaturaComprovante}
                                tema={theme}
                            />

                            <label style={styles.label}>
                                Observação (opcional)
                                <input
                                    type="text"
                                    value={observacaoComprovante}
                                    onChange={(e) => setObservacaoComprovante(e.target.value)}
                                    placeholder="Ex.: entrega conferida no balcão"
                                    style={styles.input}
                                />
                            </label>

                            <div style={styles.modalActions}>
                                <button
                                    type="button"
                                    onClick={() => setEntregaComprovante(null)}
                                    style={styles.btnCancel}
                                >
                                    Cancelar
                                </button>
                                <button
                                    type="submit"
                                    disabled={salvandoComprovante}
                                    style={
                                        salvandoComprovante ? styles.btnDisabled : styles.btnPrimary
                                    }
                                >
                                    {salvandoComprovante ? 'Salvando…' : 'Registrar comprovante'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {entregaMapa && (
                <div style={styles.modalOverlay}>
                    <div
                        style={{
                            ...styles.modal,
                            maxWidth: '760px',
                            /*
                             * Altura máxima da viewport: o conteúdo
                             * rola por dentro e cabeçalho + botões
                             * continuam visíveis.
                             */
                            maxHeight: 'calc(100vh - 40px)',
                            display: 'flex',
                            flexDirection: 'column',
                            overflow: 'hidden'
                        }}
                    >
                        <div style={styles.modalHeader}>
                            <div>
                                <h3 style={styles.modalTitle}>
                                    Mapa da entrega #{entregaMapa.id}
                                </h3>
                                <p style={styles.modalSubtitle}>
                                    {entregaMapa.enderecoOrigem || 'Origem não informada'} →{' '}
                                    {entregaMapa.enderecoDestino}
                                </p>
                            </div>
                            <button
                                type="button"
                                onClick={() => setEntregaMapa(null)}
                                style={styles.modalClose}
                            >
                                ×
                            </button>
                        </div>

                        <div
                            style={{
                                flex: '1 1 auto',
                                minHeight: 0,
                                overflowY: 'auto',
                                display: 'flex',
                                flexDirection: 'column'
                            }}
                        >
                            <MapaEntrega
                                coordenadas={coordenadasMapa}
                                rota={rotaMapa}
                                tema={theme}
                                escuro={mode === 'dark'}
                                variante={variante}
                            />

                            {mensagemMapa && (
                                <div
                                    style={{
                                        fontSize: '11px',
                                        color: theme.inkSoft,
                                        marginTop: '10px'
                                    }}
                                >
                                    {mensagemMapa}
                                </div>
                            )}

                            {etaHistorico.length > 0 && (
                                <div style={{ marginTop: '12px' }}>
                                    <div style={{ ...styles.label, marginBottom: '6px' }}>
                                        Histórico de previsões (ETA)
                                    </div>
                                    <div style={{ display: 'grid', gap: '6px' }}>
                                        {etaHistorico.slice(0, 5).map((eta) => (
                                            <div
                                                key={eta.id}
                                                style={{
                                                    display: 'flex',
                                                    justifyContent: 'space-between',
                                                    gap: '10px',
                                                    fontSize: '10px',
                                                    color: theme.inkSoft
                                                }}
                                            >
                                                <span>
                                                    {eta.distanciaKm} km · {eta.duracaoMinutos} min ·{' '}
                                                    {eta.fonte}
                                                    {eta.previsaoChegada &&
                                                        ` · chegada ${new Date(
                                                            eta.previsaoChegada
                                                        ).toLocaleString('pt-BR')}`}
                                                </span>
                                                <span>
                                                    {eta.criadoEm
                                                        ? new Date(eta.criadoEm).toLocaleString('pt-BR')
                                                        : ''}
                                                </span>
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            )}
                        </div>

                        <div style={{ ...styles.modalActions, flexShrink: 0 }}>
                            <button
                                type="button"
                                onClick={() => setEntregaMapa(null)}
                                style={styles.btnCancel}
                            >
                                Fechar
                            </button>
                            <button
                                type="button"
                                disabled={geocodificandoMapa}
                                onClick={geocodificarEnderecos}
                                style={
                                    geocodificandoMapa ? styles.btnDisabled : styles.btnSecondary
                                }
                            >
                                {geocodificandoMapa ? 'Geocodificando…' : 'Geocodificar endereços'}
                            </button>
                        </div>
                    </div>
                </div>
            )}

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

                                <input
                                    type="search"
                                    value={buscaVeiculo}
                                    onChange={event => {
                                        setCarregandoRecursos(true);
                                        setBuscaVeiculo(event.target.value);
                                    }}
                                    style={styles.input}
                                    placeholder="Filtrar por placa ou modelo"
                                    aria-label="Filtrar veículos por placa ou modelo"
                                />

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
                                            : opcoesVeiculo.length
                                                ? 'Selecione um veículo'
                                                : 'Nenhum veículo encontrado'}
                                    </option>

                                    {opcoesVeiculo.map(
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

                                <input
                                    type="search"
                                    value={buscaMotorista}
                                    onChange={event => {
                                        setCarregandoRecursos(true);
                                        setBuscaMotorista(event.target.value);
                                    }}
                                    style={styles.input}
                                    placeholder="Filtrar por nome ou CPF"
                                    aria-label="Filtrar motoristas por nome ou CPF"
                                />

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
                                            : opcoesMotorista.length
                                                ? 'Selecione um motorista'
                                                : 'Nenhum motorista encontrado'}
                                    </option>

                                    {opcoesMotorista.map(
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
                        background: radial-gradient(circle, rgba(${theme.accentRgb}, 0.34) 0%, rgba(${theme.accentRgb}, 0) 72%);
                    }

                    .sgfl-page-dark .sgfl-ambient-b {
                        width: 420px;
                        height: 420px;
                        top: 25%;
                        right: -175px;
                        background: radial-gradient(circle, rgba(${theme.ambientRgb}, 0.14) 0%, rgba(${theme.ambientRgb}, 0) 72%);
                    }

                    .sgfl-page-dark .sgfl-ambient-c {
                        width: 360px;
                        height: 360px;
                        bottom: -145px;
                        right: 20%;
                        background: radial-gradient(circle, rgba(${theme.accentRgb}, 0.18) 0%, rgba(${theme.accentRgb}, 0) 74%);
                    }

                    .sgfl-page-dark .sgfl-grid-overlay {
                        position: absolute;
                        inset: 0;
                        z-index: 0;
                        pointer-events: none;
                        opacity: 0.76;
                        background-image:
                            radial-gradient(circle, rgba(${theme.inkRgb},0.055) 0.7px, transparent 0.8px),
                            linear-gradient(rgba(${theme.inkRgb},0.016) 1px, transparent 1px),
                            linear-gradient(90deg, rgba(${theme.inkRgb},0.016) 1px, transparent 1px);
                        background-size: 8px 8px, 54px 54px, 54px 54px;
                        mask-image: linear-gradient(to bottom, rgba(0,0,0,0.72), transparent 92%);
                        -webkit-mask-image: linear-gradient(to bottom, rgba(0,0,0,0.72), transparent 92%);
                    }

                    .sgfl-scroll-cue svg {
                        animation: sgfl-scroll-bob 1.7s ease-in-out infinite;
                    }

                    .sgfl-panel-transition-exit {
                        animation: sgfl-panel-exit 220ms cubic-bezier(0.4, 0, 1, 1) both;
                        pointer-events: none;
                        will-change: transform, opacity, filter;
                        backface-visibility: hidden;
                    }

                    .sgfl-panel-transition-enter {
                        animation: sgfl-panel-enter 620ms cubic-bezier(0.22, 1, 0.36, 1) both;
                        will-change: transform, opacity, filter;
                        backface-visibility: hidden;
                    }

                    .sgfl-panel-transition-enter-cover {
                        animation-name: sgfl-cover-enter;
                    }

                    @keyframes sgfl-panel-exit {
                        from {
                            opacity: 1;
                            transform: translate3d(0, 0, 0) scale(1);
                            filter: blur(0);
                        }
                        to {
                            opacity: 0;
                            transform: translate3d(0, -8px, 0) scale(0.995);
                            filter: blur(2px);
                        }
                    }

                    @keyframes sgfl-panel-enter {
                        from {
                            opacity: 0;
                            transform: translate3d(0, 18px, 0) scale(0.99);
                            filter: blur(2px);
                        }
                        32% {
                            filter: blur(0);
                        }
                        to {
                            opacity: 1;
                            transform: translate3d(0, 0, 0) scale(1);
                            filter: blur(0);
                        }
                    }

                    @keyframes sgfl-cover-enter {
                        from {
                            opacity: 0;
                            transform: translate3d(0, 12px, 0) scale(0.995);
                            filter: blur(2px);
                        }
                        35% {
                            filter: blur(0);
                        }
                        to {
                            opacity: 1;
                            transform: translate3d(0, 0, 0) scale(1);
                            filter: blur(0);
                        }
                    }

                    .sgfl-delivery-orbit-spinner {
                        position: absolute;
                        inset: 0;
                        border-radius: 50%;
                        transform: translateZ(0);
                        transform-origin: 50% 50%;
                        backface-visibility: hidden;
                        will-change: transform;
                        animation: sgfl-orbit-spin 6s linear infinite;
                    }

                    @keyframes sgfl-orbit-spin {
                        to { transform: translateZ(0) rotate(1turn); }
                    }

                    @keyframes sgfl-scroll-bob {
                        0%, 100% { transform: translateY(-2px); }
                        50% { transform: translateY(2px); }
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
                            0 12px 34px rgba(${theme.hoverRgb}, 0.20),
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
                                rgba(${theme.selectRgb}, 0.34),
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
                                rgba(${theme.accentRgb},0.15),
                                rgba(${theme.inkRgb},0.03)
                            );
                        backdrop-filter: blur(22px) saturate(120%);
                        -webkit-backdrop-filter: blur(22px) saturate(120%);
                        box-shadow:
                            inset 0 1px 0 rgba(255,255,255,0.09),
                            0 10px 30px rgba(0,0,0,0.20);
                    }

                    .sgfl-page-dark .sgfl-blur-btn:hover {
                        border-color: rgba(${theme.accentRgb},0.56) !important;
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

                        .sgfl-delivery-cover {
                            grid-template-columns: 1fr !important;
                            min-height: 0 !important;
                            padding: 25px !important;
                        }

                        .sgfl-delivery-cover-art {
                            display: none !important;
                        }

                        .sgfl-dashboard-scroll {
                            padding-right: 3px !important;
                        }
                    }

                    @media (prefers-reduced-motion: reduce) {
                        .sgfl-scroll-cue svg {
                            animation: none;
                        }

                        .sgfl-delivery-orbit-spinner {
                            animation: none;
                        }

                        .sgfl-panel-transition-exit,
                        .sgfl-panel-transition-enter {
                            animation-duration: 1ms !important;
                            will-change: auto;
                        }
                    }
                `}
            </style>

            {/*
             * O launcher "Clientes e produtos" precisa viver DENTRO de
             * .sgfl-page (isolation: isolate): fora daqui, o z-index do
             * seu botão flutuante fica numa pilha acima dos modais do
             * dashboard e cobria os botões de ação.
             */}
            <GestaoComercial />

        </div>
    );
}
