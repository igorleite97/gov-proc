package com.govproc.dispute.service;

import com.govproc.audit.service.AuditService;
import com.govproc.dispute.domain.BidStrategy;
import com.govproc.dispute.domain.Dispute;
import com.govproc.dispute.dto.DisputeResponse;
import com.govproc.dispute.dto.StartDisputeRequest;
import com.govproc.dispute.dto.UpdateDisputeRequest;
import com.govproc.dispute.repository.DisputeRepository;
import com.govproc.process.domain.ProcurementProcess;
import com.govproc.process.dto.ProcessResponse;
import com.govproc.process.repository.ProcessRepository;
import com.govproc.quotation.domain.Quotation;
import com.govproc.quotation.repository.QuotationRepository;
import com.govproc.shared.exception.BusinessException;
import com.govproc.shared.exception.ResourceNotFoundException;
import com.govproc.timeline.domain.ProcessEventType;
import com.govproc.timeline.service.TimelineService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class DisputeService {

    private final ProcessRepository processRepository;
    private final DisputeRepository disputeRepository;
    private final QuotationRepository quotationRepository;
    private final AuditService auditService;
    private final TimelineService timelineService;

    public DisputeService(ProcessRepository processRepository,
                          DisputeRepository disputeRepository,
                          QuotationRepository quotationRepository,
                          AuditService auditService,
                          TimelineService timelineService) {
        this.processRepository = processRepository;
        this.disputeRepository = disputeRepository;
        this.quotationRepository = quotationRepository;
        this.auditService = auditService;
        this.timelineService = timelineService;
    }

    /**
     * Inicia a disputa: QUOTED → IN_DISPUTE.
     *
     * O quotedCost e capturado do custo total da cotacao SELECIONADA — custo
     * nasce na Cotacao; aqui apenas registramos um snapshot para a estrategia.
     */
    @Transactional
    public DisputeResponse startDispute(UUID processId, StartDisputeRequest request, UUID userId) {
        if (disputeRepository.existsByProcessId(processId)) {
            throw new BusinessException("Disputa já iniciada para o processo: " + processId);
        }

        ProcurementProcess process = loadProcess(processId);
        String oldStatus = process.getStatus().name();

        // Snapshot do custo: a cotacao selecionada e a fonte da verdade do custo.
        Quotation selected = quotationRepository.findByProcessIdAndSelectedTrue(processId)
                .orElseThrow(() -> new BusinessException(
                        "Nenhuma cotação selecionada — não é possível iniciar a disputa"));

        process.startDispute(); // valida QUOTED → IN_DISPUTE

        Dispute dispute = new Dispute(
                processId,
                selected.getId(),
                selected.getTotalCost(),
                request.bidStrategy(),
                request.targetMargin(),
                request.targetSalePrice(),
                request.minimumSalePrice(),
                request.strategyNotes(),
                userId
        );

        processRepository.save(process);
        disputeRepository.save(dispute);

        auditService.record("ProcurementProcess", processId, "status", oldStatus, "IN_DISPUTE", userId);
        timelineService.record(processId, ProcessEventType.DISPUTE_STARTED, "Disputa iniciada", userId);

        return DisputeResponse.from(dispute);
    }

    /**
     * Revisa a estrategia comercial enquanto a disputa esta OPEN.
     */
    @Transactional
    public DisputeResponse updateDispute(UUID processId, UpdateDisputeRequest request, UUID userId) {
        Dispute dispute = loadDispute(processId);

        // Snapshot ANTES da mutacao — auditoria campo a campo (old → new).
        // Estrategia comercial revisada durante o pregao e exatamente o que
        // alguem vai querer rastrear depois: cada alteracao fica registrada.
        BidStrategy oldStrategy   = dispute.getBidStrategy();
        BigDecimal oldMargin      = dispute.getTargetMargin();
        BigDecimal oldTargetSale  = dispute.getTargetSalePrice();
        BigDecimal oldMinimumSale = dispute.getMinimumSalePrice();
        BigDecimal oldExpected    = dispute.getExpectedProfit();

        dispute.updateStrategy(
                request.bidStrategy(),
                request.targetMargin(),
                request.targetSalePrice(),
                request.minimumSalePrice(),
                request.strategyNotes()
        );
        disputeRepository.save(dispute);

        UUID disputeId = dispute.getId();
        auditEnum(disputeId, "bidStrategy", oldStrategy, dispute.getBidStrategy(), userId);
        auditMoney(disputeId, "targetMargin", oldMargin, dispute.getTargetMargin(), userId);
        auditMoney(disputeId, "targetSalePrice", oldTargetSale, dispute.getTargetSalePrice(), userId);
        auditMoney(disputeId, "minimumSalePrice", oldMinimumSale, dispute.getMinimumSalePrice(), userId);
        auditMoney(disputeId, "expectedProfit", oldExpected, dispute.getExpectedProfit(), userId);

        return DisputeResponse.from(dispute);
    }

    /**
     * Marca o processo como VENCEDOR: IN_DISPUTE → WINNER. Encerra a disputa.
     * O resultado pertence ao PROCESSO — a disputa e apenas uma etapa do workflow.
     */
    @Transactional
    public ProcessResponse markAsWinner(UUID processId, UUID userId) {
        return concludeDispute(processId, userId, true);
    }

    /**
     * Marca o processo como PERDEDOR: IN_DISPUTE → LOSER. Encerra a disputa.
     */
    @Transactional
    public ProcessResponse markAsLoser(UUID processId, UUID userId) {
        return concludeDispute(processId, userId, false);
    }

    @Transactional(readOnly = true)
    public DisputeResponse getDispute(UUID processId) {
        return DisputeResponse.from(loadDispute(processId));
    }

    // -------------------------------------------------------------------------

    private ProcessResponse concludeDispute(UUID processId, UUID userId, boolean winner) {
        ProcurementProcess process = loadProcess(processId);
        String oldStatus = process.getStatus().name();

        Dispute dispute = loadDispute(processId);

        if (winner) {
            process.markAsWinner(); // valida IN_DISPUTE → WINNER
        } else {
            process.markAsLoser();  // valida IN_DISPUTE → LOSER
        }
        dispute.conclude();

        processRepository.save(process);
        disputeRepository.save(dispute);

        String newStatus = winner ? "WINNER" : "LOSER";
        ProcessEventType event = winner ? ProcessEventType.MARKED_AS_WINNER : ProcessEventType.MARKED_AS_LOSER;
        String message = winner ? "Processo marcado como vencedor" : "Processo marcado como perdedor";

        auditService.record("ProcurementProcess", processId, "status", oldStatus, newStatus, userId);
        timelineService.record(processId, event, message, userId);

        return ProcessResponse.from(process);
    }

    /** Audita campo monetario apenas se o valor mudou (compara por valor, nao por escala). */
    private void auditMoney(UUID disputeId, String field, BigDecimal oldVal, BigDecimal newVal, UUID userId) {
        boolean changed = (oldVal == null) != (newVal == null)
                || (oldVal != null && oldVal.compareTo(newVal) != 0);
        if (changed) {
            auditService.record("Dispute", disputeId, field,
                    oldVal == null ? null : oldVal.toPlainString(),
                    newVal == null ? null : newVal.toPlainString(), userId);
        }
    }

    /** Audita campo enum (estrategia de lance) apenas se mudou. */
    private void auditEnum(UUID disputeId, String field, BidStrategy oldVal, BidStrategy newVal, UUID userId) {
        if (oldVal != newVal) {
            auditService.record("Dispute", disputeId, field,
                    oldVal == null ? null : oldVal.name(),
                    newVal == null ? null : newVal.name(), userId);
        }
    }

    private ProcurementProcess loadProcess(UUID processId) {
        return processRepository.findById(processId)
                .orElseThrow(() -> new ResourceNotFoundException("Processo não encontrado: " + processId));
    }

    private Dispute loadDispute(UUID processId) {
        return disputeRepository.findByProcessId(processId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Disputa não encontrada para o processo: " + processId));
    }
}
