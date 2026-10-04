package com.logitech.sgfl.controller;

import com.logitech.sgfl.exceptions.RegraNegocioException;
import com.logitech.sgfl.me.Entrega;
import com.logitech.sgfl.repository.EntregaRepository;
import com.logitech.sgfl.service.ClienteService;
import com.logitech.sgfl.service.ProdutoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api/importacao")
public class ImportacaoController {
    private final ClienteService clienteService;
    private final ProdutoService produtoService;
    private final EntregaRepository entregaRepository;

    public ImportacaoController(ClienteService clienteService, ProdutoService produtoService, EntregaRepository entregaRepository) {
        this.clienteService = clienteService;
        this.produtoService = produtoService;
        this.entregaRepository = entregaRepository;
    }

    @PostMapping("/clientes")
    public ResponseEntity<Map<String, Object>> clientes(@RequestParam("arquivo") MultipartFile arquivo) {
        int processados = 0;
        for (Map<String, String> row : lerCsv(arquivo)) {
            clienteService.criar(
                    obrigatorio(row, "nome"),
                    obrigatorio(row, "cpf"),
                    obrigatorio(row, "email"),
                    row.get("telefone"), row.get("cep"), row.get("logradouro"), row.get("numero"),
                    row.get("complemento"), row.get("bairro"), row.get("cidade"), row.get("uf")
            );
            processados++;
        }
        return ResponseEntity.ok(Map.of("processados", processados));
    }

    @PostMapping("/produtos")
    public ResponseEntity<Map<String, Object>> produtos(@RequestParam("arquivo") MultipartFile arquivo) {
        int processados = 0;
        for (Map<String, String> row : lerCsv(arquivo)) {
            produtoService.criar(
                    obrigatorio(row, "codigo"), obrigatorio(row, "nome"), row.get("descricao"),
                    new BigDecimal(obrigatorio(row, "preco")),
                    Integer.parseInt(row.getOrDefault("quantidade_estoque", "0")),
                    Boolean.parseBoolean(row.getOrDefault("ativo", "true"))
            );
            processados++;
        }
        return ResponseEntity.ok(Map.of("processados", processados));
    }

    @PostMapping("/entregas")
    public ResponseEntity<Map<String, Object>> entregas(@RequestParam("arquivo") MultipartFile arquivo) {
        int processados = 0;
        for (Map<String, String> row : lerCsv(arquivo)) {
            Entrega entrega = new Entrega(
                    row.getOrDefault("origem", ""),
                    obrigatorio(row, "destino"),
                    Double.parseDouble(obrigatorio(row, "peso_kg"))
            );
            entrega.setDescricao(obrigatorio(row, "descricao"));
            entrega.setCodigoRastreio("SGFL-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
            entregaRepository.save(entrega);
            processados++;
        }
        return ResponseEntity.ok(Map.of("processados", processados));
    }

    private List<Map<String, String>> lerCsv(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new RegraNegocioException("Envie um arquivo CSV.");
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(arquivo.getInputStream(), StandardCharsets.UTF_8))) {
            String cabecalho = reader.readLine();
            if (cabecalho == null) throw new RegraNegocioException("CSV vazio.");
            String delimiter = cabecalho.contains(";") ? ";" : ",";
            String[] headers = Arrays.stream(cabecalho.replace("\uFEFF", "").split(delimiter, -1))
                    .map(this::normalizarHeader).toArray(String[]::new);
            List<Map<String, String>> rows = new ArrayList<>();
            String linha;
            while ((linha = reader.readLine()) != null) {
                if (linha.isBlank()) continue;
                String[] values = linha.split(delimiter, -1);
                Map<String, String> row = new LinkedHashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    row.put(headers[i], i < values.length ? values[i].trim() : "");
                }
                rows.add(row);
            }
            return rows;
        } catch (Exception e) {
            if (e instanceof RegraNegocioException r) throw r;
            throw new RegraNegocioException("Não foi possível ler o CSV: " + e.getMessage());
        }
    }

    private String normalizarHeader(String value) {
        return value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    private String obrigatorio(Map<String, String> row, String key) {
        String value = row.get(key);
        if (value == null || value.isBlank()) {
            throw new RegraNegocioException("A coluna '" + key + "' é obrigatória.");
        }
        return value;
    }
}
