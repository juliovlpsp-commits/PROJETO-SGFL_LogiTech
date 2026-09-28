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

No IntelliJ: **Run/Debug Configurations → Environment Variables**.

### 2. Rodar o backend

```bash
mvn spring-boot:run
```
A API sobe em `http://localhost:8080`.

### 3. Rodar o frontend

```bash
cd sgfl-frontend
npm install
npm run dev
```
Interface em `http://localhost:5173`.

### 4. Rodar os testes

```bash
mvn test
```

Os testes usam banco H2 em memória — não tocam no seu Postgres local. Cobrem:
- **Unitário**: geração/validação de JWT (`JwtServiceTest`)
- **Repositório**: paginação e ordenação estável da listagem de entregas (`EntregaRepositoryTest`)
- **Controller**: validação de entrada, exclusão, erros (`EntregaControllerTest`)
- **Integração**: login real + acesso a rota protegida de ponta a ponta (`AutenticacaoIntegrationTest`)
  A cada `push`, o GitHub Actions roda essa suíte automaticamente (veja `.github/workflows/ci.yml`).

---

## Roadmap

- [ ] Containerização (Docker)
- [ ] Logging estruturado
- [ ] Rate limiting
- [ ] Migrações versionadas de banco (Flyway) no lugar de `ddl-auto=update`