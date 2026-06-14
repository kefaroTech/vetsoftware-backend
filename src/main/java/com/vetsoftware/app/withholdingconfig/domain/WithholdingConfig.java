package com.vetsoftware.app.withholdingconfig.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Tarifas de retención del emisor (0-o-1 por empresa). reteFuente/reteIca aplican % sobre la base;
 * reteIva aplica % sobre el IVA generado. Se usan al emitir cuando el adquiriente es agente retenedor.
 */
public class WithholdingConfig {
    private Long id;
    private CompanyRef company;
    private BigDecimal reteFuenteRate;
    private BigDecimal reteIvaRate;
    private BigDecimal reteIcaRate;
    private final LocalDateTime createdDate;
    private boolean enabled;

    public WithholdingConfig(Long id, CompanyRef company, BigDecimal reteFuenteRate, BigDecimal reteIvaRate,
                             BigDecimal reteIcaRate, LocalDateTime createdDate, boolean enabled) {
        validate(company, reteFuenteRate, reteIvaRate, reteIcaRate);
        this.id = id;
        this.company = company;
        this.reteFuenteRate = reteFuenteRate;
        this.reteIvaRate = reteIvaRate;
        this.reteIcaRate = reteIcaRate;
        this.createdDate = createdDate;
        this.enabled = enabled;
    }

    public static WithholdingConfig create(CompanyRef company, BigDecimal reteFuenteRate,
                                           BigDecimal reteIvaRate, BigDecimal reteIcaRate) {
        return new WithholdingConfig(null, company, reteFuenteRate, reteIvaRate, reteIcaRate,
                LocalDateTime.now(), true);
    }

    public void update(BigDecimal reteFuenteRate, BigDecimal reteIvaRate, BigDecimal reteIcaRate) {
        validate(this.company, reteFuenteRate, reteIvaRate, reteIcaRate);
        this.reteFuenteRate = reteFuenteRate;
        this.reteIvaRate = reteIvaRate;
        this.reteIcaRate = reteIcaRate;
    }

    private static void validate(CompanyRef company, BigDecimal rf, BigDecimal ri, BigDecimal ria) {
        if (company == null) throw new IllegalArgumentException("company is required");
        requireNonNegative(rf, "reteFuenteRate");
        requireNonNegative(ri, "reteIvaRate");
        requireNonNegative(ria, "reteIcaRate");
    }

    private static void requireNonNegative(BigDecimal v, String name) {
        if (v == null) throw new IllegalArgumentException(name + " is required");
        if (v.signum() < 0) throw new IllegalArgumentException(name + " cannot be negative");
    }

    public Long getId() { return id; }
    public CompanyRef getCompany() { return company; }
    public BigDecimal getReteFuenteRate() { return reteFuenteRate; }
    public BigDecimal getReteIvaRate() { return reteIvaRate; }
    public BigDecimal getReteIcaRate() { return reteIcaRate; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public boolean isEnabled() { return enabled; }
    public void enable() { this.enabled = true; }
    public void disable() { this.enabled = false; }
}
