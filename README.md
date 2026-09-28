# SGFL - Sistema de Gestão de Frota e Logística

API REST em Spring Boot para gestão de entregas, veículos e motoristas, com autenticação JWT, validação de regras de negócio, tratamento de erros centralizado e testes automatizados (unitários, de repositório e de integração ponta a ponta).

 
---

## Tecnologias

**Backend**
- Java 17, Spring Boot 3.2.3 (Web, Data JPA, Security, Validation)
- PostgreSQL (produção/desenvolvimento) e H2 em memória (testes)
- JWT (jjwt) para autenticação stateless
- JUnit 5, Mockito, AssertJ, Spring Security Test
- Maven
  **Frontend**
- React + Vite
- Tema claro/escuro persistido, layout responsivo
  **Infraestrutura**
- GitHub Actions: roda a suíte de testes a cada `push`/pull request
---

## Arquitetura

```
controller/   -> endpoints REST (HTTP <-> aplicação)
service/      -> regras de negócio
repository/   -> acesso a dados (Spring Data JPA)
me/           -> entidades JPA
dto/          -> contratos de entrada/saída da API (nunca a entidade JPA diretamente)
security/     -> JWT, filtro de autenticação, configuração do Spring Security
exceptions/   -> exceções de domínio + tratamento de erro centralizado
enums/        -> Perfil, StatusEntrega
```

O front-end nunca fala diretamente com o banco: toda operação passa pela API REST autenticada por token.
 
---

## Autenticação

- `POST /api/auth/registrar` — cria um usuário
- `POST /api/auth/login` — autentica por **email** e senha, devolve um JWT
  Todas as demais rotas exigem o header:
```
Authorization: Bearer <token>
```

O token expira em 24h por padrão (configurável via `JWT_EXPIRATION_MS`).
 
---

## Entregas — regras de negócio

1. **Capacidade de carga**: uma entrega só pode ser alocada a um veículo se `pesoCargaKg` for menor ou igual à capacidade do veículo. Alocação incompatível retorna `400 Bad Request` com mensagem descritiva (`VeiculoIncompativelException`).
2. **CNH do motorista**:
   - Caminhão: exige CNH categoria `D` ou `E`.
   - Furgão: exige CNH categoria `B`, `C`, `D` ou `E`.
3. **Status da entrega**: `PENDENTE` → `EM_TRANSITO` → `ENTREGUE` (também existe `CANCELADA` no enum).
### Endpoints principais

| Método | Rota | Descrição |
|---|---|---|
| GET | `/api/entregas?page=0&size=20` | Lista paginada, sempre ordenada por `id` |
| POST | `/api/entregas` | Cria uma entrega (valida descrição, destino e status) |
| PATCH | `/api/entregas/{id}/status` | Atualiza o status de uma entrega |
| PUT | `/api/entregas/{id}/alocar?veiculoId=&motoristaId=` | Aloca veículo/motorista (valida peso e CNH) |
| PUT | `/api/entregas/{id}/finalizar` | Marca a entrega como concluída |
| DELETE | `/api/entregas/{id}` | Remove uma entrega |

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
- Node.js 18+ (para o frontend)
### 1. Configurar variáveis de ambiente

Nada de senha ou segredo direto no `application.properties` — configure via variáveis de ambiente (todas têm um valor padrão de desenvolvimento, então o projeto roda mesmo sem configurar nada, mas **não use os valores padrão em produção**):

| Variável | Descrição | Padrão (dev) |
|---|---|---|
| `DB_URL` | URL JDBC do Postgres | `jdbc:postgresql://localhost:5432/sgfl_db` |
| `DB_USERNAME` | Usuário do banco | `postgres` |
| `DB_PASSWORD` | Senha do banco | `4040` |
| `JWT_SECRET` | Chave de assinatura do token | valor de desenvolvimento embutido |
| `JWT_EXPIRATION_MS` | Validade do token (ms) | `86400000` (24h) |
| `FLYWAY_ENABLED` | Ativa execução de migrações automáticas | `true` |
| `DDL_AUTO` | Estratégia de DDL do Hibernate | `validate` |
| `RATE_LIMIT_ENABLED` | Habilita rate limiting por IP via Bucket4j | `true` |
| `RATE_LIMIT_AUTH_CAPACITY` | Limite de requisições em `/api/auth/**` | `15` por minuto |
| `RATE_LIMIT_API_CAPACITY` | Limite de requisições gerais em `/api/**` | `120` por minuto |

