package com.govproc.supplier.dto;

import com.govproc.supplier.domain.Supplier;

import java.time.Instant;
import java.util.UUID;

public record SupplierResponse(
        UUID id,
        String companyName,
        String tradeName,
        String document,
        String email,
        String phone,
        String segment,
        boolean active,
        Instant createdAt
) {

    public static SupplierResponse from(Supplier s) {
        return new SupplierResponse(
                s.getId(),
                s.getCompanyName(),
                s.getTradeName(),
                s.getDocument(),
                s.getEmail(),
                s.getPhone(),
                s.getSegment(),
                s.isActive(),
                s.getCreatedAt()
        );
    }
}
