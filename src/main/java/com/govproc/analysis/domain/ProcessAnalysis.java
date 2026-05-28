package com.govproc.analysis.domain;

import com.govproc.shared.domain.BaseEntity;
import com.govproc.shared.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Registro da analise operacional de um processo licitatorio.
 *
 * Criada ao iniciar a analise (decisao = PENDING) e atualizada quando
 * o analista aprova ou reprova. Contem o contexto de viabilidade tecnica
 * que nao pertence ao ProcurementProcess em si.
 *
 * Uma analise por processo (UNIQUE em process_id).
 */
@Entity
@Table(name = "process_analyses")
public class ProcessAnalysis extends BaseEntity {

    @Column(name = "process_id", nullable = false, unique = true)
    private UUID processId;

    @Column(name = "sample_required")
    private Boolean sampleRequired;

    @Column(name = "allows_adhesion")
    private Boolean allowsAdhesion;

    @Column(name = "proposal_guarantee")
    private Boolean proposalGuarantee;

    @Column(name = "delivery_deadline", length = 200)
    private String deliveryDeadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnalysisDecision decision;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(columnDefinition = "TEXT")
    private String observations;

    @Column(name = "analyzed_by", nullable = false)
    private UUID analyzedBy;

    protected ProcessAnalysis() { }

    public ProcessAnalysis(UUID processId, Boolean sampleRequired, Boolean allowsAdhesion,
                           Boolean proposalGuarantee, String deliveryDeadline,
                           String observations, UUID analyzedBy) {
        this.processId = processId;
        this.sampleRequired = sampleRequired;
        this.allowsAdhesion = allowsAdhesion;
        this.proposalGuarantee = proposalGuarantee;
        this.deliveryDeadline = deliveryDeadline;
        this.observations = observations;
        this.analyzedBy = analyzedBy;
        this.decision = AnalysisDecision.PENDING;
    }

    // -------------------------------------------------------------------------
    // Metodos de decisao — encapsulam a mutacao com validacao interna.
    // -------------------------------------------------------------------------

    public void approve(String finalObservations) {
        this.decision = AnalysisDecision.APPROVED;
        if (finalObservations != null && !finalObservations.isBlank()) {
            this.observations = finalObservations;
        }
    }

    public void reject(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("Motivo da reprovação é obrigatório");
        }
        this.decision = AnalysisDecision.REJECTED;
        this.rejectionReason = reason;
    }

    public UUID getProcessId()           { return processId; }
    public Boolean getSampleRequired()   { return sampleRequired; }
    public Boolean getAllowsAdhesion()   { return allowsAdhesion; }
    public Boolean getProposalGuarantee() { return proposalGuarantee; }
    public String getDeliveryDeadline()  { return deliveryDeadline; }
    public AnalysisDecision getDecision() { return decision; }
    public String getRejectionReason()   { return rejectionReason; }
    public String getObservations()      { return observations; }
    public UUID getAnalyzedBy()          { return analyzedBy; }
}
