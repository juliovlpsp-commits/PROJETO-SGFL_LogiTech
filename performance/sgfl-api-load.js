import http from 'k6/http';
import { check, fail, sleep } from 'k6';

const baseUrl = (__ENV.K6_BASE_URL || 'http://localhost:5173/api').replace(/\/$/, '');
const maxVus = Math.max(1, Number(__ENV.K6_MAX_VUS || 20));
const holdDuration = __ENV.K6_HOLD_DURATION || '2m';

export const options = {
    scenarios: {
        read_only_api: {
            executor: 'ramping-vus',
            startVUs: 1,
            stages: [
                { duration: '30s', target: Math.ceil(maxVus / 4) },
                { duration: '1m', target: maxVus },
                { duration: holdDuration, target: maxVus },
                { duration: '30s', target: 0 }
            ],
            gracefulRampDown: '15s'
        }
    },
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<500', 'p(99)<1000']
    }
};

export function setup() {
    const username = __ENV.K6_USERNAME;
    const password = __ENV.K6_PASSWORD;
    if (!username || !password) {
        fail('Defina K6_USERNAME e K6_PASSWORD de uma conta de teste.');
    }

    const csrfResponse = http.get(`${baseUrl}/auth/csrf`, {
        tags: { flow: 'setup', endpoint: 'csrf' }
    });
    if (!check(csrfResponse, { 'CSRF respondeu 200': response => response.status === 200 })) {
        fail(`Não foi possível obter CSRF (HTTP ${csrfResponse.status}).`);
    }

    const csrfToken = csrfResponse.json('token');
    const loginResponse = http.post(
        `${baseUrl}/auth/login`,
        JSON.stringify({ username, password }),
        {
            headers: {
                'Content-Type': 'application/json',
                ...(csrfToken ? { 'X-XSRF-TOKEN': csrfToken } : {})
            },
            tags: { flow: 'setup', endpoint: 'login' }
        }
    );
    if (!check(loginResponse, { 'login respondeu 200': response => response.status === 200 })) {
        fail(`Login de carga falhou (HTTP ${loginResponse.status}); verifique conta e CSRF.`);
    }

    const sessionCookie = loginResponse.cookies.sgfl_session?.[0]?.value;
    if (!sessionCookie) {
        fail('O login não retornou o cookie sgfl_session esperado.');
    }
    return { sessionCookie };
}

export default function ({ sessionCookie }) {
    const response = http.get(`${baseUrl}/entregas?page=0&size=20`, {
        headers: { Cookie: `sgfl_session=${sessionCookie}` },
        tags: { endpoint: 'list-deliveries' }
    });

    check(response, {
        'listagem de entregas respondeu 200': result => result.status === 200,
        'resposta contém uma página': result => {
            try {
                const body = result.json();
                return Array.isArray(body.content) && Number.isInteger(body.totalElements);
            } catch {
                return false;
            }
        }
    });
    sleep(1);
}
