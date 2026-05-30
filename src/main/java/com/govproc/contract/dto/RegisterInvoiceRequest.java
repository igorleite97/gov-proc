package com.govproc.contract.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RegisterInvoiceRequest(

        @NotBlank(message = "Número da fatura é obrigatório")
        String invoiceNumber,

        @NotNull(message = "Valor da fatura é obrigatório")
        @Positive(message = "Valor da fatura deve ser positivo")
        BigDecimal amount,

        LocalDate issuedAt
) {
}
