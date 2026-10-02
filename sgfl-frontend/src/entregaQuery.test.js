import test from 'node:test';
import assert from 'node:assert/strict';
import { buildEntregaQuery } from './entregaQuery.js';

test('monta a paginação e remove espaços do filtro de texto', () => {
    const params = new URLSearchParams(buildEntregaQuery({ page: 3, size: 10, search: '  produto T5  ' }));
    assert.equal(params.get('page'), '3');
    assert.equal(params.get('size'), '10');
    assert.equal(params.get('q'), 'produto T5');
    assert.equal(params.has('status'), false);
});

test('inclui o status selecionado sem descartar a busca', () => {
    const params = new URLSearchParams(buildEntregaQuery({ search: '#48', status: 'ENTREGUE' }));
    assert.equal(params.get('q'), '#48');
    assert.equal(params.get('status'), 'ENTREGUE');
});

test('omite filtros vazios', () => {
    const params = new URLSearchParams(buildEntregaQuery({ search: '  ', status: '' }));
    assert.deepEqual([...params.keys()], ['page', 'size']);
});
