package com.govproc.shared.exception;

/**
 * Recurso inexistente. Mapeada para HTTP 404 pelo
 * {@link GlobalExceptionHandler}.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
