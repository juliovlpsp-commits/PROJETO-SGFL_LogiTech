import { useCallback, useEffect, useRef, useState } from 'react';

const DURACAO_SAIDA = 150;
const DURACAO_ENTRADA = 320;

export default function useTabTransition(abaInicial) {
    const [aba, setAba] = useState(abaInicial);
    const [faseTransicao, setFaseTransicao] = useState('');
    const timeoutRef = useRef(null);
    const abaRef = useRef(abaInicial);
    const transicionandoRef = useRef(false);

    useEffect(() => () => {
        window.clearTimeout(timeoutRef.current);
    }, []);

    const trocarAba = useCallback((proximaAba) => {
        if (proximaAba === abaRef.current || transicionandoRef.current) {
            return false;
        }

        transicionandoRef.current = true;
        const movimentoReduzido = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
        const duracaoSaida = movimentoReduzido ? 0 : DURACAO_SAIDA;
        const duracaoEntrada = movimentoReduzido ? 0 : DURACAO_ENTRADA;

        setFaseTransicao('saindo');
        timeoutRef.current = window.setTimeout(() => {
            abaRef.current = proximaAba;
            setAba(proximaAba);
            setFaseTransicao('entrando');

            timeoutRef.current = window.setTimeout(() => {
                setFaseTransicao('');
                transicionandoRef.current = false;
                timeoutRef.current = null;
            }, duracaoEntrada);
        }, duracaoSaida);

        return true;
    }, []);

    return {
        aba,
        trocarAba,
        faseTransicao,
        transicionando: faseTransicao !== ''
    };
}
