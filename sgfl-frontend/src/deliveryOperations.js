const STATUS_LABELS = {
    PENDENTE: 'Pendente',
    EM_TRANSITO: 'Em trânsito',
    ENTREGUE: 'Entregue',
    CANCELADA: 'Cancelada'
};

export const DELIVERY_STATUS_LABELS = STATUS_LABELS;

export function listarItensOperacionais(response) {
    if (Array.isArray(response)) return response;

    const candidates = [
        response?.content,
        response?.items,
        response?.alertas,
        response?.eventos,
        response?.timeline,
        response?.data
    ];

    return candidates.find(Array.isArray) || [];
}

export function isOptionalEndpointUnavailable(error) {
    const status = error?.status ?? error?.response?.status;
    return status === 404 || status === 405 || status === 501;
}

export function formatarDataHora(valor) {
    if (!valor) return null;

    const data = new Date(valor);
    if (Number.isNaN(data.getTime())) return null;

    return data.toLocaleString('pt-BR', {
        dateStyle: 'short',
        timeStyle: 'short'
    });
}

export function calcularMetricasLocais(entregas = []) {
    const entregasSeguras = Array.isArray(entregas) ? entregas : [];
    const contar = status => entregasSeguras.filter(entrega => entrega?.status === status).length;
    const emTransito = contar('EM_TRANSITO');
    const pendentes = contar('PENDENTE');
    const entregues = contar('ENTREGUE');
    const canceladas = contar('CANCELADA');
    const pesoEmTransito = entregasSeguras
        .filter(entrega => entrega?.status === 'EM_TRANSITO')
        .reduce((total, entrega) => total + (Number(entrega?.pesoCargaKg) || 0), 0);

    return {
        totalEntregas: entregasSeguras.length,
        pendentes,
        emTransito,
        entregues,
        canceladas,
        pesoEmTransito,
        entregasAtrasadas: 0
    };
}

export function normalizarMetricasOperacionais(response, fallback) {
    const source = response || {};
    const pick = (...keys) => {
        for (const key of keys) {
            const value = source?.[key];
            if (value !== null && value !== undefined && !Number.isNaN(Number(value))) {
                return Number(value);
            }
        }
        return undefined;
    };

    return {
        totalEntregas: pick('totalEntregas', 'total', 'entregasTotal') ?? fallback.totalEntregas,
        pendentes: pick('pendentes', 'entregasPendentes') ?? fallback.pendentes,
        emTransito: pick('emTransito', 'em_transito', 'entregasEmTransito') ?? fallback.emTransito,
        entregues: pick('entregues', 'concluidas', 'entregasEntregues') ?? fallback.entregues,
        canceladas: pick('canceladas', 'entregasCanceladas') ?? fallback.canceladas,
        pesoEmTransito: pick('pesoEmTransito', 'pesoEmTransitoKg', 'pesoTransportadoKg') ?? fallback.pesoEmTransito,
        entregasAtrasadas: pick('entregasAtrasadas', 'atrasadas', 'emAtraso') ?? fallback.entregasAtrasadas
    };
}

export function criarAlertasLocais(entregas = []) {
    const alertas = [];

    for (const entrega of entregas || []) {
        if (!entrega) continue;

        if (entrega.status === 'PENDENTE') {
            alertas.push({
                id: `pendente-${entrega.id}`,
                severidade: 'ATENCAO',
                titulo: `Entrega #${entrega.id} aguardando alocação`,
                descricao: 'Associe um motorista e um veículo para iniciar o transporte.',
                entregaId: entrega.id,
                status: entrega.status
            });
        }

        if (entrega.status === 'EM_TRANSITO' && (!entrega.motorista || !entrega.veiculo)) {
            alertas.push({
                id: `recurso-${entrega.id}`,
                severidade: 'CRITICO',
                titulo: `Entrega #${entrega.id} está sem recurso identificado`,
                descricao: 'Confira o motorista e o veículo vinculados à entrega em trânsito.',
                entregaId: entrega.id,
                status: entrega.status
            });
        }

        if (!entrega.enderecoDestino?.trim()) {
            alertas.push({
                id: `destino-${entrega.id}`,
                severidade: 'ATENCAO',
                titulo: `Entrega #${entrega.id} sem destino completo`,
                descricao: 'Complete o endereço de destino antes de seguir com a operação.',
                entregaId: entrega.id,
                status: entrega.status
            });
        }
    }

    return alertas;
}

