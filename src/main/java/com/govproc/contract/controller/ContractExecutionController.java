package com.govproc.contract.controller;

import com.govproc.auth.domain.User;
import com.govproc.contract.dto.AddendumResponse;
import com.govproc.contract.dto.ApplyAddendumRequest;
import com.govproc.contract.dto.CommitmentResponse;
import com.govproc.contract.dto.InvoiceResponse;
import com.govproc.contract.dto.RegisterCommitmentRequest;
import com.govproc.contract.dto.RegisterInvoiceRequest;
import com.govproc.contract.service.ContractExecutionService;
import com.govproc.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/processes/{processId}/contract")
@Tag(name = "Contract Execution", description = "Execução do contrato — empenhos, faturas e aditivos")
public class ContractExecutionController {

    private final ContractExecutionService executionService;

    public ContractExecutionController(ContractExecutionService executionService) {
        this.executionService = executionService;
    }

    // ---------------------------------------------------------------- Empenho

    @PostMapping("/commitments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar empenho",
               description = "Reserva orçamentária. Valida e reduz o saldo do contrato (remainingBalance).")
    public ApiResponse<CommitmentResponse> registerCommitment(
            @PathVariable UUID processId,
            @Valid @RequestBody RegisterCommitmentRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.ok(
                executionService.registerCommitment(processId, request, currentUser.getId()),
                "Empenho registrado"
        );
    }

    @GetMapping("/commitments")
    @Operation(summary = "Listar empenhos do contrato")
    public ApiResponse<List<CommitmentResponse>> listCommitments(@PathVariable UUID processId) {
        return ApiResponse.ok(executionService.listCommitments(processId));
    }

    // ----------------------------------------------------------------- Fatura

    @PostMapping("/invoices")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar fatura",
               description = "Registro de execução/liquidação. NÃO altera o saldo (já reservado no empenho).")
    public ApiResponse<InvoiceResponse> registerInvoice(
            @PathVariable UUID processId,
            @Valid @RequestBody RegisterInvoiceRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.ok(
                executionService.registerInvoice(processId, request, currentUser.getId()),
                "Fatura registrada"
        );
    }

    @GetMapping("/invoices")
    @Operation(summary = "Listar faturas do contrato")
    public ApiResponse<List<InvoiceResponse>> listInvoices(@PathVariable UUID processId) {
        return ApiResponse.ok(executionService.listInvoices(processId));
    }

    // ---------------------------------------------------------------- Aditivo

    @PostMapping("/addenda")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Aplicar aditivo",
               description = "Altera a capacidade do contrato: valor (VALUE_*) ou prazo (TERM_*).")
    public ApiResponse<AddendumResponse> applyAddendum(
            @PathVariable UUID processId,
            @Valid @RequestBody ApplyAddendumRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.ok(
                executionService.applyAddendum(processId, request, currentUser.getId()),
                "Aditivo aplicado"
        );
    }

    @GetMapping("/addenda")
    @Operation(summary = "Listar aditivos do contrato")
    public ApiResponse<List<AddendumResponse>> listAddenda(@PathVariable UUID processId) {
        return ApiResponse.ok(executionService.listAddenda(processId));
    }
}
