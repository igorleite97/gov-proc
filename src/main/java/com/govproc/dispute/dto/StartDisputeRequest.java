package com.govproc.dispute.dto;

import com.govproc.dispute.domain.BidStrategy;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Inicia a disputa (QUOTED → IN_DISPUTE) e registra a estrategia comercial.
 *
 * NOTA: quotedCost NAO entra aqui — e capturado do custo total da cotacao
 * selecionada no servidor. Custo nasce na Cotacao; estrategia nasce na Disputa.
 */
public record StartDisputeRequest(

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
