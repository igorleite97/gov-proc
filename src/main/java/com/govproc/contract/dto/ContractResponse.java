package com.govproc.contract.dto;

import com.govproc.contract.domain.Contract;
import com.govproc.contract.domain.ContractStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ContractResponse(
        UUID id,
        UUID processId,
        String contractNumber,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal contractValue,
        BigDecimal remainingBalance,
        ContractStatus status,
        UUID registeredBy,
        Instant createdAt,
        Instant updatedAt
) {

    public static ContractResponse from(Contract c) {
        return new ContractResponse(
                c.getId(),
                c.getProcessId(),
                c.getContractNumber(),
                c.getStartDate(),
                c.getEndDate(),
                c.getContractValue(),
                c.getRemainingBalance(),
                c.getStatus(),
                c.getRegisteredBy(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
