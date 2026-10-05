# SGFL — roadmap de expansão

## Camada inicial (já entregue)

- Rastreamento: código público de rastreio + histórico de eventos por entrega.
- Disponibilidade: validação de conflito de veículo/motorista por status e janela agendada.
- Alertas: endpoint operacional para atrasadas, sem recurso e fora do prazo.
- Dashboard operacional: endpoint de KPIs e integração inicial no Dashboard.
- Comprovante: entidade, foto opcional, assinatura, recebedor e horário.
- Portal cliente: endpoint público de rastreio e tela `/rastreio/{codigo}`.
- Custos: custos por entrega + margem baseada em valor do frete.
- Relatórios: exportação CSV e PDF de entregas.
- Importação: CSV de clientes, produtos e entregas.
- PWA: manifest e service worker base.
- Auditoria da linha do tempo de cada entrega.

## Fases concluídas nesta expansão

| Fase | Commit | O que passou a existir |
|---|---|---|
| 1 | `72d5ccd` | 404 em JSON na API e PWA instalável (manifest, ícones, `offline.html`, service worker que nunca cacheia `/api/`) |
| 2 | `1ee2a83` | Auditoria transversal de cliente, produto e pedido (`GET /api/auditoria`, migration V9) |
| 3 | `644d45b` | Aviso por e-mail SMTP a cada mudança de status da entrega (desligado por padrão; `SGFL_MAIL_*`) |
| 4 | `ebcd753` | Geocodificação (Nominatim), rota com provedor externo (OSRM, queda para Haversine), histórico de ETA por entrega (migration V10) |
| 5 | `d1c69ee` | Tela de custos por categoria com total em R$, comprovante com câmera e assinatura em canvas, mapa Leaflet com geocodificação e histórico de ETA, fila offline que reenvia sozinhas as ações sem conexão, `PUT /api/entregas/{id}/coordenadas` |
| 6 | `78c4ea6` | Modo de estoque configurável (`SGFL_STOCK_MODE`): `IMEDIATA` (baixa na criação, padrão) ou `RESERVA` (bloqueio na criação e baixa na conclusão/despacho via `PATCH /api/pedidos/{id}/concluir`), migration V11 |

## Próxima camada necessária para fechar o roadmap

- Notificações por WhatsApp via provedor configurado.
- Trânsito em tempo real no ETA (hoje: OSRM público sem trânsito + velocidade média configurável).
- Desenho da rota real no mapa (hoje: linha reta entre origem e destino — distância e tempo já vêm do provedor).
