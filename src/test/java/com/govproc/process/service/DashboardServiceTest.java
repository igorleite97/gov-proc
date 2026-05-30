package com.govproc.process.service;

import com.govproc.process.domain.ProcessStatus;
import com.govproc.process.dto.DashboardFinancialResponse;
import com.govproc.process.dto.DashboardPerformanceResponse;
import com.govproc.process.dto.DashboardSummaryResponse;
import com.govproc.process.repository.DashboardRepository;
import com.govproc.process.repository.DashboardRepository.StatusCount;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock DashboardRepository dashboardRepository;

    @InjectMocks DashboardService dashboardService;

    private StatusCount sc(ProcessStatus status, long total) {
        return new StatusCount() {
            public ProcessStatus getStatus() { return status; }
            public long getTotal() { return total; }
        };
    }

    /** Distribuição que reproduz o exemplo: won=52, lost=34. */
    private List<StatusCount> sampleCounts() {
        return List.of(
                sc(ProcessStatus.CAPTURED, 12),
                sc(ProcessStatus.QUOTED, 25),
                sc(ProcessStatus.IN_DISPUTE, 8),
                sc(ProcessStatus.WINNER, 10),
                sc(ProcessStatus.POST_BID, 5),
                sc(ProcessStatus.CONTRACT_ACTIVE, 16),
                sc(ProcessStatus.CLOSED, 21),
                sc(ProcessStatus.LOSER, 34)
        );
    }

    @Test
    void getSummary_deveContarRamoVitoriosoInteiro() {
        when(dashboardRepository.countByStatus()).thenReturn(sampleCounts());

        DashboardSummaryResponse r = dashboardService.getSummary();

        assertThat(r.totalProcesses()).isEqualTo(131); // 12+25+8+10+5+16+21+34
        assertThat(r.capturedProcesses()).isEqualTo(12);
        assertThat(r.quotedProcesses()).isEqualTo(25);
        assertThat(r.inDisputeProcesses()).isEqualTo(8);
        // won = WINNER + POST_BID + CONTRACT_ACTIVE + CLOSED = 10+5+16+21 = 52
        assertThat(r.wonProcesses()).isEqualTo(52);
        assertThat(r.lostProcesses()).isEqualTo(34);
        assertThat(r.activeContracts()).isEqualTo(16);
    }

    @Test
    void getFinancial_deveRetornarZero_quandoSomasSaoNull() {
        // sem dados: SUM retorna null → deve virar ZERO, nunca null
        when(dashboardRepository.sumSelectedQuotationCost()).thenReturn(null);
        when(dashboardRepository.sumExpectedProfit()).thenReturn(null);
        when(dashboardRepository.sumContractValue()).thenReturn(null);
        when(dashboardRepository.sumRemainingBalance()).thenReturn(null);

        DashboardFinancialResponse r = dashboardService.getFinancial();

        assertThat(r.totalQuotedCost()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.totalExpectedProfit()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.totalContractValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.remainingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getFinancial_deveRepassarSomas() {
        when(dashboardRepository.sumSelectedQuotationCost()).thenReturn(new BigDecimal("1200000.0000"));
        when(dashboardRepository.sumExpectedProfit()).thenReturn(new BigDecimal("380000.0000"));
        when(dashboardRepository.sumContractValue()).thenReturn(new BigDecimal("8500000.0000"));
        when(dashboardRepository.sumRemainingBalance()).thenReturn(new BigDecimal("4200000.0000"));

        DashboardFinancialResponse r = dashboardService.getFinancial();

        assertThat(r.totalQuotedCost()).isEqualByComparingTo(new BigDecimal("1200000.00"));
        assertThat(r.totalContractValue()).isEqualByComparingTo(new BigDecimal("8500000.00"));
        assertThat(r.remainingBalance()).isEqualByComparingTo(new BigDecimal("4200000.00"));
    }

    @Test
    void getPerformance_deveCalcularTaxasEMedia() {
        when(dashboardRepository.countByStatus()).thenReturn(sampleCounts());
        when(dashboardRepository.sumExpectedProfit()).thenReturn(new BigDecimal("72501.50"));
        when(dashboardRepository.countDisputesWithExpectedProfit()).thenReturn(10L);

        DashboardPerformanceResponse r = dashboardService.getPerformance();

        // won=52, lost=34, decided=86 → 52/86 = 60.4651 → 60.47
        assertThat(r.winRate()).isEqualByComparingTo(new BigDecimal("60.47"));
        assertThat(r.lossRate()).isEqualByComparingTo(new BigDecimal("39.53")); // 100 - 60.47
        assertThat(r.winRate().add(r.lossRate())).isEqualByComparingTo(new BigDecimal("100.00"));
        // média = 72501.50 / 10 = 7250.15
        assertThat(r.averageExpectedProfit()).isEqualByComparingTo(new BigDecimal("7250.15"));
    }

    @Test
    void getPerformance_deveRetornarZero_quandoNaoHaProcessosDecididos() {
        // só processos em fases iniciais: nenhum won/lost
        when(dashboardRepository.countByStatus()).thenReturn(List.of(
                sc(ProcessStatus.CAPTURED, 5),
                sc(ProcessStatus.IN_QUOTATION, 3)));
        // profitCount == 0 → sumExpectedProfit() nem é chamado (curto-circuito)
        when(dashboardRepository.countDisputesWithExpectedProfit()).thenReturn(0L);

        DashboardPerformanceResponse r = dashboardService.getPerformance();

        assertThat(r.winRate()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.lossRate()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.averageExpectedProfit()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
