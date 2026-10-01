import { useCallback, useEffect, useMemo, useState } from 'react';
import api from './api';
import { useTheme } from './useTheme';
import {
    Boxes,
    ClipboardList,
    Edit3,
    Plus,
    RefreshCw,
    Trash2,
    Users,
    X
} from 'lucide-react';

const ABA_CLIENTES = 'clientes';
const ABA_PRODUTOS = 'produtos';
const ABA_PEDIDOS = 'pedidos';

const CLIENTE_VAZIO = {
    nome: '',
    cpf: '',
    email: '',
    telefone: '',
    cep: '',
    logradouro: '',
    numero: '',
    complemento: '',
    bairro: '',
    cidade: '',
    uf: ''
};

const PRODUTO_VAZIO = {
    codigo: '',
    nome: '',
    descricao: '',
    preco: '',
    quantidadeEstoqueInicial: '0',
    ativo: true
};

export default function GestaoComercial() {
    const { theme } = useTheme();
    const styles = getStyles(theme);

    const [aberto, setAberto] = useState(false);
    const [aba, setAba] = useState(ABA_CLIENTES);

    const [clientes, setClientes] = useState([]);
    const [produtos, setProdutos] = useState([]);
    const [pedidos, setPedidos] = useState([]);

    const [clienteEditando, setClienteEditando] = useState(null);
    const [clienteForm, setClienteForm] = useState({
        ...CLIENTE_VAZIO
    });

    const [produtoEditando, setProdutoEditando] = useState(null);
    const [produtoForm, setProdutoForm] = useState({
        ...PRODUTO_VAZIO
    });

    const [clientePedido, setClientePedido] = useState('');
    const [itensPedido, setItensPedido] = useState([
        {
            produtoId: '',
            quantidade: '1'
        }
    ]);

    const [mensagem, setMensagem] = useState('');
    const [tipoMensagem, setTipoMensagem] = useState('info');

    const [carregando, setCarregando] = useState(false);
    const [salvando, setSalvando] = useState(false);

    const mostrarMensagem = useCallback(
        (texto, tipo = 'info') => {
            setMensagem(texto);
            setTipoMensagem(tipo);
        },
        []
    );

    const tratarErro = useCallback(
        (error, mensagemPadrao) => {
            const status = error?.response?.status;
            const data = error?.response?.data;

            const mensagemServidor =
                data?.message ||
                data?.error ||
                (typeof data === 'string' ? data : null);

            if (status === 401) {
                mostrarMensagem(
                    'Sua sessão expirou. Faça login novamente.',
                    'erro'
                );
                return;
            }

            if (status === 403) {
                mostrarMensagem(
                    'Você não possui permissão para realizar esta operação.',
                    'erro'
                );
                return;
            }

            mostrarMensagem(
                mensagemServidor || mensagemPadrao,
                'erro'
            );
        },
        [mostrarMensagem]
    );

    const extrairLista = useCallback((response) => {
        const data = response?.data;

        if (Array.isArray(data)) {
            return data;
        }

        if (Array.isArray(data?.content)) {
            return data.content;
        }

        if (Array.isArray(data?.data)) {
            return data.data;
        }

        if (Array.isArray(data?.produtos)) {
            return data.produtos;
        }

        return [];
    }, []);

    const carregarDados = useCallback(
        async () => {
            setCarregando(true);

            try {
                const [
                    clientesResponse,
                    produtosResponse,
                    pedidosResponse
                ] = await Promise.all([
                    api.get('/clientes'),
                    api.get('/produtos'),
                    api.get('/pedidos')
                ]);

                setClientes(
                    extrairLista(clientesResponse)
                );

                setProdutos(
                    extrairLista(produtosResponse)
                );

                setPedidos(
                    extrairLista(pedidosResponse)
                );
            } catch (error) {
                tratarErro(
                    error,
                    'Não foi possível carregar os dados comerciais.'
                );
            } finally {
                setCarregando(false);
            }
        },
        [tratarErro, extrairLista]
    );

    useEffect(() => {
        if (aberto) {
            carregarDados();
        }
    }, [
        aberto,
        carregarDados
    ]);

    const limparClienteForm = () => {
        setClienteEditando(null);
        setClienteForm({
            ...CLIENTE_VAZIO
        });
    };

    const limparProdutoForm = () => {
        setProdutoEditando(null);
        setProdutoForm({
            ...PRODUTO_VAZIO
        });
    };

    const iniciarEdicaoCliente = (
        cliente
    ) => {
        setClienteEditando(cliente.id);

        setClienteForm({
            nome: cliente.nome || '',
            cpf: cliente.cpf || '',
            email: cliente.email || '',
            telefone: cliente.telefone || '',
            cep: cliente.cep || '',
            logradouro: cliente.logradouro || '',
            numero: cliente.numero || '',
            complemento: cliente.complemento || '',
            bairro: cliente.bairro || '',
            cidade: cliente.cidade || '',
            uf: cliente.uf || ''
        });

        setAba(ABA_CLIENTES);
        setMensagem('');
    };

    const iniciarEdicaoProduto = (
        produto
    ) => {
        setProdutoEditando(produto.id);

        setProdutoForm({
            codigo: produto.codigo || '',
            nome: produto.nome || '',
            descricao: produto.descricao || '',
            preco: produto.preco ?? '',
            quantidadeEstoqueInicial:
                produto.estoque?.quantidadeDisponivel ?? 0,
            ativo: produto.ativo !== false
        });

        setAba(ABA_PRODUTOS);
        setMensagem('');
    };

    const salvarCliente = async (
        event
    ) => {
        event.preventDefault();

        setSalvando(true);
        setMensagem('');

        try {
            const dados = {
                ...clienteForm,
                cpf: clienteForm.cpf
                    .replace(/\D/g, ''),
                uf: clienteForm.uf
                    .trim()
                    .toUpperCase()
            };

            if (clienteEditando) {
                await api.put(
                    `/clientes/${clienteEditando}?ativo=true`,
                    dados
                );

                mostrarMensagem(
                    'Cliente atualizado com sucesso.',
                    'sucesso'
                );
            } else {
                await api.post(
                    '/clientes',
                    dados
                );

                mostrarMensagem(
                    'Cliente cadastrado com sucesso.',
                    'sucesso'
                );
            }

            limparClienteForm();
            await carregarDados();
        } catch (error) {
            tratarErro(
                error,
                'Não foi possível salvar o cliente.'
            );
        } finally {
            setSalvando(false);
        }
    };

    const excluirCliente = async (
        cliente
    ) => {
        const confirmou =
            window.confirm(
                `Excluir o cliente "${cliente.nome}"?`
            );

        if (!confirmou) {
            return;
        }

        setSalvando(true);
        setMensagem('');

        try {
            await api.delete(
                `/clientes/${cliente.id}`
            );

            if (
                clienteEditando ===
                cliente.id
            ) {
                limparClienteForm();
            }

            mostrarMensagem(
                'Cliente excluído com sucesso.',
                'sucesso'
            );

            await carregarDados();
        } catch (error) {
            tratarErro(
                error,
                'Não foi possível excluir o cliente.'
            );
        } finally {
            setSalvando(false);
        }
    };

    const salvarProduto = async (
        event
    ) => {
        event.preventDefault();

        const preco =
            Number(produtoForm.preco);

        const estoque =
            Number(
                produtoForm.quantidadeEstoqueInicial
            );

        if (
            !Number.isFinite(preco) ||
            preco < 0
        ) {
            mostrarMensagem(
                'Informe um preço válido.',
                'erro'
            );
            return;
        }

        if (
            !Number.isInteger(estoque) ||
            estoque < 0
        ) {
            mostrarMensagem(
                'Informe um estoque válido.',
                'erro'
            );
            return;
        }

        setSalvando(true);
        setMensagem('');

        try {
            const dados = {
                codigo:
                    produtoForm.codigo.trim(),
                nome:
                    produtoForm.nome.trim(),
                descricao:
                    produtoForm.descricao.trim(),
                preco,
                quantidadeEstoqueInicial:
                estoque,
                ativo:
                produtoForm.ativo
            };

            if (produtoEditando) {
                await api.put(
                    `/produtos/${produtoEditando}`,
                    dados
                );

                await api.put(
                    `/produtos/${produtoEditando}/estoque`,
                    {
                        quantidade: estoque
                    }
                );

                mostrarMensagem(
                    'Produto e estoque atualizados com sucesso.',
                    'sucesso'
                );
            } else {
                await api.post(
                    '/produtos',
                    dados
                );

                mostrarMensagem(
                    'Produto cadastrado com sucesso.',
                    'sucesso'
                );
            }

            limparProdutoForm();
            await carregarDados();
        } catch (error) {
            tratarErro(
                error,
                'Não foi possível salvar o produto.'
            );
        } finally {
            setSalvando(false);
        }
    };

    const excluirProduto = async (
        produto
    ) => {
        const confirmou =
            window.confirm(
                `Excluir o produto "${produto.nome}"?`
            );

        if (!confirmou) {
            return;
        }

        setSalvando(true);
        setMensagem('');

        try {
            await api.delete(
                `/produtos/${produto.id}`
            );

            if (
                produtoEditando ===
                produto.id
            ) {
                limparProdutoForm();
            }

            mostrarMensagem(
                'Produto excluído com sucesso.',
                'sucesso'
            );

            await carregarDados();
        } catch (error) {
            tratarErro(
                error,
                'Não foi possível excluir o produto.'
            );
        } finally {
            setSalvando(false);
        }
    };

    const alterarItemPedido = (
        index,
        campo,
        valor
    ) => {
        setItensPedido(
            atual =>
                atual.map(
                    (item, itemIndex) =>
                        itemIndex === index
                            ? {
                                ...item,
                                [campo]: valor
                            }
                            : item
                )
        );
    };

    const adicionarItemPedido = () => {
        setItensPedido(
            atual => [
                ...atual,
                {
                    produtoId: '',
                    quantidade: '1'
                }
            ]
        );
    };

    const removerItemPedido = (
        index
    ) => {
        setItensPedido(
            atual => {
                if (
                    atual.length ===
                    1
                ) {
                    return [
                        {
                            produtoId: '',
                            quantidade: '1'
                        }
                    ];
                }

                return atual.filter(
                    (_, itemIndex) =>
                        itemIndex !== index
                );
            }
        );
    };

    const produtoPorId = useMemo(
        () =>
            new Map(
                produtos.map(
                    produto => [
                        String(produto.id),
                        produto
                    ]
                )
            ),
        [produtos]
    );

    const estoqueDisponivel = (
        produtoId
    ) => {
        const produto =
            produtoPorId.get(
                String(produtoId)
            );

        return Number(
            produto?.estoque
                ?.quantidadeDisponivel ?? 0
        );
    };

    const pedidoValido = useMemo(
        () => {
            if (
                !clientePedido ||
                itensPedido.length ===
                0
            ) {
                return false;
            }

            const usados =
                new Set();

            return itensPedido.every(
                item => {
                    const id =
                        String(
                            item.produtoId || ''
                        );

                    const quantidade =
                        Number(
                            item.quantidade
                        );

                    if (
                        !id ||
                        usados.has(id)
                    ) {
                        return false;
                    }

                    usados.add(id);

                    return (
                        Number.isInteger(
                            quantidade
                        ) &&
                        quantidade > 0 &&
                        quantidade <=
                        estoqueDisponivel(
                            id
                        )
                    );
                }
            );
        },
        [
            clientePedido,
            itensPedido,
            produtoPorId
        ]
    );

    const criarPedido = async (
        event
    ) => {
        event.preventDefault();

        if (!pedidoValido) {
            mostrarMensagem(
                'Confira cliente, produtos, quantidades e estoque disponível.',
                'erro'
            );
            return;
        }

        setSalvando(true);
        setMensagem('');

        try {
            await api.post(
                '/pedidos',
                {
                    clienteId:
                        Number(
                            clientePedido
                        ),
                    itens:
                        itensPedido.map(
                            item => ({
                                produtoId:
                                    Number(
                                        item.produtoId
                                    ),
                                quantidade:
                                    Number(
                                        item.quantidade
                                    )
                            })
                        )
                }
            );

            mostrarMensagem(
                'Pedido criado com sucesso. O estoque foi atualizado.',
                'sucesso'
            );

            setClientePedido('');
            setItensPedido([
                {
                    produtoId: '',
                    quantidade: '1'
                }
            ]);

            await carregarDados();
        } catch (error) {
            tratarErro(
                error,
                'Não foi possível criar o pedido. Verifique o estoque disponível.'
            );
        } finally {
            setSalvando(false);
        }
    };

    const cancelarPedido = async (
        pedido
    ) => {
        const confirmou =
            window.confirm(
                `Cancelar o pedido #${pedido.id}?`
            );

        if (!confirmou) {
            return;
        }

        setSalvando(true);
        setMensagem('');

        try {
            await api.patch(
                `/pedidos/${pedido.id}/cancelar`
            );

            mostrarMensagem(
                'Pedido cancelado e estoque devolvido com sucesso.',
                'sucesso'
            );

            await carregarDados();
        } catch (error) {
            tratarErro(
                error,
                'Não foi possível cancelar o pedido.'
            );
        } finally {
            setSalvando(false);
        }
    };

    const fechar = () => {
        if (salvando) {
            return;
        }

        setAberto(false);
        setMensagem('');
        limparClienteForm();
        limparProdutoForm();
    };

    if (!aberto) {
        return (
            <BlurButton
                type="button"
                onClick={() =>
                    setAberto(true)
                }
                style={styles.launcher}
            >
                <Boxes size={16} />
                Clientes e produtos
            </BlurButton>
        );
    }

    return (
        <div style={styles.overlay}>
            <section style={styles.modal}>
                <header style={styles.header}>
                    <div>
                        <div style={styles.eyebrow}>
                            GESTÃO COMERCIAL
                        </div>

                        <h2 style={styles.title}>
                            Clientes, produtos e pedidos
                        </h2>

                        <p style={styles.subtitle}>
                            Cadastre clientes, controle o estoque e registre os produtos solicitados.
                        </p>
                    </div>

                    <BlurButton
                        type="button"
                        onClick={fechar}
                        style={styles.closeButton}
                        aria-label="Fechar"
                    >
                        <X size={18} />
                    </BlurButton>
                </header>

                <div style={styles.tabs}>
                    <TabButton
                        active={
                            aba ===
                            ABA_CLIENTES
                        }
                        icon={
                            <Users size={15} />
                        }
                        label={`Clientes (${clientes.length})`}
                        onClick={() => {
                            setAba(
                                ABA_CLIENTES
                            );
                            setMensagem('');
                        }}
                        theme={theme}
                    />

                    <TabButton
                        active={
                            aba ===
                            ABA_PRODUTOS
                        }
                        icon={
                            <Boxes size={15} />
                        }
                        label={`Produtos (${produtos.length})`}
                        onClick={() => {
                            setAba(
                                ABA_PRODUTOS
                            );
                            setMensagem('');
                        }}
                        theme={theme}
                    />

                    <TabButton
                        active={
                            aba ===
                            ABA_PEDIDOS
                        }
                        icon={
                            <ClipboardList size={15} />
                        }
                        label={`Pedidos (${pedidos.length})`}
                        onClick={() => {
                            setAba(
                                ABA_PEDIDOS
                            );
                            setMensagem('');
                        }}
                        theme={theme}
                    />

                    <BlurButton
                        type="button"
                        onClick={carregarDados}
                        disabled={
                            carregando ||
                            salvando
                        }
                        style={styles.refresh}
                    >
                        <RefreshCw size={14} />
                        {carregando
                            ? 'Atualizando...'
                            : 'Atualizar'}
                    </BlurButton>
                </div>

                {mensagem && (
                    <div
                        style={
                            tipoMensagem ===
                            'erro'
                                ? styles.error
                                : styles.success
                        }
                    >
                        {mensagem}
                    </div>
                )}

                {aba ===
                    ABA_CLIENTES && (
                        <div
                            style={
                                styles.grid
                            }
                        >
                            <form
                                onSubmit={
                                    salvarCliente
                                }
                                style={
                                    styles.card
                                }
                            >
                                <Heading
                                    title={
                                        clienteEditando
                                            ? 'Editar cliente'
                                            : 'Novo cliente'
                                    }
                                    subtitle="Informações cadastrais do cliente."
                                    theme={theme}
                                />

                                <div
                                    style={
                                        styles.fields
                                    }
                                >
                                    <Field
                                        label="Nome"
                                        value={
                                            clienteForm.nome
                                        }
                                        onChange={
                                            valor =>
                                                setClienteForm(
                                                    atual => ({
                                                        ...atual,
                                                        nome:
                                                        valor
                                                    })
                                                )
                                        }
                                        theme={
                                            theme
                                        }
                                        required
                                    />

                                    <Field
                                        label="CPF"
                                        value={
                                            clienteForm.cpf
                                        }
                                        onChange={
                                            valor =>
                                                setClienteForm(
                                                    atual => ({
                                                        ...atual,
                                                        cpf:
                                                            valor
                                                                .replace(
                                                                    /\D/g,
                                                                    ''
                                                                )
                                                                .slice(
                                                                    0,
                                                                    11
                                                                )
                                                    })
                                                )
                                        }
                                        theme={
                                            theme
                                        }
                                        required
                                    />

                                    <Field
                                        label="E-mail"
                                        type="email"
                                        value={
                                            clienteForm.email
                                        }
                                        onChange={
                                            valor =>
                                                setClienteForm(
                                                    atual => ({
                                                        ...atual,
                                                        email:
                                                        valor
                                                    })
                                                )
                                        }
                                        theme={
                                            theme
                                        }
                                        required
                                    />

                                    <Field
                                        label="Telefone"
                                        value={
                                            clienteForm.telefone
                                        }
                                        onChange={
                                            valor =>
                                                setClienteForm(
                                                    atual => ({
                                                        ...atual,
                                                        telefone:
                                                        valor
                                                    })
                                                )
                                        }
                                        theme={
                                            theme
                                        }
                                    />

                                    <Field
                                        label="CEP"
                                        value={
                                            clienteForm.cep
                                        }
                                        onChange={
                                            valor =>
                                                setClienteForm(
                                                    atual => ({
                                                        ...atual,
                                                        cep:
                                                        valor
                                                    })
                                                )
                                        }
                                        theme={
                                            theme
                                        }
                                    />

                                    <Field
                                        label="UF"
                                        value={
                                            clienteForm.uf
                                        }
                                        onChange={
                                            valor =>
                                                setClienteForm(
                                                    atual => ({
                                                        ...atual,
                                                        uf:
                                                            valor
                                                                .replace(
                                                                    /[^a-zA-Z]/g,
                                                                    ''
                                                                )
                                                                .slice(
                                                                    0,
                                                                    2
                                                                )
                                                    })
                                                )
                                        }
                                        theme={
                                            theme
                                        }
                                    />

                                    <Field
                                        label="Logradouro"
                                        value={
                                            clienteForm.logradouro
                                        }
                                        onChange={
                                            valor =>
                                                setClienteForm(
                                                    atual => ({
                                                        ...atual,
                                                        logradouro:
                                                        valor
                                                    })
                                                )
                                        }
                                        theme={
                                            theme
                                        }
                                        wide
                                    />

                                    <Field
                                        label="Número"
                                        value={
                                            clienteForm.numero
                                        }
                                        onChange={
                                            valor =>
                                                setClienteForm(
                                                    atual => ({
                                                        ...atual,
                                                        numero:
                                                        valor
                                                    })
                                                )
                                        }
                                        theme={
                                            theme
                                        }
                                    />

                                    <Field
                                        label="Complemento"
                                        value={
                                            clienteForm.complemento
                                        }
                                        onChange={
                                            valor =>
                                                setClienteForm(
                                                    atual => ({
                                                        ...atual,
                                                        complemento:
                                                        valor
                                                    })
                                                )
                                        }
                                        theme={
                                            theme
                                        }
                                    />

                                    <Field
                                        label="Bairro"
                                        value={
                                            clienteForm.bairro
                                        }
                                        onChange={
                                            valor =>
                                                setClienteForm(
                                                    atual => ({
                                                        ...atual,
                                                        bairro:
                                                        valor
                                                    })
                                                )
                                        }
                                        theme={
                                            theme
                                        }
                                    />

                                    <Field
                                        label="Cidade"
                                        value={
                                            clienteForm.cidade
                                        }
                                        onChange={
                                            valor =>
                                                setClienteForm(
                                                    atual => ({
                                                        ...atual,
                                                        cidade:
                                                        valor
                                                    })
                                                )
                                        }
                                        theme={
                                            theme
                                        }
                                    />
                                </div>

                                <div
                                    style={
                                        styles.actions
                                    }
                                >
                                    {clienteEditando && (
                                        <BlurButton
                                            type="button"
                                            onClick={
                                                limparClienteForm
                                            }
                                            style={
                                                styles.secondary
                                            }
                                            disabled={
                                                salvando
                                            }
                                        >
                                            Cancelar
                                        </BlurButton>
                                    )}

                                    <BlurButton
                                        type="submit"
                                        style={
                                            styles.primary
                                        }
                                        disabled={
                                            salvando
                                        }
                                    >
                                        {salvando
                                            ? 'Salvando...'
                                            : clienteEditando
                                                ? 'Salvar alterações'
                                                : 'Cadastrar cliente'}
                                    </BlurButton>
                                </div>
                            </form>

                            <section
                                style={
                                    styles.card
                                }
                            >
                                <Heading
                                    title="Clientes cadastrados"
                                    subtitle="Clientes que podem ser vinculados aos pedidos."
                                    theme={theme}
                                />

                                <div
                                    style={
                                        styles.tableWrap
                                    }
                                >
                                    <table
                                        style={
                                            styles.table
                                        }
                                    >
                                        <thead>
                                        <tr>
                                            <th style={styles.th}>
                                                Nome
                                            </th>
                                            <th style={styles.th}>
                                                CPF
                                            </th>
                                            <th style={styles.th}>
                                                E-mail
                                            </th>
                                            <th style={styles.th}>
                                                Localização
                                            </th>
                                            <th style={styles.th}>
                                                Ações
                                            </th>
                                        </tr>
                                        </thead>

                                        <tbody>
                                        {clientes.map(
                                            cliente => (
                                                <tr
                                                    key={
                                                        cliente.id
                                                    }
                                                    style={
                                                        styles.tr
                                                    }
                                                >
                                                    <td
                                                        style={
                                                            styles.tdStrong
                                                        }
                                                    >
                                                        {
                                                            cliente.nome
                                                        }
                                                    </td>

                                                    <td
                                                        style={
                                                            styles.td
                                                        }
                                                    >
                                                        {
                                                            cliente.cpf
                                                        }
                                                    </td>

                                                    <td
                                                        style={
                                                            styles.td
                                                        }
                                                    >
                                                        {
                                                            cliente.email
                                                        }
                                                    </td>

                                                    <td
                                                        style={
                                                            styles.td
                                                        }
                                                    >
                                                        {
                                                            cliente.cidade ||
                                                            '-'
                                                        }
                                                        {cliente.uf
                                                            ? `/${cliente.uf}`
                                                            : ''}
                                                    </td>

                                                    <td
                                                        style={
                                                            styles.td
                                                        }
                                                    >
                                                        <div
                                                            style={
                                                                styles.inlineActions
                                                            }
                                                        >
                                                            <BlurButton
                                                                type="button"
                                                                onClick={() =>
                                                                    iniciarEdicaoCliente(
                                                                        cliente
                                                                    )
                                                                }
                                                                style={
                                                                    styles.small
                                                                }
                                                                disabled={
                                                                    salvando
                                                                }
                                                            >
                                                                <Edit3 size={13} />
                                                                Editar
                                                            </BlurButton>

                                                            <BlurButton
                                                                type="button"
                                                                onClick={() =>
                                                                    excluirCliente(
                                                                        cliente
                                                                    )
                                                                }
                                                                style={
                                                                    styles.danger
                                                                }
                                                                disabled={
                                                                    salvando
                                                                }
                                                            >
                                                                <Trash2 size={13} />
                                                                Excluir
                                                            </BlurButton>
                                                        </div>
                                                    </td>
                                                </tr>
                                            )
                                        )}
                                        </tbody>
                                    </table>

                                    {clientes.length ===
                                        0 && (
                                            <Empty
                                                theme={
                                                    theme
                                                }
                                                text="Nenhum cliente cadastrado."
                                            />
                                        )}
                                </div>
                            </section>
                        </div>
                    )}

                {aba ===
                    ABA_PRODUTOS && (
                        <div
                            style={
                                styles.grid
                            }
                        >
                            <form
                                onSubmit={
                                    salvarProduto
                                }
                                style={
                                    styles.card
                                }
                            >
                                <Heading
                                    title={
                                        produtoEditando
                                            ? 'Editar produto'
                                            : 'Novo produto'
                                    }
                                    subtitle="Cadastre o produto e a quantidade disponível em estoque."
                                    theme={theme}
                                />

                                <Field
                                    label="Código"
                                    value={
                                        produtoForm.codigo
                                    }
                                    onChange={
                                        valor =>
                                            setProdutoForm(
                                                atual => ({
                                                    ...atual,
                                                    codigo:
                                                    valor
                                                })
                                            )
                                    }
                                    theme={
                                        theme
                                    }
                                    required
                                />

                                <Field
                                    label="Nome"
                                    value={
                                        produtoForm.nome
                                    }
                                    onChange={
                                        valor =>
                                            setProdutoForm(
                                                atual => ({
                                                    ...atual,
                                                    nome:
                                                    valor
                                                })
                                            )
                                    }
                                    theme={
                                        theme
                                    }
                                    required
                                />

                                <Field
                                    label="Descrição"
                                    textarea
                                    value={
                                        produtoForm.descricao
                                    }
                                    onChange={
                                        valor =>
                                            setProdutoForm(
                                                atual => ({
                                                    ...atual,
                                                    descricao:
                                                    valor
                                                })
                                            )
                                    }
                                    theme={
                                        theme
                                    }
                                />

                                <div
                                    style={
                                        styles.twoColumns
                                    }
                                >
                                    <Field
                                        label="Preço"
                                        type="number"
                                        min="0"
                                        step="0.01"
                                        value={
                                            produtoForm.preco
                                        }
                                        onChange={
                                            valor =>
                                                setProdutoForm(
                                                    atual => ({
                                                        ...atual,
                                                        preco:
                                                        valor
                                                    })
                                                )
                                        }
                                        theme={
                                            theme
                                        }
                                        required
                                    />

                                    <Field
                                        label={
                                            produtoEditando
                                                ? 'Estoque'
                                                : 'Estoque inicial'
                                        }
                                        type="number"
                                        min="0"
                                        step="1"
                                        value={
                                            produtoForm.quantidadeEstoqueInicial
                                        }
                                        onChange={
                                            valor =>
                                                setProdutoForm(
                                                    atual => ({
                                                        ...atual,
                                                        quantidadeEstoqueInicial:
                                                        valor
                                                    })
                                                )
                                        }
                                        theme={
                                            theme
                                        }
                                        required
                                    />
                                </div>

                                <label
                                    style={
                                        styles.checkbox
                                    }
                                >
                                    <input
                                        type="checkbox"
                                        checked={
                                            produtoForm.ativo
                                        }
                                        onChange={
                                            event =>
                                                setProdutoForm(
                                                    atual => ({
                                                        ...atual,
                                                        ativo:
                                                        event
                                                            .target
                                                            .checked
                                                    })
                                                )
                                        }
                                    />

                                    <span>
                                        Produto ativo para novos pedidos
                                    </span>
                                </label>

                                <div
                                    style={
                                        styles.actions
                                    }
                                >
                                    {produtoEditando && (
                                        <BlurButton
                                            type="button"
                                            onClick={
                                                limparProdutoForm
                                            }
                                            style={
                                                styles.secondary
                                            }
                                            disabled={
                                                salvando
                                            }
                                        >
                                            Cancelar
                                        </BlurButton>
                                    )}

                                    <BlurButton
                                        type="submit"
                                        style={
                                            styles.primary
                                        }
                                        disabled={
                                            salvando
                                        }
                                    >
                                        {salvando
                                            ? 'Salvando...'
                                            : produtoEditando
                                                ? 'Salvar produto'
                                                : 'Cadastrar produto'}
                                    </BlurButton>
                                </div>
                            </form>

                            <section
                                style={
                                    styles.card
                                }
                            >
                                <Heading
                                    title="Produtos e estoque"
                                    subtitle="A quantidade disponível é usada para validar os pedidos."
                                    theme={theme}
                                />

                                <div
                                    style={
                                        styles.tableWrap
                                    }
                                >
                                    <table
                                        style={
                                            styles.table
                                        }
                                    >
                                        <thead>
                                        <tr>
                                            <th style={styles.th}>
                                                Produto
                                            </th>
                                            <th style={styles.th}>
                                                Código
                                            </th>
                                            <th style={styles.th}>
                                                Preço
                                            </th>
                                            <th style={styles.th}>
                                                Estoque
                                            </th>
                                            <th style={styles.th}>
                                                Status
                                            </th>
                                            <th style={styles.th}>
                                                Ações
                                            </th>
                                        </tr>
                                        </thead>

                                        <tbody>
                                        {produtos.map(
                                            produto => {
                                                const estoque =
                                                    Number(
                                                        produto
                                                            .estoque
                                                            ?.quantidadeDisponivel ??
                                                        0
                                                    );

                                                return (
                                                    <tr
                                                        key={
                                                            produto.id
                                                        }
                                                        style={
                                                            styles.tr
                                                        }
                                                    >
                                                        <td
                                                            style={
                                                                styles.tdStrong
                                                            }
                                                        >
                                                            {
                                                                produto.nome
                                                            }
                                                        </td>

                                                        <td
                                                            style={
                                                                styles.td
                                                            }
                                                        >
                                                            {
                                                                produto.codigo
                                                            }
                                                        </td>

                                                        <td
                                                            style={
                                                                styles.td
                                                            }
                                                        >
                                                            {formatarMoeda(
                                                                produto.preco
                                                            )}
                                                        </td>

                                                        <td
                                                            style={
                                                                styles.td
                                                            }
                                                        >
                                                            <span
                                                                style={
                                                                    estoque >
                                                                    0
                                                                        ? styles.stockOk
                                                                        : styles.stockEmpty
                                                                }
                                                            >
                                                                {estoque}{' '}
                                                                unidades
                                                            </span>
                                                        </td>

                                                        <td
                                                            style={
                                                                styles.td
                                                            }
                                                        >
                                                            {produto.ativo !==
                                                            false
                                                                ? 'Ativo'
                                                                : 'Inativo'}
                                                        </td>

                                                        <td
                                                            style={
                                                                styles.td
                                                            }
                                                        >
                                                            <div
                                                                style={
                                                                    styles.inlineActions
                                                                }
                                                            >
                                                                <BlurButton
                                                                    type="button"
                                                                    onClick={() =>
                                                                        iniciarEdicaoProduto(
                                                                            produto
                                                                        )
                                                                    }
                                                                    style={
                                                                        styles.small
                                                                    }
                                                                    disabled={
                                                                        salvando
                                                                    }
                                                                >
                                                                    <Edit3 size={13} />
                                                                    Editar
                                                                </BlurButton>

                                                                <BlurButton
                                                                    type="button"
                                                                    onClick={() =>
                                                                        excluirProduto(
                                                                            produto
                                                                        )
                                                                    }
                                                                    style={
                                                                        styles.danger
                                                                    }
                                                                    disabled={
                                                                        salvando
                                                                    }
                                                                >
                                                                    <Trash2 size={13} />
                                                                    Excluir
                                                                </BlurButton>
                                                            </div>
                                                        </td>
                                                    </tr>
                                                );
                                            }
                                        )}
                                        </tbody>
                                    </table>

                                    {produtos.length ===
                                        0 && (
                                            <Empty
                                                theme={
                                                    theme
                                                }
                                                text="Nenhum produto cadastrado."
                                            />
                                        )}
                                </div>
                            </section>
                        </div>
                    )}

                {aba ===
                    ABA_PEDIDOS && (
                        <div
                            style={
                                styles.grid
                            }
                        >
                            <form
                                onSubmit={
                                    criarPedido
                                }
                                style={
                                    styles.card
                                }
                            >
                                <Heading
                                    title="Novo pedido"
                                    subtitle="Associe o pedido a um cliente e aos produtos solicitados."
                                    theme={theme}
                                />

                                <SelectField
                                    label="Cliente"
                                    value={
                                        clientePedido
                                    }
                                    onChange={
                                        setClientePedido
                                    }
                                    theme={
                                        theme
                                    }
                                >
                                    <option value="">
                                        Selecione um cliente
                                    </option>

                                    {clientes
                                        .filter(
                                            cliente =>
                                                cliente.ativo !==
                                                false
                                        )
                                        .map(
                                            cliente => (
                                                <option
                                                    key={
                                                        cliente.id
                                                    }
                                                    value={
                                                        cliente.id
                                                    }
                                                >
                                                    {
                                                        cliente.nome
                                                    }
                                                </option>
                                            )
                                        )}
                                </SelectField>

                                <div
                                    style={
                                        styles.itemsHeader
                                    }
                                >
                                    <div>
                                        <strong>
                                            Produtos solicitados
                                        </strong>

                                        <span
                                            style={
                                                styles.help
                                            }
                                        >
                                            O sistema valida o estoque antes de gravar o pedido.
                                        </span>
                                    </div>

                                    <BlurButton
                                        type="button"
                                        onClick={
                                            adicionarItemPedido
                                        }
                                        style={
                                            styles.small
                                        }
                                        disabled={
                                            salvando
                                        }
                                    >
                                        <Plus size={13} />
                                        Adicionar
                                    </BlurButton>
                                </div>

                                <div
                                    style={
                                        styles.itemList
                                    }
                                >
                                    {itensPedido.map(
                                        (
                                            item,
                                            index
                                        ) => {
                                            const estoque =
                                                estoqueDisponivel(
                                                    item.produtoId
                                                );

                                            const quantidade =
                                                Number(
                                                    item.quantidade
                                                );

                                            const insuficiente =
                                                item.produtoId &&
                                                quantidade >
                                                estoque;

                                            return (
                                                <div
                                                    key={`${index}-${item.produtoId}`}
                                                    style={
                                                        styles.item
                                                    }
                                                >
                                                    <select
                                                        value={
                                                            item.produtoId
                                                        }
                                                        onChange={
                                                            event =>
                                                                alterarItemPedido(
                                                                    index,
                                                                    'produtoId',
                                                                    event
                                                                        .target
                                                                        .value
                                                                )
                                                        }
                                                        style={
                                                            styles.input
                                                        }
                                                        disabled={
                                                            salvando
                                                        }
                                                    >
                                                        <option value="">
                                                            Selecione o produto
                                                        </option>

                                                        {produtos
                                                            .filter(
                                                                produto =>
                                                                    produto.ativo !==
                                                                    false
                                                            )
                                                            .map(
                                                                produto => (
                                                                    <option
                                                                        key={
                                                                            produto.id
                                                                        }
                                                                        value={
                                                                            produto.id
                                                                        }
                                                                    >
                                                                        {
                                                                            produto.nome
                                                                        }{' '}
                                                                        —{' '}
                                                                        {Number(
                                                                            produto
                                                                                .estoque
                                                                                ?.quantidadeDisponivel ??
                                                                            0
                                                                        )}{' '}
                                                                        disponíveis
                                                                    </option>
                                                                )
                                                            )}
                                                    </select>

                                                    <input
                                                        type="number"
                                                        min="1"
                                                        step="1"
                                                        value={
                                                            item.quantidade
                                                        }
                                                        onChange={
                                                            event =>
                                                                alterarItemPedido(
                                                                    index,
                                                                    'quantidade',
                                                                    event
                                                                        .target
                                                                        .value
                                                                )
                                                        }
                                                        style={
                                                            styles.quantity
                                                        }
                                                        disabled={
                                                            salvando
                                                        }
                                                    />

                                                    <BlurButton
                                                        type="button"
                                                        onClick={() =>
                                                            removerItemPedido(
                                                                index
                                                            )
                                                        }
                                                        style={
                                                            styles.iconDanger
                                                        }
                                                        disabled={
                                                            salvando
                                                        }
                                                        aria-label="Remover item"
                                                    >
                                                        <Trash2 size={14} />
                                                    </BlurButton>

                                                    <div
                                                        style={
                                                            styles.stockLine
                                                        }
                                                    >
                                                        <span>
                                                            Estoque disponível:{' '}
                                                            {
                                                                estoque
                                                            }
                                                        </span>

                                                        {insuficiente && (
                                                            <strong
                                                                style={
                                                                    styles.stockError
                                                                }
                                                            >
                                                                Estoque insuficiente
                                                            </strong>
                                                        )}
                                                    </div>
                                                </div>
                                            );
                                        }
                                    )}
                                </div>

                                <div
                                    style={
                                        styles.rule
                                    }
                                >
                                    <strong>
                                        Regra de estoque
                                    </strong>

                                    <span>
                                        A quantidade solicitada nunca pode ser maior que o estoque disponível.
                                        Essa validação também é feita no backend.
                                    </span>
                                </div>

                                <div
                                    style={
                                        styles.actions
                                    }
                                >
                                    <BlurButton
                                        type="submit"
                                        style={
                                            pedidoValido
                                                ? styles.primary
                                                : styles.disabled
                                        }
                                        disabled={
                                            salvando ||
                                            !pedidoValido
                                        }
                                    >
                                        {salvando
                                            ? 'Registrando...'
                                            : 'Registrar pedido'}
                                    </BlurButton>
                                </div>
                            </form>

                            <section
                                style={
                                    styles.card
                                }
                            >
                                <Heading
                                    title="Pedidos registrados"
                                    subtitle="Cada pedido mantém o cliente e seus produtos associados."
                                    theme={theme}
                                />

                                <div
                                    style={
                                        styles.orders
                                    }
                                >
                                    {pedidos.map(
                                        pedido => (
                                            <article
                                                key={
                                                    pedido.id
                                                }
                                                style={
                                                    styles.order
                                                }
                                            >
                                                <div
                                                    style={
                                                        styles.orderTop
                                                    }
                                                >
                                                    <div>
                                                        <strong>
                                                            Pedido #
                                                            {
                                                                pedido.id
                                                            }
                                                        </strong>

                                                        <span
                                                            style={
                                                                styles.orderClient
                                                            }
                                                        >
                                                            {
                                                                pedido
                                                                    .cliente
                                                                    ?.nome
                                                            }
                                                        </span>
                                                    </div>

                                                    <div
                                                        style={
                                                            styles.inlineActions
                                                        }
                                                    >
                                                        <StatusBadge
                                                            status={
                                                                pedido.status
                                                            }
                                                            theme={
                                                                theme
                                                            }
                                                        />

                                                        {pedido.status ===
                                                            'ABERTO' && (
                                                                <BlurButton
                                                                    type="button"
                                                                    onClick={() =>
                                                                        cancelarPedido(
                                                                            pedido
                                                                        )
                                                                    }
                                                                    style={
                                                                        styles.danger
                                                                    }
                                                                    disabled={
                                                                        salvando
                                                                    }
                                                                >
                                                                    Cancelar
                                                                </BlurButton>
                                                            )}
                                                    </div>
                                                </div>

                                                <div
                                                    style={
                                                        styles.orderItems
                                                    }
                                                >
                                                    {(pedido.itens ||
                                                        []).map(
                                                        item => (
                                                            <div
                                                                key={
                                                                    item.id ||
                                                                    `${pedido.id}-${item.produto?.id}`
                                                                }
                                                                style={
                                                                    styles.orderItem
                                                                }
                                                            >
                                                                <span>
                                                                    {
                                                                        item
                                                                            .produto
                                                                            ?.nome
                                                                    }
                                                                </span>

                                                                <strong>
                                                                    {
                                                                        item.quantidade
                                                                    }{' '}
                                                                    un.
                                                                </strong>
                                                            </div>
                                                        )
                                                    )}
                                                </div>
                                            </article>
                                        )
                                    )}

                                    {pedidos.length ===
                                        0 && (
                                            <Empty
                                                theme={
                                                    theme
                                                }
                                                text="Nenhum pedido cadastrado."
                                            />
                                        )}
                                </div>
                            </section>
                        </div>
                    )}
            </section>
        </div>
    );
}

