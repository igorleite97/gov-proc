package com.govproc.contract.dto;

import com.govproc.contract.domain.Invoice;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        UUID contractId,
        String invoiceNumber,
        BigDecimal amount,
        LocalDate issuedAt,
        UUID registeredBy,
        Instant createdAt
) {

    public static InvoiceResponse from(Invoice i) {
        return new InvoiceResponse(
                i.getId(),
                i.getContractId(),
                i.getInvoiceNumber(),
                i.getAmount(),
                i.getIssuedAt(),
                i.getRegisteredBy(),
                i.getCreatedAt()
        );
    }
}
