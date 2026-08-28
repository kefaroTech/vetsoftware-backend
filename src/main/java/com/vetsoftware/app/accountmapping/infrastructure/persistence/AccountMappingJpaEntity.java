package com.vetsoftware.app.accountmapping.infrastructure.persistence;

import com.vetsoftware.app.accountmapping.domain.MappingKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * {@code account_mappings} (changeset 343) — que cuenta mueve cada cosa.
 *
 * <p>
 * <strong>Esta clase NO alcanza {@code CompanyJpaEntity} por ninguna
 * asociacion.</strong> La tabla es un catalogo global sin columna de empresa;
 * el dia que alguien le cuelgue un {@code @ManyToOne} a companies, las cuatro
 * reglas duras de aislamiento de BE-COV se activan sobre la feature entera y
 * rompen el build.
 *
 * <p>
 * <strong>Las cuatro claves foraneas van como escalares, no como
 * asociaciones.</strong> Tres apuntan a {@code accounting_accounts(code)} —no a
 * su id— y la cuarta a {@code catalog_items(id)}; un {@code @ManyToOne} traeria
 * a este slice el plan de cuentas entero y el catalogo comercial para usar diez
 * caracteres, y obligaria a un {@code @EntityGraph} en cada finder para evitar
 * el N+1. Las claves siguen existiendo y vigilando en la base; lo que no existe
 * es la navegacion desde Java.
 *
 * <p>
 * <strong>Las cuatro columnas GENERATED STORED no se mapean, a
 * proposito.</strong> {@code catalog_item_key}, {@code charge_type_key},
 * {@code tax_treatment_key} y {@code current_mapping_marker} las calcula MySQL
 * y existen para que dos indices unicos puedan restringir lo que con
 * {@code NULL} no restringian. Mapearlas obligaria a
 * {@code insertable = false, updatable = false} y, peor, invitaria a
 * escribirlas desde Java: el primer {@code INSERT} que llevara un valor propio
 * para una columna generada lo rechazaria el motor con el error 3105.
 */
@Entity
@Table(name = "account_mappings")
public class AccountMappingJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "mapping_kind", nullable = false, length = 25)
    private MappingKind mappingKind;

    @Column(name = "mapping_key", nullable = false, length = 60)
    private String mappingKey;

    @Column(name = "catalog_item_id")
    private Long catalogItemId;

    @Column(name = "charge_type", length = 20)
    private String chargeType;

    @Column(name = "tax_treatment", length = 20)
    private String taxTreatment;

    @Column(name = "debit_account_code", nullable = false, length = 10)
    private String debitAccountCode;

    @Column(name = "credit_account_code", nullable = false, length = 10)
    private String creditAccountCode;

    @Column(name = "deferred_account_code", length = 10)
    private String deferredAccountCode;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    /**
     * Nulo mientras el mapeo siga vigente. Es lo que alimenta el cuarto marcador.
     */
    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    /**
     * {@code TINYINT} pelado: un {@code TINYINT(1)} lo reporta el driver como
     * {@code BIT} y rompe {@code ddl-auto: validate}.
     */
    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected AccountMappingJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MappingKind getMappingKind() {
        return mappingKind;
    }

    public void setMappingKind(MappingKind mappingKind) {
        this.mappingKind = mappingKind;
    }

    public String getMappingKey() {
        return mappingKey;
    }

    public void setMappingKey(String mappingKey) {
        this.mappingKey = mappingKey;
    }

    public Long getCatalogItemId() {
        return catalogItemId;
    }

    public void setCatalogItemId(Long catalogItemId) {
        this.catalogItemId = catalogItemId;
    }

    public String getChargeType() {
        return chargeType;
    }

    public void setChargeType(String chargeType) {
        this.chargeType = chargeType;
    }

    public String getTaxTreatment() {
        return taxTreatment;
    }

    public void setTaxTreatment(String taxTreatment) {
        this.taxTreatment = taxTreatment;
    }

    public String getDebitAccountCode() {
        return debitAccountCode;
    }

    public void setDebitAccountCode(String debitAccountCode) {
        this.debitAccountCode = debitAccountCode;
    }

    public String getCreditAccountCode() {
        return creditAccountCode;
    }

    public void setCreditAccountCode(String creditAccountCode) {
        this.creditAccountCode = creditAccountCode;
    }

    public String getDeferredAccountCode() {
        return deferredAccountCode;
    }

    public void setDeferredAccountCode(String deferredAccountCode) {
        this.deferredAccountCode = deferredAccountCode;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public void setValidTo(LocalDate validTo) {
        this.validTo = validTo;
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
