package com.govproc.quotation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record AddQuotationRequest(
        @NotNull(message = "Fornecedor é obrigatório")
        UUID supplierId,

        @NotBlank(message = "Descrição do produto é obrigatória")
        String productDescription,

        @NotNull(message = "Custo unitário é obrigatório")
        @Positive(message = "Custo unitário deve ser positivo")
        BigDecimal unitCost,

        @NotNull(message = "Quantidade é obrigatória")
        @Positive(message = "Quantidade deve ser positiva")
        Integer quantity,

        BigDecimal shippingCost,
        String manufacturer,
        String brand,
        String deliveryDays,
        String observations
) {
}
