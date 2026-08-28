package com.vetsoftware.app.companyusageevent.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;

/**
 * {@code company_usage_events} — el hecho que sostiene el cobro.
 *
 * <h2>Las siete claves foraneas van como escalares y ninguna como
 * asociacion</h2>
 *
 * <p>
 * {@code company_id}, {@code limit_dimension_id}, {@code charge_id} y las
 * cuatro ramas clinicas son {@code Long} pelados. <b>No hay un solo
 * {@code @ManyToOne} en esta clase</b>, y hay dos razones independientes,
 * cualquiera de las cuales bastaria:
 *
 * <ol>
 * <li><strong>Las claves son compuestas y un {@code @JoinColumn} simple no
 * puede expresarlas.</strong> {@code fk_cue_owner} es
 * {@code (company_id, usage_owner_id) -> owners(company_id, id)}, y sus tres
 * hermanas igual. Esa forma —y no otra— es lo que impide que un hecho de la
 * clinica A apunte a la mascota de B. Un {@code @ManyToOne} sobre
 * {@code usage_animal_id} a secas mapearia <em>media</em> clave y dejaria a
 * Hibernate escribiendo por un camino que la base tiene que rechazar.
 * <li><strong>Arrastraria cuatro rodajas clinicas enteras a esta.</strong>
 * {@code owners}, {@code animals}, {@code appointments} y
 * {@code electronic_documents} son cuatro grafos completos traidos para usar un
 * numero.
 * </ol>
 *
 * <p>
 * Las siete claves foraneas <b>siguen vivas y vigilando en la base</b>
 * ({@code fk_cue_*}, todas {@code RESTRICT}/{@code RESTRICT}); lo que no existe
 * es la navegacion desde Java. <strong>Y sin asociaciones no hay N+1 que evitar
 * ni {@code @EntityGraph} que poner</strong>: la ausencia de esas anotaciones
 * en el repositorio de esta rodaja es una consecuencia, no un olvido.
 *
 * <h2>{@code usage_ref_key} no se mapea</h2>
 *
 * <p>
 * Es {@code GENERATED ALWAYS AS (CONCAT(...)) STORED}: la calcula MySQL y solo
 * existe para que {@code uq_cue_fact} pueda restringir lo que con cuatro
 * columnas nulables no restringia —en un indice unico, dos {@code NULL} no
 * chocan—. Mapearla obligaria a {@code insertable = false, updatable = false}
 * y, peor, invitaria a escribirla desde Java: el primer {@code INSERT} que
 * llevara valor propio para una columna generada lo rechazaria el motor.
 *
 * <p>
 * Su valor es {@code CONCAT(limit_dimension_code, '|', COALESCE(las cuatro
 * ramas))}, es decir la rama y su referencia; no se reproduce en Java porque
 * nadie consulta por ella —solo la usa el indice— y una copia sin consumidor es
 * una definicion mas que puede divergir de la del motor sin que nada falle.
 *
 * <h2>Version si, enabled no</h2>
 *
 * <p>
 * <strong>Lleva {@code @Version}</strong> porque la tabla tiene la columna y
 * porque hay una escritura que edita la fila: colgarle el cargo que la facturo.
 * Sin bloqueo optimista, dos pasadas concurrentes del cierre le colgarian dos
 * cargos distintos al mismo hecho y uno se perderia sin excepcion y sin log.
 *
 * <p>
 * <strong>No lleva {@code enabled}</strong> —la columna no existe— y por eso
 * tampoco lleva {@code @SQLDelete}: la ficha excluye expresamente los hechos de
 * uso de la marca de activo. Un hecho no se desactiva; o paso o no paso.
 */
@Entity
@Table(name = "company_usage_events")
public class CompanyUsageEventJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "limit_dimension_id", nullable = false)
    private Long limitDimensionId;

    /**
     * Copiado del eje y atado por {@code fk_cue_dimension (limit_dimension_id,
     * limit_dimension_code)}, el mismo mecanismo con el que
     * {@code company_capacities} copia {@code measure_kind} (changeset 314): la
     * fila copia el dato del padre y la clave compuesta impide que diverja.
     */
    @Column(name = "limit_dimension_code", nullable = false, length = 50)
    private String limitDimensionCode;

    @Column(name = "usage_owner_id")
    private Long usageOwnerId;

    @Column(name = "usage_animal_id")
    private Long usageAnimalId;

    @Column(name = "usage_appointment_id")
    private Long usageAppointmentId;

    @Column(name = "usage_electronic_document_id")
    private Long usageElectronicDocumentId;

    /**
     * <strong>El instante del registro consumido, no el del proceso.</strong> Es la
     * columna de la que depende {@code uq_cue_fact}; ver la advertencia completa en
     * {@code CompanyUsageEvent}.
     */
    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    /** {@code VARCHAR(7)}, no {@code CHAR(7)}. Ver {@code UsagePeriodKey}. */
    @Column(name = "period_key", nullable = false, length = 7)
    private String periodKey;

    /**
     * {@code TINYINT} pelado: un {@code TINYINT(1)} lo reporta el driver como
     * {@code BIT} y rompe {@code ddl-auto: validate}. Sin {@code columnDefinition}:
     * el {@code preferred_boolean_jdbc_type} de {@code application.yml} hace el
     * mapeo.
     */
    @Column(name = "billable", nullable = false)
    private boolean billable;

    /** Nulo mientras el hecho no se haya facturado. */
    @Column(name = "charge_id")
    private Long chargeId;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected CompanyUsageEventJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public Long getLimitDimensionId() {
        return limitDimensionId;
    }

    public void setLimitDimensionId(Long limitDimensionId) {
        this.limitDimensionId = limitDimensionId;
    }

    public String getLimitDimensionCode() {
        return limitDimensionCode;
    }

    public void setLimitDimensionCode(String limitDimensionCode) {
        this.limitDimensionCode = limitDimensionCode;
    }

    public Long getUsageOwnerId() {
        return usageOwnerId;
    }

    public void setUsageOwnerId(Long usageOwnerId) {
        this.usageOwnerId = usageOwnerId;
    }

    public Long getUsageAnimalId() {
        return usageAnimalId;
    }

    public void setUsageAnimalId(Long usageAnimalId) {
        this.usageAnimalId = usageAnimalId;
    }

    public Long getUsageAppointmentId() {
        return usageAppointmentId;
    }

    public void setUsageAppointmentId(Long usageAppointmentId) {
        this.usageAppointmentId = usageAppointmentId;
    }

    public Long getUsageElectronicDocumentId() {
        return usageElectronicDocumentId;
    }

    public void setUsageElectronicDocumentId(Long usageElectronicDocumentId) {
        this.usageElectronicDocumentId = usageElectronicDocumentId;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getPeriodKey() {
        return periodKey;
    }

    public void setPeriodKey(String periodKey) {
        this.periodKey = periodKey;
    }

    public boolean isBillable() {
        return billable;
    }

    public void setBillable(boolean billable) {
        this.billable = billable;
    }

    public Long getChargeId() {
        return chargeId;
    }

    public void setChargeId(Long chargeId) {
        this.chargeId = chargeId;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
