package com.govproc.analysis.dto;

import com.govproc.analysis.domain.AnalysisDecision;
import com.govproc.analysis.domain.ProcessAnalysis;

import java.time.Instant;
import java.util.UUID;

public record AnalysisResponse(
        UUID id,
        UUID processId,
        Boolean sampleRequired,
        Boolean allowsAdhesion,
        Boolean proposalGuarantee,
        String deliveryDeadline,
        AnalysisDecision decision,
        String rejectionReason,
        String observations,
        UUID analyzedBy,
        Instant createdAt,
        Instant updatedAt
) {

    public static AnalysisResponse from(ProcessAnalysis a) {
        return new AnalysisResponse(
                a.getId(),
                a.getProcessId(),
                a.getSampleRequired(),
                a.getAllowsAdhesion(),
                a.getProposalGuarantee(),
                a.getDeliveryDeadline(),
                a.getDecision(),
                a.getRejectionReason(),
                a.getObservations(),
                a.getAnalyzedBy(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}