function TabButton({
                       active,
                       onClick,
                       theme,
                       icon,
                       label
                   }) {
    const styles = getStyles(theme);

    return (
        <BlurButton
            type="button"
            onClick={onClick}
            style={
                active
                    ? {
                        ...styles.tab,
                        ...styles.tabActive
                    }
                    : styles.tab
            }
        >
            {icon}
            {label}
        </BlurButton>
    );
}

function Heading({
                     title,
                     subtitle,
                     theme
                 }) {
    const styles = getStyles(theme);

    return (
        <div
            style={{
                marginBottom: '16px'
            }}
        >
            <h3
                style={
                    styles.heading
                }
            >
                {title}
            </h3>

            <p
                style={
                    styles.headingSubtitle
                }
            >
                {subtitle}
            </p>
        </div>
    );
}

function Field({
                   label,
                   value,
                   onChange,
                   theme,
                   type = 'text',
                   min,
                   step,
                   required = false,
                   textarea = false,
                   wide = false
               }) {
    const styles = getStyles(theme);

    return (
        <label
            style={
                wide
                    ? styles.fieldWide
                    : styles.field
            }
        >
            <span>{label}</span>

            {textarea ? (
                <textarea
                    value={value}
                    onChange={event =>
                        onChange(
                            event.target.value
                        )
                    }
                    style={{
                        ...styles.input,
                        minHeight: '82px',
                        resize: 'vertical'
                    }}
                    required={
                        required
                    }
                />
            ) : (
                <input
                    type={type}
                    min={min}
                    step={step}
                    value={value}
                    onChange={event =>
                        onChange(
                            event.target.value
                        )
                    }
                    style={
                        styles.input
                    }
                    required={
                        required
                    }
                />
            )}
        </label>
    );
}

