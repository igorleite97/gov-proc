package com.govproc.supplier.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSupplierRequest(
        @NotBlank(message = "Razão social é obrigatória")
        @Size(max = 255)
        String companyName,

        String tradeName,

        @NotBlank(message = "CNPJ é obrigatório")
        @Size(max = 18, message = "CNPJ deve ter no máximo 18 caracteres")
        String document,

        @Email(message = "Formato de email inválido")
        String email,

        @Size(max = 20)
        String phone,

        @Size(max = 100)
        String segment
) {
}
