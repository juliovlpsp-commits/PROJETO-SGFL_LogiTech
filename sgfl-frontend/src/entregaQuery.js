export function buildEntregaQuery({ page = 0, size = 10, search = '', status = '' } = {}) {
    const params = new URLSearchParams({ page: String(page), size: String(size) });
    const normalizedSearch = search.trim();
    if (normalizedSearch) params.set('q', normalizedSearch);
    if (status) params.set('status', status);
    return params.toString();
}
