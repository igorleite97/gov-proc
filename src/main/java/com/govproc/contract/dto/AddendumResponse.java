package com.govproc.contract.dto;

import com.govproc.contract.domain.Addendum;
import com.govproc.contract.domain.AddendumType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AddendumResponse(
        UUID id,
        UUID contractId,
        String addendumNumber,
        AddendumType type,
        BigDecimal valueChange,
        LocalDate newEndDate,
        String reason,
        LocalDate signedAt,
        UUID registeredBy,
        Instant createdAt
) {

    public static AddendumResponse from(Addendum a) {
        return new AddendumResponse(
                a.getId(),
                a.getContractId(),
                a.getAddendumNumber(),
                a.getType(),
                a.getValueChange(),
                a.getNewEndDate(),
                a.getReason(),
                a.getSignedAt(),
                a.getRegisteredBy(),
                a.getCreatedAt()
        );
    }
}
