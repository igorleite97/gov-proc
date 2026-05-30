package com.govproc.process.service;

import com.govproc.process.domain.ProcessStatus;
import com.govproc.process.dto.DashboardFinancialResponse;
import com.govproc.process.dto.DashboardPerformanceResponse;
import com.govproc.process.dto.DashboardSummaryResponse;
import com.govproc.process.repository.DashboardRepository;
import com.govproc.process.repository.DashboardRepository.StatusCount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Read model do Dashboard: apenas leitura e agregacao, sem mutacao de estado,
 * sem auditoria, sem timeline. Toda a "inteligencia" e derivada dos dados ja
 * existentes via consultas agregadas no {@link DashboardRepository}.
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int PERCENT_SCALE = 2;

    /** Status que representam o ramo vitorioso (WINNER nao e terminal). */
    private static final List<ProcessStatus> WON_STATUSES = List.of(
            ProcessStatus.WINNER, ProcessStatus.POST_BID,
            ProcessStatus.CONTRACT_ACTIVE, ProcessStatus.CLOSED);

    private final DashboardRepository dashboardRepository;

    public DashboardService(DashboardRepository dashboardRepository) {
        this.dashboardRepository = dashboardRepository;
    }

    public DashboardSummaryResponse getSummary() {
        Map<ProcessStatus, Long> counts = statusCounts();

        long total = counts.values().stream().mapToLong(Long::longValue).sum();

        return new DashboardSummaryResponse(
                total,
                count(counts, ProcessStatus.CAPTURED),
                count(counts, ProcessStatus.QUOTED),
                count(counts, ProcessStatus.IN_DISPUTE),
                wonCount(counts),
                count(counts, ProcessStatus.LOSER),
                count(counts, ProcessStatus.CONTRACT_ACTIVE)
        );
    }

    public DashboardFinancialResponse getFinancial() {
        return new DashboardFinancialResponse(
                nz(dashboardRepository.sumSelectedQuotationCost()),
                nz(dashboardRepository.sumExpectedProfit()),
                nz(dashboardRepository.sumContractValue()),
                nz(dashboardRepository.sumRemainingBalance())
        );
    }

    public DashboardPerformanceResponse getPerformance() {
        Map<ProcessStatus, Long> counts = statusCounts();
        long won = wonCount(counts);
        long lost = count(counts, ProcessStatus.LOSER);
        long decided = won + lost;

        BigDecimal winRate = decided == 0
                ? BigDecimal.ZERO.setScale(PERCENT_SCALE)
                : BigDecimal.valueOf(won).multiply(HUNDRED)
                        .divide(BigDecimal.valueOf(decided), PERCENT_SCALE, RoundingMode.HALF_UP);
        BigDecimal lossRate = decided == 0
                ? BigDecimal.ZERO.setScale(PERCENT_SCALE)
                : HUNDRED.setScale(PERCENT_SCALE).subtract(winRate);

        long profitCount = dashboardRepository.countDisputesWithExpectedProfit();
        BigDecimal averageExpectedProfit = profitCount == 0
                ? BigDecimal.ZERO.setScale(PERCENT_SCALE)
                : nz(dashboardRepository.sumExpectedProfit())
                        .divide(BigDecimal.valueOf(profitCount), PERCENT_SCALE, RoundingMode.HALF_UP);

        return new DashboardPerformanceResponse(winRate, lossRate, averageExpectedProfit);
    }

    // -------------------------------------------------------------------------

    private Map<ProcessStatus, Long> statusCounts() {
        Map<ProcessStatus, Long> map = new EnumMap<>(ProcessStatus.class);
        for (StatusCount sc : dashboardRepository.countByStatus()) {
            map.put(sc.getStatus(), sc.getTotal());
        }
        return map;
    }

    private long wonCount(Map<ProcessStatus, Long> counts) {
        return WON_STATUSES.stream().mapToLong(s -> count(counts, s)).sum();
    }

    private long count(Map<ProcessStatus, Long> counts, ProcessStatus status) {
        return counts.getOrDefault(status, 0L);
    }

    private BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
