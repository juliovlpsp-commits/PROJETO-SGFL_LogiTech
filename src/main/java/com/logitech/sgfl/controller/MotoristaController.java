package com.logitech.sgfl.controller;

import com.logitech.sgfl.me.Motorista;
import com.logitech.sgfl.repository.MotoristaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/motoristas")
public class MotoristaController {

    private final MotoristaRepository motoristaRepository;

    public MotoristaController(MotoristaRepository motoristaRepository) {
        this.motoristaRepository = motoristaRepository;
    }

    @GetMapping
    public List<Motorista> listarTodos() {
        return motoristaRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<Motorista> criarMotorista(@RequestBody Motorista motorista) {
        return ResponseEntity.ok(motoristaRepository.save(motorista));
    }
}