function SelectField({
                         label,
                         value,
                         onChange,
                         theme,
                         children
                     }) {
    const styles = getStyles(theme);

    return (
        <label
            style={
                styles.field
            }
        >
            <span>{label}</span>

            <select
                value={value}
                onChange={event =>
                    onChange(
                        event.target.value
                    )
                }
                style={
                    styles.input
                }
            >
                {children}
            </select>
        </label>
    );
}

function Empty({
                   theme,
                   text
               }) {
    return (
        <div
            style={{
                padding: '28px',
                textAlign: 'center',
                color: theme.inkSoft,
                fontSize: '12px'
            }}
        >
            {text}
        </div>
    );
}

function StatusBadge({
                         status,
                         theme
                     }) {
    const styles = getStyles(theme);

    const mapa = {
        ABERTO: {
            bg: theme.statuses.EM_TRANSITO.bg,
            ink: theme.statuses.EM_TRANSITO.ink,
            dot: theme.statuses.EM_TRANSITO.dot,
            label: 'Aberto'
        },
        CANCELADO: {
            bg: theme.statuses.CANCELADA.bg,
            ink: theme.statuses.CANCELADA.ink,
            dot: theme.statuses.CANCELADA.dot,
            label: 'Cancelado'
        },
        CONCLUIDO: {
            bg: theme.statuses.ENTREGUE.bg,
            ink: theme.statuses.ENTREGUE.ink,
            dot: theme.statuses.ENTREGUE.dot,
            label: 'Concluído'
        }
    };

    const tone =
        mapa[status] ||
        styles.defaultStatus;

    return (
        <span
            style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '6px',
                padding: '5px 9px',
                borderRadius: '999px',
                backgroundColor: tone.bg,
                color: tone.ink,
                fontSize: '10px',
                fontWeight: 700
            }}
        >
            <span
                style={{
                    width: '6px',
                    height: '6px',
                    borderRadius: '50%',
                    backgroundColor:
                    tone.dot
                }}
            />

            {tone.label ||
                status}
        </span>
    );
}

