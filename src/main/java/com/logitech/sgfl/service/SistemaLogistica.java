package com.logitech.sgfl.service;

import com.logitech.sgfl.enums.StatusEntrega;
import com.logitech.sgfl.enums.TipoCNH;
import com.logitech.sgfl.exceptions.RecursoNaoEncontradoException;
import com.logitech.sgfl.exceptions.RegraNegocioException;
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

    public SistemaLogistica(
            EntregaRepository entregaRepository,
            VeiculoRepository veiculoRepository,
            MotoristaRepository motoristaRepository
    ) {
        this.entregaRepository = entregaRepository;
        this.veiculoRepository = veiculoRepository;
        this.motoristaRepository = motoristaRepository;
    }

    @Override
    public Entrega alocarEntrega(Long entregaId, Long veiculoId, Long motoristaId) {

        Entrega entrega = buscarEntrega(entregaId);

        /*
         * Uma entrega só pode ser alocada enquanto estiver PENDENTE.
         */
        if (entrega.getStatus() != StatusEntrega.PENDENTE) {
            throw new RegraNegocioException(
                    "A entrega só pode ser alocada quando estiver PENDENTE. " +
                            "Status atual: " + entrega.getStatus()
            );
        }

        Veiculo veiculo = veiculoRepository.findById(veiculoId)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Veículo não encontrado: " + veiculoId
                        )
                );

        Motorista motorista = motoristaRepository.findById(motoristaId)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Motorista não encontrado: " + motoristaId
                        )
                );

        /*
         * Validação da capacidade de carga.
         */
        if (entrega.getPesoCargaKg() > veiculo.getCapacidadeCargaKg()) {
            throw new VeiculoIncompativelException(
                    "O peso da carga (" +
                            entrega.getPesoCargaKg() +
                            "kg) excede a capacidade do veículo (" +
                            veiculo.getCapacidadeCargaKg() +
                            "kg)."
            );
        }

        /*
         * Validação da CNH do motorista.
         */
        TipoCNH cnh = motorista.getTipoCNH();

        boolean isCaminhao =
                veiculo instanceof Caminhao ||
                        veiculo.getClass().getSimpleName().contains("Caminhao");

        boolean isFurgao =
                veiculo instanceof Furgao ||
                        veiculo.getClass().getSimpleName().contains("Furgao");

        if (isCaminhao && !cnh.podeDirigirCaminhao()) {
            throw new VeiculoIncompativelException(
                    "Motorista com CNH tipo '" + cnh +
                            "' não possui permissão para dirigir Caminhão (Exige D ou E)."
            );
        }

        if (isFurgao && !cnh.podeDirigirFurgao()) {
            throw new VeiculoIncompativelException(
                    "Motorista com CNH tipo '" + cnh +
                            "' não possui permissão para dirigir Furgão."
            );
        }

        /*
         * A alocação define o veículo, motorista e muda
         * automaticamente a entrega para EM_TRANSITO.
         */
        entrega.setVeiculo(veiculo);
        entrega.setMotorista(motorista);
        entrega.setStatus(StatusEntrega.EM_TRANSITO);

        return entregaRepository.save(entrega);
    }

    @Override
    public Entrega finalizarEntrega(Long entregaId) {

        Entrega entrega = buscarEntrega(entregaId);

        /*
         * A finalização só pode ocorrer quando a entrega
         * estiver efetivamente em trânsito.
         */
        if (entrega.getStatus() != StatusEntrega.EM_TRANSITO) {
            throw new RegraNegocioException(
                    "A entrega só pode ser finalizada quando estiver " +
                            "EM_TRANSITO. Status atual: " + entrega.getStatus()
            );
        }

        /*
         * Uma entrega em trânsito precisa possuir veículo
         * e motorista associados.
         */
        if (entrega.getVeiculo() == null) {
            throw new RegraNegocioException(
                    "Não é possível finalizar a entrega porque nenhum veículo foi alocado."
            );
        }

        if (entrega.getMotorista() == null) {
            throw new RegraNegocioException(
                    "Não é possível finalizar a entrega porque nenhum motorista foi alocado."
            );
        }

        entrega.setStatus(StatusEntrega.ENTREGUE);

        return entregaRepository.save(entrega);
    }

    @Override
    public Entrega atualizarStatus(Long entregaId, StatusEntrega novoStatus) {

        Entrega entrega = buscarEntrega(entregaId);

        if (novoStatus == null) {
            throw new RegraNegocioException(
                    "O novo status da entrega é obrigatório."
            );
        }

        StatusEntrega statusAtual = entrega.getStatus();

        /*
         * Se o status já é o mesmo, não há alteração.
         */
        if (statusAtual == novoStatus) {
            return entrega;
        }

        /*
         * O PATCH de status não pode substituir os fluxos
         * de alocação e finalização.
         *
         * EM_TRANSITO deve ser consequência da alocação.
         * ENTREGUE deve ser consequência da finalização.
         *
         * O PATCH permite apenas cancelamento.
         */
        if (!transicaoPermitida(statusAtual, novoStatus)) {
            throw new RegraNegocioException(
                    "Transição de status inválida: " +
                            statusAtual + " → " + novoStatus
            );
        }

        entrega.setStatus(novoStatus);

        return entregaRepository.save(entrega);
    }

    /**
     * Transições permitidas diretamente pelo PATCH /status.
     *
     * PENDENTE -> CANCELADA
     * EM_TRANSITO -> CANCELADA
     */
    private boolean transicaoPermitida(
            StatusEntrega statusAtual,
            StatusEntrega novoStatus
    ) {
        return
                (statusAtual == StatusEntrega.PENDENTE &&
                        novoStatus == StatusEntrega.CANCELADA)

                        ||

                        (statusAtual == StatusEntrega.EM_TRANSITO &&
                                novoStatus == StatusEntrega.CANCELADA);
    }

    private Entrega buscarEntrega(Long entregaId) {
        return entregaRepository.findById(entregaId)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Entrega não encontrada: " + entregaId
                        )
                );
    }

    @Override
    public List<Entrega> listarTodas() {
        return entregaRepository.findAll();
    }
}