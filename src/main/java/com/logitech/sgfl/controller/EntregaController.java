package com.logitech.sgfl.controller;

import com.logitech.sgfl.dto.EntregaRequest;
import com.logitech.sgfl.dto.EntregaResponse;
import com.logitech.sgfl.dto.StatusUpdateRequest;
import com.logitech.sgfl.enums.StatusEntrega;
import com.logitech.sgfl.exceptions.RecursoNaoEncontradoException;
import com.logitech.sgfl.exceptions.RegraNegocioException;
import com.logitech.sgfl.me.Entrega;
import com.logitech.sgfl.repository.EntregaRepository;
import com.logitech.sgfl.repository.EntregaSpecifications;
import com.logitech.sgfl.service.ServicoGerenciamento;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entregas")
public class EntregaController {

    private static final int TAMANHO_PAGINA_PADRAO = 20;
    private static final int TAMANHO_PAGINA_MAXIMO = 100;

    private final ServicoGerenciamento servicoGerenciamento;
    private final EntregaRepository entregaRepository;

    public EntregaController(
            ServicoGerenciamento servicoGerenciamento,
            EntregaRepository entregaRepository
    ) {
        this.servicoGerenciamento = servicoGerenciamento;
        this.entregaRepository = entregaRepository;
    }

    /**
     * Lista entregas de forma paginada.
     */
    @GetMapping
    public Page<EntregaResponse> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + TAMANHO_PAGINA_PADRAO) int size,
            @RequestParam(required = false) StatusEntrega status,
            @RequestParam(required = false) String q
    ) {
        int tamanhoSeguro =
                Math.min(
                        Math.max(size, 1),
                        TAMANHO_PAGINA_MAXIMO
                );

        Pageable pageable =
                PageRequest.of(
                        Math.max(page, 0),
                        tamanhoSeguro,
                        Sort.by("id").ascending()
                );

        return entregaRepository.findAll(EntregaSpecifications.filtrar(status, q), pageable)
                .map(EntregaResponse::from);
    }

    /**
     * Cria uma nova entrega.
     *
     * Toda nova entrega obrigatoriamente começa como PENDENTE.
     * O cliente não pode criar uma entrega já EM_TRANSITO,
     * ENTREGUE ou CANCELADA.
     */
    @PostMapping
    public ResponseEntity<EntregaResponse> criarEntrega(
            @Valid @RequestBody EntregaRequest request
    ) {
        if (request.getStatus() != StatusEntrega.PENDENTE) {
            throw new RegraNegocioException(
                    "Uma nova entrega deve ser criada com status PENDENTE."
            );
        }

        Entrega entrega = new Entrega();

        entrega.setDescricao(request.getDescricao());
        entrega.setEnderecoDestino(request.getEnderecoDestino());
        entrega.setEnderecoOrigem(request.getEnderecoOrigem());
        entrega.setPesoCargaKg(request.getPesoCargaKg());
        entrega.setStatus(StatusEntrega.PENDENTE);

        return ResponseEntity.ok(EntregaResponse.from(entregaRepository.save(entrega)));
    }

    /**
     * Aloca veículo e motorista para uma entrega.
     *
     * A regra de negócio está no SistemaLogistica.
     * Se a alocação for válida, a entrega muda para EM_TRANSITO.
     */
    @PutMapping("/{id}/alocar")
    public ResponseEntity<EntregaResponse> alocar(
            @PathVariable Long id,
            @RequestParam Long veiculoId,
            @RequestParam Long motoristaId
    ) {
        return ResponseEntity.ok(
                EntregaResponse.from(servicoGerenciamento.alocarEntrega(
                        id,
                        veiculoId,
                        motoristaId
                ))
        );
    }

    /**
     * Finaliza uma entrega.
     *
     * A regra exige que ela esteja EM_TRANSITO
     * e possua veículo e motorista alocados.
     */
    @PutMapping("/{id}/finalizar")
    public ResponseEntity<EntregaResponse> finalizar(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(EntregaResponse.from(servicoGerenciamento.finalizarEntrega(id)));
    }

    /**
     * Atualiza o status através das transições permitidas.
     *
     * O controller não altera o status diretamente.
     * A decisão fica centralizada no serviço.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<EntregaResponse> atualizarStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request
    ) {
        return ResponseEntity.ok(
                EntregaResponse.from(servicoGerenciamento.atualizarStatus(
                        id,
                        request.getStatus()
                ))
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
            @PathVariable Long id
    ) {
        if (!entregaRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException(
                    "Entrega não encontrada: " + id
            );
        }

        if (entregaRepository.existsByIdAndStatus(id, StatusEntrega.EM_TRANSITO)) {
            throw new RegraNegocioException(
                    "Não é possível excluir uma entrega EM_TRANSITO. Cancele ou finalize antes."
            );
        }

        entregaRepository.deleteById(id);

        return ResponseEntity.noContent().build();
    }
}
