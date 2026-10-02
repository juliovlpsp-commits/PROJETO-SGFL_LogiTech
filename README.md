# SGFL - Sistema de Gestão de Frota e Logística

Sistema web com API REST em Spring Boot e frontend React para gestão de frota, entregas, clientes, produtos, estoque e pedidos. Inclui autenticação JWT, regras de negócio, tratamento centralizado de erros e testes automatizados.

 
---

## Tecnologias

**Backend**
- Java 17, Spring Boot 3.5.16 (Web, Data JPA, Security, Validation, Actuator)
- PostgreSQL (produção/desenvolvimento), Flyway (migrações) e H2 em memória (testes gerais)
- Testcontainers + Docker (PostgreSQL 15 e Redis reais nos testes de integração)
- Redis compartilhado para rate limiting no Docker; Bucket4j em memória no modo portátil
- Micrometer/Prometheus, health checks e logs estruturados em JSON
- JWT (jjwt) para autenticação stateless
- JUnit 5, Mockito, AssertJ, Spring Security Test
- Maven
  **Frontend**
- React + Vite
- Tema claro/escuro persistido, layout responsivo
  **Infraestrutura**
- GitHub Actions: roda a suíte de testes a cada `push`/pull request
- Playwright: valida os principais fluxos no navegador com contrato de API simulado
- k6: roteiro de carga somente de leitura para API de entregas
---

## Arquitetura

Consulte [`ARCHITECTURE.md`](ARCHITECTURE.md) para o mapa atualizado dos módulos de frota, entregas e gestão comercial.

O código está separado em controllers, serviços, repositórios, entidades, DTOs, segurança, tratamento de erros, logging e rate limiting. As respostas da API usam DTOs para manter os contratos HTTP separados das entidades JPA. O frontend nunca acessa o banco diretamente.
 
---

## Autenticação

- `POST /api/auth/registrar` — cadastro público; cria sempre um usuário `ROLE_OPERADOR` (senha de 8 a 72 caracteres). O `username` informado também é usado como e-mail de login.
- `POST /api/auth/login` — autentica por **email** e senha e configura um cookie de sessão JWT `HttpOnly`.
- `GET /api/auth/session` — verifica se o cookie representa uma sessão válida.
- `POST /api/auth/logout` — encerra a sessão removendo o cookie.

O navegador envia o cookie JWT automaticamente nas chamadas à API. Ele é `HttpOnly`, `SameSite=Lax` e expira em 24h por padrão (configurável via `JWT_EXPIRATION_MS`). Um cookie CSRF separado é enviado pelo Axios como header `X-XSRF-TOKEN` em operações que alteram dados. Em produção com HTTPS, configure `APP_COOKIE_SECURE=true`.

Sem token, ou com token inválido/expirado, a API responde **401**. Autenticado, mas sem permissão para a operação, responde **403**.

### Perfis e permissões

| Operação | `ROLE_OPERADOR` | `ROLE_ADMIN` |
|---|:---:|:---:|
| Listar entregas, motoristas e veículos | sim | sim |
| Criar, alocar, finalizar e cancelar entregas | sim | sim |
| Cadastrar/editar motoristas e veículos (`POST`/`PUT`) | não | sim |
| Qualquer `DELETE` | não | sim |

### Primeiro administrador

Não existe mais nenhum usuário com credencial fixa no código. Para criar o primeiro administrador, defina as variáveis abaixo **antes de subir a aplicação** (o `BootstrapAdminInitializer` só cria o usuário se ele ainda não existir e **nunca altera a senha de um usuário existente**):

```
BOOTSTRAP_ADMIN_ENABLED=true
BOOTSTRAP_ADMIN_EMAIL=admin@suaempresa.com
BOOTSTRAP_ADMIN_USERNAME=admin
BOOTSTRAP_ADMIN_PASSWORD=uma-senha-forte-com-8-ou-mais-caracteres
```

Depois do primeiro acesso, pode voltar `BOOTSTRAP_ADMIN_ENABLED` para `false`.
 
---

## Entregas — regras de negócio

