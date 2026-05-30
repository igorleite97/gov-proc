package com.govproc.contract.service;

import com.govproc.audit.service.AuditService;
import com.govproc.contract.domain.AddendumType;
import com.govproc.contract.domain.Contract;
import com.govproc.contract.dto.ApplyAddendumRequest;
import com.govproc.contract.dto.CommitmentResponse;
import com.govproc.contract.dto.RegisterCommitmentRequest;
import com.govproc.contract.dto.RegisterInvoiceRequest;
import com.govproc.contract.repository.AddendumRepository;
import com.govproc.contract.repository.CommitmentRepository;
import com.govproc.contract.repository.ContractRepository;
import com.govproc.contract.repository.InvoiceRepository;
import com.govproc.shared.exception.BusinessException;
import com.govproc.timeline.domain.ProcessEventType;
import com.govproc.timeline.service.TimelineService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContractExecutionServiceTest {

    @Mock ContractRepository contractRepository;
    @Mock CommitmentRepository commitmentRepository;
    @Mock InvoiceRepository invoiceRepository;
    @Mock AddendumRepository addendumRepository;
    @Mock AuditService auditService;
    @Mock TimelineService timelineService;

    @InjectMocks ContractExecutionService executionService;

    private static final UUID PROCESS_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID    = UUID.fromString("22222222-2222-2222-2222-222222222222");

    /** Contrato ACTIVE de R$ 1.000.000 (saldo inicial = valor total). */
    private Contract activeContract() {
        return new Contract(PROCESS_ID, "CT-2024/001",
                LocalDate.of(2024, 6, 1), LocalDate.of(2025, 6, 1),
                new BigDecimal("1000000.00"), USER_ID);
    }

    @Test
    void registerCommitment_deveReduzirSaldo() {
        Contract contract = activeContract();
        when(contractRepository.findByProcessId(PROCESS_ID)).thenReturn(Optional.of(contract));
        when(contractRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(commitmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new RegisterCommitmentRequest("EMP-2024/001",
                new BigDecimal("300000.00"), LocalDate.of(2024, 6, 5), null);
        CommitmentResponse response = executionService.registerCommitment(PROCESS_ID, request, USER_ID);

        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("300000.0000"));
        // saldo: 1.000.000 - 300.000 = 700.000
        assertThat(contract.getRemainingBalance()).isEqualByComparingTo(new BigDecimal("700000.0000"));
        verify(auditService).record(eq("Contract"), any(), eq("remainingBalance"),
                eq("1000000.0000"), eq("700000.0000"), eq(USER_ID));
        verify(timelineService).record(eq(PROCESS_ID), eq(ProcessEventType.COMMITMENT_REGISTERED), any(), eq(USER_ID));
    }

    @Test
    void registerCommitment_deveLancarBusiness_quandoExcedeSaldo() {
        Contract contract = activeContract();
        when(contractRepository.findByProcessId(PROCESS_ID)).thenReturn(Optional.of(contract));

        var request = new RegisterCommitmentRequest("EMP-X",
                new BigDecimal("1200000.00"), null, null);

        assertThatThrownBy(() -> executionService.registerCommitment(PROCESS_ID, request, USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("excede o saldo");
        // saldo intacto
        assertThat(contract.getRemainingBalance()).isEqualByComparingTo(new BigDecimal("1000000.0000"));
    }

    @Test
    void registerCommitment_deveLancarBusiness_quandoContratoNaoEstaActive() {
        Contract contract = activeContract();
        contract.close(); // CLOSED
        when(contractRepository.findByProcessId(PROCESS_ID)).thenReturn(Optional.of(contract));

        var request = new RegisterCommitmentRequest("EMP-1", new BigDecimal("100.00"), null, null);

        assertThatThrownBy(() -> executionService.registerCommitment(PROCESS_ID, request, USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ACTIVE");
    }

    @Test
    void registerInvoice_naoDeveAlterarSaldo() {
        Contract contract = activeContract();
        when(contractRepository.findByProcessId(PROCESS_ID)).thenReturn(Optional.of(contract));
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new RegisterInvoiceRequest("NF-2024/001", new BigDecimal("200000.00"), LocalDate.now());
        executionService.registerInvoice(PROCESS_ID, request, USER_ID);

        // fatura confirma execucao, nao consome saldo
        assertThat(contract.getRemainingBalance()).isEqualByComparingTo(new BigDecimal("1000000.0000"));
        verify(timelineService).record(eq(PROCESS_ID), eq(ProcessEventType.INVOICE_REGISTERED), any(), eq(USER_ID));
        // sem auditoria de saldo (nada mudou)
        verifyNoInteractions(auditService);
    }

    @Test
    void applyAddendum_valueIncrease_deveAumentarValorESaldo() {
        Contract contract = activeContract();
        when(contractRepository.findByProcessId(PROCESS_ID)).thenReturn(Optional.of(contract));
        when(contractRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(addendumRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new ApplyAddendumRequest("AD-2024/001", AddendumType.VALUE_INCREASE,
                new BigDecimal("300000.00"), null, "Acréscimo de quantitativo", LocalDate.now());
        executionService.applyAddendum(PROCESS_ID, request, USER_ID);

        // valor: 1.000.000 + 300.000 = 1.300.000 ; saldo idem
        assertThat(contract.getContractValue()).isEqualByComparingTo(new BigDecimal("1300000.0000"));
        assertThat(contract.getRemainingBalance()).isEqualByComparingTo(new BigDecimal("1300000.0000"));
        verify(auditService).record(eq("Contract"), any(), eq("contractValue"),
                eq("1000000.0000"), eq("1300000.0000"), eq(USER_ID));
    }

    @Test
    void applyAddendum_valueDecrease_deveReduzirValorESaldo() {
        Contract contract = activeContract();
        when(contractRepository.findByProcessId(PROCESS_ID)).thenReturn(Optional.of(contract));
        when(contractRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(addendumRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new ApplyAddendumRequest("AD-2024/002", AddendumType.VALUE_DECREASE,
                new BigDecimal("200000.00"), null, "Supressão", LocalDate.now());
        executionService.applyAddendum(PROCESS_ID, request, USER_ID);

        assertThat(contract.getContractValue()).isEqualByComparingTo(new BigDecimal("800000.0000"));
        assertThat(contract.getRemainingBalance()).isEqualByComparingTo(new BigDecimal("800000.0000"));
    }

    @Test
    void applyAddendum_valueDecrease_deveLancarBusiness_quandoExcedeSaldo() {
        Contract contract = activeContract();
        when(contractRepository.findByProcessId(PROCESS_ID)).thenReturn(Optional.of(contract));

        var request = new ApplyAddendumRequest("AD-X", AddendumType.VALUE_DECREASE,
                new BigDecimal("1200000.00"), null, "Supressão inválida", LocalDate.now());

        assertThatThrownBy(() -> executionService.applyAddendum(PROCESS_ID, request, USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("excede o saldo");
    }

    @Test
    void applyAddendum_termExtension_deveAtualizarEndDate() {
        Contract contract = activeContract();
        when(contractRepository.findByProcessId(PROCESS_ID)).thenReturn(Optional.of(contract));
        when(contractRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(addendumRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LocalDate novaData = LocalDate.of(2026, 6, 1);
        var request = new ApplyAddendumRequest("AD-2024/003", AddendumType.TERM_EXTENSION,
                null, novaData, "Prorrogação", LocalDate.now());
        executionService.applyAddendum(PROCESS_ID, request, USER_ID);

        assertThat(contract.getEndDate()).isEqualTo(novaData);
        // prazo nao altera valores
        assertThat(contract.getContractValue()).isEqualByComparingTo(new BigDecimal("1000000.0000"));
        assertThat(contract.getRemainingBalance()).isEqualByComparingTo(new BigDecimal("1000000.0000"));
    }
}
