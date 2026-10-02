import { useCallback, useEffect, useRef, useState } from 'react';
import { buildEntregaQuery } from './entregaQuery';

const TAMANHO_PAGINA = 10;

/** Owns delivery list pagination, debounced filters, and stale-request protection. */
export default function useEntregaList({ request, onError, search, status }) {
    const [entregas, setEntregas] = useState([]);
    const [pagina, setPagina] = useState(0);
    const [totalPaginas, setTotalPaginas] = useState(0);
    const requestSequence = useRef(0);

    const carregarEntregas = useCallback(async (paginaAlvo = 0) => {
        const sequence = ++requestSequence.current;
        try {
            const query = buildEntregaQuery({
                page: paginaAlvo,
                size: TAMANHO_PAGINA,
                search,
                status
            });
            const data = await request(`/entregas?${query}`);
            if (sequence !== requestSequence.current) return;

            setEntregas(Array.isArray(data?.content) ? data.content : []);
            setPagina(Number(data?.number ?? data?.page ?? paginaAlvo));
            setTotalPaginas(Number(data?.totalPages ?? 0));
        } catch (error) {
            if (sequence === requestSequence.current) {
                onError(error, 'Não foi possível carregar as entregas.');
            }
        }
    }, [request, onError, search, status]);

    useEffect(() => {
        // Invalidate an older request immediately when the filters change, then
        // wait briefly before querying so typing does not flood the API.
        requestSequence.current += 1;
        const delay = search.trim() ? 350 : 0;
        const timeout = window.setTimeout(() => carregarEntregas(0), delay);
        return () => window.clearTimeout(timeout);
    }, [search, status, carregarEntregas]);

    return { entregas, pagina, totalPaginas, carregarEntregas };
}