1. **Capacidade de carga**: uma entrega só pode ser alocada a um veículo se `pesoCargaKg` for menor ou igual à capacidade do veículo. Alocação incompatível retorna `400 Bad Request` com mensagem descritiva (`VeiculoIncompativelException`).
2. **CNH do motorista**:
   - Caminhão: exige CNH categoria `D` ou `E`.
   - Furgão: exige CNH categoria `B`, `C`, `D` ou `E`.
3. **Status da entrega**: `PENDENTE` → `EM_TRANSITO` → `ENTREGUE`; `PENDENTE` ou `EM_TRANSITO` → `CANCELADA`.
4. **Um veículo e um motorista só podem estar em uma entrega `EM_TRANSITO` por vez.** Além da checagem no serviço, isso é garantido por índices únicos parciais no banco (migration V5), o que protege contra requisições simultâneas.
5. **Não é possível excluir uma entrega `EM_TRANSITO`** (cancele ou finalize antes).
6. **Placa e CPF são únicos.** O CPF precisa ter dígitos verificadores válidos.
### Endpoints principais

| Método | Rota | Descrição |
|---|---|---|
| GET | `/api/entregas?page=0&size=20` | Lista paginada, sempre ordenada por `id` |
| POST | `/api/entregas` | Cria uma entrega (valida descrição, destino e status) |
| PATCH | `/api/entregas/{id}/status` | Atualiza o status de uma entrega |
| PUT | `/api/entregas/{id}/alocar?veiculoId=&motoristaId=` | Aloca veículo/motorista (valida peso e CNH) |
| PUT | `/api/entregas/{id}/finalizar` | Marca a entrega como concluída |
| DELETE | `/api/entregas/{id}` | Remove uma entrega |

Clientes, produtos e pedidos também usam `page` e `size` nas rotas de listagem, com 20 itens por padrão e máximo de 100. A resposta inclui `content`, `page`, `size`, `totalElements` e `totalPages`; a interface comercial oferece controles de página.

Motoristas e veículos usam o mesmo contrato paginado e aceitam `q` para filtrar por nome/CPF ou placa/modelo. A busca de recursos na alocação consulta o servidor e continua encontrando registros além dos primeiros 100.

Erros seguem sempre o mesmo formato:
```json
{
  "timestamp": "...",
  "status": 400,
  "error": "Dados inválidos",
  "message": "Um ou mais campos são inválidos",
  "campos": { "descricao": "A descrição é obrigatória" }
}
```
 
---

## Rodando o projeto

### Pré-requisitos
- JDK 17+
- PostgreSQL em execução
- Node.js 22+ (para compilar o frontend)
### 1. Configurar variáveis de ambiente

Nada de senha ou segredo direto no `application.properties` — configure via variáveis de ambiente (as que não têm padrão são obrigatórias: a aplicação não sobe sem `DB_PASSWORD` e `JWT_SECRET`):

| Variável | Descrição | Padrão (dev) |
|---|---|---|
| `DB_URL` | URL JDBC do Postgres | `jdbc:postgresql://localhost:5432/sgfl_db` |
| `DB_USERNAME` | Usuário do banco | `postgres` |
| `DB_PASSWORD` | Senha do banco | **obrigatória** (sem padrão) |
| `JWT_SECRET` | Chave de assinatura do token (mínimo 32 caracteres) | **obrigatória** (sem padrão) |
| `BOOTSTRAP_ADMIN_ENABLED` / `_EMAIL` / `_USERNAME` / `_PASSWORD` | Criação do primeiro administrador (veja acima) | desligado |
| `CORS_ALLOWED_ORIGINS` | Origens do front-end permitidas, separadas por vírgula | `http://localhost:5173,http://localhost:3000` |
| `APP_COOKIE_SECURE` | Exige HTTPS para enviar o cookie JWT; habilite atrás de proxy TLS | `false` (desenvolvimento local) |
| `FORWARD_HEADERS_STRATEGY` | `native` atrás de proxy (nginx), `none` se exposta diretamente | `native` |
| `JWT_EXPIRATION_MS` | Validade do token (ms) | `86400000` (24h) |
| `FLYWAY_ENABLED` | Ativa execução de migrações automáticas | `true` |
| `DDL_AUTO` | Estratégia de DDL do Hibernate | `validate` |
| `RATE_LIMIT_ENABLED` | Habilita rate limiting por IP | `true` |
| `RATE_LIMIT_STORAGE` | Armazenamento dos contadores (`memory` ou `redis`) | `memory` (Compose: `redis`)
| `REDIS_PASSWORD` | Senha opcional do Redis interno | vazio |
| `RATE_LIMIT_AUTH_CAPACITY` | Limite de requisições em `/api/auth/**` | `15` por minuto |
| `RATE_LIMIT_API_CAPACITY` | Limite de requisições gerais em `/api/**` | `120` por minuto |

