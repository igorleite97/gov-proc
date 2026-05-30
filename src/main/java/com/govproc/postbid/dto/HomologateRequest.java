package com.govproc.postbid.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record HomologateRequest(

        @NotBlank(message = "Número da homologação é obrigatório")
        String homologationNumber,

        LocalDate homologationDate,

        String notes
) {
}
