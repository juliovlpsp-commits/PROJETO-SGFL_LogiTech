# SGFL - Sistema de Gerenciamento de Frota e Logística

API REST desenvolvida em Spring Boot, com validações de regras de negócio (capacidade de carga e permissões de CNH) e testes unitários.

---

## Tecnologias

- Linguagem: Java
- Framework: Spring Boot 3.2.3 (Spring Web, Spring Data JPA)
- Banco de Dados: PostgreSQL
- Testes: JUnit 5 & Mockito
- Gerenciador de Dependências: Maven

---

## Regras de Negócio

1. Validação de Capacidade de Carga:
    - Uma entrega só pode ser alocada a um veículo se o peso total da carga (`pesoCargaKg`) for menor ou igual à capacidade suportada pelo veículo.

2. Validação de CNH do Motorista:
    - Caminhão: Exige que o motorista possua CNH do tipo `D` ou `E`.
    - Furgão: Exige CNH válida para veículos leves/médios (categorias `B`, `C`, `D` ou `E`).
    - Tentativas de alocação incompatíveis retornam `HTTP 400 Bad Request` com mensagem descritiva do erro.

3. Status da Entrega:
    - Transição de status: `PENDENTE`  `EM_TRANSITO`  `ENTREGUE`.

---

## Execuçao do projeto:

### Pré-requisitos
- JDK 17 ou superior instalado.
- PostgreSQL em execução.
- Maven configurado (ou executador embutido no IDE).

### 1. Configurar o Banco de Dados
No ficheiro `src/main/resources/application.properties`, configure as credenciais da sua base de dados PostgreSQL:

```properties
# Configuração do Banco de Dados PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/sgfl_db
spring.datasource.username=postgres
spring.datasource.password=sua_senha_aqui

# Configuração do JPA / Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true