No IntelliJ: **Run/Debug Configurations → Environment Variables**.

---

## Execução com Docker (Recomendado)

O projeto possui orquestração completa via **Docker Compose**, subindo banco PostgreSQL 15, backend Spring Boot e frontend React servido por Nginx com proxy reverso.

### Antes de subir: crie o arquivo `.env`

Na raiz do projeto (ele já está no `.gitignore`; nunca o versione), copie `.env.example` e substitua os valores de exemplo:

```
POSTGRES_PASSWORD=troque-esta-senha
JWT_SECRET=troque-por-uma-chave-aleatoria-com-mais-de-32-caracteres
REDIS_PASSWORD=use-outra-senha-forte
BOOTSTRAP_ADMIN_ENABLED=true
BOOTSTRAP_ADMIN_EMAIL=admin@suaempresa.com
BOOTSTRAP_ADMIN_PASSWORD=uma-senha-forte
```

### Subir todo o ambiente com um comando:

```bash
docker compose up --build -d
```

- **Frontend (Web)**: [http://localhost:5173](http://localhost:5173) ou [http://localhost](http://localhost)
- **Backend (API)**: [http://localhost:8080/api](http://localhost:8080/api)
- **PostgreSQL Docker**: `localhost:5433` por padrão (database `sgfl_db`, user `postgres`; altere com `POSTGRES_HOST_PORT` se precisar)
- **Redis**: privado na rede do Compose; os contadores compartilhados de rate limit não são expostos no host

Para visualizar os logs:
```bash
docker compose logs -f backend
```

Para parar os serviços:
```bash
docker compose down
```

Para métricas e alertas locais, inicie o Prometheus com `docker compose --profile observability up -d`; o painel fica em `http://localhost:9090`. A porta de gerenciamento do backend (8081) permanece apenas na rede Docker. Health, readiness/liveness e métricas ficam nessa porta.

Procedimentos de backup, restauração e operação estão em [`docs/OPERATIONS.md`](docs/OPERATIONS.md).

Para publicar os containers usando PostgreSQL e Redis gerenciados, consulte [`docker-compose.production.yml`](docker-compose.production.yml) e a seção de deploy em [`docs/OPERATIONS.md`](docs/OPERATIONS.md). O domínio/TLS e os segredos ficam no provedor de hospedagem.

### Testes ponta a ponta e carga

```bash
cd sgfl-frontend
npm ci
npx playwright install chromium
npm run test:e2e
```

Os testes de navegador isolam a interface de uma API falsa para serem reproduzíveis na CI. Os testes do backend continuam cobrindo integração e regras de autorização. O teste de carga k6 e a carga sintética controlada estão em `performance/`; veja o roteiro e os limites em `docs/OPERATIONS.md`.

---

## Execução portátil (sem Docker e sem PostgreSQL)

O perfil `local` usa H2 embutido em arquivo e serve a interface React pelo próprio backend. Assim, não é preciso instalar ou iniciar PostgreSQL, Docker, Nginx ou Node na máquina que só vai executar um JAR já compilado.

Para compilar o projeto a partir do código-fonte, instale Java 17 ou superior e Node.js 20.19+ (ou 22.12+) com npm. Na primeira compilação, Maven e npm precisam baixar dependências pela internet. Na pasta raiz, use o script do seu sistema:

```powershell
# Windows PowerShell: compila frontend + backend e inicia a aplicação
.\run-local.ps1
# Se o Windows bloquear a execução do script:
powershell -NoProfile -ExecutionPolicy Bypass -File .\run-local.ps1
```

```bash
# Linux/macOS: compila frontend + backend e inicia a aplicação
bash ./run-local.sh
```

Na primeira execução, o script pede e-mail, usuário e senha do administrador. Depois, abra [http://localhost:8080](http://localhost:8080). O banco local fica em `data/sgfl.mv.db` e permanece salvo entre execuções. O diretório `data/` está ignorado pelo Git.

Se o JAR já estiver compilado dentro do projeto, `.\run-local.ps1 -NoBuild` no Windows ou `bash ./run-local.sh --no-build` em Linux/macOS inicia sem Node/npm. Para transportar o aplicativo, gere o pacote portátil abaixo; ele inclui o JAR e os iniciadores que configuram o banco e o primeiro administrador. A UI já está dentro do JAR.

Para montar uma pasta pronta para outro computador, execute `powershell -ExecutionPolicy Bypass -File .\package-portable.ps1` no Windows ou `bash ./package-portable.sh` em Linux/macOS. O resultado fica em `portable-runtime/` e também em `sgfl-portatil.zip` (o ZIP Linux/macOS é criado quando o utilitário `zip` está disponível). Copie essa pasta ou o ZIP para a outra máquina, extraia e inicie com `run-local.bat` ou `run-local.ps1` (Windows) ou `bash ./run-local.sh` (Linux/macOS). A máquina de destino precisa de Java 17 ou superior; ela não precisa de Node/npm, Maven, PostgreSQL ou Docker. Na primeira abertura, o iniciador solicita a criação do administrador local.

O pacote é compilado para a plataforma Java e pode rodar em Windows, Linux ou macOS compatíveis com Java 17+. Para levar também os dados H2 de uma instalação existente, pare a aplicação e copie a pasta `data/` junto do pacote. O pacote recém-gerado não contém dados pessoais.

O H2 local é para executar uma cópia do sistema em um computador. Ele não importa automaticamente os dados do PostgreSQL/Docker nem substitui um banco servidor para uso simultâneo por vários computadores. Para preservar os dados antigos, é preciso migrá-los separadamente; para compartilhar uma base entre máquinas, mantenha PostgreSQL.

## Desenvolvimento manual (frontend e backend separados)

Estes comandos continuam disponíveis para desenvolvimento com Vite. O backend no perfil padrão ainda exige um PostgreSQL iniciado e as variáveis de ambiente configuradas. Para não usar PostgreSQL nem Docker, prefira os scripts portáteis acima.

### 1. Rodar o backend

```bash
# Com Maven global:
mvn spring-boot:run

# Ou com o Maven Wrapper (incluso no projeto, não exige Maven instalado):
./mvnw spring-boot:run     # Linux/macOS
.\mvnw spring-boot:run     # Windows
```
A API sobe em `http://localhost:8080`.

### 2. Rodar o frontend

```bash
cd sgfl-frontend
npm install
npm run dev
```
Interface em `http://localhost:5173`.

### 3. Rodar os testes

```bash
mvn test
# ou:
.\mvnw test
```

Os testes gerais usam banco H2 em memória — não tocam no seu Postgres local. Testes Testcontainers usam PostgreSQL 15 e Redis reais quando Docker está disponível; sem Docker, os testes de integração de container são marcados como ignorados. Cobrem:
- **Unitário**: geração/validação de JWT (`JwtServiceTest`)
- **Repositório**: paginação e ordenação estável da listagem de entregas (`EntregaRepositoryTest`)
- **Controller**: validação de entrada, exclusão, erros (`EntregaControllerTest`)
- **Integração**: login, cadastro, 401/403 e permissões por perfil de ponta a ponta (`AutenticacaoIntegrationTest`)
- **Segurança/negócio**: bootstrap do admin (`BootstrapAdminInitializerTest`), CPF (`MotoristaControllerCpfTest`), tradução de erros de constraint (`GlobalExceptionHandlerTest`)
- **Migração Flyway**: aplica todas as migrations em um PostgreSQL real e confere unicidade, índices e ausência de dados duplicados (`FlywayMigrationTest`)
- **Rate Limiting**: validação de controle de vazão e resposta 429 (`RateLimitingFilterTest`)
- **Rate Limiting distribuído**: duas instâncias compartilham tokens no Redis (`RedisRateLimiterIntegrationTest`)
- **Frontend**: serialização dos filtros de página, status e busca (`npm test`)

---

## Recursos Implementados

### 1. Migrações Versionadas (Flyway)
- Em substituição ao arriscado `hibernate.ddl-auto=update`, o banco agora é gerenciado de forma determinística e versionada pelo **Flyway**.
- O Hibernate atua em modo `validate` (`spring.jpa.hibernate.ddl-auto=validate`), garantindo que a aplicação só suba se o esquema do banco bater perfeitamente com os mapeamentos das entidades JPA.
- Scripts localizados em `src/main/resources/db/migration/`:
  - `V1__create_tables.sql`: cria tabelas (`usuarios`, `veiculo`, `caminhao`, `furgao`, `motorista`, `entrega`), índices e chaves estrangeiras.
  - `V2__seed_initial_data.sql`: carga idempotente de motoristas de demonstração.
  - `V3__carregar_dados_locais.sql`: carga histórica do ambiente local (não edite, o Flyway valida o checksum).
  - `V4__corrigir_nomes_colunas.sql`: ajusta nomes de colunas para os mapeamentos JPA.
  - `V5__integridade_dados_e_concorrencia.sql`: normaliza dados, cria constraints únicas de placa e CPF e índices contra dupla alocação.
  - `V6__clientes_produtos_estoque_pedidos.sql`: cria as tabelas do módulo comercial.
  - **Regra daqui para frente:** nunca use migrations para dados de demonstração ou dumps. Migrations são só estrutura e dados de referência.

### 2. Logging Estruturado (JSON / Correlation ID)
- Em produção / Docker, os logs são gerados no formato **JSON estruturado** (`logstash-logback-encoder`), prontos para ingestão em Elasticsearch, Loki, CloudWatch ou Datadog.
- Filtro `StructuredLoggingFilter` injeta automaticamente no **SLF4J MDC**:
  - `requestId` (Correlation ID obtido via `X-Request-ID` ou gerado via UUID)
  - `clientIp`, `httpMethod`, `uri`, `status`, `durationMs`
- O `requestId` é devolvido no header HTTP `X-Request-ID` e incluído nas respostas de erro do `GlobalExceptionHandler`, simplificando o rastreamento ponta a ponta.
- Em desenvolvimento local, mantém formato colorido legível no console.

### 3. Rate Limiting
- Proteção contra ataques de força bruta e abuso de recursos usando o algoritmo Token Bucket.
- Políticas configuráveis e diferenciadas:
  - **Rotas de Autenticação (`/api/auth/**`)**: limite restritivo de 15 requisições/min por IP.
  - **Demais Rotas da API (`/api/**`)**: limite de 120 requisições/min por IP.
- Headers devolvidos em cada resposta:
  - `X-Rate-Limit-Remaining`: tokens restantes na janela.
  - `Retry-After`: tempo em segundos para tentar novamente caso exceda.
- No Compose, cada requisição atualiza atomicamente um bucket no Redis usando o relógio do próprio Redis. Réplicas do backend compartilham os mesmos limites. O modo portátil usa buckets locais em memória e serve a uma única instância.
- Se o Redis estiver indisponível no modo compartilhado, requisições de API falham fechadas com `503` para não remover a proteção silenciosamente.
- Resposta padronizada com HTTP `429 Too Many Requests`:
```json
{
  "timestamp": "2026-09-28T...",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Limite de requisições excedido. Tente novamente em 27 segundo(s).",
  "requestId": "9426f4fa-..."
}
```

---

## Roadmap

- [x] Containerização (Docker & Docker Compose)
- [x] Logging estruturado (Logstash JSON + Correlation ID)
- [x] Rate limiting (Bucket4j por IP)
- [x] Limites compartilhados por Redis e modo portátil em memória
- [x] CSRF para autenticação por cookie JWT
- [x] Health/readiness, métricas Prometheus e alertas operacionais
- [x] Scripts seguros de backup e restauração do PostgreSQL
- [x] Busca e paginação de motoristas e veículos
- [x] Migrações versionadas de banco (Flyway) no lugar de `ddl-auto=update`
