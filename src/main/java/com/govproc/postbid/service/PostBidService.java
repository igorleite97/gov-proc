package com.govproc.postbid.service;

import com.govproc.audit.service.AuditService;
import com.govproc.postbid.domain.PostBid;
import com.govproc.postbid.dto.AdjudicateRequest;
import com.govproc.postbid.dto.HomologateRequest;
import com.govproc.postbid.dto.PostBidResponse;
import com.govproc.postbid.repository.PostBidRepository;
import com.govproc.process.domain.ProcurementProcess;
import com.govproc.process.repository.ProcessRepository;
import com.govproc.shared.exception.BusinessException;
import com.govproc.shared.exception.ResourceNotFoundException;
import com.govproc.timeline.domain.ProcessEventType;
import com.govproc.timeline.service.TimelineService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PostBidService {

    private final ProcessRepository processRepository;
    private final PostBidRepository postBidRepository;
    private final AuditService auditService;
    private final TimelineService timelineService;

    public PostBidService(ProcessRepository processRepository,
                          PostBidRepository postBidRepository,
                          AuditService auditService,
                          TimelineService timelineService) {
        this.processRepository = processRepository;
        this.postBidRepository = postBidRepository;
        this.auditService = auditService;
        this.timelineService = timelineService;
    }

    /**
     * Inicia a fase pos-disputa: WINNER → POST_BID.
     * A partir daqui o rito juridico (homologacao/adjudicacao) vive no PostBid.
     */
    @Transactional
    public PostBidResponse startPostBid(UUID processId, UUID userId) {
        if (postBidRepository.existsByProcessId(processId)) {
            throw new BusinessException("Fase pós-disputa já iniciada para o processo: " + processId);
        }

        ProcurementProcess process = loadProcess(processId);
        String oldStatus = process.getStatus().name();

        process.startPostBid(); // valida WINNER → POST_BID

        PostBid postBid = new PostBid(processId, userId);

        processRepository.save(process);
        postBidRepository.save(postBid);

        auditService.record("ProcurementProcess", processId, "status", oldStatus, "POST_BID", userId);
        timelineService.record(processId, ProcessEventType.POST_BID_STARTED, "Fase pós-disputa iniciada", userId);

        return PostBidResponse.from(postBid);
    }

    /** Homologacao: PENDING → HOMOLOGATED. */
    @Transactional
    public PostBidResponse homologate(UUID processId, HomologateRequest request, UUID userId) {
        PostBid postBid = loadPostBid(processId);
        String oldStatus = postBid.getStatus().name();

        postBid.homologate(request.homologationNumber(), request.homologationDate(), request.notes());
        postBidRepository.save(postBid);

        auditService.record("PostBid", postBid.getId(), "status", oldStatus, "HOMOLOGATED", userId);
        auditService.record("PostBid", postBid.getId(), "homologationNumber",
                null, request.homologationNumber(), userId);
        timelineService.record(processId, ProcessEventType.POST_BID_HOMOLOGATED,
                "Homologado: " + request.homologationNumber(), userId);

        return PostBidResponse.from(postBid);
    }

    /** Adjudicacao: HOMOLOGATED → ADJUDICATED. */
    @Transactional
    public PostBidResponse adjudicate(UUID processId, AdjudicateRequest request, UUID userId) {
        PostBid postBid = loadPostBid(processId);
        String oldStatus = postBid.getStatus().name();

        postBid.adjudicate(request.adjudicationNumber(), request.adjudicationDate(), request.notes());
        postBidRepository.save(postBid);

        auditService.record("PostBid", postBid.getId(), "status", oldStatus, "ADJUDICATED", userId);
        auditService.record("PostBid", postBid.getId(), "adjudicationNumber",
                null, request.adjudicationNumber(), userId);
        timelineService.record(processId, ProcessEventType.POST_BID_ADJUDICATED,
                "Adjudicado: " + request.adjudicationNumber(), userId);

        return PostBidResponse.from(postBid);
    }

    /** Conclusao da fase pos-disputa: ADJUDICATED → COMPLETED (habilita ativacao do contrato). */
    @Transactional
    public PostBidResponse complete(UUID processId, UUID userId) {
        PostBid postBid = loadPostBid(processId);
        String oldStatus = postBid.getStatus().name();

        postBid.complete();
        postBidRepository.save(postBid);

        auditService.record("PostBid", postBid.getId(), "status", oldStatus, "COMPLETED", userId);
        timelineService.record(processId, ProcessEventType.POST_BID_COMPLETED,
                "Fase pós-disputa concluída", userId);

        return PostBidResponse.from(postBid);
    }

    @Transactional(readOnly = true)
    public PostBidResponse getPostBid(UUID processId) {
        return PostBidResponse.from(loadPostBid(processId));
    }

    private ProcurementProcess loadProcess(UUID processId) {
        return processRepository.findById(processId)
                .orElseThrow(() -> new ResourceNotFoundException("Processo não encontrado: " + processId));
    }

    private PostBid loadPostBid(UUID processId) {
        return postBidRepository.findByProcessId(processId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Fase pós-disputa não encontrada para o processo: " + processId));
    }
}
