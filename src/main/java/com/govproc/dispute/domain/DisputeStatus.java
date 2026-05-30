package com.govproc.dispute.domain;

/**
 * Ciclo de vida da DISPUTA em si — ortogonal ao resultado do processo.
 *
 * O resultado (WINNER / LOSER) pertence ao {@code ProcurementProcess},
 * pois representa o desfecho do processo licitatorio como um todo.
 * Aqui registramos apenas se a atividade de disputa ainda esta em
 * andamento (OPEN) ou ja foi encerrada (CONCLUDED).
 */
public enum DisputeStatus {
    OPEN,
    CONCLUDED
}
