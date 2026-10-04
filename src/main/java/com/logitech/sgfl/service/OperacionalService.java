package com.logitech.sgfl.service;

import com.logitech.sgfl.dto.AlertaOperacionalResponse;
import com.logitech.sgfl.dto.CustoEntregaRequest;
import com.logitech.sgfl.dto.OperacionalKpiResponse;
import com.logitech.sgfl.exceptions.RecursoNaoEncontradoException;
import com.logitech.sgfl.me.CustoEntrega;
import com.logitech.sgfl.me.Entrega;
import com.logitech.sgfl.me.Motorista;
import com.logitech.sgfl.me.Veiculo;
import com.logitech.sgfl.repository.CustoEntregaRepository;
import com.logitech.sgfl.repository.EntregaRepository;
import com.logitech.sgfl.repository.ProdutoRepository;
import com.logitech.sgfl.repository.MotoristaRepository;
import com.logitech.sgfl.repository.VeiculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static com.logitech.sgfl.enums.StatusEntrega.*;

@Service
public class OperacionalService {
    private final EntregaRepository entregaRepository;
    private final MotoristaRepository motoristaRepository;
    private final VeiculoRepository veiculoRepository;
    private final CustoEntregaRepository custoRepository;
    private final ProdutoRepository produtoRepository;

    public OperacionalService(EntregaRepository entregaRepository,
                              MotoristaRepository motoristaRepository,
                              VeiculoRepository veiculoRepository,
                              CustoEntregaRepository custoRepository,
                              ProdutoRepository produtoRepository) {
        this.entregaRepository = entregaRepository;
        this.motoristaRepository = motoristaRepository;
        this.veiculoRepository = veiculoRepository;
        this.custoRepository = custoRepository;
        this.produtoRepository = produtoRepository;
    }

    @Transactional(readOnly = true)
    public List<AlertaOperacionalResponse> alertas() {
        LocalDateTime agora = LocalDateTime.now();
        List<AlertaOperacionalResponse> result = new ArrayList<>();
        for (Entrega e : entregaRepository.findAll()) {
            if (e.getStatus() == PENDENTE && e.getAgendadaInicio() != null && agora.isAfter(e.getAgendadaInicio())) {
                result.add(new AlertaOperacionalResponse(
                        "atrasada-" + e.getId(), "CRITICO",
                        "Entrega #" + e.getId() + " atrasada para alocação",
                        "A janela prevista para início já passou e a entrega continua pendente.", e.getId()));
            }
            if (e.getStatus() == EM_TRANSITO && e.getAgendadaFim() != null && agora.isAfter(e.getAgendadaFim())) {
                result.add(new AlertaOperacionalResponse(
                        "transito-" + e.getId(), "CRITICO",
                        "Entrega #" + e.getId() + " ultrapassou o prazo previsto",
                        "A entrega está em trânsito além do horário final planejado.", e.getId()));
            }
            if (e.getStatus() == EM_TRANSITO && (e.getMotorista() == null || e.getVeiculo() == null)) {
                result.add(new AlertaOperacionalResponse(
                        "recurso-" + e.getId(), "CRITICO",
                        "Entrega #" + e.getId() + " sem recurso operacional",
                        "Confira motorista e veículo antes de continuar o transporte.", e.getId()));
            }
        }

        produtoRepository.findAll().forEach(produto -> {
            if (produto.getEstoque() != null && produto.getEstoque().getQuantidadeDisponivel() <= 5 && produto.isAtivo()) {
                result.add(new AlertaOperacionalResponse(
                        "estoque-" + produto.getId(),
                        produto.getEstoque().getQuantidadeDisponivel() == 0 ? "CRITICO" : "ATENCAO",
                        "Estoque baixo: " + produto.getNome(),
                        "Restam " + produto.getEstoque().getQuantidadeDisponivel() + " unidade(s) disponíveis.",
                        null
                ));
            }
        });

        return result;
    }

    @Transactional(readOnly = true)
    public OperacionalKpiResponse kpis() {
        LocalDate hoje = LocalDate.now();
        LocalDateTime inicio = hoje.atStartOfDay();
        LocalDateTime fim = hoje.atTime(LocalTime.MAX);
        LocalDateTime agora = LocalDateTime.now();

        long entregasHoje = 0;
        long atrasadas = 0;
        long concluidasHoje = 0;
        double peso = 0;
        Set<Long> motoristasAtivos = new HashSet<>();
        Map<String, Long> atividade = new HashMap<>();
        BigDecimal faturamento = BigDecimal.ZERO;
        BigDecimal custos = BigDecimal.ZERO;

        for (Entrega e : entregaRepository.findAll()) {
            if (e.getAgendadaInicio() != null && !e.getAgendadaInicio().isBefore(inicio) && !e.getAgendadaInicio().isAfter(fim)) {
                entregasHoje++;
            }
            if (e.getStatus() == EM_TRANSITO || e.getStatus() == ENTREGUE) {
                if (e.getIniciadaEm() != null && !e.getIniciadaEm().isBefore(inicio) && !e.getIniciadaEm().isAfter(fim)) {
                    peso += e.getPesoCargaKg();
                    if (e.getMotorista() != null) {
                        motoristasAtivos.add(e.getMotorista().getId());
                        atividade.merge(e.getMotorista().getNome(), 1L, Long::sum);
                    }
                }
            }
            if (e.getEntregueEm() != null && !e.getEntregueEm().isBefore(inicio) && !e.getEntregueEm().isAfter(fim)) {
                concluidasHoje++;
                faturamento = faturamento.add(e.getValorFrete() == null ? BigDecimal.ZERO : e.getValorFrete());
            }
            if ((e.getStatus() == PENDENTE && e.getAgendadaInicio() != null && agora.isAfter(e.getAgendadaInicio()))
                    || (e.getStatus() == EM_TRANSITO && e.getAgendadaFim() != null && agora.isAfter(e.getAgendadaFim()))) {
                atrasadas++;
            }
            if (e.getEntregueEm() != null && !e.getEntregueEm().isBefore(inicio) && !e.getEntregueEm().isAfter(fim)) {
                for (CustoEntrega c : custoRepository.findByEntrega_IdOrderByCriadoEmAsc(e.getId())) {
                    custos = custos.add(c.getValor());
                }
            }
        }

        long disponiveis = veiculoRepository.findAll().stream()
                .filter(v -> !entregaRepository.existsByVeiculo_IdAndStatus(v.getId(), EM_TRANSITO))
                .count();

        String maisAtivo = atividade.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        return new OperacionalKpiResponse(
                entregasHoje,
                atrasadas,
                concluidasHoje,
                peso,
                motoristasAtivos.size(),
                disponiveis,
                faturamento,
                custos,
                faturamento.subtract(custos),
                maisAtivo
        );
    }

    @Transactional
    public CustoEntrega adicionarCusto(Long entregaId, CustoEntregaRequest request) {
        Entrega entrega = entregaRepository.findById(entregaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Entrega não encontrada: " + entregaId));
        return custoRepository.save(new CustoEntrega(entrega, request.tipo().trim().toUpperCase(),
                request.descricao(), request.valor()));
    }

    @Transactional(readOnly = true)
    public List<CustoEntrega> custos(Long entregaId) {
        if (!entregaRepository.existsById(entregaId)) {
            throw new RecursoNaoEncontradoException("Entrega não encontrada: " + entregaId);
        }
        return custoRepository.findByEntrega_IdOrderByCriadoEmAsc(entregaId);
    }
}
