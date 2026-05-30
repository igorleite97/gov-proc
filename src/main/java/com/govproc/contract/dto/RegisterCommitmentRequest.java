package com.govproc.contract.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RegisterCommitmentRequest(

        @NotBlank(message = "Número do empenho é obrigatório")
        String commitmentNumber,

        @NotNull(message = "Valor do empenho é obrigatório")
        @Positive(message = "Valor do empenho deve ser positivo")
        BigDecimal amount,

        LocalDate issueDate,

        String notes
) {
}
