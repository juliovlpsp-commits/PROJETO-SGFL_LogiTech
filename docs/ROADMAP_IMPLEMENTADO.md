# SGFL — roadmap de expansão

## Entregas de alta prioridade já iniciadas

- Rastreamento: código público de rastreio + histórico de eventos por entrega.
- Disponibilidade: validação de conflito de veículo/motorista por status e janela agendada.
- Alertas: endpoint operacional para atrasadas, sem recurso e fora do prazo.
- Dashboard operacional: endpoint de KPIs e integração inicial no Dashboard.
- Comprovante: entidade, foto opcional, assinatura, recebedor e horário.
- Portal cliente: endpoint público de rastreio e tela `/rastreio/{codigo}`.
- Custos: custos por entrega + margem baseada em valor do frete.
- Relatórios: exportação CSV de entregas.
- Importação: CSV de clientes, produtos e entregas.
- PWA: manifest e service worker base.

## Próxima camada necessária para fechar o roadmap

- Mapa/rota com geocodificação e cálculo de rota.
- Notificações reais por SMTP e WhatsApp via provedor configurado.
- Auditoria transversal de cliente/produto/pedido, além da linha do tempo da entrega.
- PDF de relatórios.
- PWA completo para motorista com câmera/assinatura e sincronização offline.
- Custos detalhados de combustível, pedágio e manutenção.
- Baixa de estoque no despacho/envio, caso o negócio passe a reservar no pedido e baixar no envio.
- ETA com distância, histórico e provedor de trânsito.
