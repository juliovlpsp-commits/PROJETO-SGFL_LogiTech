package com.logitech.sgfl.service;

import com.logitech.sgfl.dto.PedidoRequest;
import com.logitech.sgfl.exceptions.RegraNegocioException;
import com.logitech.sgfl.me.Cliente;
import com.logitech.sgfl.me.ItemPedido;
import com.logitech.sgfl.me.Estoque;
import com.logitech.sgfl.me.Pedido;
import com.logitech.sgfl.me.Produto;
import com.logitech.sgfl.repository.ClienteRepository;
import com.logitech.sgfl.repository.EstoqueRepository;
import com.logitech.sgfl.repository.PedidoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private EstoqueRepository estoqueRepository;

    @Mock
    private AuditoriaTransversalService auditoria;

    @InjectMocks
    private PedidoService pedidoService;

    @Test
    void deveCriarPedidoEbaixarEstoque() {

        Cliente cliente =
                new Cliente(
                        "Cliente Teste",
                        "12345678909",
                        "cliente@teste.com"
                );

        Produto produto =
                new Produto(
                        "P001",
                        "Produto Teste",
                        "Produto de teste",
                        new BigDecimal("50.00")
                );

        Estoque estoque =
                new Estoque(
                        produto,
                        10
                );

        PedidoRequest.ItemRequest item =
                new PedidoRequest.ItemRequest();

        item.setProdutoId(1L);
        item.setQuantidade(3);

        PedidoRequest request =
                new PedidoRequest();

        request.setClienteId(1L);
        request.setItens(
                List.of(item)
        );

        when(clienteRepository.findById(1L))
                .thenReturn(
                        Optional.of(cliente)
                );

        when(
                estoqueRepository
                        .findByProdutoIdForUpdate(1L)
        )
                .thenReturn(
                        Optional.of(estoque)
                );

        when(pedidoRepository.save(any(Pedido.class)))
                .thenAnswer(
                        invocation ->
                                invocation.getArgument(0)
                );

        Pedido pedido =
                pedidoService.criar(request);

        assertNotNull(pedido);

        assertEquals(
                7,
                estoque.getQuantidadeDisponivel()
        );

        verify(
                estoqueRepository
        ).save(estoque);

        verify(
                pedidoRepository
        ).save(any(Pedido.class));

        verify(auditoria).registrar(
                eq("PEDIDO"),
                any(),
                eq("CRIADO"),
                anyString(),
                isNull(),
                any(Pedido.class)
        );
    }

    @Test
    void deveRecusarPedidoQuandoEstoqueForInsuficiente() {

        Cliente cliente =
                new Cliente(
                        "Cliente Teste",
                        "12345678909",
                        "cliente@teste.com"
                );

        Produto produto =
                new Produto(
                        "P001",
                        "Produto Teste",
                        "Produto de teste",
                        new BigDecimal("50.00")
                );

        Estoque estoque =
                new Estoque(
                        produto,
                        2
                );

        PedidoRequest.ItemRequest item =
                new PedidoRequest.ItemRequest();

        item.setProdutoId(1L);
        item.setQuantidade(5);

        PedidoRequest request =
                new PedidoRequest();

        request.setClienteId(1L);
        request.setItens(
                List.of(item)
        );

        when(clienteRepository.findById(1L))
                .thenReturn(
                        Optional.of(cliente)
                );

        when(
                estoqueRepository
                        .findByProdutoIdForUpdate(1L)
        )
                .thenReturn(
                        Optional.of(estoque)
                );

        RegraNegocioException exception =
                assertThrows(
                        RegraNegocioException.class,
                        () -> pedidoService.criar(request)
                );

        assertTrue(
                exception.getMessage()
                        .contains("Estoque insuficiente")
        );

        assertEquals(
                2,
                estoque.getQuantidadeDisponivel()
        );

        verify(
                pedidoRepository,
                never()
        ).save(any());
    }

    @Test
    void deveRecusarProdutoInativo() {

        Cliente cliente =
                new Cliente(
                        "Cliente Teste",
                        "12345678909",
                        "cliente@teste.com"
                );

        Produto produto =
                new Produto(
                        "P001",
                        "Produto Inativo",
                        "Produto desativado",
                        new BigDecimal("50.00")
                );

        produto.setAtivo(false);

        Estoque estoque =
                new Estoque(
                        produto,
                        20
                );

        PedidoRequest.ItemRequest item =
                new PedidoRequest.ItemRequest();

        item.setProdutoId(1L);
        item.setQuantidade(1);

        PedidoRequest request =
                new PedidoRequest();

        request.setClienteId(1L);
        request.setItens(
                List.of(item)
        );

        when(clienteRepository.findById(1L))
                .thenReturn(
                        Optional.of(cliente)
                );

        when(
                estoqueRepository
                        .findByProdutoIdForUpdate(1L)
        )
                .thenReturn(
                        Optional.of(estoque)
                );

        assertThrows(
                RegraNegocioException.class,
                () -> pedidoService.criar(request)
        );

        assertEquals(
                20,
                estoque.getQuantidadeDisponivel()
        );

        verify(
                pedidoRepository,
                never()
        ).save(any());
    }

    @Test
    void deveDevolverEstoqueQuandoPedidoForCancelado() {

        Cliente cliente =
                new Cliente(
                        "Cliente Teste",
                        "12345678909",
                        "cliente@teste.com"
                );

        Produto produto =
                new Produto(
                        "P001",
                        "Produto Teste",
                        "Produto utilizado no teste de cancelamento",
                        new BigDecimal("50.00")
                );

        org.springframework.test.util.ReflectionTestUtils.setField(
                produto,
                "id",
                1L
        );

        Estoque estoque =
                new Estoque(
                        produto,
                        5
                );

        Pedido pedido =
                new Pedido(cliente);

        ItemPedido item =
                new ItemPedido(
                        produto,
                        3,
                        new BigDecimal("50.00")
                );

        pedido.adicionarItem(item);
        when(
                pedidoRepository
                        .findByIdComItensForUpdate(1L)
        )
                .thenReturn(
                        Optional.of(pedido)
                );

        when(
                estoqueRepository
                        .findByProdutoIdForUpdate(1L)
        )
                .thenReturn(
                        Optional.of(estoque)
                );

        when(
                estoqueRepository.save(any(Estoque.class))
        )
                .thenAnswer(
                        invocation ->
                                invocation.getArgument(0)
                );

        when(
                pedidoRepository.save(any(Pedido.class))
        )
                .thenAnswer(
                        invocation ->
                                invocation.getArgument(0)
                );

        Pedido resultado =
                pedidoService.cancelar(1L);

        assertEquals(
                com.logitech.sgfl.enums.StatusPedido.CANCELADO,
                resultado.getStatus()
        );

        assertEquals(
                8,
                estoque.getQuantidadeDisponivel()
        );

        verify(auditoria).registrar(
                eq("PEDIDO"),
                any(),
                eq("CANCELADO"),
                anyString(),
                any(),
                any(Pedido.class)
        );
    }
}