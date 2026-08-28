package com.vetsoftware.app.billingdocumentstatushistory.infrastructure.persistence;

import com.vetsoftware.app.billingdocumentstatushistory.domain.BillingDocumentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * {@code billing_document_status_history} — la pelicula del documento de cobro.
 *
 * <p>
 * <strong>Sin {@code @Version} y sin {@code enabled}, y las dos ausencias son
 * la misma decision.</strong> La tabla solo se agrega: no hay ninguna escritura
 * declarada sobre una fila existente, asi que no hay ciclo
 * leer-modificar-guardar que el bloqueo optimista pueda proteger, y una fila
 * que se pudiera desactivar no probaria nada en una bitacora que existe
 * precisamente para ser irrefutable. La exencion va escrita en
 * {@code ENTIDADES_EXENTAS_DE_VERSION} con el codigo {@code E1_APPEND_ONLY};
 * sin esa linea, {@code ENTIDADES_CON_BLOQUEO_OPTIMISTA} rompe el build.
 *
 * <p>
 * <strong>La FK compuesta va como columnas escalares y NO como
 * {@code @ManyToOne}, y no es pereza.</strong> La referencia contra
 * {@code subscription_billing_documents} es
 * {@code (company_id, billing_document_id)} → {@code (company_id, id)}, y
 * {@code company_id} es a la vez columna propia de esta tabla —el filtro de
 * tenant de todas sus consultas— y mitad de esa clave. Mapeada como asociacion
 * habria que declarar dos {@code @JoinColumn} sobre la misma columna fisica que
 * ya mapea la propiedad {@code companyId}; Hibernate exige que una columna
 * tenga un unico mapeo dueño de su escritura, asi que la asociacion tendria que
 * ir {@code insertable = false, updatable = false} y <strong>la empresa dejaria
 * de escribirse</strong>. Y el desenlace no es una fila mala: es que el
 * {@code entityManagerFactory} no arranca y se lleva por delante la aplicacion
 * entera, con un mensaje que no señala a esta clase. Es la trampa que documenta
 * {@code DocumentWithholdingJpaEntity} sobre sus dos FK compuestas.
 *
 * <p>
 * Sin asociaciones tampoco hay N+1 que evitar ni {@code @EntityGraph} que
 * poner. La FK sigue existiendo y sigue vigilando en la base —un fotograma no
 * puede colgar de la factura de otra empresa—; lo que no existe es la
 * navegacion desde Java.
 *
 * <p>
 * <strong>Los dos estados son {@code @Enumerated(EnumType.STRING)}</strong>
 * sobre {@code VARCHAR(20)}, que es lo que hace que el {@code name()} del enum
 * y la lista de {@code chk_bdsh_statuses} tengan que coincidir letra a letra.
 * Un {@code ORDINAL} guardaria un numero que la comprobacion rechazaria entero,
 * y reordenar el enum reescribiria la historia ya guardada.
 */
@Entity
@Table(name = "billing_document_status_history")
public class BillingDocumentStatusHistoryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "billing_document_id", nullable = false)
    private Long billingDocumentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", nullable = false, length = 20)
    private BillingDocumentStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private BillingDocumentStatus toStatus;

    /**
     * {@code DATETIME(6)}: la precision de microsegundos no es lujo. Varios
     * fotogramas del mismo documento pueden caer en el mismo segundo cuando los
     * mueve el proceso automatico, y con precision de segundo el orden entre ellos
     * lo decidiria solo el desempate por {@code id}.
     */
    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "actor", nullable = false, length = 120)
    private String actor;

    @Column(name = "reason", nullable = false, length = 255)
    private String reason;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    protected BillingDocumentStatusHistoryJpaEntity() {
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

    public Long getBillingDocumentId() {
        return billingDocumentId;
    }

    public void setBillingDocumentId(Long billingDocumentId) {
        this.billingDocumentId = billingDocumentId;
    }

    public BillingDocumentStatus getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(BillingDocumentStatus fromStatus) {
        this.fromStatus = fromStatus;
    }

    public BillingDocumentStatus getToStatus() {
        return toStatus;
    }

    public void setToStatus(BillingDocumentStatus toStatus) {
        this.toStatus = toStatus;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }
}
