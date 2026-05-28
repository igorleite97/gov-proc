package com.govproc.shared.web;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Envelope padrao para respostas de sucesso da API.
 *
 * Mantem o formato de resposta consistente em todos os endpoints,
 * o que facilita o consumo por clientes e a documentacao.
 *
 * @param success indica sucesso (sempre true neste envelope; erros usam ErrorResponse)
 * @param message mensagem opcional legivel
 * @param data    payload da resposta
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        Instant timestamp
) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, null, data, Instant.now());
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, message, data, Instant.now());
    }
}
