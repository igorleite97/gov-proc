package com.govproc.dispute.dto;

import com.govproc.dispute.domain.BidStrategy;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Revisa a estrategia comercial enquanto a disputa esta OPEN.
 */
public record UpdateDisputeRequest(

        BidStrategy bidStrategy,

        @PositiveOrZero(message = "Margem alvo não pode ser negativa")
        BigDecimal targetMargin,

        @PositiveOrZero(message = "Preço de venda alvo não pode ser negativo")
        BigDecimal targetSalePrice,

        @PositiveOrZero(message = "Preço mínimo não pode ser negativo")
        BigDecimal minimumSalePrice,

        String strategyNotes
) {
}
