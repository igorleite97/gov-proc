package com.govproc.contract.service;

import com.govproc.audit.service.AuditService;
import com.govproc.contract.domain.Contract;
import com.govproc.contract.dto.ActivateContractRequest;
import com.govproc.contract.dto.ContractResponse;
import com.govproc.contract.dto.TerminateContractRequest;
import com.govproc.contract.repository.ContractRepository;
import com.govproc.postbid.domain.PostBid;
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
public class ContractService {

    private final ProcessRepository processRepository;
    private final ContractRepository contractRepository;
    private final PostBidRepository postBidRepository;
    private final AuditService auditService;
    private final TimelineService timelineService;

    public ContractService(ProcessRepository processRepository,
                           ContractRepository contractRepository,
                           PostBidRepository postBidRepository,
                           AuditService auditService,
                           TimelineService timelineService) {
        this.processRepository = processRepository;
        this.contractRepository = contractRepository;
        this.postBidRepository = postBidRepository;
        this.auditService = auditService;
        this.timelineService = timelineService;
    }

    /**
     * Ativa o contrato: POST_BID → CONTRACT_ACTIVE.
     *
     * GUARD: so e permitido quando a fase pos-disputa esta COMPLETED. O processo
     * nao conhece o PostBid — por isso esta regra vive no service, nao no dominio.
     */
    @Transactional
    public ContractResponse activate(UUID processId, ActivateContractRequest request, UUID userId) {
        if (contractRepository.existsByProcessId(processId)) {
            throw new BusinessException("Contrato já criado para o processo: " + processId);
        }

        PostBid postBid = postBidRepository.findByProcessId(processId)
                .orElseThrow(() -> new BusinessException(
                        "Fase pós-disputa não encontrada — não é possível ativar o contrato"));
        if (!postBid.isCompleted()) {
            throw new BusinessException(
                    "Fase pós-disputa deve estar COMPLETED para ativar o contrato");
        }

        ProcurementProcess process = loadProcess(processId);
        String oldStatus = process.getStatus().name();

        process.activateContract(); // valida POST_BID → CONTRACT_ACTIVE

        Contract contract = new Contract(
                processId,
                request.contractNumber(),
                request.startDate(),
                request.endDate(),
                request.contractValue(),
                userId
        );

        processRepository.save(process);
        contractRepository.save(contract);

        auditService.record("ProcurementProcess", processId, "status", oldStatus, "CONTRACT_ACTIVE", userId);
        timelineService.record(processId, ProcessEventType.CONTRACT_ACTIVATED,
                "Contrato ativado: " + request.contractNumber(), userId);

        return ContractResponse.from(contract);
    }

    /** Encerramento normal: contrato ACTIVE → CLOSED e processo → CLOSED. */
    @Transactional
    public ContractResponse close(UUID processId, UUID userId) {
        Contract contract = loadContract(processId);
        String oldStatus = contract.getStatus().name();

        contract.close();
        contractRepository.save(contract);

        auditService.record("Contract", contract.getId(), "status", oldStatus, "CLOSED", userId);
        closeProcess(processId, userId, "Contrato encerrado");

        return ContractResponse.from(contract);
    }

    /** Rescisao antecipada: contrato ACTIVE → TERMINATED e processo → CLOSED. */
    @Transactional
    public ContractResponse terminate(UUID processId, TerminateContractRequest request, UUID userId) {
        Contract contract = loadContract(processId);
        String oldStatus = contract.getStatus().name();

        contract.terminate();
        contractRepository.save(contract);

        auditService.record("Contract", contract.getId(), "status", oldStatus, "TERMINATED", userId);
        closeProcess(processId, userId, "Contrato rescindido: " + request.reason());

        return ContractResponse.from(contract);
    }

    /** Vencimento pelo decurso do prazo: contrato ACTIVE → EXPIRED e processo → CLOSED. */
    @Transactional
    public ContractResponse expire(UUID processId, UUID userId) {
        Contract contract = loadContract(processId);
        String oldStatus = contract.getStatus().name();

        contract.expire();
        contractRepository.save(contract);

        auditService.record("Contract", contract.getId(), "status", oldStatus, "EXPIRED", userId);
        closeProcess(processId, userId, "Contrato vencido");

        return ContractResponse.from(contract);
    }

    @Transactional(readOnly = true)
    public ContractResponse getContract(UUID processId) {
        return ContractResponse.from(loadContract(processId));
    }

    // -------------------------------------------------------------------------

    /** Encerra o processo (CONTRACT_ACTIVE → CLOSED) com audit + timeline. */
    private void closeProcess(UUID processId, UUID userId, String message) {
        ProcurementProcess process = loadProcess(processId);
        String oldStatus = process.getStatus().name();

        process.close(); // valida CONTRACT_ACTIVE → CLOSED

        processRepository.save(process);
        auditService.record("ProcurementProcess", processId, "status", oldStatus, "CLOSED", userId);
        timelineService.record(processId, ProcessEventType.PROCESS_CLOSED, message, userId);
    }

    private ProcurementProcess loadProcess(UUID processId) {
        return processRepository.findById(processId)
                .orElseThrow(() -> new ResourceNotFoundException("Processo não encontrado: " + processId));
    }

    private Contract loadContract(UUID processId) {
        return contractRepository.findByProcessId(processId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Contrato não encontrado para o processo: " + processId));
    }
}
