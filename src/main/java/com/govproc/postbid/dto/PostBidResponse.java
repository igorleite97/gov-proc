package com.govproc.postbid.dto;

import com.govproc.postbid.domain.PostBid;
import com.govproc.postbid.domain.PostBidStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PostBidResponse(
        UUID id,
        UUID processId,
        PostBidStatus status,
        String homologationNumber,
        LocalDate homologationDate,
        String adjudicationNumber,
        LocalDate adjudicationDate,
        String notes,
        UUID registeredBy,
        Instant createdAt,
        Instant updatedAt
) {

    public static PostBidResponse from(PostBid p) {
        return new PostBidResponse(
                p.getId(),
                p.getProcessId(),
                p.getStatus(),
                p.getHomologationNumber(),
                p.getHomologationDate(),
                p.getAdjudicationNumber(),
                p.getAdjudicationDate(),
                p.getNotes(),
                p.getRegisteredBy(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
