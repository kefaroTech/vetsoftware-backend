package com.vetsoftware.app.companyactivitymonth.infrastructure.persistence;

import com.vetsoftware.app.companyactivitymonth.domain.CommercialState;
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
import java.time.LocalDateTime;

/**
 * {@code company_activity_months} — la actividad de una clinica, mes a mes.
 *
 * <p>
 * <strong>{@code company_id} va como escalar {@code Long} y NO como
 * {@code @ManyToOne} a {@code CompanyJpaEntity}, y eso no es estetica.</strong>
 * Esta feature no necesita un solo dato de la empresa: guarda el identificador
 * y nada mas. Colgar aqui una asociacion «para poder pintar el nombre» traeria
 * a la rodaja el agregado entero de {@code company} y, sobre todo, obligaria a
 * cargar un grafo ajeno en el barrido de dormidos, que es una consulta de
 * plataforma sobre todas las clinicas a la vez. El nombre lo resuelve quien
 * pinta, con su propia consulta.
 *
 * <p>
 * {@code fk_cam_company} sigue viva y vigilando en la base
 * ({@code RESTRICT}/{@code RESTRICT}); lo que no existe es la navegacion desde
 * Java. Y como no hay ninguna asociacion, <strong>no hay N+1 que evitar ni
 * {@code @EntityGraph} que poner</strong>: los cinco listados de esta rodaja
 * son una sola consulta cada uno.
 *
 * <p>
 * <strong>Lleva {@code @Version}</strong> porque la tabla tiene la columna
 * desde su primer changeset y porque hay una escritura que edita: el mes en
 * curso se recalcula sobre si mismo cada dia hasta que termina. Sin el, dos
 * recalculos concurrentes se pisarian y la serie quedaria con un valor
 * arbitrario, sin excepcion y sin log.
 *
 * <p>
 * <strong>Sin {@code enabled} y sin {@code @SQLDelete}, a proposito.</strong>
 * Una medicion no se desactiva ni se retira: poder ocultar un mes flojo seria
 * poder maquillar la unica serie que dice si un cliente se esta yendo. Al no
 * llevar borrado logico, la trampa del {@code @SQLDelete} de dos parametros
 * ({@code BORRADO_LOGICO_RESPETA_LA_VERSION}) no aplica aqui.
 */
@Entity
@Table(name = "company_activity_months")
public class CompanyActivityMonthJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Escalar a proposito. Ver el javadoc de la clase. */
    @Column(name = "company_id", nullable = false)
    private Long companyId;

    /**
     * {@code CHAR(7)} con colacion {@code ascii_bin}: siempre {@code AAAA-MM}, sin
     * relleno posible porque {@code chk_cam_period_key} prohibe cualquier otra
     * longitud. La comparacion binaria hace que el orden lexicografico sea el
     * cronologico.
     */
    @Column(name = "period_key", nullable = false, length = 7)
    private String periodKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "commercial_state", nullable = false, length = 15)
    private CommercialState commercialState;

    @Column(name = "active_days", nullable = false)
    private int activeDays;

    @Column(name = "active_users", nullable = false)
    private int activeUsers;

    @Column(name = "records_created", nullable = false)
    private int recordsCreated;

    /**
     * {@code precision}/{@code scale} tienen que coincidir con
     * {@code DECIMAL(19,2)} o {@code ddl-auto: validate} lo rechaza al arrancar.
     */
    @Column(name = "mrr_snapshot", nullable = false, precision = 19, scale = 2)
    private BigDecimal mrrSnapshot;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected CompanyActivityMonthJpaEntity() {
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

    public String getPeriodKey() {
        return periodKey;
    }

    public void setPeriodKey(String periodKey) {
        this.periodKey = periodKey;
    }

    public CommercialState getCommercialState() {
        return commercialState;
    }

    public void setCommercialState(CommercialState commercialState) {
        this.commercialState = commercialState;
    }

    public int getActiveDays() {
        return activeDays;
    }

    public void setActiveDays(int activeDays) {
        this.activeDays = activeDays;
    }

    public int getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(int activeUsers) {
        this.activeUsers = activeUsers;
    }

    public int getRecordsCreated() {
        return recordsCreated;
    }

    public void setRecordsCreated(int recordsCreated) {
        this.recordsCreated = recordsCreated;
    }

    public BigDecimal getMrrSnapshot() {
        return mrrSnapshot;
    }

    public void setMrrSnapshot(BigDecimal mrrSnapshot) {
        this.mrrSnapshot = mrrSnapshot;
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
