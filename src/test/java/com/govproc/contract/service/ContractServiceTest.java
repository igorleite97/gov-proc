package com.govproc.contract.service;

import com.govproc.audit.service.AuditService;
import com.govproc.contract.domain.Contract;
import com.govproc.contract.domain.ContractStatus;
import com.govproc.contract.dto.ActivateContractRequest;
import com.govproc.contract.dto.ContractResponse;
import com.govproc.contract.dto.TerminateContractRequest;
import com.govproc.contract.repository.ContractRepository;
import com.govproc.postbid.domain.PostBid;
import com.govproc.postbid.repository.PostBidRepository;
import com.govproc.process.domain.PriorityLevel;
import com.govproc.process.domain.ProcurementProcess;
import com.govproc.process.domain.ProcessStatus;
import com.govproc.process.domain.RiskLevel;
import com.govproc.process.repository.ProcessRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContractServiceTest {

    @Mock ProcessRepository processRepository;
    @Mock ContractRepository contractRepository;
    @Mock PostBidRepository postBidRepository;
    @Mock AuditService auditService;
    @Mock TimelineService timelineService;

    @InjectMocks ContractService contractService;

    private static final UUID PROCESS_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID    = UUID.fromString("22222222-2222-2222-2222-222222222222");

    /** Processo em POST_BID via cadeia real de transicoes. */
    private ProcurementProcess postBidProcess() {
        ProcurementProcess p = new ProcurementProcess(
                "001/2024", "654321", "Orgao X", "Compras.gov.br",
                "Objeto da licitacao", null, null, null,
                PriorityLevel.HIGH, RiskLevel.LOW, USER_ID);
        p.startAnalysis();
        p.approveAnalysis();
        p.startQuotation();
        p.markAsQuoted();
        p.startDispute();
        p.markAsWinner();
        p.startPostBid();
        return p; // status = POST_BID
    }

    /** Processo em CONTRACT_ACTIVE. */
    private ProcurementProcess contractActiveProcess() {
        ProcurementProcess p = postBidProcess();
        p.activateContract();
        return p; // status = CONTRACT_ACTIVE
    }

    private PostBid completedPostBid() {
        PostBid pb = new PostBid(PROCESS_ID, USER_ID);
        pb.homologate("HOM-2024/001", LocalDate.now(), null);
        pb.adjudicate("ADJ-2024/001", LocalDate.now(), null);
        pb.complete();
        return pb;
    }

    private Contract activeContract() {
        return new Contract(PROCESS_ID, "CT-2024/001",
                LocalDate.of(2024, 6, 1), LocalDate.of(2025, 6, 1),
                new BigDecimal("100000.00"), USER_ID);
    }

    private ActivateContractRequest activateRequest() {
        return new ActivateContractRequest("CT-2024/001",
                LocalDate.of(2024, 6, 1), LocalDate.of(2025, 6, 1),
                new BigDecimal("100000.00"));
    }

    @Test
    void activate_deveTransicionarParaContractActive_quandoPostBidCompleted() {
        ProcurementProcess process = postBidProcess();
        when(contractRepository.existsByProcessId(PROCESS_ID)).thenReturn(false);
        when(postBidRepository.findByProcessId(PROCESS_ID)).thenReturn(Optional.of(completedPostBid()));
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(process));
        when(processRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(contractRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ContractResponse response = contractService.activate(PROCESS_ID, activateRequest(), USER_ID);

        assertThat(process.getStatus()).isEqualTo(ProcessStatus.CONTRACT_ACTIVE);
        assertThat(response.status()).isEqualTo(ContractStatus.ACTIVE);
        // saldo inicial = valor total do contrato
        assertThat(response.remainingBalance()).isEqualByComparingTo(new BigDecimal("100000.0000"));
        verify(auditService).record(
                eq("ProcurementProcess"), eq(PROCESS_ID), eq("status"),
                eq("POST_BID"), eq("CONTRACT_ACTIVE"), eq(USER_ID));
        verify(timelineService).record(eq(PROCESS_ID), eq(ProcessEventType.CONTRACT_ACTIVATED), any(), eq(USER_ID));
    }

    @Test
    void activate_deveLancarBusiness_quandoPostBidNaoEstaCompleted() {
        // GUARD principal da fatia: contrato so ativa com pos-disputa COMPLETED
        PostBid pending = new PostBid(PROCESS_ID, USER_ID); // PENDING
        when(contractRepository.existsByProcessId(PROCESS_ID)).thenReturn(false);
        when(postBidRepository.findByProcessId(PROCESS_ID)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> contractService.activate(PROCESS_ID, activateRequest(), USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("COMPLETED");
    }

    @Test
    void activate_deveLancarBusiness_quandoContratoJaExiste() {
        when(contractRepository.existsByProcessId(PROCESS_ID)).thenReturn(true);

        assertThatThrownBy(() -> contractService.activate(PROCESS_ID, activateRequest(), USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("já criado");
    }

    @Test
    void activate_deveLancarBusiness_quandoPostBidNaoEncontrado() {
        when(contractRepository.existsByProcessId(PROCESS_ID)).thenReturn(false);
        when(postBidRepository.findByProcessId(PROCESS_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contractService.activate(PROCESS_ID, activateRequest(), USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("pós-disputa");
    }

    @Test
    void close_deveEncerrarContratoEProcesso() {
        Contract contract = activeContract();
        ProcurementProcess process = contractActiveProcess();
        when(contractRepository.findByProcessId(PROCESS_ID)).thenReturn(Optional.of(contract));
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(process));
        when(contractRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(processRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ContractResponse response = contractService.close(PROCESS_ID, USER_ID);

        assertThat(response.status()).isEqualTo(ContractStatus.CLOSED);
        assertThat(process.getStatus()).isEqualTo(ProcessStatus.CLOSED);
        verify(timelineService).record(eq(PROCESS_ID), eq(ProcessEventType.PROCESS_CLOSED), any(), eq(USER_ID));
    }

    @Test
    void terminate_deveRescindirContrato_eEncerrarProcesso() {
        Contract contract = activeContract();
        ProcurementProcess process = contractActiveProcess();
        when(contractRepository.findByProcessId(PROCESS_ID)).thenReturn(Optional.of(contract));
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(process));
        when(contractRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(processRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new TerminateContractRequest("Descumprimento de cláusula");
        ContractResponse response = contractService.terminate(PROCESS_ID, request, USER_ID);

        assertThat(response.status()).isEqualTo(ContractStatus.TERMINATED);
        assertThat(process.getStatus()).isEqualTo(ProcessStatus.CLOSED);
        verify(auditService).record(eq("Contract"), any(), eq("status"),
                eq("ACTIVE"), eq("TERMINATED"), eq(USER_ID));
    }

    @Test
    void expire_deveMarcarVencido_eEncerrarProcesso() {
        Contract contract = activeContract();
        ProcurementProcess process = contractActiveProcess();
        when(contractRepository.findByProcessId(PROCESS_ID)).thenReturn(Optional.of(contract));
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(process));
        when(contractRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(processRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ContractResponse response = contractService.expire(PROCESS_ID, USER_ID);

        assertThat(response.status()).isEqualTo(ContractStatus.EXPIRED);
        assertThat(process.getStatus()).isEqualTo(ProcessStatus.CLOSED);
    }

    @Test
    void close_deveLancarBusiness_quandoContratoNaoEstaActive() {
        Contract contract = activeContract();
        contract.close(); // ja CLOSED
        when(contractRepository.findByProcessId(PROCESS_ID)).thenReturn(Optional.of(contract));

        assertThatThrownBy(() -> contractService.close(PROCESS_ID, USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ACTIVE");
    }
}
