package com.govproc.postbid.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record AdjudicateRequest(

        @NotBlank(message = "Número da adjudicação é obrigatório")
        String adjudicationNumber,

        LocalDate adjudicationDate,

        String notes
) {
}
