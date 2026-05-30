package com.govproc.contract.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ActivateContractRequest(

        @NotBlank(message = "Número do contrato é obrigatório")
        String contractNumber,

        @NotNull(message = "Data de início é obrigatória")
        LocalDate startDate,

        LocalDate endDate,

        @NotNull(message = "Valor do contrato é obrigatório")
        @Positive(message = "Valor do contrato deve ser positivo")
        BigDecimal contractValue
) {
}
