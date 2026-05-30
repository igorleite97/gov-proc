package com.govproc.contract.dto;

import com.govproc.contract.domain.AddendumType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Para tipos VALUE_*, informe valueChange (positivo).
 * Para tipos TERM_*, informe newEndDate. A raiz Contract valida a coerência.
 */
public record ApplyAddendumRequest(

        @NotBlank(message = "Número do aditivo é obrigatório")
        String addendumNumber,

        @NotNull(message = "Tipo do aditivo é obrigatório")
        AddendumType type,

        @Positive(message = "Valor do aditivo deve ser positivo")
        BigDecimal valueChange,

        LocalDate newEndDate,

        String reason,

        LocalDate signedAt
) {
}
