package com.govproc.shared.exception;

/**
 * Violacao de regra de negocio / dominio.
 *
 * Ex.: tentar uma transicao de status invalida, duplicar processo,
 * reprovar analise sem motivo. Mapeada para HTTP 409 (Conflict) pelo
 * {@link GlobalExceptionHandler}.
 *
 * Usada pelo dominio para proteger invariantes — nao confundir com
 * erros de validacao de entrada (Bean Validation -> 400).
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
