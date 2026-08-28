package com.vetsoftware.app.companycontactchannel.infrastructure.persistence;

import com.vetsoftware.app.companycontactchannel.domain.ContactChannelType;
import com.vetsoftware.app.companycontactchannel.domain.ContactPurpose;
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
 * {@code company_contact_channels} — por donde se le puede escribir a una
 * empresa, y con que permiso.
 *
 * <h2>{@code primary_marker} NO se mapea, y mapearla rompe la aplicacion</h2>
 *
 * <p>
 * La tabla tiene una columna mas de las que hay aqui:
 * {@code primary_marker BIGINT GENERATED ALWAYS AS (...) STORED}, que vale
 * {@code company_id} cuando el canal es primario y esta vivo, y {@code NULL} en
 * cualquier otro caso. Existe para que
 * {@code uq_company_contact_channels_primary} pueda ser un indice unico
 * ordinario —en un indice unico de MySQL dos {@code NULL} no chocan entre si,
 * asi que los canales no primarios y los revocados no compiten por el hueco—.
 *
 * <p>
 * <strong>Declararla como campo haria que Hibernate intentara escribirla y
 * MySQL rechazaria el {@code INSERT}</strong>: una columna generada no admite
 * valor. Es la misma trampa que documenta {@code DocumentWithholdingJpaEntity}
 * con {@code municipality_key}. Si algun dia hace falta leerla desde Java —hoy
 * no—, la unica forma es un campo {@code @Generated(event = {})} de solo
 * lectura, nunca un {@code @Column} corriente.
 *
 * <p>
 * <strong>Y el indice unico son DOS columnas, no la generada sola.</strong>
 * {@code (primary_marker, purpose)}: con la generada sola habria un unico canal
 * primario por empresa en total, y el correo primario de facturacion y el movil
 * primario de mora —propositos distintos, que conviven— fallarian el segundo.
 *
 * <h2>El resto de decisiones del mapeo</h2>
 *
 * <p>
 * <strong>Con {@code @Version}</strong>: la fila recibe una segunda escritura
 * declarada —la revocacion, y el relevo del marcador de primario— y declararla
 * exenta seria una exencion que miente. Dos operarios revocando desde la misma
 * pantalla se pisarian sin excepcion y sin log.
 *
 * <p>
 * <strong>Sin {@code enabled} y sin {@code @SQLDelete}</strong>: esto es una
 * bitacora probatoria y una prueba que se puede desactivar no prueba nada. Como
 * no hay borrado logico, tampoco existe aqui la trampa de los dos parametros
 * que documenta {@code BORRADO_LOGICO_RESPETA_LA_VERSION}.
 *
 * <p>
 * <strong>{@code is_primary} va como {@code boolean} sin
 * {@code columnDefinition}.</strong> El proyecto fija
 * {@code hibernate.type.preferred_boolean_jdbc_type: TINYINT}, asi que el mapeo
 * sale solo; escribir {@code TINYINT(1)} haria que el driver reportara la
 * columna como {@code BIT} y {@code ddl-auto: validate} tumbaria el arranque.
 *
 * <p>
 * <strong>La empresa va como escalar y no como {@code @ManyToOne}.</strong>
 * Esta rodaja no necesita ni un dato de la ficha de la empresa —solo saber de
 * quien es la fila—, asi que la asociacion solo anadiria un proxy que hidratar
 * y un {@code @EntityGraph} que mantener. La clave foranea sigue existiendo y
 * sigue vigilando en la base; lo que no existe es la navegacion desde Java.
 * <strong>Ojo</strong>: el campo se llama {@code companyId} y eso basta para
 * que las cuatro reglas duras de BE-COV vigilen la feature entera.
 */
@Entity
@Table(name = "company_contact_channels")
public class CompanyContactChannelJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false, length = 20)
    private ContactChannelType channelType;

    @Column(name = "address", nullable = false, length = 160)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 20)
    private ContactPurpose purpose;

    @Column(name = "authorized_at", nullable = false)
    private LocalDateTime authorizedAt;

    @Column(name = "authorization_evidence", nullable = false, length = 255)
    private String authorizationEvidence;

    /** Nulo si y solo si {@code revokedReason} lo es. */
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    /** Nulo si y solo si {@code revokedAt} lo es. */
    @Column(name = "revoked_reason", length = 255)
    private String revokedReason;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected CompanyContactChannelJpaEntity() {
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

    public ContactChannelType getChannelType() {
        return channelType;
    }

    public void setChannelType(ContactChannelType channelType) {
        this.channelType = channelType;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public ContactPurpose getPurpose() {
        return purpose;
    }

    public void setPurpose(ContactPurpose purpose) {
        this.purpose = purpose;
    }

    public LocalDateTime getAuthorizedAt() {
        return authorizedAt;
    }

    public void setAuthorizedAt(LocalDateTime authorizedAt) {
        this.authorizedAt = authorizedAt;
    }

    public String getAuthorizationEvidence() {
        return authorizationEvidence;
    }

    public void setAuthorizationEvidence(String authorizationEvidence) {
        this.authorizationEvidence = authorizationEvidence;
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

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
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
