package com.logitech.sgfl.controller;

import com.logitech.sgfl.me.Caminhao;
import com.logitech.sgfl.me.Furgao;
import com.logitech.sgfl.me.Veiculo;
import com.logitech.sgfl.repository.VeiculoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/veiculos")
public class VeiculoController {

    private final VeiculoRepository veiculoRepository;

    public VeiculoController(VeiculoRepository veiculoRepository) {
        this.veiculoRepository = veiculoRepository;
    }

    @GetMapping
    public List<Veiculo> listarTodos() {
        return veiculoRepository.findAll();
    }

    @PostMapping("/caminhao")
    public ResponseEntity<Caminhao> criarCaminhao(@RequestBody Caminhao caminhao) {
        return ResponseEntity.ok(veiculoRepository.save(caminhao));
    }

    @PostMapping("/furgao")
    public ResponseEntity<Furgao> criarFurgao(@RequestBody Furgao furgao) {
        return ResponseEntity.ok(veiculoRepository.save(furgao));
    }
}