package com.govproc.postbid.service;

import com.govproc.audit.service.AuditService;
import com.govproc.postbid.domain.PostBid;
import com.govproc.postbid.domain.PostBidStatus;
import com.govproc.postbid.dto.AdjudicateRequest;
import com.govproc.postbid.dto.HomologateRequest;
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
class PostBidServiceTest {

    @Mock ProcessRepository processRepository;
    @Mock PostBidRepository postBidRepository;
    @Mock AuditService auditService;
    @Mock TimelineService timelineService;

    @InjectMocks PostBidService postBidService;

    private static final UUID PROCESS_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID    = UUID.fromString("22222222-2222-2222-2222-222222222222");

    /** Processo em WINNER via cadeia real de transicoes. */
    private ProcurementProcess winnerProcess() {
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
        return p; // status = WINNER
    }

    private PostBid pendingPostBid() {
        return new PostBid(PROCESS_ID, USER_ID); // status = PENDING
    }

    private PostBid homologatedPostBid() {
        PostBid p = pendingPostBid();
        p.homologate("HOM-2024/001", LocalDate.of(2024, 5, 10), null);
        return p;
    }

    private PostBid adjudicatedPostBid() {
        PostBid p = homologatedPostBid();
        p.adjudicate("ADJ-2024/001", LocalDate.of(2024, 5, 12), null);
        return p;
    }

    @Test
    void startPostBid_deveTransicionarParaPostBid_quandoProcessoEhWinner() {
        ProcurementProcess process = winnerProcess();
        when(postBidRepository.existsByProcessId(PROCESS_ID)).thenReturn(false);
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(process));
        when(processRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(postBidRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = postBidService.startPostBid(PROCESS_ID, USER_ID);

        assertThat(process.getStatus()).isEqualTo(ProcessStatus.POST_BID);
        assertThat(response.status()).isEqualTo(PostBidStatus.PENDING);
        verify(auditService).record(
                eq("ProcurementProcess"), eq(PROCESS_ID), eq("status"),
                eq("WINNER"), eq("POST_BID"), eq(USER_ID));
        verify(timelineService).record(eq(PROCESS_ID), eq(ProcessEventType.POST_BID_STARTED), any(), eq(USER_ID));
    }

    @Test
    void startPostBid_deveLancarBusiness_quandoJaExiste() {
        when(postBidRepository.existsByProcessId(PROCESS_ID)).thenReturn(true);

        assertThatThrownBy(() -> postBidService.startPostBid(PROCESS_ID, USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("já iniciada");
    }

    @Test
    void startPostBid_deveLancarBusiness_quandoProcessoNaoEhWinner() {
        // Processo em CAPTURED nao pode iniciar pos-disputa
        ProcurementProcess captured = new ProcurementProcess(
                "001/2024", "654321", "Orgao", "Portal", "Objeto",
                null, null, null, PriorityLevel.HIGH, RiskLevel.LOW, USER_ID);
        when(postBidRepository.existsByProcessId(PROCESS_ID)).thenReturn(false);
        when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(captured));

        assertThatThrownBy(() -> postBidService.startPostBid(PROCESS_ID, USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("WINNER");
    }

    @Test
    void homologate_deveTransicionarParaHomologated_eRegistrarNumero() {
        PostBid postBid = pendingPostBid();
        when(postBidRepository.findByProcessId(PROCESS_ID)).thenReturn(Optional.of(postBid));
        when(postBidRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new HomologateRequest("HOM-2024/001", LocalDate.of(2024, 5, 10), "Sem intercorrências");
        var response = postBidService.homologate(PROCESS_ID, request, USER_ID);

        assertThat(response.status()).isEqualTo(PostBidStatus.HOMOLOGATED);
        assertThat(response.homologationNumber()).isEqualTo("HOM-2024/001");
        verify(auditService).record(eq("PostBid"), any(), eq("status"),
                eq("PENDING"), eq("HOMOLOGATED"), eq(USER_ID));
        verify(timelineService).record(eq(PROCESS_ID), eq(ProcessEventType.POST_BID_HOMOLOGATED), any(), eq(USER_ID));
    }

    @Test
    void adjudicate_deveTransicionarParaAdjudicated_quandoHomologated() {
        PostBid postBid = homologatedPostBid();
        when(postBidRepository.findByProcessId(PROCESS_ID)).thenReturn(Optional.of(postBid));
        when(postBidRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new AdjudicateRequest("ADJ-2024/001", LocalDate.of(2024, 5, 12), null);
        var response = postBidService.adjudicate(PROCESS_ID, request, USER_ID);

        assertThat(response.status()).isEqualTo(PostBidStatus.ADJUDICATED);
        assertThat(response.adjudicationNumber()).isEqualTo("ADJ-2024/001");
        verify(timelineService).record(eq(PROCESS_ID), eq(ProcessEventType.POST_BID_ADJUDICATED), any(), eq(USER_ID));
    }

    @Test
    void complete_deveTransicionarParaCompleted_quandoAdjudicated() {
        PostBid postBid = adjudicatedPostBid();
        when(postBidRepository.findByProcessId(PROCESS_ID)).thenReturn(Optional.of(postBid));
        when(postBidRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = postBidService.complete(PROCESS_ID, USER_ID);

        assertThat(response.status()).isEqualTo(PostBidStatus.COMPLETED);
        assertThat(postBid.isCompleted()).isTrue();
        verify(timelineService).record(eq(PROCESS_ID), eq(ProcessEventType.POST_BID_COMPLETED), any(), eq(USER_ID));
    }

    @Test
    void adjudicate_deveLancarBusiness_quandoAindaPending() {
        // Nao se pode adjudicar sem homologar antes
        PostBid postBid = pendingPostBid();
        when(postBidRepository.findByProcessId(PROCESS_ID)).thenReturn(Optional.of(postBid));

        var request = new AdjudicateRequest("ADJ-2024/001", null, null);
        assertThatThrownBy(() -> postBidService.adjudicate(PROCESS_ID, request, USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("HOMOLOGATED");
    }

    @Test
    void complete_deveLancarBusiness_quandoNaoEstaAdjudicated() {
        PostBid postBid = pendingPostBid();
        when(postBidRepository.findByProcessId(PROCESS_ID)).thenReturn(Optional.of(postBid));

        assertThatThrownBy(() -> postBidService.complete(PROCESS_ID, USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ADJUDICATED");
    }
}
