package com.govproc.process.dto;

/**
 * Resumo operacional do pipeline licitatório.
 *
 * wonProcesses conta o ramo vitorioso inteiro (WINNER, POST_BID, CONTRACT_ACTIVE,
 * CLOSED), pois WINNER não é estado terminal — o processo vencido avança.
 */
public record DashboardSummaryResponse(
        long totalProcesses,
        long capturedProcesses,
        long quotedProcesses,
        long inDisputeProcesses,
        long wonProcesses,
        long lostProcesses,
        long activeContracts
) {
}
