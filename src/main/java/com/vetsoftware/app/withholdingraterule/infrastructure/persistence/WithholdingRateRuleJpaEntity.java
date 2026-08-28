package com.vetsoftware.app.withholdingraterule.infrastructure.persistence;

import com.vetsoftware.app.withholdingraterule.domain.ServiceNature;
import com.vetsoftware.app.withholdingraterule.domain.WithholdingType;
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
 * {@code withholding_rate_rules} — que retencion esperar de cada cliente.
 *
 * <p>
 * <strong>Esta clase NO alcanza {@code CompanyJpaEntity} por ninguna
 * asociacion, y no es estetica.</strong> La tabla es un catalogo global sin
 * columna de empresa; el dia que alguien le cuelgue un {@code @ManyToOne} a
 * companies «para saber quien la cargo», las cuatro reglas duras de aislamiento
 * de BE-COV se activan sobre la feature entera y rompen el build. Lo que
 * depende del cliente —si es agente de retencion— vive en
 * {@code company_billing_profiles}, no aqui.
 *
 * <p>
 * <strong>La FK al municipio va como escalar y no como asociacion</strong>, por
 * la misma razon y por otra propia: apunta a {@code cities.dane_code}, no a
 * {@code cities.id}. Un {@code @ManyToOne} sobre {@code CityJpaEntity} traeria
 * a este slice el grafo de geografia entero —ciudad, departamento, pais— para
 * usar cinco caracteres. La clave foranea sigue existiendo y vigilando en la
 * base ({@code fk_withholding_rate_rules_municipality}); lo que no existe es la
 * navegacion desde Java. Sin asociaciones tampoco hay N+1 que evitar ni
 * {@code @EntityGraph} que poner.
 *
 * <p>
 * <strong>Las dos columnas GENERATED STORED no se mapean, a proposito.</strong>
 * {@code municipality_key} y {@code current_rule_marker} las calcula MySQL y
 * solo existen para que dos indices unicos puedan restringir lo que con
 * {@code NULL} no restringian. Mapearlas obligaria a {@code insertable = false,
 * updatable = false} y, peor, invitaria a escribirlas desde Java: el primer
 * {@code INSERT} que llevara un valor propio para una columna generada lo
 * rechazaria el motor.
 *
 * <p>
 * <strong>Lleva {@code @Version}</strong> porque la tabla tiene la columna y
 * porque hay una escritura que edita: el cierre de la vigencia. Sin el, dos
 * cierres concurrentes se pisarian y la fecha desde la que la tarifa dejo de
 * aplicarse se perderia sin excepcion y sin log.
 */
@Entity
@Table(name = "withholding_rate_rules")
public class WithholdingRateRuleJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "withholding_type", nullable = false, length = 20)
    private WithholdingType withholdingType;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_nature", nullable = false, length = 30)
    private ServiceNature serviceNature;

    /** Nulo salvo en {@code ICA}. La otra mitad la cuida el CHECK del esquema. */
    @Column(name = "municipality_code", length = 5)
    private String municipalityCode;

    /**
     * Porcentaje, no fraccion, y con los seis decimales que declara la columna:
     * {@code precision}/{@code scale} tienen que coincidir con {@code DECIMAL(9,6)}
     * o {@code ddl-auto: validate} lo rechaza al arrancar.
     */
    @Column(name = "rate_percent", nullable = false, precision = 9, scale = 6)
    private BigDecimal ratePercent;

    @Column(name = "minimum_base_amount", precision = 19, scale = 2)
    private BigDecimal minimumBaseAmount;

    @Column(name = "minimum_base_uvt", precision = 9, scale = 2)
    private BigDecimal minimumBaseUvt;

    @Column(name = "legal_reference", length = 255)
    private String legalReference;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    /**
     * Nulo mientras la regla siga vigente. Es lo que alimenta las dos generadas.
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

    protected WithholdingRateRuleJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public WithholdingType getWithholdingType() {
        return withholdingType;
    }

    public void setWithholdingType(WithholdingType withholdingType) {
        this.withholdingType = withholdingType;
    }

    public ServiceNature getServiceNature() {
        return serviceNature;
    }

    public void setServiceNature(ServiceNature serviceNature) {
        this.serviceNature = serviceNature;
    }

    public String getMunicipalityCode() {
        return municipalityCode;
    }

    public void setMunicipalityCode(String municipalityCode) {
        this.municipalityCode = municipalityCode;
    }

    public BigDecimal getRatePercent() {
        return ratePercent;
    }

    public void setRatePercent(BigDecimal ratePercent) {
        this.ratePercent = ratePercent;
    }

    public BigDecimal getMinimumBaseAmount() {
        return minimumBaseAmount;
    }

    public void setMinimumBaseAmount(BigDecimal minimumBaseAmount) {
        this.minimumBaseAmount = minimumBaseAmount;
    }

    public BigDecimal getMinimumBaseUvt() {
        return minimumBaseUvt;
    }

    public void setMinimumBaseUvt(BigDecimal minimumBaseUvt) {
        this.minimumBaseUvt = minimumBaseUvt;
    }

    public String getLegalReference() {
        return legalReference;
    }

    public void setLegalReference(String legalReference) {
        this.legalReference = legalReference;
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
