import { expect, test } from '@playwright/test';

const usuario = 'admin@sgfl.test';
const senhaValida = 'SenhaValida123!';

async function instalarApiFalsa(page) {
    const entregas = [];
    const logins = [];
    const buscas = [];

    await page.route('**/api/**', async route => {
        const request = route.request();
        const url = new URL(request.url());
        const path = url.pathname.replace(/^\/api/, '');
        const metodo = request.method();

        if (path === '/auth/csrf') {
            return route.fulfill({
                status: 200,
                contentType: 'application/json',
                headers: { 'set-cookie': 'XSRF-TOKEN=e2e-token; Path=/; SameSite=Lax' },
                body: JSON.stringify({
                    headerName: 'X-XSRF-TOKEN',
                    parameterName: '_csrf',
                    token: 'e2e-token'
                })
            });
        }

        if (path === '/auth/session') {
            return route.fulfill({ status: 401, body: '' });
        }

        if (path === '/auth/login' && metodo === 'POST') {
            const body = request.postDataJSON();
            logins.push({ ...body, csrf: request.headers()['x-xsrf-token'] });
            if (body.username !== usuario || body.password !== senhaValida) {
                return route.fulfill({ status: 401, body: '' });
            }
            return route.fulfill({
                status: 200,
                headers: { 'set-cookie': 'sgfl_session=e2e-session; Path=/api; HttpOnly; SameSite=Lax' },
                body: ''
            });
        }

        if (path === '/auth/logout' && metodo === 'POST') {
            return route.fulfill({ status: 204, body: '' });
        }

        if (path === '/entregas' && metodo === 'GET') {
            const termo = url.searchParams.get('q')?.toLocaleLowerCase('pt-BR') || '';
            const status = url.searchParams.get('status') || '';
            if (termo) buscas.push(termo);
            const filtradas = entregas.filter(entrega => {
                const correspondeAoTermo = !termo || [
                    String(entrega.id),
                    entrega.descricao,
                    entrega.enderecoOrigem,
                    entrega.enderecoDestino
                ].some(valor => valor?.toLocaleLowerCase('pt-BR').includes(termo));
                return correspondeAoTermo && (!status || entrega.status === status);
            });
            return route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({
                    content: filtradas,
                    number: Number(url.searchParams.get('page') || 0),
                    totalPages: filtradas.length ? 1 : 0,
                    totalElements: filtradas.length,
                    size: Number(url.searchParams.get('size') || 10),
                    first: true,
                    last: true
                })
            });
        }

        if (path === '/entregas' && metodo === 'POST') {
            const body = request.postDataJSON();
            const novaEntrega = {
                id: 51,
                descricao: body.descricao,
                enderecoOrigem: body.enderecoOrigem,
                enderecoDestino: body.enderecoDestino,
                pesoCargaKg: body.pesoCargaKg,
                status: 'PENDENTE'
            };
            entregas.unshift(novaEntrega);
            return route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify(novaEntrega)
            });
        }

        const entregaMatch = path.match(/^\/entregas\/(\d+)(?:\/(alocar|finalizar|status))?$/);
        if (entregaMatch) {
            const id = Number(entregaMatch[1]);
            const acao = entregaMatch[2];
            const entrega = entregas.find(item => item.id === id);

            if (metodo === 'DELETE') {
                const index = entregas.findIndex(item => item.id === id);
                if (index >= 0) entregas.splice(index, 1);
                return route.fulfill({ status: 204, body: '' });
            }

            if (metodo === 'PUT' && acao === 'alocar' && entrega) {
                entrega.status = 'EM_TRANSITO';
                entrega.veiculo = { id: Number(url.searchParams.get('veiculoId')), modelo: 'Furgão', placa: 'E2E1234' };
                entrega.motorista = { id: Number(url.searchParams.get('motoristaId')), nome: 'Motorista de teste', tipoCNH: 'B' };
                return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(entrega) });
            }

            if (metodo === 'PUT' && acao === 'finalizar' && entrega) {
                entrega.status = 'ENTREGUE';
                return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(entrega) });
            }

            if (metodo === 'PATCH' && acao === 'status' && entrega) {
                entrega.status = request.postDataJSON().status;
                return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(entrega) });
            }
        }

        if (path === '/veiculos' && metodo === 'GET') {
            const veiculos = [{ id: 5, placa: 'E2E1234', modelo: 'Furgão', capacidadeCargaKg: 1200 }];
            return route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({ content: veiculos, number: 0, totalPages: 1, totalElements: veiculos.length, size: 100 })
            });
        }

        if (path === '/motoristas' && metodo === 'GET') {
            const motoristas = [{ id: 7, nome: 'Motorista de teste', cpf: '12345678901', tipoCNH: 'B' }];
            return route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({ content: motoristas, number: 0, totalPages: 1, totalElements: motoristas.length, size: 100 })
            });
        }

        if (metodo === 'GET') {
            return route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({ content: [], number: 0, totalPages: 0, totalElements: 0, size: 20 })
            });
        }

        return route.fulfill({ status: 204, body: '' });
    });

    return { logins, buscas };
}

