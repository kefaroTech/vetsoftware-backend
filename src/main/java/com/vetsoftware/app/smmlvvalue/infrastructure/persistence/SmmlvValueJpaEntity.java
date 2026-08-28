package com.vetsoftware.app.smmlvvalue.infrastructure.persistence;

import com.vetsoftware.app.smmlvvalue.domain.SmmlvStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Espejo de {@code smmlv_values} (changeset 348).
 *
 * <p>
 * <strong>Con {@code @Version}</strong>, al contrario que sus tres hermanas del
 * bloque de referencia: aqui hay una mutacion declarada —la suspension judicial
 * se anota sobre la fila que ya existe— y donde hay mutacion hay dos operadores
 * que pueden llegar a la vez. Sin {@code @Version}, el segundo grabaria encima
 * del primero sin excepcion y sin log.
 *
 * <p>
 * No lleva {@code @SQLDelete}: esta fila no se borra en logico, asi que la
 * trampa de los dos parametros de {@code BORRADO_LOGICO_RESPETA_LA_VERSION} no
 * aplica.
 */
@Entity
@Table(name = "smmlv_values")
public class SmmlvValueJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fiscal_year", nullable = false)
    private short fiscalYear;

    @Column(name = "value_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal valueAmount;

    @Column(name = "legal_reference", nullable = false, length = 255)
    private String legalReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SmmlvStatus status;

    @Column(name = "status_reference", length = 255)
    private String statusReference;

    @Column(name = "status_changed_on")
    private LocalDate statusChangedOn;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected SmmlvValueJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public short getFiscalYear() {
        return fiscalYear;
    }

    public void setFiscalYear(short fiscalYear) {
        this.fiscalYear = fiscalYear;
    }

    public BigDecimal getValueAmount() {
        return valueAmount;
    }

    public void setValueAmount(BigDecimal valueAmount) {
        this.valueAmount = valueAmount;
    }

    public String getLegalReference() {
        return legalReference;
    }

    public void setLegalReference(String legalReference) {
        this.legalReference = legalReference;
    }

    public SmmlvStatus getStatus() {
        return status;
    }

    public void setStatus(SmmlvStatus status) {
        this.status = status;
    }

    public String getStatusReference() {
        return statusReference;
    }

    public void setStatusReference(String statusReference) {
        this.statusReference = statusReference;
    }

    public LocalDate getStatusChangedOn() {
        return statusChangedOn;
    }

    public void setStatusChangedOn(LocalDate statusChangedOn) {
        this.statusChangedOn = statusChangedOn;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
