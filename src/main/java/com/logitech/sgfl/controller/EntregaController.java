package com.logitech.sgfl.controller;

import com.logitech.sgfl.dto.EntregaRequest;
import com.logitech.sgfl.dto.StatusUpdateRequest;
import com.logitech.sgfl.enums.StatusEntrega;
import com.logitech.sgfl.exceptions.RecursoNaoEncontradoException;
import com.logitech.sgfl.me.Entrega;
import com.logitech.sgfl.repository.EntregaRepository;
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
@CrossOrigin(origins = "http://localhost:5173")
public class EntregaController {

    private static final int TAMANHO_PAGINA_PADRAO = 20;
    private static final int TAMANHO_PAGINA_MAXIMO = 100;

    private final ServicoGerenciamento servicoGerenciamento;
    private final EntregaRepository entregaRepository;

    public EntregaController(ServicoGerenciamento servicoGerenciamento, EntregaRepository entregaRepository) {
        this.servicoGerenciamento = servicoGerenciamento;
        this.entregaRepository = entregaRepository;
    }

    /**
     * Lista entregas de forma paginada (nunca devolve a tabela inteira de uma vez —
     * com milhões de registros isso derrubaria o banco e o navegador do cliente).
     * Ordenado por id para que a posição de cada linha na tela não mude sozinha
     * quando um registro é atualizado.
     */
    @GetMapping
    public Page<Entrega> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + TAMANHO_PAGINA_PADRAO) int size
    ) {
        int tamanhoSeguro = Math.min(Math.max(size, 1), TAMANHO_PAGINA_MAXIMO);
        Pageable pageable = PageRequest.of(Math.max(page, 0), tamanhoSeguro, Sort.by("id").ascending());
        return entregaRepository.findAll(pageable);
    }

    @PostMapping
    public ResponseEntity<Entrega> criarEntrega(@Valid @RequestBody EntregaRequest request) {
        Entrega entrega = new Entrega();
        entrega.setDescricao(request.getDescricao());
        entrega.setEnderecoDestino(request.getEnderecoDestino());
        entrega.setEnderecoOrigem(request.getEnderecoOrigem());
        entrega.setPesoCargaKg(request.getPesoCargaKg());
        entrega.setStatus(request.getStatus());
        return ResponseEntity.ok(entregaRepository.save(entrega));
    }

    @PutMapping("/{id}/alocar")
    public ResponseEntity<Entrega> alocar(@PathVariable Long id, @RequestParam Long veiculoId, @RequestParam Long motoristaId) {
        return ResponseEntity.ok(servicoGerenciamento.alocarEntrega(id, veiculoId, motoristaId));
    }

    @PutMapping("/{id}/finalizar")
    public ResponseEntity<Entrega> finalizar(@PathVariable Long id) {
        return ResponseEntity.ok(servicoGerenciamento.finalizarEntrega(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Entrega> atualizarStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        Entrega entrega = entregaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Entrega não encontrada: " + id));
        entrega.setStatus(request.getStatus());
        return ResponseEntity.ok(entregaRepository.save(entrega));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        if (!entregaRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Entrega não encontrada: " + id);
        }
        entregaRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
