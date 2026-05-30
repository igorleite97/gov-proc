package com.govproc.dispute.domain;

import com.govproc.shared.domain.BaseEntity;
import com.govproc.shared.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Estrategia comercial da disputa de um processo licitatorio.
 *
 * RESPONSABILIDADE: estrategia (margem alvo, preco de venda, preco minimo,
 * lucro esperado, estrategia de lance). NUNCA registra custo de fornecedor —
 * isso e responsabilidade de {@code Quotation}.
 *
 * O {@code quotedCost} aqui e um SNAPSHOT do custo total da cotacao selecionada,
 * capturado no inicio da disputa. Mantemos {@code quotationId} como UUID puro
 * (sem @ManyToOne) para baixo acoplamento e sem grafos/cascades do Hibernate —
 * mesma filosofia adotada com Supplier.
 *
 * Uma disputa por processo (UNIQUE em process_id).
 *
 * Padrao monetario: BigDecimal precision=19, scale=4, normalizado na entrada.
 */
@Entity
@Table(name = "disputes")
public class Dispute extends BaseEntity {

    private static final int MONETARY_SCALE = 4;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    @Column(name = "process_id", nullable = false, unique = true)
    private UUID processId;

    // Snapshot da cotacao selecionada (UUID puro — sem @ManyToOne).
    @Column(name = "quotation_id", nullable = false)
    private UUID quotationId;

    @Column(name = "quoted_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal quotedCost;

    @Column(name = "target_margin", precision = 19, scale = 4)
    private BigDecimal targetMargin;

    @Column(name = "target_sale_price", precision = 19, scale = 4)
    private BigDecimal targetSalePrice;

    @Column(name = "minimum_sale_price", precision = 19, scale = 4)
    private BigDecimal minimumSalePrice;

    // Calculado e persistido: targetSalePrice - quotedCost.
    @Column(name = "expected_profit", precision = 19, scale = 4)
    private BigDecimal expectedProfit;

    @Enumerated(EnumType.STRING)
    @Column(name = "bid_strategy", nullable = false, length = 20)
    private BidStrategy bidStrategy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DisputeStatus status;

    @Column(name = "strategy_notes", columnDefinition = "TEXT")
    private String strategyNotes;

    @Column(name = "registered_by", nullable = false)
    private UUID registeredBy;

    protected Dispute() { }

    public Dispute(UUID processId, UUID quotationId, BigDecimal quotedCost,
                   BidStrategy bidStrategy, BigDecimal targetMargin,
                   BigDecimal targetSalePrice, BigDecimal minimumSalePrice,
                   String strategyNotes, UUID registeredBy) {
        this.processId = processId;
        this.quotationId = quotationId;
        this.quotedCost = quotedCost.setScale(MONETARY_SCALE, ROUNDING);
        this.bidStrategy = bidStrategy != null ? bidStrategy : BidStrategy.MODERATE;
        this.registeredBy = registeredBy;
        this.status = DisputeStatus.OPEN;
        applyStrategy(targetMargin, targetSalePrice, minimumSalePrice, strategyNotes);
    }

    // -------------------------------------------------------------------------
    // Mutacoes encapsuladas — toda mudanca passa por validacao interna.
    // -------------------------------------------------------------------------

    /**
     * Revisa a estrategia comercial enquanto a disputa esta OPEN.
     */
    public void updateStrategy(BidStrategy bidStrategy, BigDecimal targetMargin,
                               BigDecimal targetSalePrice, BigDecimal minimumSalePrice,
                               String strategyNotes) {
        if (this.status != DisputeStatus.OPEN) {
            throw new BusinessException("Disputa já encerrada — estratégia não pode ser alterada");
        }
        if (bidStrategy != null) {
            this.bidStrategy = bidStrategy;
        }
        applyStrategy(targetMargin, targetSalePrice, minimumSalePrice, strategyNotes);
    }

    /**
     * Encerra a disputa. Chamado quando o processo e marcado como WINNER ou LOSER.
     */
    public void conclude() {
        if (this.status == DisputeStatus.CONCLUDED) {
            throw new BusinessException("Disputa já encerrada");
        }
        this.status = DisputeStatus.CONCLUDED;
    }

    // -------------------------------------------------------------------------
    // Regras internas de estrategia/calculo.
    // -------------------------------------------------------------------------

    private void applyStrategy(BigDecimal targetMargin, BigDecimal targetSalePrice,
                               BigDecimal minimumSalePrice, String strategyNotes) {
        this.targetMargin = normalize(targetMargin);
        this.targetSalePrice = normalize(targetSalePrice);
        this.minimumSalePrice = normalize(minimumSalePrice);
        this.strategyNotes = strategyNotes;

        // Invariante: o alvo nao pode ficar abaixo do piso.
        if (this.targetSalePrice != null && this.minimumSalePrice != null
                && this.targetSalePrice.compareTo(this.minimumSalePrice) < 0) {
            throw new BusinessException(
                    "Preço de venda alvo não pode ser menor que o preço mínimo");
        }

        // Lucro esperado = preco de venda alvo - custo cotado (calculado e persistido).
        this.expectedProfit = this.targetSalePrice != null
                ? this.targetSalePrice.subtract(this.quotedCost).setScale(MONETARY_SCALE, ROUNDING)
                : null;
    }

    private BigDecimal normalize(BigDecimal value) {
        return value != null ? value.setScale(MONETARY_SCALE, ROUNDING) : null;
    }

    // -------------------------------------------------------------------------
    // Getters — sem setters; estado muda apenas pelos metodos acima.
    // -------------------------------------------------------------------------

    public UUID getProcessId()            { return processId; }
    public UUID getQuotationId()          { return quotationId; }
    public BigDecimal getQuotedCost()     { return quotedCost; }
    public BigDecimal getTargetMargin()   { return targetMargin; }
    public BigDecimal getTargetSalePrice() { return targetSalePrice; }
    public BigDecimal getMinimumSalePrice() { return minimumSalePrice; }
    public BigDecimal getExpectedProfit() { return expectedProfit; }
    public BidStrategy getBidStrategy()   { return bidStrategy; }
    public DisputeStatus getStatus()      { return status; }
    public String getStrategyNotes()      { return strategyNotes; }
    public UUID getRegisteredBy()         { return registeredBy; }
}
