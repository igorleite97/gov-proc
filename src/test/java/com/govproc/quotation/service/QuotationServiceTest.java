package com.govproc.quotation.service;

import com.govproc.audit.service.AuditService;
import com.govproc.process.domain.PriorityLevel;
import com.govproc.process.domain.ProcurementProcess;
import com.govproc.process.domain.ProcessStatus;
import com.govproc.process.domain.RiskLevel;
import com.govproc.process.repository.ProcessRepository;
import com.govproc.quotation.domain.Quotation;
import com.govproc.quotation.dto.AddQuotationRequest;
import com.govproc.quotation.dto.QuotationResponse;
import com.govproc.quotation.repository.QuotationRepository;
import com.govproc.shared.exception.BusinessException;
import com.govproc.supplier.repository.SupplierRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuotationServiceTest {

    @Mock ProcessRepository processRepository;
    @Mock QuotationRepository quotationRepository;
    @Mock SupplierRepository supplierRepository;
    @Mock AuditService auditService;
    @Mock TimelineService timelineService;

    @InjectMocks QuotationService quotationService;

    private static final UUID PROCESS_ID  = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SUPPLIER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID QUOT_ID     = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID USER_ID     = UUID.fromString("22222222-2222-2222-2222-222222222222");

    /** Processo com estado IN_QUOTATION via cadeia real de transicoes. */
    private ProcurementProcess inQuotationProcess() {
        ProcurementProcess p = new ProcurementProcess(
                "001/2024", "654321", "Orgao X", "Compras.gov.br",
                "Objeto da licitacao", null, null, null,
                PriorityLevel.HIGH, RiskLevel.LOW, USER_ID);
        p.startAnalysis();
        p.approveAnalysis();
        p.startQuotation();
        return p; // status = IN_QUOTATION
    }

    private AddQuotationRequest validRequest() {
        return new AddQuotationRequest(SUPPLIER_ID, "Notebook Dell",
                new BigDecimal("2500.00"), 10, new BigDecimal("150.00"),
                "Dell", "Latitude 5540", "15 dias", null);
    }

    private Quotation sampleQuotation() {
        return new Quotation(PROCESS_ID, SUPPLIER_ID, "Notebook Dell",
                new BigDecimal("2500.00"), 10, new BigDecimal("150.00"),
                "Dell", "Latitude 5540", "15 dias", null, USER_ID);
    }

    @Test
    void addQuotation_deveCalcularTotalCorreto_quandoDadosValidos() {
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(inQuotationProcess()));
        when(supplierRepository.existsById(SUPPLIER_ID)).thenReturn(true);
        when(quotationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        QuotationResponse response = quotationService.addQuotation(PROCESS_ID, validRequest(), USER_ID);

        // totalCost = 2500 * 10 + 150 = 25150
        assertThat(response.totalCost()).isEqualByComparingTo(new BigDecimal("25150.0000"));
        verify(timelineService).record(eq(PROCESS_ID), eq(ProcessEventType.QUOTATION_ADDED), any(), eq(USER_ID));
    }

    @Test
    void addQuotation_deveLancarBusiness_quandoProcessoNaoEstaInQuotation() {
        // Processo em CAPTURED nao pode receber cotacoes
        ProcurementProcess captured = new ProcurementProcess(
                "001/2024", "654321", "Orgao", "Portal", "Objeto",
                null, null, null, PriorityLevel.HIGH, RiskLevel.LOW, USER_ID);
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(captured));

        assertThatThrownBy(() -> quotationService.addQuotation(PROCESS_ID, validRequest(), USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("IN_QUOTATION");
    }

    @Test
    void selectQuotation_deveClearAllEsselecionarAEscolhida() {
        Quotation quotation = sampleQuotation();
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(inQuotationProcess()));
        when(quotationRepository.findByIdAndProcessId(QUOT_ID, PROCESS_ID)).thenReturn(Optional.of(quotation));
        when(quotationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        quotationService.selectQuotation(PROCESS_ID, QUOT_ID, USER_ID);

        // Regra: deseleciona todas antes de selecionar (atomico)
        verify(quotationRepository).clearSelectedByProcess(PROCESS_ID);
        assertThat(quotation.isSelected()).isTrue();
    }

    @Test
    void markAsQuoted_deveLancarBusiness_quandoNenhumaCotacaoSelecionada() {
        when(quotationRepository.existsByProcessIdAndSelectedTrue(PROCESS_ID)).thenReturn(false);

        assertThatThrownBy(() -> quotationService.markAsQuoted(PROCESS_ID, USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Selecione");
    }

    @Test
    void markAsQuoted_deveTransicionarParaQuoted_quandoCotacaoSelecionada() {
        ProcurementProcess process = inQuotationProcess();
        when(quotationRepository.existsByProcessIdAndSelectedTrue(PROCESS_ID)).thenReturn(true);
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(process));
        when(processRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        quotationService.markAsQuoted(PROCESS_ID, USER_ID);

        assertThat(process.getStatus()).isEqualTo(ProcessStatus.QUOTED);
        verify(auditService).record(any(), any(), any(), eq("IN_QUOTATION"), eq("QUOTED"), any());
    }
}
