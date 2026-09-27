package com.logitech.sgfl.service;

import com.logitech.sgfl.enums.StatusEntrega;
import com.logitech.sgfl.enums.TipoCNH;
import com.logitech.sgfl.exceptions.VeiculoIncompativelException;
import com.logitech.sgfl.me.Caminhao;
import com.logitech.sgfl.me.Entrega;
import com.logitech.sgfl.me.Furgao;
import com.logitech.sgfl.me.Motorista;
import com.logitech.sgfl.me.Veiculo;
import com.logitech.sgfl.repository.EntregaRepository;
import com.logitech.sgfl.repository.MotoristaRepository;
import com.logitech.sgfl.repository.VeiculoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SistemaLogistica implements ServicoGerenciamento {

    private final EntregaRepository entregaRepository;
    private final VeiculoRepository veiculoRepository;
    private final MotoristaRepository motoristaRepository;

    public SistemaLogistica(EntregaRepository entregaRepository,
                            VeiculoRepository veiculoRepository,
                            MotoristaRepository motoristaRepository) {
        this.entregaRepository = entregaRepository;
        this.veiculoRepository = veiculoRepository;
        this.motoristaRepository = motoristaRepository;
    }

    @Override
    public Entrega alocarEntrega(Long entregaId, Long veiculoId, Long motoristaId) {
        Entrega entrega = entregaRepository.findById(entregaId)
                .orElseThrow(() -> new RuntimeException("Entrega não encontrada"));

        Veiculo veiculo = veiculoRepository.findById(veiculoId)
                .orElseThrow(() -> new RuntimeException("Veículo não encontrado"));

        Motorista motorista = motoristaRepository.findById(motoristaId)
                .orElseThrow(() -> new RuntimeException("Motorista não encontrado"));

        // 1. Validação de Capacidade de Carga
        if (entrega.getPesoCargaKg() > veiculo.getCapacidadeCargaKg()) {
            throw new VeiculoIncompativelException("O peso da carga (" + entrega.getPesoCargaKg() +
                    "kg) excede a capacidade do veículo (" + veiculo.getCapacidadeCargaKg() + "kg).");
        }

        TipoCNH cnh = motorista.getTipoCNH();

        boolean isCaminhao = veiculo instanceof Caminhao || veiculo.getClass().getSimpleName().contains("Caminhao");
        boolean isFurgao = veiculo instanceof Furgao || veiculo.getClass().getSimpleName().contains("Furgao");

        if (isCaminhao && !cnh.podeDirigirCaminhao()) {
            throw new VeiculoIncompativelException("Motorista com CNH tipo '" + cnh +
                    "' não possui permissão para dirigir Caminhão (Exige D ou E).");
        }

        if (isFurgao && !cnh.podeDirigirFurgao()) {
            throw new VeiculoIncompativelException("Motorista com CNH tipo '" + cnh +
                    "' não possui permissão para dirigir Furgão.");
        }

        entrega.setVeiculo(veiculo);
        entrega.setMotorista(motorista);
        entrega.setStatus(StatusEntrega.EM_TRANSITO);

        return entregaRepository.save(entrega);
    }

    @Override
    public Entrega finalizarEntrega(Long entregaId) {
        Entrega entrega = entregaRepository.findById(entregaId)
                .orElseThrow(() -> new RuntimeException("Entrega não encontrada"));

        entrega.setStatus(StatusEntrega.ENTREGUE);
        return entregaRepository.save(entrega);
    }

    @Override
    public List<Entrega> listarTodas() {
        return List.of();
    }
}