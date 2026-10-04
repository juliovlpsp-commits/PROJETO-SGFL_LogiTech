package com.logitech.sgfl.service;

import com.logitech.sgfl.dto.PedidoRequest;
import com.logitech.sgfl.enums.StatusPedido;
import com.logitech.sgfl.exceptions.RecursoNaoEncontradoException;
import com.logitech.sgfl.exceptions.RegraNegocioException;
import com.logitech.sgfl.me.*;
import com.logitech.sgfl.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final EstoqueRepository estoqueRepository;

    public PedidoService(
            PedidoRepository pedidoRepository,
            ClienteRepository clienteRepository,
            EstoqueRepository estoqueRepository
    ) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.estoqueRepository = estoqueRepository;
    }

    @Transactional
    public Pedido criar(
            PedidoRequest request
    ) {

        Cliente cliente =
                clienteRepository.findById(
                                request.getClienteId()
                        )
                        .orElseThrow(() ->
                                new RecursoNaoEncontradoException(
                                        "Cliente não encontrado: " +
                                                request.getClienteId()
                                )
                        );

        if (!cliente.isAtivo()) {
            throw new RegraNegocioException(
                    "O cliente está inativo e não pode realizar pedidos."
            );
        }

        /*
         * Agrupa produtos repetidos no mesmo pedido.
         *
         * Exemplo:
         *
         * produto 10 -> 2
         * produto 10 -> 3
         *
         * vira:
         *
         * produto 10 -> 5
         */
        Map<Long, Integer> quantidades =
                new TreeMap<>();

        for (
                PedidoRequest.ItemRequest item :
                request.getItens()
        ) {

            try {

                quantidades.merge(
                        item.getProdutoId(),
                        item.getQuantidade(),
                        Math::addExact
                );

            } catch (ArithmeticException exception) {

                throw new RegraNegocioException(
                        "Quantidade solicitada é muito grande."
                );
            }
        }

        /*
         * Lock pessimista dos estoques.
         *
         * A TreeMap garante que os produtos sejam bloqueados
         * sempre na mesma ordem, reduzindo risco de deadlock
         * quando dois pedidos possuem vários produtos.
         */
        Map<Long, Estoque> estoques =
                new LinkedHashMap<>();

        for (Map.Entry<Long, Integer> entry :
                quantidades.entrySet()) {

            Long produtoId =
                    entry.getKey();

            int quantidadeSolicitada =
                    entry.getValue();

            Estoque estoque =
                    estoqueRepository
                            .findByProdutoIdForUpdate(
                                    produtoId
                            )
                            .orElseThrow(() ->
                                    new RecursoNaoEncontradoException(
                                            "Estoque não encontrado para o produto: " +
                                                    produtoId
                                    )
                            );

            Produto produto =
                    estoque.getProduto();

            if (!produto.isAtivo()) {

                throw new RegraNegocioException(
                        "O produto '" +
                                produto.getNome() +
                                "' está inativo."
                );
            }

            /*
             * REGRA DE NEGÓCIO PRINCIPAL:
             *
             * Nunca permitimos estoque negativo.
             */
            if (
                    quantidadeSolicitada >
                            estoque.getQuantidadeDisponivel()
            ) {

                throw new RegraNegocioException(
                        "Estoque insuficiente para o produto '" +
                                produto.getNome() +
                                "'. " +
                                "Disponível: " +
                                estoque.getQuantidadeDisponivel() +
                                ". Solicitado: " +
                                quantidadeSolicitada +
                                "."
                );
            }

            estoques.put(
                    produtoId,
                    estoque
            );
        }

        /*
         * Todas as validações passaram.
         *
         * Só agora o estoque é baixado.
         */
        for (Map.Entry<Long, Integer> entry :
                quantidades.entrySet()) {

            Estoque estoque =
                    estoques.get(
                            entry.getKey()
                    );

            estoque.setQuantidadeDisponivel(
                    estoque.getQuantidadeDisponivel()
                            -
                            entry.getValue()
            );

            estoqueRepository.save(
                    estoque
            );
        }

        Pedido pedido =
                new Pedido(cliente);

        pedido.setStatus(
                StatusPedido.ABERTO
        );

        pedido.setCriadoEm(
                LocalDateTime.now()
        );

        for (Map.Entry<Long, Integer> entry :
                quantidades.entrySet()) {

            Estoque estoque =
                    estoques.get(
                            entry.getKey()
                    );

            Produto produto =
                    estoque.getProduto();

            ItemPedido item =
                    new ItemPedido(
                            produto,
                            entry.getValue(),
                            produto.getPreco()
                    );

            pedido.adicionarItem(item);
        }

        return pedidoRepository.save(
                pedido
        );
    }

    @Transactional
    public Pedido buscar(Long id) {

        return pedidoRepository
                .findByIdComItens(id)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Pedido não encontrado: " + id
                        )
                );
    }

    @Transactional
    public List<Pedido> listar() {

        return pedidoRepository
                .findAllComItens();
    }

    @Transactional
    public Page<Pedido> listar(Pageable pageable) {
        Page<Pedido> pedidos = pedidoRepository.findAll(pageable);
        // Inicializa as relações necessárias ao DTO ainda dentro da transação.
        pedidos.forEach(pedido -> {
            pedido.getCliente().getNome();
            pedido.getItens().forEach(item -> item.getProduto().getNome());
        });
        return pedidos;
    }

    @Transactional
    public Pedido cancelar(Long id) {

        Pedido pedido =
                pedidoRepository
                        .findByIdComItensForUpdate(id)
                        .orElseThrow(() ->
                                new RecursoNaoEncontradoException(
                                        "Pedido não encontrado: " + id
                                )
                        );

        if (
                pedido.getStatus() !=
                        StatusPedido.ABERTO
        ) {

            throw new RegraNegocioException(
                    "Somente pedidos ABERTOS podem ser cancelados."
            );
        }

        /*
         * Ao cancelar, devolvemos as quantidades
         * ao estoque.
         */
        for (
                ItemPedido item :
                pedido.getItens()
        ) {

            Long produtoId =
                    item.getProduto()
                            .getId();

            Estoque estoque =
                    estoqueRepository
                            .findByProdutoIdForUpdate(
                                    produtoId
                            )
                            .orElseThrow(() ->
                                    new RecursoNaoEncontradoException(
                                            "Estoque não encontrado para o produto: " +
                                                    produtoId
                                    )
                            );

            long novoEstoque =
                    (long)
                            estoque.getQuantidadeDisponivel()
                            +
                            item.getQuantidade();

            if (
                    novoEstoque >
                            Integer.MAX_VALUE
            ) {

                throw new RegraNegocioException(
                        "Não foi possível devolver o estoque do produto '" +
                                item.getProduto().getNome() +
                                "' porque a quantidade excederia o limite."
                );
            }

            estoque.setQuantidadeDisponivel(
                    (int) novoEstoque
            );

            estoqueRepository.save(
                    estoque
            );
        }

        pedido.setStatus(
                StatusPedido.CANCELADO
        );

        return pedidoRepository.save(
                pedido
        );
    }
}
