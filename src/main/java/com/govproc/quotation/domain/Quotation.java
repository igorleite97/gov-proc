package com.govproc.quotation.domain;

import com.govproc.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Cotacao de um produto/servico de um fornecedor para um processo licitatorio.
 *
 * RESPONSABILIDADE: registrar CUSTO real (unitCost, shippingCost, totalCost).
 * NÃO registra markup, margem ou estrategia comercial — isso e responsabilidade
 * de {@code Dispute} na proxima fase.
 *
 * Padrao monetario: BigDecimal com precision=19, scale=4, normalizado no construtor.
 * Snapshot do fornecedor (supplierNameSnapshot, supplierDocumentSnapshot) e um
 * ROADMAP futuro — importante para rastreabilidade historica quando o cadastro muda.
 */
@Entity
@Table(name = "quotations")
public class Quotation extends BaseEntity {

    private static final int MONETARY_SCALE = 4;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    @Column(name = "process_id", nullable = false)
    private UUID processId;

    @Column(name = "supplier_id", nullable = false)
    private UUID supplierId;

    @Column(name = "product_description", nullable = false, columnDefinition = "TEXT")
    private String productDescription;

    @Column(name = "unit_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitCost;

    @Column(name = "shipping_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal shippingCost;

    @Column(nullable = false)
    private Integer quantity;

    /**
     * Total calculado: (unitCost × quantity) + shippingCost.
     * Persiste o valor calculado — evita recalculo em leitura e
     * preserva o historico mesmo que regras de calculo mudem.
     */
    @Column(name = "total_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalCost;

    @Column(length = 255)
    private String manufacturer;

    @Column(length = 100)
    private String brand;

    @Column(name = "delivery_days", length = 100)
    private String deliveryDays;

    @Column(nullable = false)
    private boolean selected = false;

    @Column(columnDefinition = "TEXT")
    private String observations;

    @Column(name = "registered_by", nullable = false)
    private UUID registeredBy;

    protected Quotation() { }

    public Quotation(UUID processId, UUID supplierId, String productDescription,
                     BigDecimal unitCost, Integer quantity, BigDecimal shippingCost,
                     String manufacturer, String brand, String deliveryDays,
                     String observations, UUID registeredBy) {
        this.processId = processId;
        this.supplierId = supplierId;
        this.productDescription = productDescription;
        // Normaliza na entrada — BigDecimal mal padronizado vira inferno silencioso
        this.unitCost = unitCost.setScale(MONETARY_SCALE, ROUNDING);
        this.quantity = quantity;
        this.shippingCost = shippingCost != null
                ? shippingCost.setScale(MONETARY_SCALE, ROUNDING)
                : BigDecimal.ZERO.setScale(MONETARY_SCALE);
        this.totalCost = this.unitCost
                .multiply(BigDecimal.valueOf(quantity))
                .add(this.shippingCost)
                .setScale(MONETARY_SCALE, ROUNDING);
        this.manufacturer = manufacturer;
        this.brand = brand;
        this.deliveryDays = deliveryDays;
        this.observations = observations;
        this.registeredBy = registeredBy;
    }

    public void select()   { this.selected = true; }
    public void deselect() { this.selected = false; }

    public UUID getProcessId()            { return processId; }
    public UUID getSupplierId()           { return supplierId; }
    public String getProductDescription() { return productDescription; }
    public BigDecimal getUnitCost()       { return unitCost; }
    public BigDecimal getShippingCost()   { return shippingCost; }
    public Integer getQuantity()          { return quantity; }
    public BigDecimal getTotalCost()      { return totalCost; }
    public String getManufacturer()       { return manufacturer; }
    public String getBrand()              { return brand; }
    public String getDeliveryDays()       { return deliveryDays; }
    public boolean isSelected()           { return selected; }
    public String getObservations()       { return observations; }
    public UUID getRegisteredBy()         { return registeredBy; }
}
