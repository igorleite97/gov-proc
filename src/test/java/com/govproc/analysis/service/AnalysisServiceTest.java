package com.govproc.analysis.service;

import com.govproc.analysis.domain.AnalysisDecision;
import com.govproc.analysis.domain.ProcessAnalysis;
import com.govproc.analysis.dto.ApproveAnalysisRequest;
import com.govproc.analysis.dto.RejectAnalysisRequest;
import com.govproc.analysis.dto.StartAnalysisRequest;
import com.govproc.analysis.repository.AnalysisRepository;
import com.govproc.audit.service.AuditService;
import com.govproc.process.domain.PriorityLevel;
import com.govproc.process.domain.ProcurementProcess;
import com.govproc.process.domain.ProcessStatus;
import com.govproc.process.domain.RiskLevel;
import com.govproc.process.repository.ProcessRepository;
import com.govproc.shared.exception.BusinessException;
import com.govproc.timeline.service.TimelineService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest {

    @Mock ProcessRepository processRepository;
    @Mock AnalysisRepository analysisRepository;
    @Mock AuditService auditService;
    @Mock TimelineService timelineService;

    @InjectMocks
    AnalysisService analysisService;

    private static final UUID PROCESS_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID    = UUID.fromString("22222222-2222-2222-2222-222222222222");

    /**
     * Cria um ProcurementProcess real no estado CAPTURED.
     * Usado nos testes para exercitar a maquina de estados de verdade.
     */
    private ProcurementProcess capturedProcess() {
        return new ProcurementProcess(
                "001/2024", "654321", "Ministerio X", "Compras.gov.br",
                "Aquisicao de equipamentos de TI", null, null, null,
                PriorityLevel.HIGH, RiskLevel.MEDIUM, USER_ID);
    }

    @Test
    void startAnalysis_deveTransicionarParaUnderAnalysis_quandoProcessoEstaCaptured() {
        var request = new StartAnalysisRequest(true, false, null, "30 dias", null);
        ProcurementProcess process = capturedProcess();

        when(analysisRepository.existsByProcessId(PROCESS_ID)).thenReturn(false);
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(process));
        when(processRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(analysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        analysisService.startAnalysis(PROCESS_ID, request, USER_ID);

        assertThat(process.getStatus()).isEqualTo(ProcessStatus.UNDER_ANALYSIS);

        verify(auditService).record(
                eq("ProcurementProcess"), eq(PROCESS_ID), eq("status"),
                eq("CAPTURED"), eq("UNDER_ANALYSIS"), eq(USER_ID));
    }

    @Test
    void startAnalysis_deveLancarBusiness_quandoAnaliseJaExiste() {
        when(analysisRepository.existsByProcessId(PROCESS_ID)).thenReturn(true);

        assertThatThrownBy(() ->
                analysisService.startAnalysis(PROCESS_ID, new StartAnalysisRequest(null, null, null, null, null), USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("já iniciada");
    }

    @Test
    void startAnalysis_deveLancarBusiness_quandoProcessoNaoEstaCaptured() {
        // Processo ja esta em UNDER_ANALYSIS — nao pode iniciar analise novamente
        ProcurementProcess process = capturedProcess();
        process.startAnalysis(); // muda para UNDER_ANALYSIS

        when(analysisRepository.existsByProcessId(PROCESS_ID)).thenReturn(false);
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(process));

        assertThatThrownBy(() ->
                analysisService.startAnalysis(PROCESS_ID, new StartAnalysisRequest(null, null, null, null, null), USER_ID))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void approveAnalysis_deveTransicionarParaApproved_quandoAnaliseEhPendente() {
        ProcurementProcess process = capturedProcess();
        process.startAnalysis(); // UNDER_ANALYSIS

        ProcessAnalysis analysis = new ProcessAnalysis(PROCESS_ID, null, null, null, null, null, USER_ID);
        var request = new ApproveAnalysisRequest("Processo viável.");

        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(process));
        when(analysisRepository.findByProcessId(PROCESS_ID)).thenReturn(Optional.of(analysis));
        when(processRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(analysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        analysisService.approveAnalysis(PROCESS_ID, request, USER_ID);

        assertThat(process.getStatus()).isEqualTo(ProcessStatus.ANALYSIS_APPROVED);
        assertThat(analysis.getDecision()).isEqualTo(AnalysisDecision.APPROVED);
        verify(auditService).record(any(), any(), any(), eq("UNDER_ANALYSIS"), eq("ANALYSIS_APPROVED"), any());
    }

    @Test
    void rejectAnalysis_deveTransicionarParaRejected_quandoMotivoFornecido() {
        ProcurementProcess process = capturedProcess();
        process.startAnalysis(); // UNDER_ANALYSIS

        ProcessAnalysis analysis = new ProcessAnalysis(PROCESS_ID, null, null, null, null, null, USER_ID);
        var request = new RejectAnalysisRequest("Risco técnico elevado");

        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(process));
        when(analysisRepository.findByProcessId(PROCESS_ID)).thenReturn(Optional.of(analysis));
        when(processRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(analysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        analysisService.rejectAnalysis(PROCESS_ID, request, USER_ID);

        assertThat(process.getStatus()).isEqualTo(ProcessStatus.ANALYSIS_REJECTED);
        assertThat(analysis.getDecision()).isEqualTo(AnalysisDecision.REJECTED);
        assertThat(analysis.getRejectionReason()).isEqualTo("Risco técnico elevado");
    }

    @Test
    void rejectAnalysis_deveLancarBusiness_quandoMotivoEhVazio() {
        // O dominio (ProcurementProcess.rejectAnalysis) valida o motivo antes da analise
        ProcurementProcess process = capturedProcess();
        process.startAnalysis(); // UNDER_ANALYSIS

        var request = new RejectAnalysisRequest("  "); // em branco

        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(process));
        // analysisRepository NAO stubado: excecao ocorre antes de loadAnalysis()

        assertThatThrownBy(() -> analysisService.rejectAnalysis(PROCESS_ID, request, USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Motivo");
    }
}
