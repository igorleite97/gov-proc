package com.govproc.dispute.dto;

import com.govproc.dispute.domain.BidStrategy;
import com.govproc.dispute.domain.Dispute;
import com.govproc.dispute.domain.DisputeStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DisputeResponse(
        UUID id,
        UUID processId,
        UUID quotationId,
        BigDecimal quotedCost,
        BigDecimal targetMargin,
        BigDecimal targetSalePrice,
        BigDecimal minimumSalePrice,
        BigDecimal expectedProfit,
        BidStrategy bidStrategy,
        DisputeStatus status,
        String strategyNotes,
        UUID registeredBy,
        Instant createdAt,
        Instant updatedAt
) {

    public static DisputeResponse from(Dispute d) {
        return new DisputeResponse(
                d.getId(),
                d.getProcessId(),
                d.getQuotationId(),
                d.getQuotedCost(),
                d.getTargetMargin(),
                d.getTargetSalePrice(),
                d.getMinimumSalePrice(),
                d.getExpectedProfit(),
                d.getBidStrategy(),
                d.getStatus(),
                d.getStrategyNotes(),
                d.getRegisteredBy(),
                d.getCreatedAt(),
                d.getUpdatedAt()
        );
    }
}
