package com.govproc.analysis.controller;

import com.govproc.analysis.dto.AnalysisResponse;
import com.govproc.analysis.dto.ApproveAnalysisRequest;
import com.govproc.analysis.dto.RejectAnalysisRequest;
import com.govproc.analysis.dto.StartAnalysisRequest;
import com.govproc.analysis.service.AnalysisService;
import com.govproc.auth.domain.User;
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
@RequestMapping("/processes/{processId}/analysis")
@Tag(name = "Analysis", description = "Análise de viabilidade dos processos licitatórios")
public class AnalysisController {

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping("/start")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Iniciar análise",
               description = "Transição CAPTURED → UNDER_ANALYSIS. Cria o registro de análise.")
    public ApiResponse<AnalysisResponse> start(
            @PathVariable UUID processId,
            @RequestBody StartAnalysisRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.ok(
                analysisService.startAnalysis(processId, request, currentUser.getId()),
                "Análise iniciada"
        );
    }

    @PostMapping("/approve")
    @Operation(summary = "Aprovar análise",
               description = "Transição UNDER_ANALYSIS → ANALYSIS_APPROVED.")
    public ApiResponse<AnalysisResponse> approve(
            @PathVariable UUID processId,
            @RequestBody ApproveAnalysisRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.ok(
                analysisService.approveAnalysis(processId, request, currentUser.getId()),
                "Análise aprovada"
        );
    }

    @PostMapping("/reject")
    @Operation(summary = "Reprovar análise",
               description = "Transição UNDER_ANALYSIS → ANALYSIS_REJECTED. Motivo obrigatório.")
    public ApiResponse<AnalysisResponse> reject(
            @PathVariable UUID processId,
            @Valid @RequestBody RejectAnalysisRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.ok(
                analysisService.rejectAnalysis(processId, request, currentUser.getId()),
                "Análise reprovada"
        );
    }

    @GetMapping
    @Operation(summary = "Consultar análise do processo")
    public ApiResponse<AnalysisResponse> getAnalysis(@PathVariable UUID processId) {
        return ApiResponse.ok(analysisService.getAnalysis(processId));
    }
}
