package com.govproc.process.dto;

import java.math.BigDecimal;

/**
 * Indicadores de performance (percentuais com escala 2).
 *
 * winRate + lossRate somam 100 (lossRate derivado como 100 - winRate para evitar
 * desvio de arredondamento). Calculados sobre processos decididos (won + lost).
 */
public record DashboardPerformanceResponse(
        BigDecimal winRate,
        BigDecimal lossRate,
        BigDecimal averageExpectedProfit
) {
}
