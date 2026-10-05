/**
 * Fila de ações POST/PUT/PATCH/PATCH que falharam por falta de conexão.
 *
 * Quando o dispositivo está offline o app guarda a ação em localStorage e
 * reenvia sozinho quando a rede volta (evento `online`) ou quando o app
 * é reaberto. GET nunca entra na fila. Erros de regra/servidor (HTTP)
 * descartam a ação — repetir não resolveria —, apenas falhas de rede
 * ficam pendentes.
 */

const CHAVE = 'sgfl-fila-offline';

export function lerFila() {
    try {
        const bruto = window.localStorage.getItem(CHAVE);
        const fila = bruto ? JSON.parse(bruto) : [];
        return Array.isArray(fila) ? fila : [];
    } catch {
        return [];
    }
}

function gravarFila(fila) {
    try {
        window.localStorage.setItem(CHAVE, JSON.stringify(fila));
    } catch {
        // Armazenamento cheio/indisponível: descarta a fila em vez de quebrar.
    }
}

export function enfileirar(acao) {
    const fila = lerFila();

    fila.push({
        url: acao.url,
        method: acao.method,
        body: acao.body ?? null,
        criadoEm: new Date().toISOString()
    });

    // Limite de segurança para não estourar o localStorage.
    while (fila.length > 50) {
        fila.shift();
    }

    gravarFila(fila);
    return fila.length;
}

/**
 * Reenvia a fila na ordem em que foi gravada.
 *
 * @param {(url: string, options: object) => Promise<any>} request
 * @returns {Promise<number>} quantidade que continua pendente
 */
export async function reenviarFila(request) {
    const fila = lerFila();
    const pendentes = [];

    for (let indice = 0; indice < fila.length; indice += 1) {
        const acao = fila[indice];

        try {
            await request(acao.url, {
                method: acao.method,
                body: acao.body ?? undefined
            });
        } catch (error) {
            if (error?.code === 'NETWORK_ERROR') {
                // Ainda offline: guarda o restante e tenta de novo depois.
                pendentes.push(...fila.slice(indice));
                break;
            }
            // HTTP (regra de negócio/servidor): descarta — retry não resolve.
            console.warn('SGFL - ação offline descartada:', acao.url, error?.message);
        }
    }

    gravarFila(pendentes);
    return pendentes.length;
}
