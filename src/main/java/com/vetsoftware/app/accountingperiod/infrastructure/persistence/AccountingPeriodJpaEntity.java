package com.vetsoftware.app.accountingperiod.infrastructure.persistence;

import com.vetsoftware.app.accountingperiod.domain.AccountingPeriodStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;

/**
 * {@code accounting_periods} — el mes cerrado y quien lo cerro.
 *
 * <p>
 * <strong>Las dos firmas son columnas escalares {@code Long} y NO
 * {@code @ManyToOne} a {@code SystemUserJpaEntity}.</strong> Es el precedente
 * vivo del repositorio —{@code price_lists.published_by_system_user_id},
 * {@code company_limit_overrides}, {@code subscription_billing_documents}— y
 * tiene dos motivos: de una firma solo hace falta que exista, y una asociacion
 * traeria un agregado ajeno al dominio, un {@code @EntityGraph} obligatorio en
 * cada lectura para no caer en N+1, y un mapper con dos sobrecargas para no
 * disparar el proxy en cada escritura. Nada de eso compra nada aqui: el nombre
 * de quien cerro lo resuelve la consola de plataforma por su cuenta.
 *
 * <p>
 * <strong>Esta entidad NO alcanza {@code CompanyJpaEntity} por ninguna
 * asociacion, y eso es una propiedad que hay que conservar.</strong> El
 * calendario contable es de la plataforma. Ademas tiene una consecuencia
 * mecanica que conviene saber antes de anadir el primer {@code @ManyToOne}: el
 * discriminador de las cuatro reglas duras de BE-COV es «alguna entidad de la
 * feature llega a la tabla de empresas». Colgar aqui una asociacion que llegue
 * —aunque sea indirecta y aunque sea {@code LAZY}— las activa de golpe sobre
 * <em>toda</em> la feature, y los siete puertos pasarian a tener que acotar por
 * un {@code companyId} que la tabla no tiene.
 *
 * <p>
 * <strong>Con {@code @Version}</strong>, y el propio changeset lo dice por
 * escrito: el estado muta dos veces declaradamente —cierre y reapertura— y
 * eximirla seria una exencion que miente. Dos personas atendiendo el mismo
 * cierre mensual leen las dos {@code OPEN}, las dos pasan la comprobacion del
 * dominio y sin el bloqueo optimista la segunda pisaria a la primera sin
 * excepcion y sin log.
 *
 * <p>
 * <strong>Sin {@code enabled} y sin {@code @SQLDelete}</strong>: no hay borrado
 * logico, asi que tampoco existe aqui la trampa de los dos parametros que
 * documenta {@code BORRADO_LOGICO_RESPETA_LA_VERSION}.
 *
 * <p>
 * <strong>{@code period_key} no lleva {@code columnDefinition}.</strong> Su
 * juego de caracteres {@code ascii} y su colacion {@code ascii_bin} los fija el
 * changeset 331 con un {@code MODIFY COLUMN} —hacen falta para poder crear la
 * clave foranea desde {@code external_invoice_reconciliations}, que ya es
 * {@code ascii_bin}: cruzar dos colaciones distintas la impide con un errno
 * 3780—. Declararlo otra vez aqui seria duplicar la decision en dos sitios que
 * pueden divergir, y el que manda es el esquema.
 */
@Entity
@Table(name = "accounting_periods")
public class AccountingPeriodJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "period_key", nullable = false, length = 7)
    private String periodKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private AccountingPeriodStatus status;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "closed_by_system_user_id")
    private Long closedBySystemUserId;

    @Column(name = "reopened_at")
    private LocalDateTime reopenedAt;

    @Column(name = "reopened_by_system_user_id")
    private Long reopenedBySystemUserId;

    @Column(name = "reopened_reason", length = 255)
    private String reopenedReason;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected AccountingPeriodJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPeriodKey() {
        return periodKey;
    }

    public void setPeriodKey(String periodKey) {
        this.periodKey = periodKey;
    }

    public AccountingPeriodStatus getStatus() {
        return status;
    }

    public void setStatus(AccountingPeriodStatus status) {
        this.status = status;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public Long getClosedBySystemUserId() {
        return closedBySystemUserId;
    }

    public void setClosedBySystemUserId(Long closedBySystemUserId) {
        this.closedBySystemUserId = closedBySystemUserId;
    }

    public LocalDateTime getReopenedAt() {
        return reopenedAt;
    }

    public void setReopenedAt(LocalDateTime reopenedAt) {
        this.reopenedAt = reopenedAt;
    }

    public Long getReopenedBySystemUserId() {
        return reopenedBySystemUserId;
    }

    public void setReopenedBySystemUserId(Long reopenedBySystemUserId) {
        this.reopenedBySystemUserId = reopenedBySystemUserId;
    }

    public String getReopenedReason() {
        return reopenedReason;
    }

    public void setReopenedReason(String reopenedReason) {
        this.reopenedReason = reopenedReason;
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
