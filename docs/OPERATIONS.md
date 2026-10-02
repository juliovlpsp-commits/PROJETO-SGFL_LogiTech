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
- Não há ainda deploy, migração de dados PostgreSQL → H2, backup agendado ou armazenamento externo automático neste repositório.
- O Redis do Compose é para contadores temporários, sem persistência e sem porta no host. Em produção, use serviço Redis gerenciado/restrito com autenticação, TLS, política de rede e monitoramento.
- O endpoint Prometheus fica sem autenticação porque só está acessível na rede Docker privada. Não publique a porta de gerenciamento diretamente na Internet.

## Variáveis essenciais

Configure `.env` localmente. `JWT_SECRET` precisa ter ao menos 32 caracteres aleatórios. `POSTGRES_PASSWORD` e `REDIS_PASSWORD` devem ser valores fortes, distintos e não versionados. Habilite `APP_COOKIE_SECURE=true` quando a aplicação estiver atrás de HTTPS.
