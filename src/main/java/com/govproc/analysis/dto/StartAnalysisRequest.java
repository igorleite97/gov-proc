package com.govproc.analysis.dto;

public record StartAnalysisRequest(
        Boolean sampleRequired,
        Boolean allowsAdhesion,
        Boolean proposalGuarantee,
        String deliveryDeadline,
        String observations
) {
}
