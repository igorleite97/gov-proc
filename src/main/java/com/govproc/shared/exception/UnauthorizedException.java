package com.govproc.shared.exception;

/**
 * Acesso negado por credenciais invalidas ou usuario inativo.
 * Mapeada para HTTP 401 pelo {@link GlobalExceptionHandler}.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
