export function formatarPeso(valor) {
    if (
        valor === null ||
        valor === undefined ||
        Number.isNaN(Number(valor))
    ) {
        return '-';
    }

    return `${Number(valor).toLocaleString('pt-BR', {
        maximumFractionDigits: 2
    })} kg`;
}
