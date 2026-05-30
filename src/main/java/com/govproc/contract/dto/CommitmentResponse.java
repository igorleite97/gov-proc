package com.govproc.contract.dto;

import com.govproc.contract.domain.Commitment;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CommitmentResponse(
        UUID id,
        UUID contractId,
        String commitmentNumber,
        BigDecimal amount,
        LocalDate issueDate,
        String notes,
        UUID registeredBy,
        Instant createdAt
) {

    public static CommitmentResponse from(Commitment c) {
        return new CommitmentResponse(
                c.getId(),
                c.getContractId(),
                c.getCommitmentNumber(),
                c.getAmount(),
                c.getIssueDate(),
                c.getNotes(),
                c.getRegisteredBy(),
                c.getCreatedAt()
        );
    }
}
