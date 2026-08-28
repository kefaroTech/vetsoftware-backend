package com.vetsoftware.app.subscriptionpaymentmethod.infrastructure.persistence;

import com.vetsoftware.app.subscriptionpaymentmethod.domain.MandateStatus;
import com.vetsoftware.app.subscriptionpaymentmethod.domain.PaymentMethodKind;
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
 * {@code subscription_payment_methods} (changeset 319).
 *
 * <p>
 * <strong>{@code default_marker} NO se mapea, y es la decision mas importante
 * de esta clase.</strong> Es una columna {@code GENERATED ALWAYS AS (...)
 * STORED}: la calcula el motor a partir de {@code is_default} y
 * {@code mandate_status}, y sobre ella cuelga
 * {@code uq_subscription_payment_methods_default}, que es lo que garantiza un
 * solo predeterminado por empresa. Mapearla haria que Hibernate la incluyera en
 * el {@code INSERT} y en el {@code UPDATE}, y MySQL rechaza cualquier escritura
 * sobre una columna generada: fallarian <em>todas</em> las altas de la tabla.
 * Dejarla fuera es correcto tambien frente a {@code ddl-auto: validate}, que
 * comprueba que exista lo mapeado, no que se mapee todo lo que existe.
 *
 * <p>
 * <strong>{@code company_id} es un escalar y no una asociacion a
 * {@code CompanyJpaEntity}</strong>, igual que en
 * {@code SubscriptionPaymentJpaEntity}: es la forma que usan las tablas de
 * dinero de este bloque, evita un {@code @ManyToOne} que nadie navega, y hace
 * que el par {@code (company_id, id)} con el que {@code payment_attempts}
 * apunta a esta tabla resuelva contra una propiedad basica, que es el camino
 * robusto en Hibernate.
 *
 * <p>
 * <strong>Con {@code @Version}</strong>: el mandato se revoca y caduca, y la
 * marca de predeterminado se pone y se quita. Son segundas escrituras
 * declaradas, y sin bloqueo optimista dos de ellas simultaneas se pisarian sin
 * excepcion y sin log.
 *
 * <p>
 * <strong>Sin {@code @SQLDelete} ni {@code @SQLRestriction} a
 * proposito.</strong> La columna {@code enabled} existe, pero este slice no
 * borra en logico: dar de baja un medio de pago es <em>revocar su mandato</em>,
 * que es un hecho con fecha y motivo, no una fila invisible. Añadir el borrado
 * logico obligaria ademas a escribir {@code AND version = ?} en el
 * {@code WHERE} del {@code @SQLDelete} —Hibernate liga dos parametros en cuanto
 * la entidad lleva {@code @Version}— y esa es la bomba que describe
 * {@code BORRADO_LOGICO_RESPETA_LA_VERSION}.
 */
@Entity
@Table(name = "subscription_payment_methods")
public class SubscriptionPaymentMethodJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "method_kind", nullable = false, length = 20)
    private PaymentMethodKind methodKind;

    @Column(name = "gateway", nullable = false, length = 40)
    private String gateway;

    /** El testigo de la pasarela. Nunca el numero de la tarjeta. */
    @Column(name = "token", nullable = false, length = 255)
    private String token;

    @Column(name = "brand", length = 30)
    private String brand;

    @Column(name = "last_four", length = 4)
    private String lastFour;

    @Column(name = "expires_on")
    private LocalDate expiresOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "mandate_status", nullable = false, length = 20)
    private MandateStatus mandateStatus;

    @Column(name = "mandate_evidence", nullable = false, length = 255)
    private String mandateEvidence;

    @Column(name = "authorized_at", nullable = false)
    private LocalDateTime authorizedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoked_reason", length = 255)
    private String revokedReason;

    /**
     * Sin {@code columnDefinition}: el proyecto fija
     * {@code preferred_boolean_jdbc_type: TINYINT}, asi que un {@code boolean}
     * mapea a {@code tinyint} pelado. Forzar {@code TINYINT(1)} haria que el driver
     * lo reportara como {@code BIT} y rompiera {@code ddl-auto: validate}.
     */
    @Column(name = "is_default", nullable = false)
    private boolean defaultMethod;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected SubscriptionPaymentMethodJpaEntity() {
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

    public PaymentMethodKind getMethodKind() {
        return methodKind;
    }

    public void setMethodKind(PaymentMethodKind methodKind) {
        this.methodKind = methodKind;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getLastFour() {
        return lastFour;
    }

    public void setLastFour(String lastFour) {
        this.lastFour = lastFour;
    }

    public LocalDate getExpiresOn() {
        return expiresOn;
    }

    public void setExpiresOn(LocalDate expiresOn) {
        this.expiresOn = expiresOn;
    }

    public MandateStatus getMandateStatus() {
        return mandateStatus;
    }

    public void setMandateStatus(MandateStatus mandateStatus) {
        this.mandateStatus = mandateStatus;
    }

    public String getMandateEvidence() {
        return mandateEvidence;
    }

    public void setMandateEvidence(String mandateEvidence) {
        this.mandateEvidence = mandateEvidence;
    }

    public LocalDateTime getAuthorizedAt() {
        return authorizedAt;
    }

    public void setAuthorizedAt(LocalDateTime authorizedAt) {
        this.authorizedAt = authorizedAt;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    public String getRevokedReason() {
        return revokedReason;
    }

    public void setRevokedReason(String revokedReason) {
        this.revokedReason = revokedReason;
    }

    public boolean isDefaultMethod() {
        return defaultMethod;
    }

    public void setDefaultMethod(boolean defaultMethod) {
        this.defaultMethod = defaultMethod;
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