export function normalizarAlertas(response, fallback = []) {
    const items = listarItensOperacionais(response);
    if (!items.length) return fallback;

    return items.map((item, index) => ({
        id: item.id ?? `alerta-${index}`,
        severidade: String(item.severidade ?? item.nivel ?? item.prioridade ?? 'ATENCAO').toUpperCase(),
        titulo: item.titulo ?? item.mensagem ?? item.descricao ?? 'Atenção operacional',
        descricao: item.descricao ?? item.detalhe ?? item.mensagem ?? '',
        entregaId: item.entregaId ?? item.idEntrega ?? item.entrega?.id ?? null,
        status: item.status ?? item.entrega?.status ?? null
    }));
}

export function criarTimelineFallback(entrega) {
    if (!entrega) return [];

    const eventos = [{
        id: `criada-${entrega.id}`,
        status: 'PENDENTE',
        titulo: 'Entrega registrada',
        descricao: 'A entrega foi criada e está pronta para a alocação.',
        data: entrega.criadaEm ?? entrega.createdAt ?? null
    }];

    if (entrega.veiculo || entrega.motorista || entrega.status === 'EM_TRANSITO' || entrega.status === 'ENTREGUE') {
        eventos.push({
            id: `alocada-${entrega.id}`,
            status: 'EM_TRANSITO',
            titulo: 'Recursos alocados',
            descricao: `${entrega.motorista?.nome || 'Motorista'} e ${entrega.veiculo?.placa || 'veículo'} vinculados à entrega.`,
            data: entrega.alocadaEm ?? null
        });
    }

    if (entrega.status === 'ENTREGUE') {
        eventos.push({
            id: `entregue-${entrega.id}`,
            status: 'ENTREGUE',
            titulo: 'Entrega concluída',
            descricao: 'A operação foi finalizada com sucesso.',
            data: entrega.entregueEm ?? entrega.finalizadaEm ?? null
        });
    }

    if (entrega.status === 'CANCELADA') {
        eventos.push({
            id: `cancelada-${entrega.id}`,
            status: 'CANCELADA',
            titulo: 'Entrega cancelada',
            descricao: 'A operação foi interrompida antes da conclusão.',
            data: entrega.canceladaEm ?? null
        });
    }

    return eventos;
}

export function normalizarTimeline(response, entrega) {
    const eventos = listarItensOperacionais(response);
    if (!eventos.length) return criarTimelineFallback(entrega);

    return eventos.map((evento, index) => ({
        id: evento.id ?? `evento-${index}`,
        status: evento.status ?? evento.statusEntrega ?? entrega?.status ?? 'PENDENTE',
        titulo: evento.titulo ?? evento.tipo ?? evento.nome ?? 'Atualização operacional',
        descricao: evento.descricao ?? evento.mensagem ?? evento.detalhe ?? '',
        data: evento.ocorridoEm ?? evento.dataHora ?? evento.criadoEm ?? evento.createdAt ?? null,
        responsavel: evento.responsavel ?? evento.autor ?? evento.usuario ?? null
    }));
}

export function obterCodigoRastreio(entrega) {
    if (!entrega) return '';
    return String(entrega.codigoRastreio ?? entrega.codigo ?? entrega.trackingCode ?? entrega.id ?? '');
}

export function buscarEntregaLocal(entregas, termo) {
    const normalizado = String(termo || '').trim().toLowerCase().replace(/^#/, '');
    if (!normalizado) return null;

    return (entregas || []).find(entrega => [
        entrega?.id,
        entrega?.codigoRastreio,
        entrega?.codigo,
        entrega?.trackingCode
    ].some(valor => String(valor ?? '').toLowerCase() === normalizado)) || null;
}

export function rotuloStatus(status) {
    return STATUS_LABELS[status] || status || 'Em atualização';
}
