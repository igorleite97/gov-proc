package com.govproc.process.controller;

import com.govproc.process.dto.DashboardFinancialResponse;
import com.govproc.process.dto.DashboardPerformanceResponse;
import com.govproc.process.dto.DashboardSummaryResponse;
import com.govproc.process.service.DashboardService;
import com.govproc.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dashboard/KPIs — projeção de leitura (CQRS-lite) sobre os dados existentes.
 * Sem estado, sem persistência própria, sem mutação. Apenas agregações.
 */
@RestController
@RequestMapping("/dashboard")
@Tag(name = "Dashboard", description = "KPIs operacionais, financeiros e de performance (somente leitura)")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Resumo operacional",
               description = "Contagem de processos por fase do pipeline e contratos ativos.")
    public ApiResponse<DashboardSummaryResponse> summary() {
        return ApiResponse.ok(dashboardService.getSummary());
    }

    @GetMapping("/financial")
    @Operation(summary = "Indicadores financeiros",
               description = "Custo cotado, lucro esperado, valor contratado e saldo remanescente.")
    public ApiResponse<DashboardFinancialResponse> financial() {
        return ApiResponse.ok(dashboardService.getFinancial());
    }

    @GetMapping("/performance")
    @Operation(summary = "Indicadores de performance",
               description = "Taxa de vitória, taxa de derrota e lucro esperado médio.")
    public ApiResponse<DashboardPerformanceResponse> performance() {
        return ApiResponse.ok(dashboardService.getPerformance());
    }
}