function formatarMoeda(valor) {
    if (
        valor === null ||
        valor === undefined
    ) {
        return '-';
    }

    return Number(
        valor
    ).toLocaleString(
        'pt-BR',
        {
            style: 'currency',
            currency: 'BRL'
        }
    );
}

function BlurButton({
                        children,
                        style,
                        disabled = false,
                        onMouseEnter,
                        onMouseLeave,
                        ...props
                    }) {
    const [hover, setHover] = useState(false);

    return (
        <button
            {...props}
            disabled={disabled}
            onMouseEnter={event => {
                setHover(true);
                onMouseEnter?.(event);
            }}
            onMouseLeave={event => {
                setHover(false);
                onMouseLeave?.(event);
            }}
            style={{
                ...style,
                transform:
                    hover && !disabled
                        ? 'translateY(-1px) scale(1.01)'
                        : 'translateY(0) scale(1)',
                filter:
                    hover && !disabled
                        ? 'brightness(1.08)'
                        : 'brightness(1)',
            }}
        >
            {children}
        </button>
    );
}

function getStyles(theme) {
    return {
        launcher: {
            position: 'fixed',
            right: '24px',
            bottom: '24px',
            zIndex: 2500,
            display: 'inline-flex',
            alignItems: 'center',
            gap: '8px',
            padding: '11px 14px',
            borderRadius: '9px',
            border: `1px solid ${theme.border}`,
            backgroundColor: 'rgba(255,255,255,0.06)',
            backdropFilter: 'blur(14px) saturate(120%)',
            WebkitBackdropFilter: 'blur(14px) saturate(120%)',
            color: theme.ink,
            fontSize: '12px',
            fontWeight: 700,
            boxShadow:
                '0 12px 32px rgba(0,0,0,0.24)',
            cursor: 'pointer',
            transition: 'all 0.2s ease'
        },

        overlay: {
            position: 'fixed',
            inset: 0,
            zIndex: 5000,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '20px',
            backgroundColor:
                'rgba(0,0,0,0.62)'
        },

        modal: {
            width: '100%',
            maxWidth: '1380px',
            maxHeight: '94vh',
            overflowY: 'auto',
            padding: '24px',
            borderRadius: '16px',
            border: `1px solid ${theme.border}`,
            backgroundColor: theme.surface,
            color: theme.ink,
            boxShadow:
                '0 30px 80px rgba(0,0,0,0.35)'
        },

        header: {
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'flex-start',
            gap: '18px'
        },

        eyebrow: {
            marginBottom: '5px',
            color: theme.accent,
            fontSize: '10px',
            fontWeight: 800,
            letterSpacing: '0.12em'
        },

        title: {
            margin: 0,
            fontFamily:
                "Georgia, 'Iowan Old Style', serif",
            fontSize: '25px',
            fontWeight: 400
        },

        subtitle: {
            margin: '6px 0 0',
            color: theme.inkSoft,
            fontSize: '12px',
            lineHeight: 1.5
        },

        closeButton: {
            width: '36px',
            height: '36px',
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            borderRadius: '8px',
            border: `1px solid ${theme.border}`,
            backgroundColor: 'rgba(255,255,255,0.06)',
            backdropFilter: 'blur(10px)',
            WebkitBackdropFilter: 'blur(10px)',
            color: theme.ink,
            cursor: 'pointer'
        },

        tabs: {
            display: 'flex',
            flexWrap: 'wrap',
            alignItems: 'center',
            gap: '8px',
            marginTop: '20px',
            paddingBottom: '14px',
            borderBottom: `1px solid ${theme.border}`
        },

        tab: {
            display: 'inline-flex',
            alignItems: 'center',
            gap: '7px',
            padding: '9px 12px',
            borderRadius: '8px',
            border: `1px solid ${theme.border}`,
            backgroundColor: 'rgba(255,255,255,0.05)',
            backdropFilter: 'blur(10px)',
            WebkitBackdropFilter: 'blur(10px)',
            color: theme.inkSoft,
            fontSize: '11px',
            fontWeight: 700,
            cursor: 'pointer',
            boxShadow: '0 4px 14px rgba(0,0,0,0.12)',
            transition: 'all 0.2s ease'
        },

        tabActive: {
            backgroundColor: theme.accent,
            color: theme.accentInk,
            borderColor: theme.accent
        },

        refresh: {
            marginLeft: 'auto',
            display: 'inline-flex',
            alignItems: 'center',
            gap: '7px',
            padding: '9px 12px',
            borderRadius: '8px',
            border: `1px solid ${theme.border}`,
            backgroundColor: 'rgba(255,255,255,0.05)',
            backdropFilter: 'blur(10px)',
            WebkitBackdropFilter: 'blur(10px)',
            color: theme.ink,
            fontSize: '11px',
            fontWeight: 700,
            cursor: 'pointer',
            boxShadow: '0 5px 16px rgba(0,0,0,0.12)',
            transition: 'all 0.2s ease'
        },

        success: {
            marginTop: '14px',
            padding: '11px 13px',
            borderRadius: '8px',
            border: '1px solid #36543B',
            backgroundColor: '#213225',
            color: '#A4CAA9',
            fontSize: '11px'
        },

        error: {
            marginTop: '14px',
            padding: '11px 13px',
            borderRadius: '8px',
            border: '1px solid #6E2A3C',
            backgroundColor: '#3A1522',
            color: '#F2A7B8',
            fontSize: '11px'
        },

        grid: {
            display: 'grid',
            gridTemplateColumns:
                '360px minmax(0, 1fr)',
            gap: '16px',
            marginTop: '18px'
        },

        card: {
            minWidth: 0,
            padding: '18px',
            borderRadius: '12px',
            border: `1px solid ${theme.border}`,
            backgroundColor: theme.surface
        },

        heading: {
            margin: 0,
            fontFamily:
                "Georgia, 'Iowan Old Style', serif",
            fontSize: '19px',
            fontWeight: 400
        },

        headingSubtitle: {
            margin: '5px 0 0',
            color: theme.inkSoft,
            fontSize: '10px',
            lineHeight: 1.45
        },

        fields: {
            display: 'grid',
            gridTemplateColumns:
                '1fr 1fr',
            gap: '12px'
        },

        field: {
            display: 'flex',
            flexDirection: 'column',
            gap: '6px',
            marginBottom: '12px',
            color: theme.inkSoft,
            fontSize: '10px',
            fontWeight: 700
        },

        fieldWide: {
            display: 'flex',
            flexDirection: 'column',
            gap: '6px',
            marginBottom: '12px',
            color: theme.inkSoft,
            fontSize: '10px',
            fontWeight: 700,
            gridColumn: '1 / -1'
        },

        input: {
            width: '100%',
            boxSizing: 'border-box',
            padding: '10px 11px',
            borderRadius: '8px',
            border: `1px solid ${theme.borderStrong}`,
            backgroundColor: theme.surfaceAlt,
            color: theme.ink,
            fontSize: '12px'
        },

        twoColumns: {
            display: 'grid',
            gridTemplateColumns:
                '1fr 1fr',
            gap: '12px'
        },

        checkbox: {
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            margin: '2px 0 15px',
            color: theme.inkSoft,
            fontSize: '10px'
        },

        actions: {
            display: 'flex',
            justifyContent: 'flex-end',
            gap: '8px',
            alignItems: 'center',
            marginTop: '8px'
        },

        primary: {
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '7px',
            padding: '10px 14px',
            border: `1px solid ${theme.accent}`,
            borderRadius: '8px',
            backgroundColor: `${theme.accent}CC`,
            backdropFilter: 'blur(10px)',
            WebkitBackdropFilter: 'blur(10px)',
            color: theme.accentInk,
            fontSize: '11px',
            fontWeight: 800,
            cursor: 'pointer',
            boxShadow: '0 6px 20px rgba(0,0,0,0.18)',
            transition: 'all 0.2s ease'
        },

        secondary: {
            padding: '10px 14px',
            borderRadius: '8px',
            border: `1px solid ${theme.border}`,
            backgroundColor: 'rgba(255,255,255,0.06)',
            backdropFilter: 'blur(10px)',
            WebkitBackdropFilter: 'blur(10px)',
            color: theme.ink,
            fontSize: '11px',
            fontWeight: 700,
            cursor: 'pointer',
            boxShadow: '0 4px 14px rgba(0,0,0,0.12)',
            transition: 'all 0.2s ease'
        },

        disabled: {
            padding: '10px 14px',
            borderRadius: '8px',
            border: `1px solid ${theme.borderStrong}`,
            backgroundColor: theme.borderStrong,
            color: theme.inkSoft,
            fontSize: '11px',
            fontWeight: 800,
            cursor: 'not-allowed'
        },

        small: {
            display: 'inline-flex',
            alignItems: 'center',
            gap: '5px',
            padding: '7px 9px',
            borderRadius: '7px',
            border: `1px solid ${theme.border}`,
            backgroundColor: 'rgba(255,255,255,0.05)',
            backdropFilter: 'blur(8px)',
            WebkitBackdropFilter: 'blur(8px)',
            color: theme.ink,
            fontSize: '10px',
            fontWeight: 700,
            cursor: 'pointer',
            boxShadow: '0 4px 12px rgba(0,0,0,0.10)',
            transition: 'all 0.2s ease'
        },

        danger: {
            display: 'inline-flex',
            alignItems: 'center',
            gap: '5px',
            padding: '7px 9px',
            borderRadius: '7px',
            border: `1px solid ${theme.border}`,
            backgroundColor: 'rgba(255,255,255,0.04)',
            backdropFilter: 'blur(8px)',
            WebkitBackdropFilter: 'blur(8px)',
            color: theme.danger,
            fontSize: '10px',
            fontWeight: 700,
            cursor: 'pointer',
            boxShadow: '0 4px 12px rgba(0,0,0,0.10)',
            transition: 'all 0.2s ease'
        },

        inlineActions: {
            display: 'flex',
            alignItems: 'center',
            flexWrap: 'wrap',
            gap: '5px'
        },

        tableWrap: {
            width: '100%',
            overflowX: 'auto'
        },

        table: {
            width: '100%',
            minWidth: '760px',
            borderCollapse: 'collapse'
        },

        th: {
            textAlign: 'left',
            padding: '8px',
            borderBottom: `1px solid ${theme.border}`,
            color: theme.inkSoft,
            fontSize: '9px',
            textTransform: 'uppercase',
            letterSpacing: '0.05em'
        },

        tr: {
            borderBottom: `1px solid ${theme.border}`
        },

        td: {
            padding: '10px 8px',
            fontSize: '11px',
            verticalAlign: 'middle'
        },

        tdStrong: {
            padding: '10px 8px',
            fontSize: '11px',
            fontWeight: 700,
            verticalAlign: 'middle'
        },

        stockOk: {
            display: 'inline-flex',
            padding: '4px 8px',
            borderRadius: '999px',
            backgroundColor: '#25332A',
            color: '#94BE99',
            fontSize: '9px',
            fontWeight: 700
        },

        stockEmpty: {
            display: 'inline-flex',
            padding: '4px 8px',
            borderRadius: '999px',
            backgroundColor: '#3A1522',
            color: '#F2A7B8',
            fontSize: '9px',
            fontWeight: 700
        },

        itemsHeader: {
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'flex-end',
            gap: '10px',
            margin: '6px 0 10px'
        },

        help: {
            display: 'block',
            marginTop: '4px',
            color: theme.inkSoft,
            fontSize: '9px'
        },

        itemList: {
            display: 'flex',
            flexDirection: 'column',
            gap: '9px'
        },

        item: {
            display: 'grid',
            gridTemplateColumns:
                'minmax(0, 1fr) 80px 34px',
            gap: '8px',
            alignItems: 'center',
            padding: '9px',
            borderRadius: '8px',
            border: `1px solid ${theme.border}`,
            backgroundColor: theme.surfaceAlt
        },

        quantity: {
            width: '100%',
            boxSizing: 'border-box',
            padding: '9px 10px',
            borderRadius: '8px',
            border: `1px solid ${theme.borderStrong}`,
            backgroundColor: theme.surface,
            color: theme.ink,
            fontSize: '11px'
        },

        iconDanger: {
            width: '34px',
            height: '34px',
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            borderRadius: '7px',
            border: `1px solid ${theme.border}`,
            backgroundColor: 'rgba(255,255,255,0.04)',
            backdropFilter: 'blur(8px)',
            WebkitBackdropFilter: 'blur(8px)',
            color: theme.danger,
            cursor: 'pointer',
            boxShadow: '0 4px 12px rgba(0,0,0,0.10)',
            transition: 'all 0.2s ease'
        },

        stockLine: {
            gridColumn: '1 / -1',
            display: 'flex',
            justifyContent: 'space-between',
            gap: '8px',
            color: theme.inkSoft,
            fontSize: '9px'
        },

        stockError: {
            color: theme.danger
        },

        rule: {
            display: 'flex',
            flexDirection: 'column',
            gap: '4px',
            marginTop: '13px',
            padding: '10px 11px',
            borderRadius: '8px',
            border: `1px solid ${theme.border}`,
            backgroundColor: theme.surfaceAlt,
            color: theme.inkSoft,
            fontSize: '9px',
            lineHeight: 1.45
        },

        orders: {
            display: 'flex',
            flexDirection: 'column',
            gap: '10px'
        },

        order: {
            padding: '12px',
            borderRadius: '9px',
            border: `1px solid ${theme.border}`,
            backgroundColor: theme.surfaceAlt
        },

        orderTop: {
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'flex-start',
            gap: '12px'
        },

        orderClient: {
            display: 'block',
            marginTop: '4px',
            color: theme.inkSoft,
            fontSize: '9px'
        },

        orderItems: {
            display: 'flex',
            flexDirection: 'column',
            gap: '5px',
            marginTop: '10px',
            paddingTop: '9px',
            borderTop: `1px solid ${theme.border}`
        },

        orderItem: {
            display: 'flex',
            justifyContent: 'space-between',
            gap: '10px',
            fontSize: '10px'
        },

        defaultStatus: {
            bg: theme.surfaceAlt,
            ink: theme.ink,
            dot: theme.accent,
            label: 'Status'
        }
    };

}