package com.govproc.contract.controller;

import com.govproc.auth.domain.User;
import com.govproc.contract.dto.ActivateContractRequest;
import com.govproc.contract.dto.ContractResponse;
import com.govproc.contract.dto.TerminateContractRequest;
import com.govproc.contract.service.ContractService;
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

import java.util.UUID;

@RestController
@RequestMapping("/processes/{processId}/contract")
@Tag(name = "Contract", description = "Gestão contratual — ativação, encerramento, rescisão e vencimento")
public class ContractController {

    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @PostMapping("/activate")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Ativar contrato",
               description = "Transição POST_BID → CONTRACT_ACTIVE. Requer fase pós-disputa COMPLETED.")
    public ApiResponse<ContractResponse> activate(
            @PathVariable UUID processId,
            @Valid @RequestBody ActivateContractRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.ok(
                contractService.activate(processId, request, currentUser.getId()),
                "Contrato ativado"
        );
    }

    @PostMapping("/close")
    @Operation(summary = "Encerrar contrato",
               description = "Contrato ACTIVE → CLOSED e processo CONTRACT_ACTIVE → CLOSED.")
    public ApiResponse<ContractResponse> close(
            @PathVariable UUID processId,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.ok(
                contractService.close(processId, currentUser.getId()),
                "Contrato encerrado"
        );
    }

    @PostMapping("/terminate")
    @Operation(summary = "Rescindir contrato",
               description = "Contrato ACTIVE → TERMINATED e processo → CLOSED. Motivo obrigatório.")
    public ApiResponse<ContractResponse> terminate(
            @PathVariable UUID processId,
            @Valid @RequestBody TerminateContractRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.ok(
                contractService.terminate(processId, request, currentUser.getId()),
                "Contrato rescindido"
        );
    }

    @PostMapping("/expire")
    @Operation(summary = "Marcar contrato como vencido",
               description = "Contrato ACTIVE → EXPIRED e processo → CLOSED.")
    public ApiResponse<ContractResponse> expire(
            @PathVariable UUID processId,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.ok(
                contractService.expire(processId, currentUser.getId()),
                "Contrato marcado como vencido"
        );
    }

    @GetMapping
    @Operation(summary = "Consultar contrato do processo")
    public ApiResponse<ContractResponse> getContract(@PathVariable UUID processId) {
        return ApiResponse.ok(contractService.getContract(processId));
    }
}
