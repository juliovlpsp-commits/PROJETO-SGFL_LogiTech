# Arquitetura do SGFL

O SGFL é uma aplicação web em três partes: interface React, API REST em Spring Boot e banco PostgreSQL. O frontend acessa o backend por HTTP; somente o backend conversa com o banco.

```mermaid
flowchart LR
    U[Usuário] --> FE[React + Vite]
    FE -->|/api + JWT| NG[Nginx]
    NG --> API[Spring Boot]
    API --> SEC[Spring Security + JWT]
    API --> CTRL[Controllers REST]
    CTRL --> SVC[Services]
    SVC --> REP[Spring Data JPA]
    REP --> DB[(PostgreSQL)]
    MIG[Flyway] --> DB
    API --> OBS[Logs estruturados]
    API --> RL[Rate limiting]
```

## Frontend

O frontend em `sgfl-frontend/` usa React, Vite e Axios. `App.jsx` controla a sessão e compõe login, dashboard de frota/entregas e gestão comercial. `api.js` centraliza chamadas Axios e o tratamento de respostas 401. `Dashboard.jsx` implementa as telas de frota e entregas; `GestaoComercial.jsx` implementa clientes, produtos e pedidos; `CadastroRecursos.jsx` cuida dos cadastros de frota.

Em desenvolvimento o Vite serve a interface. No Docker, o build estático é servido por Nginx, que também encaminha `/api/` ao backend.

## Backend

O backend fica em `src/main/java/com/logitech/sgfl/` e organiza o código por responsabilidade:

| Pacote | Responsabilidade |
|---|---|
| `controller/` | Endpoints HTTP para autenticação, frota, entregas, clientes, produtos e pedidos |
| `service/` | Casos de uso e regras de negócio |
| `repository/` | Consultas e persistência via Spring Data JPA |
| `me/` | Entidades JPA do domínio |
| `dto/` | Contratos de entrada e validação |
| `security/` | Spring Security, JWT, autenticação e autorização |
| `exceptions/` | Exceções de domínio e tratamento uniforme de erros |
| `ratelimit/` | Limite de requisições por IP |
| `logging/` | Logs estruturados, request ID e metadados HTTP |
| `config/` | Configuração e bootstrap opcional do administrador |

O domínio comercial inclui `Cliente`, `Produto`, `Estoque`, `Pedido` e `ItemPedido`. O domínio logístico inclui `Veiculo` (com `Caminhao` e `Furgao`), `Motorista` e `Entrega`. Pedidos reservam estoque em transação; entregas validam capacidade, CNH e disponibilidade de recursos.

Listagens de clientes, produtos e pedidos usam páginas de 20 itens por padrão, aceitam `page` e `size` e limitam páginas a 100 itens. Entregas também são paginadas. Respostas de cliente, produto, pedido, motorista, veículo e entrega passam por DTOs; a forma dos dados HTTP não depende diretamente das entidades JPA.

## Persistência e integridade

PostgreSQL é o banco de execução. Flyway aplica scripts versionados em `src/main/resources/db/migration/`; Hibernate valida o esquema (`ddl-auto=validate`). Constraints e índices no banco reforçam unicidade e impedem alocações simultâneas de veículo ou motorista em entregas em trânsito.

## Segurança e operação

O login público devolve JWT; as demais rotas exigem autenticação. Spring Security aplica autorização por perfil e BCrypt protege as senhas. O filtro de rate limiting protege rotas de autenticação e API. Logs estruturados incluem request ID para rastrear chamadas.

O JWT do navegador fica em cookie `HttpOnly`, `SameSite=Lax`, com validade alinhada à expiração do token. Defina `APP_COOKIE_SECURE=true` quando o tráfego externo usar HTTPS. O rate limiter atual usa buckets em memória por instância e remove entradas ociosas; uma implantação com várias réplicas precisa de armazenamento compartilhado para os contadores.

O `docker-compose.yml` sobe PostgreSQL, backend e frontend/Nginx. Segredos são fornecidos por variáveis de ambiente, não pelo código-fonte. A pipeline em `.github/workflows/ci.yml` executa testes Maven e lint/build do frontend.

## Estado da documentação

Este documento descreve os módulos encontrados no código atual. O `README.md` contém instruções de configuração e execução; consulte ambos para entender a arquitetura e operar o projeto.
