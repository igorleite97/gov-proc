package com.govproc.quotation.dto;

import com.govproc.quotation.domain.Quotation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record QuotationResponse(
        UUID id,
        UUID processId,
        UUID supplierId,
        String productDescription,
        BigDecimal unitCost,
        BigDecimal shippingCost,
        Integer quantity,
        BigDecimal totalCost,
        String manufacturer,
        String brand,
        String deliveryDays,
        boolean selected,
        String observations,
        UUID registeredBy,
        Instant createdAt
) {

    public static QuotationResponse from(Quotation q) {
        return new QuotationResponse(
                q.getId(),
                q.getProcessId(),
                q.getSupplierId(),
                q.getProductDescription(),
                q.getUnitCost(),
                q.getShippingCost(),
                q.getQuantity(),
                q.getTotalCost(),
                q.getManufacturer(),
                q.getBrand(),
                q.getDeliveryDays(),
                q.isSelected(),
                q.getObservations(),
                q.getRegisteredBy(),
                q.getCreatedAt()
        );
    }
}
