package com.logitech.sgfl.service;

import com.logitech.sgfl.exceptions.RecursoNaoEncontradoException;
import com.logitech.sgfl.exceptions.RegraNegocioException;
import com.logitech.sgfl.me.Estoque;
import com.logitech.sgfl.me.Produto;
import com.logitech.sgfl.repository.EstoqueRepository;
import com.logitech.sgfl.repository.ItemPedidoRepository;
import com.logitech.sgfl.repository.ProdutoRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final EstoqueRepository estoqueRepository;
    private final ItemPedidoRepository itemPedidoRepository;

    public ProdutoService(
            ProdutoRepository produtoRepository,
            EstoqueRepository estoqueRepository,
            ItemPedidoRepository itemPedidoRepository
    ) {
        this.produtoRepository = produtoRepository;
        this.estoqueRepository = estoqueRepository;
        this.itemPedidoRepository = itemPedidoRepository;
    }

    @Transactional
    public Produto criar(
            String codigo,
            String nome,
            String descricao,
            BigDecimal preco,
            Integer quantidadeInicial,
            boolean ativo
    ) {

        String codigoNormalizado =
                codigo.trim().toUpperCase();

        if (produtoRepository.existsByCodigo(
                codigoNormalizado
        )) {
            throw new RegraNegocioException(
                    "Já existe um produto com este código."
            );
        }

        Produto produto =
                new Produto(
                        codigoNormalizado,
                        nome.trim(),
                        normalizarTexto(descricao),
                        preco
                );

        produto.setAtivo(ativo);

        Produto salvo =
                produtoRepository.save(produto);

        Estoque estoque =
                new Estoque(
                        salvo,
                        quantidadeInicial
                );

        estoqueRepository.save(estoque);

        salvo.setEstoque(estoque);

        return salvo;
    }

    @Transactional
    public Produto atualizar(
            Long id,
            String codigo,
            String nome,
            String descricao,
            BigDecimal preco,
            boolean ativo
    ) {

        Produto produto =
                buscar(id);

        String codigoNormalizado =
                codigo.trim().toUpperCase();

        if (produtoRepository
                .existsByCodigoAndIdNot(
                        codigoNormalizado,
                        id
                )) {

            throw new RegraNegocioException(
                    "Já existe outro produto com este código."
            );
        }

        produto.setCodigo(
                codigoNormalizado
        );

        produto.setNome(
                nome.trim()
        );

        produto.setDescricao(
                normalizarTexto(descricao)
        );

        produto.setPreco(
                preco
        );

        produto.setAtivo(
                ativo
        );

        return produtoRepository.save(
                produto
        );
    }

    @Transactional
    public Produto atualizarEstoque(
            Long produtoId,
            int quantidade
    ) {

        if (quantidade < 0) {
            throw new RegraNegocioException(
                    "O estoque não pode ser negativo."
            );
        }

        Produto produto =
                buscar(produtoId);

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

        estoque.setQuantidadeDisponivel(
                quantidade
        );

        estoqueRepository.save(estoque);

        produto.setEstoque(estoque);

        return produto;
    }

    @Transactional
    public void excluir(Long id) {

        Produto produto =
                buscar(id);

        if (itemPedidoRepository
                .existsByProduto_Id(id)) {

            throw new RegraNegocioException(
                    "Não é possível excluir o produto porque ele aparece em pedidos. " +
                            "Desative o produto em vez de apagar seu histórico."
            );
        }

        produtoRepository.delete(produto);
    }

    @Transactional
    public Produto buscar(Long id) {

        Produto produto =
                produtoRepository.findById(id)
                        .orElseThrow(() ->
                                new RecursoNaoEncontradoException(
                                        "Produto não encontrado: " + id
                                )
                        );

        estoqueRepository.findByProduto_Id(id)
                .ifPresent(produto::setEstoque);

        return produto;
    }

    @Transactional
    public List<Produto> listar() {

        List<Produto> produtos =
                produtoRepository.findAll();

        produtos.forEach(produto ->
                estoqueRepository
                        .findByProduto_Id(
                                produto.getId()
                        )
                        .ifPresent(
                                produto::setEstoque
                        )
        );

        return produtos;
    }

    private String normalizarTexto(
            String texto
    ) {

        if (texto == null) {
            return null;
        }

        String resultado =
                texto.trim();

        return resultado.isEmpty()
                ? null
                : resultado;
    }
}