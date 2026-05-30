package com.govproc.dispute.service;

import com.govproc.audit.service.AuditService;
import com.govproc.dispute.domain.BidStrategy;
import com.govproc.dispute.domain.Dispute;
import com.govproc.dispute.domain.DisputeStatus;
import com.govproc.dispute.dto.DisputeResponse;
import com.govproc.dispute.dto.StartDisputeRequest;
import com.govproc.dispute.dto.UpdateDisputeRequest;
import com.govproc.dispute.repository.DisputeRepository;
import com.govproc.process.domain.PriorityLevel;
import com.govproc.process.domain.ProcurementProcess;
import com.govproc.process.domain.ProcessStatus;
import com.govproc.process.domain.RiskLevel;
import com.govproc.process.repository.ProcessRepository;
import com.govproc.quotation.domain.Quotation;
import com.govproc.quotation.repository.QuotationRepository;
import com.govproc.shared.exception.BusinessException;
import com.govproc.timeline.domain.ProcessEventType;
import com.govproc.timeline.service.TimelineService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisputeServiceTest {

    @Mock ProcessRepository processRepository;
    @Mock DisputeRepository disputeRepository;
    @Mock QuotationRepository quotationRepository;
    @Mock AuditService auditService;
    @Mock TimelineService timelineService;

    @InjectMocks DisputeService disputeService;

    private static final UUID PROCESS_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SUPPLIER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID QUOT_ID    = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID USER_ID    = UUID.fromString("22222222-2222-2222-2222-222222222222");

    /** Processo em QUOTED via cadeia real de transicoes. */
    private ProcurementProcess quotedProcess() {
        ProcurementProcess p = new ProcurementProcess(
                "001/2024", "654321", "Orgao X", "Compras.gov.br",
                "Objeto da licitacao", null, null, null,
                PriorityLevel.HIGH, RiskLevel.LOW, USER_ID);
        p.startAnalysis();
        p.approveAnalysis();
        p.startQuotation();
        p.markAsQuoted();
        return p; // status = QUOTED
    }

    /** Processo em IN_DISPUTE via cadeia real de transicoes. */
    private ProcurementProcess inDisputeProcess() {
        ProcurementProcess p = quotedProcess();
        p.startDispute();
        return p; // status = IN_DISPUTE
    }

    /** Cotacao selecionada: total = 2500 * 10 + 150 = 25150.0000. */
    private Quotation selectedQuotation() {
        Quotation q = new Quotation(PROCESS_ID, SUPPLIER_ID, "Notebook Dell",
                new BigDecimal("2500.00"), 10, new BigDecimal("150.00"),
                "Dell", "Latitude 5540", "15 dias", null, USER_ID);
        q.select();
        return q;
    }

    private Dispute openDispute() {
        return new Dispute(PROCESS_ID, QUOT_ID, new BigDecimal("25150.00"),
                BidStrategy.MODERATE, null, null, null, null, USER_ID);
    }

    private StartDisputeRequest startRequest(BigDecimal targetSale, BigDecimal minSale) {
        return new StartDisputeRequest(BidStrategy.AGGRESSIVE, new BigDecimal("18.00"),
                targetSale, minSale, "Lance agressivo nos minutos finais");
    }

    @Test
    void startDispute_deveTransicionarParaInDispute_eCapturarCustoDaCotacaoSelecionada() {
        when(disputeRepository.existsByProcessId(PROCESS_ID)).thenReturn(false);
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(quotedProcess()));
        when(quotationRepository.findByProcessIdAndSelectedTrue(PROCESS_ID))
                .thenReturn(Optional.of(selectedQuotation()));
        when(processRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(disputeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DisputeResponse response = disputeService.startDispute(
                PROCESS_ID, startRequest(new BigDecimal("30000.00"), new BigDecimal("26000.00")), USER_ID);

        // quotedCost e snapshot do totalCost da cotacao selecionada
        assertThat(response.quotedCost()).isEqualByComparingTo(new BigDecimal("25150.0000"));
        assertThat(response.status()).isEqualTo(DisputeStatus.OPEN);
        verify(auditService).record(
                eq("ProcurementProcess"), eq(PROCESS_ID), eq("status"),
                eq("QUOTED"), eq("IN_DISPUTE"), eq(USER_ID));
        verify(timelineService).record(eq(PROCESS_ID), eq(ProcessEventType.DISPUTE_STARTED), any(), eq(USER_ID));
    }

    @Test
    void startDispute_deveCalcularExpectedProfit_comoTargetSalePriceMenosQuotedCost() {
        when(disputeRepository.existsByProcessId(PROCESS_ID)).thenReturn(false);
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(quotedProcess()));
        when(quotationRepository.findByProcessIdAndSelectedTrue(PROCESS_ID))
                .thenReturn(Optional.of(selectedQuotation()));
        when(processRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(disputeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DisputeResponse response = disputeService.startDispute(
                PROCESS_ID, startRequest(new BigDecimal("30000.00"), new BigDecimal("26000.00")), USER_ID);

        // expectedProfit = 30000 - 25150 = 4850.0000
        assertThat(response.expectedProfit()).isEqualByComparingTo(new BigDecimal("4850.0000"));
    }

    @Test
    void startDispute_deveLancarBusiness_quandoNenhumaCotacaoSelecionada() {
        when(disputeRepository.existsByProcessId(PROCESS_ID)).thenReturn(false);
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(quotedProcess()));
        when(quotationRepository.findByProcessIdAndSelectedTrue(PROCESS_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> disputeService.startDispute(
                PROCESS_ID, startRequest(new BigDecimal("30000.00"), null), USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Nenhuma cotação selecionada");
    }

    @Test
    void startDispute_deveLancarBusiness_quandoDisputaJaExiste() {
        when(disputeRepository.existsByProcessId(PROCESS_ID)).thenReturn(true);

        assertThatThrownBy(() -> disputeService.startDispute(
                PROCESS_ID, startRequest(new BigDecimal("30000.00"), null), USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("já iniciada");
    }

    @Test
    void startDispute_deveLancarBusiness_quandoTargetSalePriceMenorQuePrecoMinimo() {
        // Invariante de dominio: alvo nao pode ficar abaixo do piso
        when(disputeRepository.existsByProcessId(PROCESS_ID)).thenReturn(false);
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(quotedProcess()));
        when(quotationRepository.findByProcessIdAndSelectedTrue(PROCESS_ID))
                .thenReturn(Optional.of(selectedQuotation()));

        assertThatThrownBy(() -> disputeService.startDispute(
                PROCESS_ID, startRequest(new BigDecimal("26000.00"), new BigDecimal("28000.00")), USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("menor que o preço mínimo");
    }

    @Test
    void updateDispute_deveRevisarEstrategia_quandoDisputaEstaOpen() {
        Dispute dispute = openDispute();
        when(disputeRepository.findByProcessId(PROCESS_ID)).thenReturn(Optional.of(dispute));
        when(disputeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateDisputeRequest request = new UpdateDisputeRequest(
                BidStrategy.CONSERVATIVE, new BigDecimal("20.00"),
                new BigDecimal("31000.00"), new BigDecimal("27000.00"), "Revisado");

        DisputeResponse response = disputeService.updateDispute(PROCESS_ID, request, USER_ID);

        assertThat(response.bidStrategy()).isEqualTo(BidStrategy.CONSERVATIVE);
        assertThat(response.expectedProfit()).isEqualByComparingTo(new BigDecimal("5850.0000")); // 31000 - 25150

        // Auditoria campo a campo: cada alteracao de estrategia fica rastreavel (old → new)
        verify(auditService).record(eq("Dispute"), eq(dispute.getId()), eq("bidStrategy"),
                eq("MODERATE"), eq("CONSERVATIVE"), eq(USER_ID));
        verify(auditService).record(eq("Dispute"), eq(dispute.getId()), eq("targetSalePrice"),
                isNull(), eq("31000.0000"), eq(USER_ID));
        verify(auditService).record(eq("Dispute"), eq(dispute.getId()), eq("expectedProfit"),
                isNull(), eq("5850.0000"), eq(USER_ID));
    }

    @Test
    void updateDispute_naoDeveAuditarCampoQueNaoMudou() {
        // bidStrategy permanece MODERATE (request envia o mesmo valor) → sem audit desse campo
        Dispute dispute = openDispute(); // MODERATE, demais campos null
        when(disputeRepository.findByProcessId(PROCESS_ID)).thenReturn(Optional.of(dispute));
        when(disputeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateDisputeRequest request = new UpdateDisputeRequest(
                BidStrategy.MODERATE, null, null, null, "Só uma nota");

        disputeService.updateDispute(PROCESS_ID, request, USER_ID);

        // Nenhum campo estruturado mudou de valor → nenhuma chamada de auditoria
        verifyNoInteractions(auditService);
    }

    @Test
    void markAsWinner_deveTransicionarParaWinner_eConcluirDisputa() {
        ProcurementProcess process = inDisputeProcess();
        Dispute dispute = openDispute();
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(process));
        when(disputeRepository.findByProcessId(PROCESS_ID)).thenReturn(Optional.of(dispute));
        when(processRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(disputeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        disputeService.markAsWinner(PROCESS_ID, USER_ID);

        assertThat(process.getStatus()).isEqualTo(ProcessStatus.WINNER);
        assertThat(dispute.getStatus()).isEqualTo(DisputeStatus.CONCLUDED);
        verify(auditService).record(any(), any(), any(), eq("IN_DISPUTE"), eq("WINNER"), any());
        verify(timelineService).record(eq(PROCESS_ID), eq(ProcessEventType.MARKED_AS_WINNER), any(), eq(USER_ID));
    }

    @Test
    void markAsLoser_deveTransicionarParaLoser_eConcluirDisputa() {
        ProcurementProcess process = inDisputeProcess();
        Dispute dispute = openDispute();
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(process));
        when(disputeRepository.findByProcessId(PROCESS_ID)).thenReturn(Optional.of(dispute));
        when(processRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(disputeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        disputeService.markAsLoser(PROCESS_ID, USER_ID);

        assertThat(process.getStatus()).isEqualTo(ProcessStatus.LOSER);
        assertThat(dispute.getStatus()).isEqualTo(DisputeStatus.CONCLUDED);
        verify(auditService).record(any(), any(), any(), eq("IN_DISPUTE"), eq("LOSER"), any());
        verify(timelineService).record(eq(PROCESS_ID), eq(ProcessEventType.MARKED_AS_LOSER), any(), eq(USER_ID));
    }
}
