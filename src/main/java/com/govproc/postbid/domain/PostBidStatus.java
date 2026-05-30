package com.govproc.postbid.domain;

/**
 * Ciclo de vida da fase POS-DISPUTA — encapsulado neste bounded context,
 * fora do {@code ProcessStatus}.
 *
 * Homologacao e adjudicacao sao atos juridicos da autoridade do orgao.
 * O processo precisa saber apenas que entrou na fase pos-disputa (POST_BID);
 * os detalhes do rito ficam aqui, assim como OPEN/CONCLUDED ficam na Disputa.
 */
public enum PostBidStatus {
    PENDING,
    HOMOLOGATED,
    ADJUDICATED,
    COMPLETED
}
