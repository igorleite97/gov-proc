package com.govproc.quotation.controller;

import com.govproc.auth.domain.User;
import com.govproc.process.dto.ProcessResponse;
import com.govproc.quotation.dto.AddQuotationRequest;
import com.govproc.quotation.dto.QuotationResponse;
import com.govproc.quotation.service.QuotationService;
import com.govproc.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/processes/{processId}")
@Tag(name = "Quotation", description = "Fase de cotação — custo real por fornecedor")
public class QuotationController {

    private final QuotationService quotationService;

    public QuotationController(QuotationService quotationService) {
        this.quotationService = quotationService;
    }

    @PostMapping("/quotation/start")
    @Operation(summary = "Iniciar fase de cotação",
               description = "Transição ANALYSIS_APPROVED → IN_QUOTATION.")
    public ApiResponse<ProcessResponse> startQuotation(
            @PathVariable UUID processId,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.ok(
                quotationService.startQuotation(processId, currentUser.getId()),
                "Fase de cotação iniciada"
        );
    }

    @PostMapping("/quotations")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Adicionar cotação",
               description = "Registra custo unitário, frete e quantidade de um fornecedor. Sem markup — isso é Disputa.")
    public ApiResponse<QuotationResponse> addQuotation(
            @PathVariable UUID processId,
            @Valid @RequestBody AddQuotationRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.ok(
                quotationService.addQuotation(processId, request, currentUser.getId()),
                "Cotação adicionada"
        );
    }

    @PutMapping("/quotations/{quotationId}/select")
    @Operation(summary = "Selecionar cotação",
               description = "Marca esta cotação como selecionada e desseleciona as demais atomicamente.")
    public ApiResponse<QuotationResponse> selectQuotation(
            @PathVariable UUID processId,
            @PathVariable UUID quotationId,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.ok(
                quotationService.selectQuotation(processId, quotationId, currentUser.getId()),
                "Cotação selecionada"
        );
    }

    @PostMapping("/quotation/mark-quoted")
    @Operation(summary = "Marcar processo como cotado",
               description = "Transição IN_QUOTATION → QUOTED. Requer ao menos uma cotação selecionada.")
    public ApiResponse<ProcessResponse> markAsQuoted(
            @PathVariable UUID processId,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.ok(
                quotationService.markAsQuoted(processId, currentUser.getId()),
                "Processo marcado como cotado"
        );
    }

    @GetMapping("/quotations")
    @Operation(summary = "Listar cotações do processo",
               description = "Ordenadas por menor custo total.")
    public ApiResponse<List<QuotationResponse>> findAll(@PathVariable UUID processId) {
        return ApiResponse.ok(quotationService.findByProcess(processId));
    }
}
