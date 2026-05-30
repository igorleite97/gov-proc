package com.govproc.process.dto;

import java.math.BigDecimal;

/**
 * Indicadores financeiros consolidados.
 *
 * totalQuotedCost: soma do custo das cotações selecionadas (toda proposta cotada).
 * totalExpectedProfit: soma do lucro esperado das disputas (onde há preço alvo).
 * São populações diferentes por natureza — custo abrange mais que disputa.
 */
public record DashboardFinancialResponse(
        BigDecimal totalQuotedCost,
        BigDecimal totalExpectedProfit,
        BigDecimal totalContractValue,
        BigDecimal remainingBalance
) {
}
