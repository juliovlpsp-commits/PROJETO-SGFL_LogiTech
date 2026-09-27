package com.logitech.sgfl.controller;

import com.logitech.sgfl.enums.StatusEntrega;
import com.logitech.sgfl.me.Entrega;
import com.logitech.sgfl.repository.EntregaRepository;
import com.logitech.sgfl.service.ServicoGerenciamento;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/entregas")
@CrossOrigin(origins = "http://localhost:5173")
public class EntregaController {

    private final ServicoGerenciamento servicoGerenciamento;
    private final EntregaRepository entregaRepository;

    public EntregaController(ServicoGerenciamento servicoGerenciamento, EntregaRepository entregaRepository) {
        this.servicoGerenciamento = servicoGerenciamento;
        this.entregaRepository = entregaRepository;
    }

    @GetMapping
    public List<Entrega> listarTodas() {
        return entregaRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<Entrega> criarEntrega(@RequestBody Entrega entrega) {
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
    public ResponseEntity<Entrega> atualizarStatus(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        Entrega entrega = entregaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entrega não encontrada"));
        entrega.setStatus(StatusEntrega.valueOf(body.get("status")));
        return ResponseEntity.ok(entregaRepository.save(entrega));
    }
}