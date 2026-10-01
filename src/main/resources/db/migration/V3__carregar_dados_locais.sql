-- Inserir Veículos
INSERT INTO public.veiculo (id, capacidade_carga_kg, modelo, placa) VALUES
(5, 15000, 'Volvo FH 540', 'ABC1D23'),
(6, 15000, 'Volvo FH 540', 'ABC1D23'),
(7, 15000, 'Volvo FH 540', 'ABC1D23')
    ON CONFLICT (id) DO NOTHING;

-- Inserir Caminhões
INSERT INTO public.caminhao (quantidade_eixos, id) VALUES
(6, 5),
(6, 6),
(6, 7)
    ON CONFLICT (id) DO NOTHING;

-- Inserir Motoristas
INSERT INTO public.motorista (id, cpf, nome, tipocnh) VALUES
(1, '12345678900', 'Carlos Silva', 'E'),
(2, '11122233344', 'Lucas Silva', 'B'),
(3, '99988877766', 'Lucas CNH B', 'B'),
(4, '99988877766', 'Lucas CNH B', 'B'),
(5, '123.456.789-01', 'Carlos Eduardo Silva', 'E'),
(6, '234.567.890-12', 'Marcos Antonio Souza', 'B')
    ON CONFLICT (id) DO NOTHING;

-- Inserir Entregas
INSERT INTO public.entrega (id, endereco_destino, endereco_origem, peso_carga_kg, status, motorista_id, veiculo_id, descricao) VALUES
(1, 'Rio de Janeiro, RJ', 'São Paulo, SP', 8000, 'PENDENTE', 4, 5, NULL),
(2, 'Porto Alegre, RS', 'Curitiba, PR', 20000, 'ENTREGUE', NULL, NULL, NULL),
(3, 'Florianópolis, SC', 'Curitiba, PR', 5000, 'ENTREGUE', 1, 5, NULL),
(5, 'sao paulo', NULL, 0, 'EM_TRANSITO', NULL, NULL, NULL),
(6, 'santa catarina', NULL, 0, 'PENDENTE', NULL, NULL, NULL),
(9, 'gramado RS', NULL, 0, 'ENTREGUE', NULL, NULL, 'produto T5')
    ON CONFLICT (id) DO NOTHING;

-- Inserir Usuários
INSERT INTO public.usuarios (id, email, password, perfil, username) VALUES
    (3, 'admin@gmail.com', '$2a$10$2rl1FHAShiTOvNbYgyqYwuudmhsqN33C9de1l43YE8yeDtQZsmhJq', 'ROLE_ADMIN', 'admin')
    ON CONFLICT (id) DO NOTHING;

-- Ajustar os IDs das sequências para que novas criações não deem erro de chave primária duplicada
SELECT setval('public.entrega_id_seq', 9, true);
SELECT setval('public.motorista_id_seq', 6, true);
SELECT setval('public.usuarios_id_seq', 3, true);
SELECT setval('public.veiculo_id_seq', 7, true);