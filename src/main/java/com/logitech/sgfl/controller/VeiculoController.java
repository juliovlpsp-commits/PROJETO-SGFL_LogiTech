package com.logitech.sgfl.controller;

import com.logitech.sgfl.exceptions.RecursoNaoEncontradoException;
import com.logitech.sgfl.me.Caminhao;
import com.logitech.sgfl.me.Furgao;
import com.logitech.sgfl.me.Veiculo;
import com.logitech.sgfl.repository.EntregaRepository;
import com.logitech.sgfl.repository.VeiculoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/veiculos")
public class VeiculoController {

    private final VeiculoRepository veiculoRepository;
    private final EntregaRepository entregaRepository;

    public VeiculoController(
            VeiculoRepository veiculoRepository,
            EntregaRepository entregaRepository
    ) {
        this.veiculoRepository = veiculoRepository;
        this.entregaRepository = entregaRepository;
    }

    @GetMapping
    public ResponseEntity<List<Veiculo>> listarTodos() {
        return ResponseEntity.ok(
                veiculoRepository.findAll()
        );
    }

    @PostMapping("/caminhao")
    public ResponseEntity<Caminhao> criarCaminhao(
            @RequestBody Caminhao caminhao
    ) {
        validarCaminhao(caminhao);

        String placa =
                normalizarPlaca(caminhao.getPlaca());

        if (veiculoRepository.existsByPlaca(placa)) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .build();
        }

        caminhao.setPlaca(placa);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        veiculoRepository.save(caminhao)
                );
    }

    @PostMapping("/furgao")
    public ResponseEntity<Furgao> criarFurgao(
            @RequestBody Furgao furgao
    ) {
        validarFurgao(furgao);

        String placa =
                normalizarPlaca(furgao.getPlaca());

        if (veiculoRepository.existsByPlaca(placa)) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .build();
        }

        furgao.setPlaca(placa);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        veiculoRepository.save(furgao)
                );
    }

    @PutMapping("/caminhao/{id}")
    public ResponseEntity<Caminhao> atualizarCaminhao(
            @PathVariable Long id,
            @RequestBody Caminhao dados
    ) {
        validarCaminhao(dados);

        Veiculo existente =
                veiculoRepository.findById(id)
                        .orElseThrow(() ->
                                new RecursoNaoEncontradoException(
                                        "Veículo não encontrado: " + id
                                )
                        );

        if (!(existente instanceof Caminhao caminhao)) {
            throw new IllegalArgumentException(
                    "O veículo informado não é um Caminhão."
            );
        }

        String placa =
                normalizarPlaca(dados.getPlaca());

        if (!placa.equals(
                caminhao.getPlaca()
        ) &&
                veiculoRepository.existsByPlaca(placa)) {

            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .build();
        }

        caminhao.setPlaca(placa);
        caminhao.setModelo(
                dados.getModelo().trim()
        );

        caminhao.setCapacidadeCargaKg(
                dados.getCapacidadeCargaKg()
        );

        caminhao.setQuantidadeEixos(
                dados.getQuantidadeEixos()
        );

        return ResponseEntity.ok(
                veiculoRepository.save(caminhao)
        );
    }

    @PutMapping("/furgao/{id}")
    public ResponseEntity<Furgao> atualizarFurgao(
            @PathVariable Long id,
            @RequestBody Furgao dados
    ) {
        validarFurgao(dados);

        Veiculo existente =
                veiculoRepository.findById(id)
                        .orElseThrow(() ->
                                new RecursoNaoEncontradoException(
                                        "Veículo não encontrado: " + id
                                )
                        );

        if (!(existente instanceof Furgao furgao)) {
            throw new IllegalArgumentException(
                    "O veículo informado não é um Furgão."
            );
        }

        String placa =
                normalizarPlaca(dados.getPlaca());

        if (!placa.equals(
                furgao.getPlaca()
        ) &&
                veiculoRepository.existsByPlaca(placa)) {

            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .build();
        }

        furgao.setPlaca(placa);
        furgao.setModelo(
                dados.getModelo().trim()
        );

        furgao.setCapacidadeCargaKg(
                dados.getCapacidadeCargaKg()
        );

        furgao.setVolumeM3(
                dados.getVolumeM3()
        );

        return ResponseEntity.ok(
                veiculoRepository.save(furgao)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirVeiculo(
            @PathVariable Long id
    ) {
        if (!veiculoRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException(
                    "Veículo não encontrado: " + id
            );
        }

        /*
         * Não permitimos excluir um veículo que esteja
         * relacionado a alguma entrega.
         */
        if (entregaRepository.existsByVeiculo_Id(id)) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .build();
        }

        veiculoRepository.deleteById(id);

        return ResponseEntity.noContent().build();
    }

    private void validarCaminhao(
            Caminhao caminhao
    ) {

        if (caminhao == null) {
            throw new IllegalArgumentException(
                    "Os dados do caminhão são obrigatórios."
            );
        }

        validarVeiculo(
                caminhao.getPlaca(),
                caminhao.getModelo(),
                caminhao.getCapacidadeCargaKg()
        );

        if (caminhao.getQuantidadeEixos() <= 0) {
            throw new IllegalArgumentException(
                    "A quantidade de eixos deve ser maior que zero."
            );
        }
    }

    private void validarFurgao(
            Furgao furgao
    ) {

        if (furgao == null) {
            throw new IllegalArgumentException(
                    "Os dados do furgão são obrigatórios."
            );
        }

        validarVeiculo(
                furgao.getPlaca(),
                furgao.getModelo(),
                furgao.getCapacidadeCargaKg()
        );

        if (furgao.getVolumeM3() <= 0) {
            throw new IllegalArgumentException(
                    "O volume do furgão deve ser maior que zero."
            );
        }
    }

    private void validarVeiculo(
            String placa,
            String modelo,
            double capacidadeCargaKg
    ) {

        if (placa == null ||
                placa.isBlank()) {

            throw new IllegalArgumentException(
                    "A placa é obrigatória."
            );
        }

        if (modelo == null ||
                modelo.isBlank()) {

            throw new IllegalArgumentException(
                    "O modelo é obrigatório."
            );
        }

        if (capacidadeCargaKg <= 0) {

            throw new IllegalArgumentException(
                    "A capacidade de carga deve ser maior que zero."
            );
        }

        String placaNormalizada =
                normalizarPlaca(placa);

        if (placaNormalizada.length() != 7) {
            throw new IllegalArgumentException(
                    "A placa deve possuir 7 caracteres."
            );
        }
    }

    private String normalizarPlaca(
            String placa
    ) {
        return placa
                .replaceAll(
                        "[^a-zA-Z0-9]",
                        ""
                )
                .toUpperCase();
    }
}