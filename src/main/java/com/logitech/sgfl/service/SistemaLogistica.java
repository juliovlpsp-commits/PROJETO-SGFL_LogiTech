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
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * Aloca veículo e motorista para uma entrega PENDENTE.
     *
     * Ao concluir a alocação:
     *
     * PENDENTE -> EM_TRANSITO
     */
    @Override
    @Transactional
    public Entrega alocarEntrega(
            Long entregaId,
            Long veiculoId,
            Long motoristaId
    ) {

        Entrega entrega =
                buscarEntrega(entregaId);

        if (entrega.getStatus() != StatusEntrega.PENDENTE) {

            throw new RegraNegocioException(
                    "A entrega só pode ser alocada quando estiver PENDENTE. " +
                            "Status atual: " +
                            entrega.getStatus()
            );
        }

        Veiculo veiculo =
                veiculoRepository.findById(veiculoId)
                        .orElseThrow(() ->
                                new RecursoNaoEncontradoException(
                                        "Veículo não encontrado: " +
                                                veiculoId
                                )
                        );

        Motorista motorista =
                motoristaRepository.findById(motoristaId)
                        .orElseThrow(() ->
                                new RecursoNaoEncontradoException(
                                        "Motorista não encontrado: " +
                                                motoristaId
                                )
                        );

        /*
         * Um veículo não pode estar em duas entregas
         * simultaneamente.
         */
        if (entregaRepository
                .existsByVeiculo_IdAndStatus(
                        veiculoId,
                        StatusEntrega.EM_TRANSITO
                )) {

            throw new RegraNegocioException(
                    "O veículo já está alocado em outra entrega EM_TRANSITO."
            );
        }

        /*
         * Um motorista não pode estar em duas entregas
         * simultaneamente.
         */
        if (entregaRepository
                .existsByMotorista_IdAndStatus(
                        motoristaId,
                        StatusEntrega.EM_TRANSITO
                )) {

            throw new RegraNegocioException(
                    "O motorista já está alocado em outra entrega EM_TRANSITO."
            );
        }

        /*
         * A carga deve caber no veículo.
         */
        if (entrega.getPesoCargaKg() >
                veiculo.getCapacidadeCargaKg()) {

            throw new VeiculoIncompativelException(
                    "O peso da carga (" +
                            entrega.getPesoCargaKg() +
                            " kg) excede a capacidade do veículo (" +
                            veiculo.getCapacidadeCargaKg() +
                            " kg)."
            );
        }

        /*
         * Validação de CNH.
         */
        TipoCNH cnh =
                motorista.getTipoCNH();

        if (cnh == null) {

            throw new RegraNegocioException(
                    "O motorista não possui uma categoria de CNH informada."
            );
        }

        boolean isCaminhao = veiculo instanceof Caminhao;
        boolean isFurgao = veiculo instanceof Furgao;

        if (isCaminhao &&
                !cnh.podeDirigirCaminhao()) {

            throw new VeiculoIncompativelException(
                    "Motorista com CNH tipo '" +
                            cnh +
                            "' não possui permissão para dirigir Caminhão."
            );
        }

        if (isFurgao &&
                !cnh.podeDirigirFurgao()) {

            throw new VeiculoIncompativelException(
                    "Motorista com CNH tipo '" +
                            cnh +
                            "' não possui permissão para dirigir Furgão."
            );
        }

        /*
         * Associação dos recursos.
         */
        entrega.setVeiculo(veiculo);
        entrega.setMotorista(motorista);

        /*
         * A alocação coloca automaticamente
         * a entrega em trânsito.
         */
        entrega.setStatus(
                StatusEntrega.EM_TRANSITO
        );

        return entregaRepository.save(
                entrega
        );
    }

    /**
     * Finaliza uma entrega em trânsito.
     *
     * EM_TRANSITO -> ENTREGUE
     */
    @Override
    @Transactional
    public Entrega finalizarEntrega(
            Long entregaId
    ) {

        Entrega entrega =
                buscarEntrega(entregaId);

        if (entrega.getStatus() !=
                StatusEntrega.EM_TRANSITO) {

            throw new RegraNegocioException(
                    "A entrega só pode ser finalizada quando estiver EM_TRANSITO. " +
                            "Status atual: " +
                            entrega.getStatus()
            );
        }

        /*
         * Uma entrega em trânsito precisa possuir
         * os dois recursos.
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

        /*
         * Finalização oficial.
         */
        entrega.setStatus(
                StatusEntrega.ENTREGUE
        );

        return entregaRepository.save(
                entrega
        );
    }

    /**
     * Altera status somente através das transições
     * permitidas pelo PATCH /status.
     *
     * PENDENTE    -> CANCELADA
     * EM_TRANSITO -> CANCELADA
     *
     * EM_TRANSITO -> ENTREGUE não é permitido aqui.
     * Deve passar por finalizarEntrega().
     *
     * PENDENTE -> EM_TRANSITO não é permitido aqui.
     * Deve passar por alocarEntrega().
     */
    @Override
    @Transactional
    public Entrega atualizarStatus(
            Long entregaId,
            StatusEntrega novoStatus
    ) {

        Entrega entrega =
                buscarEntrega(entregaId);

        if (novoStatus == null) {

            throw new RegraNegocioException(
                    "O novo status da entrega é obrigatório."
            );
        }

        StatusEntrega statusAtual =
                entrega.getStatus();

        /*
         * Não há nada para alterar.
         */
        if (statusAtual == novoStatus) {
            return entrega;
        }

        boolean cancelamentoPermitido =
                (
                        statusAtual ==
                                StatusEntrega.PENDENTE
                                ||
                                statusAtual ==
                                        StatusEntrega.EM_TRANSITO
                )
                        &&
                        novoStatus ==
                                StatusEntrega.CANCELADA;

        if (!cancelamentoPermitido) {

            throw new RegraNegocioException(
                    "Transição de status inválida: " +
                            statusAtual +
                            " → " +
                            novoStatus
            );
        }

        entrega.setStatus(
                novoStatus
        );

        return entregaRepository.save(
                entrega
        );
    }

    /**
     * Procura a entrega ou lança 404 através
     * do GlobalExceptionHandler.
     */
    private Entrega buscarEntrega(
            Long entregaId
    ) {

        return entregaRepository.findById(
                        entregaId
                )
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Entrega não encontrada: " +
                                        entregaId
                        )
                );
    }

    @Override
    public List<Entrega> listarTodas() {
        return entregaRepository.findAll();
    }
}