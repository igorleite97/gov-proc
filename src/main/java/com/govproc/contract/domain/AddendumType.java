package com.govproc.contract.domain;

/**
 * Tipo de termo aditivo contratual.
 *
 * VALUE_* alteram a capacidade financeira (contractValue + remainingBalance).
 * TERM_*  alteram a vigencia (endDate). Nunca os dois ao mesmo tempo.
 */
public enum AddendumType {
    VALUE_INCREASE,   // acrescimo de valor
    VALUE_DECREASE,   // supressao de valor
    TERM_EXTENSION,   // prorrogacao de prazo
    TERM_REDUCTION;   // reducao de prazo

    public boolean isValueChange() {
        return this == VALUE_INCREASE || this == VALUE_DECREASE;
    }

    public boolean isTermChange() {
        return this == TERM_EXTENSION || this == TERM_REDUCTION;
    }
}
