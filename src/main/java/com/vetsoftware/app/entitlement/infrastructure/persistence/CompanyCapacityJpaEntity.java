package com.vetsoftware.app.entitlement.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Fila de {@code company_capacities}: el techo contratado de un eje y lo que la
 * empresa lleva usado.
 *
 * <p>
 * <strong>El eje va como columna, no como asociacion</strong>, igual que en
 * {@code catalog_item_limits} y por el mismo motivo: la copia de
 * {@code measure_kind} y el id van atados juntos por una clave foranea
 * compuesta contra {@code limit_dimensions(id, measure_kind)}, y un
 * {@code @ManyToOne} sobre el id obligaria a declarar la copia
 * {@code insertable = false} para no duplicar la columna, con lo que el mapper
 * dejaria de poder escribirla. La clave foranea vive en Liquibase y es la que
 * hace que cambiar un eje de acumulativo a flujo con contadores colgando sea un
 * error del motor (R-LIMIT-22).
 *
 * <p>
 * <strong>{@code period_key} nunca es nulo</strong> (R-LIMIT-05): los cupos que
 * no son de flujo llevan el centinela. En el indice unico dos NULL no chocan
 * entre si, asi que una columna nulable dejaria caber dos contadores para
 * exactamente la misma cosa.
 *
 * <p>
 * <strong>Sin {@code @Version}</strong> ({@code E6_YA_PROTEGIDO}):
 * {@code used_quantity} se mueve con {@code UPDATE ... SET used_quantity =
 * used_quantity + ?}, atomico en el motor. {@code @Version} lo convertiria en
 * un 409 cada vez que dos usuarios se dan de alta a la vez, sin proteger nada
 * que el motor no proteja ya.
 *
 * <p>
 * <strong>Sin {@code enabled}</strong>: es una tabla derivada, misma razon que
 * {@code company_entitlements}.
 */
@Entity
@Table(name = "company_capacities", uniqueConstraints = {
        @UniqueConstraint(name = "uq_company_capacities", columnNames = {"company_id",
                "limit_dimension_id", "period_key"})})
public class CompanyCapacityJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyJpaEntity company;

    @Column(name = "limit_dimension_id", nullable = false)
    private Long limitDimensionId;

    /**
     * Copia atada por clave foranea contra
     * {@code limit_dimensions(id, measure_kind)}.
     */
    @Column(name = "measure_kind", nullable = false, length = 20)
    private String measureKind;

    /**
     * {@code VARCHAR(7)} y no {@code CHAR(7)} pese a lo que dice la ficha: MySQL
     * recorta los espacios finales de un {@code CHAR} al leerlo, asi que dos claves
     * que solo difieren en relleno se comparan iguales al leer y distintas al
     * indexar. En una columna que forma parte de la clave del contador eso es
     * exactamente el fallo que el centinela existe para impedir.
     */
    @Column(name = "period_key", nullable = false, length = 7)
    private String periodKey;

    @Column(name = "limit_quantity", nullable = false)
    private int limitQuantity;

    @Column(name = "used_quantity", nullable = false)
    private int usedQuantity;

    @Column(name = "subscription_id")
    private Long subscriptionId;

    /** El sello del techo: cuando se derivo del contrato. */
    @Column(name = "limit_recalculated_at", nullable = false)
    private LocalDateTime limitRecalculatedAt;

    /**
     * El sello del consumo: cuando se comprobo por ultima vez contra las filas
     * reales. Nulo mientras nadie lo haya comprobado nunca --el recalculo NO lo
     * escribe, porque no mira el consumo--.
     */
    @Column(name = "usage_reconciled_at")
    private LocalDateTime usageReconciledAt;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    protected CompanyCapacityJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CompanyJpaEntity getCompany() {
        return company;
    }

    public void setCompany(CompanyJpaEntity company) {
        this.company = company;
    }

    public Long getLimitDimensionId() {
        return limitDimensionId;
    }

    public void setLimitDimensionId(Long limitDimensionId) {
        this.limitDimensionId = limitDimensionId;
    }

    public String getMeasureKind() {
        return measureKind;
    }

    public void setMeasureKind(String measureKind) {
        this.measureKind = measureKind;
    }

    public String getPeriodKey() {
        return periodKey;
    }

    public void setPeriodKey(String periodKey) {
        this.periodKey = periodKey;
    }

    public int getLimitQuantity() {
        return limitQuantity;
    }

    public void setLimitQuantity(int limitQuantity) {
        this.limitQuantity = limitQuantity;
    }

    public int getUsedQuantity() {
        return usedQuantity;
    }

    public void setUsedQuantity(int usedQuantity) {
        this.usedQuantity = usedQuantity;
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Long subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public LocalDateTime getLimitRecalculatedAt() {
        return limitRecalculatedAt;
    }

    public void setLimitRecalculatedAt(LocalDateTime limitRecalculatedAt) {
        this.limitRecalculatedAt = limitRecalculatedAt;
    }

    public LocalDateTime getUsageReconciledAt() {
        return usageReconciledAt;
    }

    public void setUsageReconciledAt(LocalDateTime usageReconciledAt) {
        this.usageReconciledAt = usageReconciledAt;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }
}