No IntelliJ: **Run/Debug Configurations → Environment Variables**.

---

## Execução com Docker (Recomendado)

O projeto possui orquestração completa via **Docker Compose**, subindo banco PostgreSQL 15, backend Spring Boot e frontend React servido por Nginx com proxy reverso.

### Subir todo o ambiente com um comando:

```bash
docker compose up --build -d
```

- **Frontend (Web)**: [http://localhost:5173](http://localhost:5173) ou [http://localhost](http://localhost)
- **Backend (API)**: [http://localhost:8080/api](http://localhost:8080/api)
- **PostgreSQL**: `localhost:5432` (database `sgfl_db`, user `postgres`, password `4040`)

Para visualizar os logs:
```bash
docker compose logs -f backend
```

Para parar os serviços:
```bash
docker compose down
```

---

## Execução Local (sem Docker)

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

Os testes usam banco H2 em memória — não tocam no seu Postgres local. Cobrem:
- **Unitário**: geração/validação de JWT (`JwtServiceTest`)
- **Repositório**: paginação e ordenação estável da listagem de entregas (`EntregaRepositoryTest`)
- **Controller**: validação de entrada, exclusão, erros (`EntregaControllerTest`)
- **Integração**: login real + acesso a rota protegida de ponta a ponta (`AutenticacaoIntegrationTest`)
- **Migração Flyway**: validação do schema SQL e histórico de migrações (`FlywayMigrationTest`)
- **Rate Limiting**: validação de controle de vazão e resposta 429 (`RateLimitingFilterTest`)

---

## Recursos Implementados

### 1. Migrações Versionadas (Flyway)
- Em substituição ao arriscado `hibernate.ddl-auto=update`, o banco agora é gerenciado de forma determinística e versionada pelo **Flyway**.
- O Hibernate atua em modo `validate` (`spring.jpa.hibernate.ddl-auto=validate`), garantindo que a aplicação só suba se o esquema do banco bater perfeitamente com os mapeamentos das entidades JPA.
- Scripts localizados em `src/main/resources/db/migration/`:
  - `V1__create_tables.sql`: cria tabelas (`usuarios`, `veiculo`, `caminhao`, `furgao`, `motorista`, `entrega`), índices e chaves estrangeiras.
  - `V2__seed_initial_data.sql`: carga idempotente de motoristas e frotas de demonstração.

### 2. Logging Estruturado (JSON / Correlation ID)
- Em produção / Docker, os logs são gerados no formato **JSON estruturado** (`logstash-logback-encoder`), prontos para ingestão em Elasticsearch, Loki, CloudWatch ou Datadog.
- Filtro `StructuredLoggingFilter` injeta automaticamente no **SLF4J MDC**:
  - `requestId` (Correlation ID obtido via `X-Request-ID` ou gerado via UUID)
  - `clientIp`, `httpMethod`, `uri`, `status`, `durationMs`
- O `requestId` é devolvido no header HTTP `X-Request-ID` e incluído nas respostas de erro do `GlobalExceptionHandler`, simplificando o rastreamento ponta a ponta.
- Em desenvolvimento local, mantém formato colorido legível no console.

### 3. Rate Limiting (Bucket4j)
- Proteção contra ataques de força bruta, abuso de recursos e DoS usando o algoritmo Token Bucket com **Bucket4j**.
- Políticas configuráveis e diferenciadas:
  - **Rotas de Autenticação (`/api/auth/**`)**: limite restritivo de 15 requisições/min por IP.
  - **Demais Rotas da API (`/api/**`)**: limite de 120 requisições/min por IP.
- Headers devolvidos em cada resposta:
  - `X-Rate-Limit-Remaining`: tokens restantes na janela.
  - `Retry-After`: tempo em segundos para tentar novamente caso exceda.
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
- [x] Migrações versionadas de banco (Flyway) no lugar de `ddl-auto=update`