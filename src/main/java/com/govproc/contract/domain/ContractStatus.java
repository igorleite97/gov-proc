package com.govproc.contract.domain;

/**
 * Ciclo de vida do CONTRATO — encapsulado neste bounded context.
 *
 * O processo sabe apenas que esta em CONTRACT_ACTIVE ou CLOSED. O contrato,
 * porem, tem vida propria e estados mais ricos (vencimento, rescisao), assim
 * como Dispute (OPEN/CONCLUDED) e PostBid (PENDING.../COMPLETED).
 *
 * ACTIVE      — contrato vigente
 * EXPIRED     — venceu pelo decurso do prazo (endDate)
 * TERMINATED  — rescindido antecipadamente
 * CLOSED      — encerrado normalmente apos execucao
 */
public enum ContractStatus {
    ACTIVE,
    EXPIRED,
    TERMINATED,
    CLOSED
}
