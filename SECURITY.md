# Segurança do SGFL

## Controles aplicados

- Senhas de usuário com BCrypt; JWT em cookie `HttpOnly`, `SameSite=Lax` e expiração configurável.
- Token CSRF em cookie próprio e header `X-XSRF-TOKEN` para operações mutáveis no navegador.
- CORS com lista explícita de origens, credenciais habilitadas e headers permitidos enumerados.
- Autorização por perfil para operações da frota, clientes e exclusões.
- Limite por IP nas rotas de login e na API; Redis atômico e compartilhado no Compose.
- Segredos externos ao Git por `.env`; administrador inicial só é criado se habilitado.
- Backend containerizado como usuário não privilegiado; porta de gerenciamento não publicada pelo Compose.

## Implantação

1. Troque todos os valores de `.env.example`, use credenciais diferentes para PostgreSQL, Redis e administrador, e não reutilize segredos de desenvolvimento.
2. Habilite HTTPS e `APP_COOKIE_SECURE=true`; termine TLS em um proxy confiável e permita somente as origens reais em `CORS_ALLOWED_ORIGINS`.
3. Não publique `8081`, Redis nem Prometheus na Internet. Restrinja conexões entre serviços por rede e firewall.
4. Proteja, criptografe e retenha backups fora do host; controle acesso às credenciais e teste restauração.
5. Revise atualizações do Dependabot e rode CI antes de integrar dependências novas.

## Relato de vulnerabilidade

Não abra um issue público com segredos ou dados pessoais. Para uma implantação privada, encaminhe o relato ao mantenedor do repositório por um canal privado e inclua versão afetada, impacto e passos mínimos para reproduzir.
