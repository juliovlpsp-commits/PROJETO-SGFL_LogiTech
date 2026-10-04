package com.logitech.sgfl.service;

import com.logitech.sgfl.dto.RotaEstimativaResponse;
import com.logitech.sgfl.exceptions.RecursoNaoEncontradoException;
import com.logitech.sgfl.exceptions.RegraNegocioException;
import com.logitech.sgfl.me.Entrega;
import com.logitech.sgfl.repository.EntregaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class RotaService {
    private final EntregaRepository entregaRepository;
    private final int velocidadeMediaKmH;

    public RotaService(
            EntregaRepository entregaRepository,
            @Value("${SGFL_ETA_AVG_SPEED_KMH:50}") int velocidadeMediaKmH
    ) {
        this.entregaRepository = entregaRepository;
        this.velocidadeMediaKmH = Math.max(1, velocidadeMediaKmH);
    }

    @Transactional(readOnly = true)
    public RotaEstimativaResponse estimar(Long entregaId) {
        Entrega e = entregaRepository.findById(entregaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Entrega não encontrada: " + entregaId));

        if (e.getLatitudeOrigem() == null || e.getLongitudeOrigem() == null
                || e.getLatitudeDestino() == null || e.getLongitudeDestino() == null) {
            return new RotaEstimativaResponse(entregaId, null, velocidadeMediaKmH, null, null, false);
        }

        validarCoordenada(e.getLatitudeOrigem(), e.getLongitudeOrigem());
        validarCoordenada(e.getLatitudeDestino(), e.getLongitudeDestino());

        double distancia = haversine(
                e.getLatitudeOrigem(), e.getLongitudeOrigem(),
                e.getLatitudeDestino(), e.getLongitudeDestino()
        );
        int duracao = (int) Math.max(1, Math.ceil((distancia / velocidadeMediaKmH) * 60.0));
        LocalDateTime inicio = e.getIniciadaEm() != null ? e.getIniciadaEm() : LocalDateTime.now();
        LocalDateTime chegada = inicio.plusMinutes(duracao);

        return new RotaEstimativaResponse(entregaId, Math.round(distancia * 100.0) / 100.0,
                velocidadeMediaKmH, duracao, chegada, true);
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
