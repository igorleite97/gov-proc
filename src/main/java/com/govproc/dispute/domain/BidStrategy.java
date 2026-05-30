package com.govproc.dispute.domain;

/**
 * Estrategia de lance adotada na disputa.
 *
 * Pertence exclusivamente ao contexto de Disputa — e uma decisao
 * comercial/estrategica, jamais um dado de custo (que vive na Cotacao).
 */
public enum BidStrategy {
    CONSERVATIVE,
    MODERATE,
    AGGRESSIVE
}
