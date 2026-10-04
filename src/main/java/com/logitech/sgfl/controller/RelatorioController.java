package com.logitech.sgfl.controller;

import com.logitech.sgfl.enums.StatusEntrega;
import com.logitech.sgfl.me.Entrega;
import com.logitech.sgfl.repository.EntregaRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.logitech.sgfl.service.PdfRelatorioService;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/relatorios")
public class RelatorioController {
    private final EntregaRepository entregaRepository;
    private final PdfRelatorioService pdfService;

    public RelatorioController(EntregaRepository entregaRepository, PdfRelatorioService pdfService) {
        this.entregaRepository = entregaRepository;
        this.pdfService = pdfService;
    }

    @GetMapping(value = "/entregas.csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportarEntregas(
            @RequestParam(required = false) StatusEntrega status,
            @RequestParam(required = false) String q
    ) {
        List<Entrega> entregas = entregaRepository.findAll(
                com.logitech.sgfl.repository.EntregaSpecifications.filtrar(status, q),
                org.springframework.data.domain.PageRequest.of(0, 10000, org.springframework.data.domain.Sort.by("id"))
        ).getContent();

        StringBuilder csv = new StringBuilder();
        csv.append("id;codigo_rastreio;descricao;origem;destino;peso_kg;status;motorista;veiculo;agendada_inicio;agendada_fim;valor_frete\n");
        for (Entrega e : entregas) {
            csv.append(campo(e.getId())).append(';')
                    .append(campo(e.getCodigoRastreio())).append(';')
                    .append(campo(e.getDescricao())).append(';')
                    .append(campo(e.getEnderecoOrigem())).append(';')
                    .append(campo(e.getEnderecoDestino())).append(';')
                    .append(campo(e.getPesoCargaKg())).append(';')
                    .append(campo(e.getStatus())).append(';')
                    .append(campo(e.getMotorista() == null ? null : e.getMotorista().getNome())).append(';')
                    .append(campo(e.getVeiculo() == null ? null : e.getVeiculo().getPlaca())).append(';')
                    .append(campo(e.getAgendadaInicio())).append(';')
                    .append(campo(e.getAgendadaFim())).append(';')
                    .append(campo(e.getValorFrete())).append('\n');
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sgfl-entregas.csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping(value = "/entregas.pdf", produces = "application/pdf")
    public ResponseEntity<byte[]> exportarEntregasPdf(
            @RequestParam(required = false) StatusEntrega status,
            @RequestParam(required = false) String q
    ) {
        List<Entrega> entregas = entregaRepository.findAll(
                com.logitech.sgfl.repository.EntregaSpecifications.filtrar(status, q),
                org.springframework.data.domain.PageRequest.of(0, 10000, org.springframework.data.domain.Sort.by("id"))
        ).getContent();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sgfl-entregas.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfService.gerarEntregas(entregas));
    }

    private String campo(Object value) {
        if (value == null) return "";
        String text = String.valueOf(value).replace("\"", "\"\"").replace("\r", " ").replace("\n", " ");
        return "\"" + text + "\"";
    }
}
