# Operação do SGFL

## Modos de execução

- **Docker Compose**: PostgreSQL 15, Redis, backend e Nginx. O backend aplica Flyway e valida o esquema. Os contadores de rate limit ficam no Redis compartilhado.
- **Portátil (`local`)**: H2 em arquivo e rate limit em memória. É uma instância local, não uma base multiusuário compartilhada. O banco PostgreSQL/Docker não é importado automaticamente.

## Health checks e métricas

O backend escuta API em `8080` e a porta de gerenciamento em `8081`. No Compose, apenas a API é publicada no host; a porta de gerenciamento fica na rede interna.

- `/actuator/health/liveness`
- `/actuator/health/readiness`
- `/actuator/prometheus`

Para uma instalação local, Prometheus pode ser iniciado junto com o Compose:

```powershell
docker compose --profile observability up -d
```

O Prometheus fica limitado a `127.0.0.1:9090`. As regras em `observability/sgfl-alerts.yml` são exemplos de alertas; configure um Alertmanager e um canal de notificação antes de depender deles em produção.

## Backups PostgreSQL

Crie um backup custom-format (`pg_dump -Fc`) depois de iniciar o Compose:

```powershell
.\scripts\backup-postgres.ps1
```

```bash
bash ./scripts/backup-postgres.sh
```

Por padrão, arquivos vão para `backups/`, que é ignorada pelo Git. O script mostra o SHA-256 para conferência de cópia. Armazene cópias criptografadas e fora da máquina/volume Docker; um volume nomeado não substitui backup.

Para gravar também em uma pasta externa (unidade removível, volume de rede ou pasta sincronizada com armazenamento externo), passe o destino explicitamente. O script copia o dump e compara o SHA-256 antes de informar sucesso:

```powershell
.\scripts\backup-postgres.ps1 -ExternalDirectory "E:\Backups-SGFL"
```

```bash
bash ./scripts/backup-postgres.sh ./backups /mnt/backup-externo/sgfl
```

No Windows, registre uma tarefa diária às 02:00 (ou informe outro horário). Ela roda na sessão atual; Docker Desktop e o destino precisam estar acessíveis nesse horário:

```powershell
.\scripts\install-backup-task.ps1 -ExternalDirectory "E:\Backups-SGFL" -At "02:00"
```

No Linux, adicione ao cron do usuário que controla Docker Compose uma entrada como `0 2 * * * cd /srv/sgfl && /usr/bin/bash scripts/backup-postgres.sh /srv/sgfl/backups /mnt/backup-externo/sgfl`. Monitore o código de saída e a idade do backup mais recente. O script não remove dumps antigos nem cifra arquivos: configure retenção e criptografia no armazenamento escolhido e teste uma restauração periodicamente. Uma pasta sincronizada ainda não substitui armazenamento imutável ou versionado.

Para restaurar, informe o arquivo e confirme digitando literalmente `RESTAURAR`:

```powershell
.\scripts\restore-postgres.ps1 -BackupFile .\backups\sgfl-AAAAmmdd-HHmmss.dump
```

```bash
bash ./scripts/restore-postgres.sh ./backups/sgfl-AAAAmmdd-HHmmss.dump
```

A restauração cria um backup de segurança, para o backend e usa `pg_restore --clean`. Não a execute em produção sem janela de manutenção e sem conferir a cópia. Faça um exercício periódico em um banco isolado e confirme a integridade funcional após subir o backend.

## Limites operacionais atuais

- Redis distribui o limite HTTP entre réplicas, mas não torna automaticamente o sistema pronto para escalar sem limite. Confira pool de conexões do PostgreSQL, latência, throughput, índices, memória e réplicas via carga representativa antes de aumentar instâncias.
- O limite é por IP de origem; implantações atrás de proxy precisam confiar cabeçalhos encaminhados somente de proxies controlados.
- Não há migração de dados PostgreSQL → H2. O backup externo pode ser feito para um destino indicado no script; o agendamento, criptografia e retenção devem ser configurados no host/armazenamento.
- O Redis do Compose é para contadores temporários, sem persistência e sem porta no host. Em produção, use serviço Redis gerenciado/restrito com autenticação, TLS, política de rede e monitoramento.
- O endpoint Prometheus fica sem autenticação porque só está acessível na rede Docker privada. Não publique a porta de gerenciamento diretamente na Internet.

## Variáveis essenciais

Configure `.env` localmente. `JWT_SECRET` precisa ter ao menos 32 caracteres aleatórios. `POSTGRES_PASSWORD` e `REDIS_PASSWORD` devem ser valores fortes, distintos e não versionados. Habilite `APP_COOKIE_SECURE=true` quando a aplicação estiver atrás de HTTPS.

## Publicação com Docker e serviços gerenciados

`docker-compose.production.yml` separa a aplicação de PostgreSQL/Redis locais e conecta o backend aos serviços externos por variáveis de ambiente. Copie `deployment/env.production.example` para `deployment/.env.production`, substitua todos os marcadores e carregue senhas/chaves pelo cofre de segredos do host sempre que houver suporte. Faça primeiro um deploy de staging com cópia isolada dos dados.

```powershell
docker compose --env-file deployment/.env.production -f docker-compose.production.yml config --quiet
docker compose --env-file deployment/.env.production -f docker-compose.production.yml up --build -d
docker compose --env-file deployment/.env.production -f docker-compose.production.yml logs -f backend
docker compose --env-file deployment/.env.production -f docker-compose.production.yml down
```

Configure o domínio e TLS no ingress/reverse proxy da hospedagem; encaminhe HTTPS para `SGFL_HTTP_PORT` e não exponha as portas 8080/8081 do backend. Use PostgreSQL e Redis privados com TLS, usuários restritos, backups gerenciados e firewall. Ative o bootstrap de administrador somente no primeiro deploy e então remova/desabilite suas variáveis. Para rollback, volte à imagem/revisão anterior e execute migrações somente se forem compatíveis com o código anterior; mudanças destrutivas exigem plano de restauração.

O arquivo Compose entrega uma base portátil, não provisiona DNS, TLS, banco, Redis, segredos, alertas de notificação nem política de recuperação. Esses recursos dependem do provedor escolhido.

## Teste de carga reproduzível

Instale o Grafana k6 no host e suba um ambiente de staging com dados representativos. Opcionalmente, alimente até mil entregas sintéticas com `performance/seed-deliveries.sql` **somente em banco descartável/staging**. O roteiro executa autenticação uma vez e depois só lista entregas; não cria dados durante a carga. Compare os planos de paginação e busca com `performance/explain-deliveries.sql` antes de adicionar índices ou aumentar o pool.

```powershell
$env:K6_BASE_URL = 'http://localhost:5173/api'
$env:K6_USERNAME = 'conta-de-teste@empresa.com'
$env:K6_PASSWORD = 'senha-da-conta-de-teste'
$env:K6_MAX_VUS = '20'
k6 run .\performance\sgfl-api-load.js
Remove-Item Env:K6_USERNAME, Env:K6_PASSWORD
```

O perfil inicial sobe gradualmente até 20 usuários virtuais, mantém a carga por dois minutos e exige menos de 1% de falhas, p95 abaixo de 500 ms e p99 abaixo de 1 s. São limites iniciais para comparação; calibre-os com o hardware e SLOs reais. Aumente `K6_MAX_VUS` em staging, acompanhe métricas do pool e do banco, e não execute testes de estresse contra produção sem janela e autorização.
