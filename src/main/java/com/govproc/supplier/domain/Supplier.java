package com.govproc.supplier.domain;

import com.govproc.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "suppliers")
public class Supplier extends BaseEntity {

    @Column(name = "company_name", nullable = false, length = 255)
    private String companyName;

    @Column(name = "trade_name", length = 255)
    private String tradeName;

    /**
     * CNPJ armazenado sem mascara (apenas digitos).
     * Validacao do algoritmo pode ser adicionada futuramente.
     */
    @Column(nullable = false, unique = true, length = 18)
    private String document;

    @Column(length = 255)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String segment;

    @Column(nullable = false)
    private boolean active = true;

    protected Supplier() { }

    public Supplier(String companyName, String tradeName, String document,
                    String email, String phone, String segment) {
        this.companyName = companyName;
        this.tradeName = tradeName;
        this.document = document;
        this.email = email;
        this.phone = phone;
        this.segment = segment;
    }

    public void deactivate() { this.active = false; }
    public void activate()   { this.active = true; }

    public String getCompanyName()  { return companyName; }
    public String getTradeName()    { return tradeName; }
    public String getDocument()     { return document; }
    public String getEmail()        { return email; }
    public String getPhone()        { return phone; }
    public String getSegment()      { return segment; }
    public boolean isActive()       { return active; }
}
