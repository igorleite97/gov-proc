package com.govproc.process.repository;

import com.govproc.process.domain.ProcessStatus;
import com.govproc.process.domain.ProcurementProcess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Read model (CQRS-lite) para o Dashboard/KPIs.
 *
 * NAO e um novo bounded context: nao possui entidade, tabela, estado ou ciclo
 * de vida. E apenas uma projecao de LEITURA sobre dados ja existentes.
 *
 * Tipado em {@link ProcurementProcess}, mas as @Query JPQL referenciam outras
 * entidades (Quotation, Dispute, Contract) por NOME — sem import/acoplamento de
 * compilacao com os demais modulos. So agregacoes (COUNT/SUM).
 */
public interface DashboardRepository extends JpaRepository<ProcurementProcess, UUID> {

    /** Contagem de processos agrupada por status — base do resumo operacional. */
    @Query("SELECT p.status AS status, COUNT(p) AS total FROM ProcurementProcess p GROUP BY p.status")
    List<StatusCount> countByStatus();

    /** Soma do custo das cotacoes selecionadas (base de custo das propostas). */
    @Query("SELECT SUM(q.totalCost) FROM Quotation q WHERE q.selected = true")
    BigDecimal sumSelectedQuotationCost();

    /** Soma do lucro esperado das disputas (somente onde ha preco de venda alvo). */
    @Query("SELECT SUM(d.expectedProfit) FROM Dispute d")
    BigDecimal sumExpectedProfit();

    /** Quantidade de disputas com lucro esperado definido (denominador da media). */
    @Query("SELECT COUNT(d) FROM Dispute d WHERE d.expectedProfit IS NOT NULL")
    long countDisputesWithExpectedProfit();

    /** Soma do valor total dos contratos. */
    @Query("SELECT SUM(c.contractValue) FROM Contract c")
    BigDecimal sumContractValue();

    /** Soma do saldo remanescente dos contratos. */
    @Query("SELECT SUM(c.remainingBalance) FROM Contract c")
    BigDecimal sumRemainingBalance();

    /** Projecao fechada para o agrupamento por status. */
    interface StatusCount {
        ProcessStatus getStatus();
        long getTotal();
    }
}
