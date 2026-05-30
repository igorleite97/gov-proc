package com.govproc.dispute.controller;

import com.govproc.auth.domain.User;
import com.govproc.dispute.dto.DisputeResponse;
import com.govproc.dispute.dto.StartDisputeRequest;
import com.govproc.dispute.dto.UpdateDisputeRequest;
import com.govproc.dispute.service.DisputeService;
import com.govproc.process.dto.ProcessResponse;
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

import java.util.UUID;

@RestController
@RequestMapping("/processes/{processId}/dispute")
@Tag(name = "Dispute", description = "Fase de disputa — estratégia comercial (margem, preço, lance)")
public class DisputeController {

    private final DisputeService disputeService;

    public DisputeController(DisputeService disputeService) {
        this.disputeService = disputeService;
    }

    @PostMapping("/start")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Iniciar disputa",
               description = "Transição QUOTED → IN_DISPUTE. O custo cotado é capturado da cotação selecionada.")
    public ApiResponse<DisputeResponse> start(
            @PathVariable UUID processId,
            @Valid @RequestBody StartDisputeRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.ok(
                disputeService.startDispute(processId, request, currentUser.getId()),
                "Disputa iniciada"
        );
    }

    @PutMapping
    @Operation(summary = "Atualizar estratégia da disputa",
               description = "Revisa margem, preço de venda alvo, preço mínimo e estratégia de lance. Disputa deve estar OPEN.")
    public ApiResponse<DisputeResponse> update(
            @PathVariable UUID processId,
            @Valid @RequestBody UpdateDisputeRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.ok(
                disputeService.updateDispute(processId, request, currentUser.getId()),
                "Estratégia atualizada"
        );
    }

    @PostMapping("/winner")
    @Operation(summary = "Marcar como vencedor",
               description = "Transição IN_DISPUTE → WINNER. Encerra a disputa. O resultado pertence ao processo.")
    public ApiResponse<ProcessResponse> markAsWinner(
            @PathVariable UUID processId,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.ok(
                disputeService.markAsWinner(processId, currentUser.getId()),
                "Processo marcado como vencedor"
        );
    }

    @PostMapping("/loser")
    @Operation(summary = "Marcar como perdedor",
               description = "Transição IN_DISPUTE → LOSER. Encerra a disputa. O resultado pertence ao processo.")
    public ApiResponse<ProcessResponse> markAsLoser(
            @PathVariable UUID processId,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.ok(
                disputeService.markAsLoser(processId, currentUser.getId()),
                "Processo marcado como perdedor"
        );
    }

    @GetMapping
    @Operation(summary = "Consultar disputa do processo")
    public ApiResponse<DisputeResponse> getDispute(@PathVariable UUID processId) {
        return ApiResponse.ok(disputeService.getDispute(processId));
    }
}
