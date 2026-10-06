package com.logitech.sgfl.service;

import com.logitech.sgfl.dto.EntregaEtaResponse;
import com.logitech.sgfl.dto.PontoRota;
import com.logitech.sgfl.dto.RotaEstimativaResponse;
import com.logitech.sgfl.exceptions.RecursoNaoEncontradoException;
import com.logitech.sgfl.exceptions.RegraNegocioException;
import com.logitech.sgfl.me.Entrega;
import com.logitech.sgfl.me.EntregaEta;
import com.logitech.sgfl.repository.EntregaEtaRepository;
import com.logitech.sgfl.repository.EntregaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Estimativa de rota e ETA de uma entrega.
 *
 * A ordem é: provedor de rota real (OSRM ou equivalente, via
 * {@link RoteamentoService}) e, se não houver provedor ou ele falhar,
 * cálculo em linha reta por Haversine com a velocidade média configurada.
 * A resposta traz a fonte usada para o usuário saber em que confiar.
 *
 * Cada estimativa também alimenta o histórico da entrega
 * ({@code entrega_eta}), limitado a um registro por 5 minutos.
 */
@Service
public class RotaService {

    private static final Logger log =
            LoggerFactory.getLogger(RotaService.class);

    private static final int JANELA_HISTORICO_MINUTOS = 5;

    private final EntregaRepository entregaRepository;
    private final EntregaEtaRepository etaRepository;
    private final RoteamentoService roteamento;
    private final int velocidadeMediaKmH;

    public RotaService(
            EntregaRepository entregaRepository,
            EntregaEtaRepository etaRepository,
            RoteamentoService roteamento,
            @Value("${SGFL_ETA_AVG_SPEED_KMH:50}") int velocidadeMediaKmH
    ) {
        this.entregaRepository = entregaRepository;
        this.etaRepository = etaRepository;
        this.roteamento = roteamento;
        this.velocidadeMediaKmH = Math.max(1, velocidadeMediaKmH);
    }

    @Transactional
    public RotaEstimativaResponse estimar(Long entregaId) {
        Entrega e = entregaRepository.findById(entregaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Entrega não encontrada: " + entregaId));

        if (e.getLatitudeOrigem() == null || e.getLongitudeOrigem() == null
                || e.getLatitudeDestino() == null || e.getLongitudeDestino() == null) {
            return new RotaEstimativaResponse(
                    entregaId, null, velocidadeMediaKmH, null, null, false, null, null);
        }

        validarCoordenada(e.getLatitudeOrigem(), e.getLongitudeOrigem());
        validarCoordenada(e.getLatitudeDestino(), e.getLongitudeDestino());

        RotaCalculada rota = roteamento.rota(
                        e.getLatitudeOrigem(), e.getLongitudeOrigem(),
                        e.getLatitudeDestino(), e.getLongitudeDestino()
                )
                .orElseGet(() -> linhaReta(
                        e.getLatitudeOrigem(), e.getLongitudeOrigem(),
                        e.getLatitudeDestino(), e.getLongitudeDestino()
                ));

        int duracao = rota.duracaoMinutos();
        LocalDateTime inicio = e.getIniciadaEm() != null ? e.getIniciadaEm() : LocalDateTime.now();
        LocalDateTime chegada = inicio.plusMinutes(duracao);

        registrarHistorico(e, rota, chegada);

        return new RotaEstimativaResponse(
                entregaId,
                arredondar(rota.distanciaKm()),
                velocidadeUtilizada(rota),
                duracao,
                chegada,
                true,
                rota.fonte(),
                rota.geometria()
        );
    }

    /**
     * Histórico de previsões já calculadas para a entrega (mais recente
     * primeiro).
     */
    @Transactional(readOnly = true)
    public List<EntregaEtaResponse> historico(Long entregaId) {

        if (!entregaRepository.existsById(entregaId)) {
            throw new RecursoNaoEncontradoException(
                    "Entrega não encontrada: " + entregaId
            );
        }

        return etaRepository
                .findByEntregaIdOrderByCriadoEmDesc(entregaId)
                .stream()
                .map(EntregaEtaResponse::from)
                .toList();
    }

    private RotaCalculada linhaReta(double latOrigem, double lonOrigem,
                                    double latDestino, double lonDestino) {
        double distancia = haversine(latOrigem, lonOrigem, latDestino, lonDestino);
        int duracao = (int) Math.max(1, Math.ceil((distancia / velocidadeMediaKmH) * 60.0));
        return new RotaCalculada(
                distancia,
                duracao,
                RotaCalculada.FONTE_HAVERSINE,
                List.of(
                        new PontoRota(latOrigem, lonOrigem),
                        new PontoRota(latDestino, lonDestino)
                )
        );
    }

    private int velocidadeUtilizada(RotaCalculada rota) {
        if (RotaCalculada.FONTE_HAVERSINE.equals(rota.fonte())
                || rota.duracaoMinutos() <= 0) {
            return velocidadeMediaKmH;
        }
        return (int) Math.max(
                1,
                Math.round(rota.distanciaKm() * 60.0 / rota.duracaoMinutos())
        );
    }

    private void registrarHistorico(Entrega entrega, RotaCalculada rota, LocalDateTime chegada) {

        if (entrega.getId() == null) {
            return;
        }

        try {
            LocalDateTime agora = LocalDateTime.now();

            Optional<EntregaEta> ultimo =
                    etaRepository.findTopByEntregaIdOrderByCriadoEmDesc(entrega.getId());

            if (ultimo.isPresent()
                    && ultimo.get().getCriadoEm() != null
                    && ultimo.get().getCriadoEm().isAfter(agora.minusMinutes(JANELA_HISTORICO_MINUTOS))) {
                return;
            }

            etaRepository.save(new EntregaEta(
                    entrega,
                    arredondar(rota.distanciaKm()),
                    rota.duracaoMinutos(),
                    chegada,
                    rota.fonte(),
                    agora
            ));
        } catch (RuntimeException ex) {
            log.warn(
                    "Não foi possível registrar o histórico de ETA da entrega {}: {}",
                    entrega.getId(),
                    ex.getMessage()
            );
        }
    }

    private double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    private void validarCoordenada(double lat, double lon) {
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            throw new RegraNegocioException("Coordenadas geográficas inválidas.");
        }
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final double raioTerraKm = 6371.0088;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * raioTerraKm * Math.asin(Math.sqrt(a));
    }
}