async function entrar(page, password = senhaValida) {
    await page.goto('/');
    await page.getByLabel('E-mail').fill(usuario);
    await page.getByLabel('Senha').fill(password);
    await page.getByRole('button', { name: /entrar/i }).click();
}

test('faz login, registra e pesquisa uma entrega, e encerra a sessão', async ({ page }) => {
    const api = await instalarApiFalsa(page);

    await entrar(page);
    await expect(page.getByRole('button', { name: /acessar entregas/i })).toBeVisible();
    expect(api.logins[0]).toMatchObject({ username: usuario, password: senhaValida, csrf: 'e2e-token' });

    await page.getByRole('button', { name: /acessar entregas/i }).click();
    await expect(page.getByRole('heading', { name: 'Painel de entregas' })).toBeVisible();

    await page.getByPlaceholder('Ex.: Encomenda de equipamentos').fill('Caixa frágil');
    await page.getByPlaceholder('Centro de distribuição').fill('Centro de Fortaleza');
    await page.getByPlaceholder('Av. Central, 500').fill('Rua das Flores, 120');
    await page.getByPlaceholder('2500').fill('75');
    await page.getByRole('button', { name: 'Registrar entrega' }).click();

    await expect(page.getByText('Entrega criada com sucesso.')).toBeVisible();
    await expect(page.getByText('Caixa frágil')).toBeVisible();

    const linhaEntrega = page.getByRole('row').filter({ hasText: '#51' });
    await page.getByRole('button', { name: 'Alocar' }).click();
    const formularioAlocacao = page.locator('form').last();
    await expect(formularioAlocacao.locator('select').nth(0)).toBeEnabled();
    await formularioAlocacao.locator('select').nth(0).selectOption('5');
    await formularioAlocacao.locator('select').nth(1).selectOption('7');
    await formularioAlocacao.getByRole('button', { name: 'Confirmar alocação' }).click();
    await expect(linhaEntrega.getByText('Em trânsito')).toBeVisible();

    page.once('dialog', dialog => dialog.accept());
    await page.getByRole('button', { name: 'Finalizar' }).click();
    await expect(linhaEntrega.getByText('Entregue')).toBeVisible();

    await page.getByRole('searchbox', {
        name: 'Buscar entregas por ID, descrição, endereço, motorista ou veículo'
    }).fill('Caixa frágil');
    await expect.poll(() => api.buscas).toContain('caixa frágil');
    await expect(page.getByText('Caixa frágil')).toBeVisible();

    page.once('dialog', dialog => dialog.accept());
    await page.getByRole('button', { name: 'Excluir' }).click();
    await expect(page.getByText('Caixa frágil')).toHaveCount(0);

    await page.getByRole('button', { name: 'Sair' }).click();
    await expect(page.getByRole('heading', { name: 'Entrar no sistema' })).toBeVisible();
});

test('informa credenciais inválidas sem abrir o painel', async ({ page }) => {
    await instalarApiFalsa(page);

    await entrar(page, 'senha-incorreta');

    await expect(page.getByRole('alert')).toContainText('Credenciais inválidas.');
    await expect(page.getByRole('button', { name: /acessar entregas/i })).toHaveCount(0);
});
