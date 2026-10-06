# SGFL - Sistema de Gestão de Frota e Logística

Sistema web com API REST em Spring Boot e frontend React para gestão de frota, entregas, clientes, produtos, estoque e pedidos. Inclui autenticação JWT, regras de negócio, tratamento centralizado de erros e testes automatizados.

Funcionalidades em destaque:

- **Portal público de rastreio** — cada entrega recebe um código único `SGFL-XXXXXXXXXXXX`, consultável sem login em `/rastreio/{codigo}`.
- **Operação avançada** — KPIs, alertas, linha do tempo, comprovante de entrega com foto/assinatura, custos por entrega, estimativa de rota/ETA, relatórios CSV e PDF e importação em CSV.
- **Gestão comercial** — clientes, produtos, estoque e pedidos com reserva de estoque em transação.
- **Operação pronta para produção** — Flyway, logs estruturados JSON com *correlation ID*, rate limiting por IP, CSRF, health/readiness e métricas Prometheus.

---

## Sumário

- [Arquitetura](#arquitetura)
- [Tecnologias](#tecnologias)
- [Telas e rotas do frontend](#telas-e-rotas-do-frontend)
- [Autenticação](#autenticação)
- [API — endpoints](#api--endpoints)
- [Regras de negócio](#regras-de-negócio)
- [Variáveis de ambiente](#variáveis-de-ambiente)
- [Execução com Docker (recomendado)](#execução-com-docker-recomendado)
- [Execução portátil (sem Docker e sem PostgreSQL)](#execução-portátil-sem-docker-e-sem-postgresql)
- [Desenvolvimento manual](#desenvolvimento-manual)
- [Testes e integração contínua](#testes-e-integração-contínua)
- [Recursos implementados](#recursos-implementados)
- [Estrutura do repositório](#estrutura-do-repositório)
- [Roadmap](#roadmap)
- [Documentação relacionada](#documentação-relacionada)

---

## Arquitetura

Aplicação em três partes: interface React, API REST em Spring Boot e PostgreSQL. O frontend fala com a API por HTTP (cookies JWT e CSRF); somente o backend acessa o banco.

Consulte [`ARCHITECTURE.md`](ARCHITECTURE.md) para o mapa completo dos módulos de frota, entregas e gestão comercial.

O código está separado em `controller`, `service`, `repository`, `me` (entidades JPA), `dto`, `security`, `exceptions`, `ratelimit`, `logging` e `config`. As respostas da API usam DTOs, mantendo os contratos HTTP separados das entidades JPA.

---

## Tecnologias

**Backend**

- Java 17, Spring Boot 3.5.16 (Web, Data JPA, Security, Validation, Actuator, Redis)
- PostgreSQL 15 em produção/desenvolvimento, Flyway para migrações e H2 nos testes gerais
- Testcontainers + Docker (PostgreSQL 15 e Redis reais nos testes de integração)
- Redis compartilhado para rate limiting no Docker; buckets em memória no modo portátil
- Micrometer/Prometheus, health/readiness, logs estruturados em JSON
- JWT (jjwt) para autenticação stateless via cookie `HttpOnly`
- JUnit 5, Mockito, AssertJ, Spring Security Test
- Maven (wrapper incluso: `./mvnw`)

**Frontend**

- React 19 + Vite 8 + React Router, Axios e ícones Lucide
- Tema único vinho escuro com layout responsivo e tokens centralizados em `theme.js`/`useTheme.js`
- PWA básico: `manifest.webmanifest` e service worker (`sw.js`)
- Lint com Oxlint, testes com `node --test` e Playwright

**Infraestrutura**

- GitHub Actions com 3 jobs: testes do backend, build/lint/testes do frontend e testes de navegador (E2E)
- Playwright: valida os principais fluxos no navegador com contrato de API simulado
- k6: roteiro de carga somente de leitura para a API de entregas (`performance/`)

---

## Telas e rotas do frontend

| Rota | Tela | Acesso |
|---|---|---|
| `/` | Login (`Login.jsx`) | público |
| `/rastreio/{codigo}` | Portal público de rastreio (`RastreioPublico.jsx`) | público, sem login |
| `/` autenticado | Dashboard de frota e entregas (`Dashboard.jsx`) | autenticado |
| `/` autenticado | Gestão comercial: clientes, produtos e pedidos (`GestaoComercial.jsx`) | autenticado |
| `/` autenticado | Cadastro de motoristas e veículos (`CadastroRecursos.jsx`) | autenticado (escrita = ADMIN) |

`App.jsx` valida a sessão (`/api/auth/csrf` + `/api/auth/session`), reage à expiração do token e compõe as telas. `api.js` centraliza as chamadas Axios, o cookie CSRF (`X-XSRF-TOKEN`) e o tratamento de 401. A listagem paginada de entregas vive no hook `useEntregaList.js`.

---

## Autenticação

- `GET /api/auth/csrf` — **público**; entrega o cookie CSRF usado pelo Axios nas operações que alteram dados.
- `POST /api/auth/registrar` — cadastro público; cria sempre um usuário `ROLE_OPERADOR` (senha de 8 a 72 caracteres). O `username` informado também é usado como e-mail de login.
- `POST /api/auth/login` — autentica por **email** e senha e configura um cookie de sessão JWT `HttpOnly`.
- `GET /api/auth/session` — verifica se o cookie representa uma sessão válida.
- `POST /api/auth/logout` — encerra a sessão removendo o cookie.

O navegador envia o cookie JWT automaticamente nas chamadas à API. Ele é `HttpOnly`, `SameSite=Lax` e expira em 24h por padrão (configurável via `JWT_EXPIRATION_MS`). Um cookie CSRF separado é enviado pelo Axios como header `X-XSRF-TOKEN` em operações que alteram dados. Em produção com HTTPS, configure `APP_COOKIE_SECURE=true`.

Sem token, ou com token inválido/expirado, a API responde **401**. Autenticado, mas sem permissão para a operação, responde **403**.

### Perfis e permissões

Regras aplicadas em `SecurityConfig`:

| Operação | `ROLE_OPERADOR` | `ROLE_ADMIN` |
|---|:---:|:---:|
| Consultar `/api/rastreio/{codigo}` | público | público |
| Listar entregas, motoristas, veículos, clientes, produtos e pedidos | sim | sim |
| Criar, alocar, finalizar e cancelar entregas; criar pedidos | sim | sim |
| Cadastrar/editar clientes (`POST`/`PUT` `/api/clientes/**`) | sim | sim |
| Cadastrar/editar motoristas e veículos (`POST`/`PUT`) | não | sim |
| Cadastrar/editar produtos e ajustar estoque (`POST`/`PUT` `/api/produtos/**`) | não | sim |
| Qualquer `DELETE` em `/api/**` | não | sim |

### Primeiro administrador

Não existe nenhuma credencial fixa no código. Para criar o primeiro administrador, defina as variáveis abaixo **antes de subir a aplicação** (o `BootstrapAdminInitializer` só cria o usuário se ele ainda não existir e **nunca altera a senha de um usuário existente**):

```
BOOTSTRAP_ADMIN_ENABLED=true
BOOTSTRAP_ADMIN_EMAIL=admin@suaempresa.com
BOOTSTRAP_ADMIN_USERNAME=admin
BOOTSTRAP_ADMIN_PASSWORD=uma-senha-forte-com-8-ou-mais-caracteres
```

Depois do primeiro acesso, pode voltar `BOOTSTRAP_ADMIN_ENABLED` para `false`.

---

## API — endpoints

> Base URL de desenvolvimento: `http://localhost:8080/api`. Todas as rotas exigem autenticação, exceto as marcadas como **público**.

### Autenticação

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| GET | `/api/auth/csrf` | público | Entrega o token CSRF |
| POST | `/api/auth/registrar` | público | Cria usuário `ROLE_OPERADOR` |
| POST | `/api/auth/login` | público | Login por e-mail/senha, define cookie JWT |
| GET | `/api/auth/session` | autenticado | Valida a sessão atual |
| POST | `/api/auth/logout` | autenticado | Remove o cookie de sessão |

### Entregas

| Método | Rota | Descrição |
|---|---|---|
| GET | `/api/entregas?page=0&size=20&status=&q=` | Lista paginada, ordenada por `id`, com filtros de status e busca |
| POST | `/api/entregas` | Cria uma entrega (descrição, destino, peso, status inicial, janela de agendamento, coordenadas e `valorFrete`) |
| PATCH | `/api/entregas/{id}/status` | Atualiza o status (transições válidas) |
| PUT | `/api/entregas/{id}/alocar?veiculoId=&motoristaId=` | Aloca veículo/motorista (valida peso, CNH e disponibilidade) |
| PUT | `/api/entregas/{id}/finalizar` | Marca a entrega como concluída |
| DELETE | `/api/entregas/{id}` | Remove uma entrega |
| GET | `/api/entregas/{id}/timeline` | Linha do tempo de eventos da entrega |
| GET | `/api/entregas/{id}/comprovante` | Consulta o comprovante (recebedor, assinatura, foto) |
| POST | `/api/entregas/{id}/comprovante` | Envia o comprovante (`multipart/form-data`) |
| GET | `/api/entregas/{id}/rota` | Estimativa de distância, duração e ETA |

### Operação e relatórios

| Método | Rota | Descrição |
|---|---|---|
| GET | `/api/operacional/kpis` | KPIs do painel operacional |
| GET | `/api/operacional/alertas` | Alertas (atrasadas, sem recurso, fora do prazo) |
| GET | `/api/operacional/entregas/{id}/custos` | Custos lançados na entrega |
| POST | `/api/operacional/entregas/{id}/custos` | Lança um custo (tipo, descrição, valor ≥ 0) |
| GET | `/api/relatorios/entregas.csv?status=&q=` | Exportação CSV das entregas |
| GET | `/api/relatorios/entregas.pdf?status=&q=` | Exportação PDF (gerado sem dependência externa) |
| POST | `/api/importacao/{clientes\|produtos\|entregas}` | Importa um CSV (`arquivo`, UTF-8, separador `;` ou `,`) |

### Rastreio público

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| GET | `/api/rastreio/{codigo}` | público | Consulta a entrega pelo código `SGFL-XXXXXXXXXXXX` |

### Frota

| Método | Rota | Permissão |
|---|---|---|
| GET | `/api/motoristas?page=&size=&q=` | autenticado (busca por nome/CPF) |
| POST | `/api/motoristas` | ADMIN |
| PUT | `/api/motoristas/{id}` | ADMIN |
| DELETE | `/api/motoristas/{id}` | ADMIN |
| GET | `/api/veiculos?page=&size=&q=` | autenticado (busca por placa/modelo) |
| POST | `/api/veiculos/caminhao` · `/api/veiculos/furgao` | ADMIN |
| PUT | `/api/veiculos/caminhao/{id}` · `/api/veiculos/furgao/{id}` | ADMIN |
| DELETE | `/api/veiculos/{id}` | ADMIN |

### Gestão comercial

| Método | Rota | Permissão |
|---|---|---|
| GET | `/api/clientes?page=&size=` · `GET /{id}` | autenticado |
| POST | `/api/clientes` · `PUT /{id}` | ADMIN ou OPERADOR |
| DELETE | `/api/clientes/{id}` | ADMIN |
| GET | `/api/produtos?page=&size=` · `GET /{id}` | autenticado |
| POST | `/api/produtos` · `PUT /{id}` · `PUT /{id}/estoque` | ADMIN |
| DELETE | `/api/produtos/{id}` | ADMIN |
| GET | `/api/pedidos?page=&size=` · `GET /{id}` | autenticado |
| POST | `/api/pedidos` | autenticado |
| PATCH | `/api/pedidos/{id}/cancelar` | autenticado |

### Contrato comum de listagem

Todas as listagens usam `page` e `size`, com **20 itens por padrão e máximo de 100**, e resposta no formato:

```json
{
  "content": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 87,
  "totalPages": 5
}
```

Motoristas e veículos também aceitam `q` para filtrar por nome/CPF ou placa/modelo. A busca de recursos na alocação consulta o servidor e continua encontrando registros além dos primeiros 100.

### Formato de erro

Todos os erros seguem o mesmo envelope (o `requestId` acompanha o log estruturado da requisição):

```json
{
  "timestamp": "...",
  "status": 400,
  "error": "Dados inválidos",
  "message": "Um ou mais campos são inválidos",
  "campos": { "descricao": "A descrição é obrigatória" },
  "requestId": "9426f4fa-..."
}
```

---

## Regras de negócio

### Entregas

1. **Capacidade de carga**: uma entrega só pode ser alocada a um veículo se `pesoCargaKg` for menor ou igual à capacidade do veículo (`VeiculoIncompativelException`, HTTP 400).
2. **CNH do motorista**: caminhão exige categoria `D` ou `E`; furgão exige `B`, `C`, `D` ou `E`.
3. **Status da entrega**: `PENDENTE` → `EM_TRANSITO` → `ENTREGUE`; `PENDENTE` ou `EM_TRANSITO` → `CANCELADA`.
4. **Alocação exclusiva**: um veículo e um motorista só podem estar em uma entrega `EM_TRANSITO` por vez — garantido no serviço **e** por índices únicos parciais no banco (migration V5), protegendo contra requisições simultâneas.
5. **Janela de agendamento**: a data final não pode ser anterior à inicial, e veículo/motorista já reservados no mesmo intervalo de horário são recusados.
6. **Finalização**: exige status `EM_TRANSITO` com veículo e motorista alocados.
7. **Exclusão**: não é possível remover uma entrega `EM_TRANSITO` (cancele ou finalize antes).
8. **Placa e CPF são únicos**; o CPF precisa ter dígitos verificadores válidos.
9. **Código de rastreio**: gerado automaticamente (`SGFL-XXXXXXXXXXXX`), `NOT NULL` e único (migration V7).

### Pedidos e estoque

- Pedido só é criado para cliente ativo, com quantidade dentro do estoque disponível.
- A criação **reserva o estoque em transação**; cancelar um pedido devolve a quantidade.
- Somente pedidos `ABERTOS` podem ser cancelados.

---

## Variáveis de ambiente

Nada de senha ou segredo no `application.properties` — tudo via variáveis de ambiente. As que não têm padrão são obrigatórias: a aplicação não sobe sem `DB_PASSWORD` e `JWT_SECRET`.

| Variável | Descrição | Padrão (dev) |
|---|---|---|
| `DB_URL` | URL JDBC do Postgres | `jdbc:postgresql://localhost:5432/sgfl_db` |
| `DB_USERNAME` | Usuário do banco | `postgres` |
| `DB_PASSWORD` | Senha do banco | **obrigatória** (sem padrão) |
| `DB_POOL_SIZE` | Tamanho máximo do pool Hikari | `10` |
| `JWT_SECRET` | Chave de assinatura do token (mínimo 32 caracteres) | **obrigatória** (sem padrão) |
| `JWT_EXPIRATION_MS` | Validade do token (ms) | `86400000` (24h) |
| `BOOTSTRAP_ADMIN_ENABLED` / `_EMAIL` / `_USERNAME` / `_PASSWORD` | Criação do primeiro administrador (veja acima) | desligado |
| `CORS_ALLOWED_ORIGINS` | Origens do front-end permitidas, separadas por vírgula | `http://localhost:5173,http://localhost:3000` |
| `APP_COOKIE_SECURE` | Exige HTTPS para enviar o cookie JWT; habilite atrás de proxy TLS | `false` (dev local) |
| `FORWARD_HEADERS_STRATEGY` | `native` atrás de proxy (nginx), `none` se exposta diretamente | `native` |
| `FLYWAY_ENABLED` | Ativa a execução das migrações | `true` |
| `DDL_AUTO` | Estratégia de DDL do Hibernate | `validate` |
| `SHOW_SQL` | Exibe as consultas no log | `false` |
| `RATE_LIMIT_ENABLED` | Habilita rate limiting por IP | `true` |
| `RATE_LIMIT_STORAGE` | Contadores (`memory` ou `redis`) | `memory` (Compose: `redis`) |
| `RATE_LIMIT_AUTH_CAPACITY` / `RATE_LIMIT_AUTH_TOKENS` | Limite em `/api/auth/**` | `15` por minuto |
| `RATE_LIMIT_API_CAPACITY` / `RATE_LIMIT_API_TOKENS` | Limite geral em `/api/**` | `120` por minuto |
| `REDIS_PASSWORD` | Senha opcional do Redis interno | vazio |
| `MANAGEMENT_SERVER_PORT` | Porta de health/métricas (Actuator) | `8081` |
| `MANAGEMENT_SERVER_ADDRESS` | Endereço da porta de gerenciamento | `127.0.0.1` |
| `SGFL_ETA_AVG_SPEED_KMH` | Velocidade média usada na estimativa de rota/ETA | `50` |
| `SGFL_STOCK_MODE` | Baixa de estoque dos pedidos: `IMEDIATA` (padrão) ou `RESERVA` (baixa na conclusão/despacho) | `IMEDIATA` |
| `SGFL_GEOCODER_URL` | Base do geocodificador (compatível com Nominatim) | `https://nominatim.openstreetmap.org` |
| `SGFL_GEOCODER_USER_AGENT` | User-Agent enviado ao geocodificador (identificação obrigatória do Nominatim) | `SGFL-LogiTech/1.0` |
| `SGFL_ROUTER_URL` | Base do roteador externo (compatível com OSRM); vazio/inacessível cai para Haversine | `https://router.project-osrm.org` |
| `SGFL_ROUTER_PROVIDER` | Provedor de rota: `OSRM` (padrão, sem chave) ou `TOMTOM` (rota com **trânsito em tempo real**) | `OSRM` |
| `SGFL_ROUTER_API_KEY` | Chave do provedor (obrigatória em `TOMTOM`; gratuita em developer.tomtom.com) | vazio |
| `SGFL_MAIL_ENABLED` | Habilita o envio de e-mail em mudanças de status da entrega | `false` |
| `SGFL_MAIL_HOST` / `SGFL_MAIL_PORT` | Servidor SMTP e porta | vazio / `587` |
| `SGFL_MAIL_USERNAME` / `SGFL_MAIL_PASSWORD` | Credenciais SMTP (opcional em servidores que aceitam relaying) | vazio |
| `SGFL_MAIL_FROM` | Remetente das mensagens | `noreply@sgfl.local` |
| `SGFL_MAIL_TO` | Destinatários separados por vírgula | vazio |
| `SGFL_MAIL_STARTTLS` | Usa STARTTLS na conexão SMTP | `true` |
| `SGFL_WHATSAPP_ENABLED` | Habilita o aviso de mudança de status por WhatsApp | `false` |
| `SGFL_WHATSAPP_API_URL` | Endpoint HTTP do gateway no contrato JSON `{"to","from","message"}` | vazio |
| `SGFL_WHATSAPP_TOKEN` | Token enviado como `Authorization: Bearer` (opcional) | vazio |
| `SGFL_WHATSAPP_FROM` | Identificação do remetente no payload | `SGFL` |
| `SGFL_WHATSAPP_TO` | Números de destino separados por vírgula (BR: 10–11 dígitos ganham DDI 55) | vazio |
| `SGFL_PUBLIC_URL` | URL pública do painel usada no link de rastreio dos avisos (e-mail/WhatsApp) | `http://localhost:5173` |
| `SGFL_UPLOAD_DIR` | Pasta de armazenamento das fotos de comprovante | `uploads/comprovantes` |

No IntelliJ: **Run/Debug Configurations → Environment Variables**.

---

## Execução com Docker (recomendado)

Orquestração completa via **Docker Compose**: PostgreSQL 15, backend Spring Boot e frontend React servido por Nginx com proxy reverso.

### Antes de subir: crie o arquivo `.env`

Na raiz do projeto (já está no `.gitignore`; nunca o versione), copie `.env.example` e substitua os valores:

```
POSTGRES_PASSWORD=troque-esta-senha
JWT_SECRET=troque-por-uma-chave-aleatoria-com-mais-de-32-caracteres
REDIS_PASSWORD=use-outra-senha-forte
BOOTSTRAP_ADMIN_ENABLED=true
BOOTSTRAP_ADMIN_EMAIL=admin@suaempresa.com
BOOTSTRAP_ADMIN_PASSWORD=uma-senha-forte
```

### Subir todo o ambiente com um comando

```bash
docker compose up --build -d
```

- **Frontend (Web)**: [http://localhost:5173](http://localhost:5173) ou [http://localhost](http://localhost)
- **Backend (API)**: [http://localhost:8080/api](http://localhost:8080/api)
- **PostgreSQL Docker**: `localhost:5433` por padrão (database `sgfl_db`, usuário `postgres`; altere com `POSTGRES_HOST_PORT` se precisar)
- **Redis**: privado na rede do Compose; os contadores de rate limit não são expostos no host

```bash
docker compose logs -f backend   # logs
docker compose down              # parar serviços
```

Para métricas e alertas locais, suba o Prometheus com `docker compose --profile observability up -d` — painel em `http://localhost:9090`. A porta de gerenciamento do backend (8081) fica apenas na rede Docker; health, readiness/liveness e métricas ficam nela.

Procedimentos de backup, restauração e operação estão em [`docs/OPERATIONS.md`](docs/OPERATIONS.md). Para publicar usando PostgreSQL e Redis gerenciados, veja [`docker-compose.production.yml`](docker-compose.production.yml) e a seção de deploy do mesmo documento — domínio, TLS e segredos ficam no provedor de hospedagem.

---

## Execução portátil (sem Docker e sem PostgreSQL)

O perfil `local` usa H2 embutido em arquivo e serve a interface React pelo próprio backend: não é preciso instalar PostgreSQL, Docker, Nginx ou Node na máquina que só vai executar um JAR já compilado.

Para compilar a partir do código-fonte, instale **Java 17+** e **Node.js 20.19+ (ou 22.12+)** com npm. Na primeira compilação, Maven e npm baixam as dependências pela internet.

```powershell
# Windows PowerShell: compila frontend + backend e inicia a aplicação
.\run-local.ps1
# Se o Windows bloquear o script:
powershell -NoProfile -ExecutionPolicy Bypass -File .\run-local.ps1
```

```bash
# Linux/macOS
bash ./run-local.sh
```

Na primeira execução o script pede e-mail, usuário e senha do administrador. Depois, abra [http://localhost:8080](http://localhost:8080). O banco local fica em `data/sgfl.mv.db` e permanece salvo entre execuções (a pasta `data/` está ignorada pelo Git).

- JAR já compilado: `.\run-local.ps1 -NoBuild` (Windows) ou `bash ./run-local.sh --no-build` (Linux/macOS), sem precisar de Node/npm.
- **Pacote para outra máquina**: `powershell -ExecutionPolicy Bypass -File .\package-portable.ps1` ou `bash ./package-portable.sh`. O resultado vai para `portable-runtime/` e para `sgfl-portatil.zip`. A máquina de destino precisa apenas de **Java 17+** — sem Node, Maven, PostgreSQL ou Docker. Na primeira abertura, o iniciador solicita o administrador local.
- Para levar os dados de uma instalação existente, pare a aplicação e copie a pasta `data/` junto do pacote.

O H2 local serve para rodar uma cópia isolada do sistema. Ele não importa os dados do PostgreSQL/Docker nem atende várias máquinas ao mesmo tempo — para uso compartilhado, mantenha PostgreSQL.

---

## Desenvolvimento manual (frontend e backend separados)

Útil para desenvolvimento com hot reload do Vite. O backend no perfil padrão exige PostgreSQL e as variáveis de ambiente configuradas; para evitar isso, use os scripts portáteis acima.

### 1. Backend

```bash
mvn spring-boot:run      # Maven global
./mvnw spring-boot:run   # Linux/macOS (wrapper incluso)
.\mvnw spring-boot:run   # Windows
```

API em `http://localhost:8080`.

### 2. Frontend

```bash
cd sgfl-frontend
npm install
npm run dev
```

Interface em `http://localhost:5173`.

---

## Testes e integração contínua

### Local

```bash
mvn test            # backend (ou .\mvnw test)
cd sgfl-frontend && npm test       # frontend (node --test)
npm run lint                     # oxlint
npm run build                    # build de produção
```

Os testes gerais usam H2 em memória e **não tocam no seu Postgres local**. Com Docker disponível, os Testcontainers sobem PostgreSQL 15 e Redis reais; sem Docker, esses testes são marcados como ignorados.

### Ponta a ponta e carga

```bash
cd sgfl-frontend
npm ci
npx playwright install chromium
npm run test:e2e
```

O teste de navegador (`e2e/operacao.spec.js`) isola a interface de uma API falsa, tornando-o reproduzível na CI. O script de carga k6 está em `performance/sgfl-api-load.js` (somente leitura); veja o roteiro e os limites em [`docs/OPERATIONS.md`](docs/OPERATIONS.md).

### O que a suíte cobre

| Área | Testes |
|---|---|
| Unitário | `JwtServiceTest` (geração/validação de JWT), `SistemaLogisticaTest` (alocação, CNH, status), `PedidoServiceTest` (reserva e devolução de estoque) |
| Persistência | `EntregaRepositoryTest` (paginação/ordenação), `RecursosRepositoryTest` (busca de motoristas e veículos) |
| Contrato | `PaginationTest`, `PageResponseTest` (formato de página), `GlobalExceptionHandlerTest` (erros de constraint) |
| Controller | `EntregaControllerTest` (validação, exclusão, erros), `MotoristaControllerCpfTest` (dígitos verificadores do CPF) |
| Integração | `AutenticacaoIntegrationTest` (login, cadastro, 401/403 e permissões por perfil) |
| Migrações | `FlywayMigrationTest` (aplica todas as migrations em PostgreSQL real e valida unicidade/índices) |
| Rate limiting | `RateLimitingFilterTest` (429 e headers), `RedisRateLimiterIntegrationTest` (duas instâncias compartilham tokens no Redis) |
| Frontend | `entregaQuery.test.js` (filtros de página, status e busca) |
| Navegador (E2E) | `operacao.spec.js` — login, busca de entrega, credenciais inválidas e logout |

### CI (GitHub Actions)

O workflow [`.github/workflows/ci.yml`](.github/workflows/ci.yml) roda a cada `push` e `pull request` em `master`/`main`:

1. **backend-tests** — `mvn -B verify` (testes H2 + Testcontainers) e publica os relatórios do Surefire.
2. **frontend-build** — `npm ci`, `npm run lint`, `npm test` e `npm run build` no Node 22.
3. **browser-e2e** — instala o Chromium e executa `npm run test:e2e`, publicando o relatório do Playwright.

---

## Recursos implementados

### 1. Migrações versionadas (Flyway)

- O banco é gerenciado de forma determinística pelo **Flyway**; o Hibernate roda em `validate`, então a aplicação só sobe se o esquema bater com os mapeamentos JPA.
- Scripts em `src/main/resources/db/migration/` (11 migrações, `V1`..`V11`):
   - `V1__create_tables.sql` — tabelas `usuarios`, `veiculo`, `caminhao`, `furgao`, `motorista`, `entrega`, índices e FKs.
   - `V2__seed_initial_data.sql` — carga idempotente de motoristas de demonstração.
   - `V3__carregar_dados_locais.sql` — carga histórica do ambiente local (não edite: o Flyway valida o checksum).
   - `V4__corrigir_nomes_colunas.sql` — ajusta nomes de colunas para os mapeamentos JPA.
   - `V5__integridade_dados_e_concorrencia.sql` — normaliza dados, cria as constraints únicas de placa/CPF e os índices contra dupla alocação.
   - `V6__clientes_produtos_estoque_pedidos.sql` — tabelas do módulo comercial (`cliente`, `produto`, `estoque`, `pedido`, `item_pedido`).
   - `V7__operacao_avancada.sql` — código de rastreio, janela de agendamento, coordenadas/geocodificação, `valor_frete`, linha do tempo (`entrega_evento`), comprovante e custos.
   - `V8__latitude_longitude_double_precision.sql` — converte as coordenadas de `NUMERIC(9,6)` para `DOUBLE PRECISION`, alinhando o banco ao mapeamento `Double` das entidades (sem isso o `validate` do Hibernate impede a subida do backend).
   - `V9__auditoria_transversal.sql` — tabela `auditoria_registro` para histórico de criação/alteração/exclusão de cliente, produto e pedido (`entidade`, `acao`, `dados_antes`/`dados_depois` em JSON).
   - `V10__eta_historico.sql` — tabela `entrega_eta` para histórico de estimativas por entrega (`distancia_km`, `duracao_minutos`, `previsao_chegada`, `fonte`), um registro a cada 5 min.
   - `V11__estoque_reservado.sql` — coluna `estoque.quantidade_reservada` para o modo `SGFL_STOCK_MODE=RESERVA` (bloqueio na criação do pedido, baixa na conclusão).
- **Regra**: migrations são só estrutura e dados de referência — nunca dumps nem dados de demonstração novos.

### 2. Logging estruturado (JSON / correlation ID)

- Em produção/Docker, os logs saem em **JSON estruturado** (`logstash-logback-encoder`), prontos para Elasticsearch, Loki, CloudWatch ou Datadog.
- O `StructuredLoggingFilter` injeta no MDC: `requestId`, `clientIp`, `httpMethod`, `uri`, `status`, `durationMs`.
- O `requestId` é devolvido no header `X-Request-ID` e nos erros do `GlobalExceptionHandler`, permitindo rastrear a requisição ponta a ponta.
- Em desenvolvimento local, mantém o formato colorido legível no console.

### 3. Rate limiting

- Token Bucket por IP, com políticas configuráveis:
  - **`/api/auth/**`**: 15 requisições/min por IP.
  - **`/api/**`**: 120 requisições/min por IP.
- Headers em cada resposta: `X-Rate-Limit-Remaining` e, ao exceder, `Retry-After`.
- No Compose, o bucket é atualizado atomicamente no **Redis** (relógio do próprio Redis), compartilhado entre réplicas. O modo portátil usa buckets locais em memória.
- Se o Redis indisponível no modo compartilhado, a API falha fechada com `503` em vez de remover a proteção silenciosamente.

```json
{
  "timestamp": "2026-09-28T...",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Limite de requisições excedido. Tente novamente em 27 segundo(s).",
  "requestId": "9426f4fa-..."
}
```

### 4. Segurança

- JWT em cookie `HttpOnly`, `SameSite=Lax`, com CSRF próprio enviado por header `X-XSRF-TOKEN`.
- Senhas com BCrypt, CORS restrito às origens configuradas, autorização por perfil aplicada no `SecurityConfig`.
- Nenhuma credencial fixa no código; primeiro admin criado por variáveis de ambiente.

---

## Estrutura do repositório

```
PROJETO-SGFL_LogiTech/
├── src/                          # Backend Spring Boot
│   ├── main/java/com/logitech/sgfl/
│   │   ├── controller/           # Endpoints REST
│   │   ├── service/              # Regras de negócio
│   │   ├── repository/           # Spring Data JPA + specifications
│   │   ├── me/                   # Entidades JPA
│   │   ├── dto/                  # Contratos de entrada/saída
│   │   ├── security/             # Spring Security + JWT
│   │   ├── exceptions/           # Exceções e GlobalExceptionHandler
│   │   ├── ratelimit/            # Token Bucket (memória e Redis)
│   │   ├── logging/              # Logs estruturados + correlation ID
│   │   └── config/               # Bootstrap do admin, paginação
│   ├── main/resources/db/migration/   # Flyway V1..V11
│   └── test/java/                # Suíte JUnit 5 + Testcontainers
├── sgfl-frontend/                # React + Vite
│   ├── src/                      # Telas, hooks e api.js
│   └── e2e/                      # Testes Playwright
├── docs/                         # OPERATIONS.md, ROADMAP_IMPLEMENTADO.md
├── scripts/                      # Backup/restore do PostgreSQL
├── performance/                  # Carga k6 e scripts SQL
├── observability/                # Regras do Prometheus
├── deployment/                   # Exemplo de .env de produção
├── .github/workflows/ci.yml      # CI (backend, frontend, E2E)
├── docker-compose.yml            # Ambiente completo
├── docker-compose.production.yml # Publicação com serviços gerenciados
├── Dockerfile
├── ARCHITECTURE.md · SECURITY.md · README.md
└── run-local.* · package-portable.*   # Execução portátil
```

---

## Roadmap

### Concluído

- [x] Containerização (Docker & Docker Compose)
- [x] Migrações versionadas de banco (Flyway) no lugar de `ddl-auto=update`
- [x] Logging estruturado (Logstash JSON + correlation ID)
- [x] Rate limiting por IP com buckets compartilhados por Redis e modo portátil em memória
- [x] CSRF para autenticação por cookie JWT
- [x] Health/readiness, métricas Prometheus e alertas operacionais
- [x] Scripts seguros de backup e restauração do PostgreSQL
- [x] Busca e paginação de motoristas, veículos, entregas e módulo comercial
- [x] Portal público de rastreio (`SGFL-XXXXXXXXXXXX`) com linha do tempo de eventos
- [x] Comprovante de entrega (foto, assinatura, recebedor e horário)
- [x] Dashboard de KPIs e alertas operacionais
- [x] Custos por entrega e margem sobre o valor do frete
- [x] Relatórios de entregas em CSV e PDF
- [x] Importação em CSV de clientes, produtos e entregas
- [x] Estimativa de rota/ETA e janela de agendamento
- [x] PWA básico (manifest + service worker)
- [x] CI com testes de backend, frontend e navegador
- [x] Auditoria transversal de cliente, produto e pedido (além da linha do tempo da entrega)
- [x] Notificações por e-mail SMTP em mudanças de status da entrega (desligadas por padrão)
- [x] Geocodificação dos endereços (Nominatim) e rota/ETA com provedor externo (OSRM), com queda para Haversine
- [x] Mapa da entrega no painel (Leaflet + OpenStreetMap) com geocodificação e histórico de ETA
- [x] Histórico de ETA por entrega (uma estimativa a cada 5 minutos)
- [x] Custos detalhados por categoria (combustível, pedágio, manutenção, outro) com total em R$
- [x] Comprovante pelo painel com câmera (`capture`), assinatura em canvas e observação
- [x] Fila offline no painel: ações sem conexão são guardadas e reenviadas sozinhas
- [x] Baixa de estoque no despacho/envio via modo configurável `SGFL_STOCK_MODE` (`IMEDIATA` ou `RESERVA`)
- [x] Notificações por WhatsApp via gateway HTTP configurável — mesmo aviso do e-mail, desligadas por padrão
- [x] ETA com trânsito em tempo real via provedor TomTom opcional (`SGFL_ROUTER_PROVIDER=TOMTOM` + chave gratuita)
- [x] Rota real desenhada no mapa (geometria do provedor); linha reta tracejada só quando não há rota

### Próximos passos

- [ ] Modelos de mensagem de WhatsApp editáveis por ambiente (hoje: texto fixo no código)
- [ ] Reenvio automático de avisos que falharem (hoje: a falha é apenas registrada no log)

O histórico detalhado está em [`docs/ROADMAP_IMPLEMENTADO.md`](docs/ROADMAP_IMPLEMENTADO.md).

---

## Documentação relacionada

| Documento | Conteúdo |
|---|---|
| [`ARCHITECTURE.md`](ARCHITECTURE.md) | Mapa da arquitetura e dos módulos |
| [`docs/OPERATIONS.md`](docs/OPERATIONS.md) | Backup, restauração, deploy, limites de carga e rotinas operacionais |
| [`docs/ROADMAP_IMPLEMENTADO.md`](docs/ROADMAP_IMPLEMENTADO.md) | Evolução do roadmap do projeto |
| [`SECURITY.md`](SECURITY.md) | Política de segurança e divulgação de vulnerabilidades |
| [`.env.example`](.env.example) | Modelo de configuração para o Docker Compose |
