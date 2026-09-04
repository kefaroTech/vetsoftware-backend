package com.vetsoftware.app.platformtaxprofile.infrastructure.persistence;

import com.vetsoftware.app.economicactivity.infrastructure.persistence.EconomicActivityJpaEntity;
import com.vetsoftware.app.platformtaxprofile.domain.PlatformDocumentType;
import com.vetsoftware.app.platformtaxprofile.domain.PlatformTaxRegime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * {@code platform_tax_profiles} — quien es Lumbre ante la DIAN.
 *
 * <h2>{@code current_profile_marker} NO se mapea, y es lo primero que hay que
 * saber de esta clase</h2>
 *
 * <p>
 * La columna existe en la tabla y es
 * {@code TINYINT GENERATED ALWAYS AS (CASE WHEN valid_to IS NULL THEN 1 ELSE
 * NULL END) STORED}: vale {@code 1} mientras la ficha no tenga fecha de cierre
 * y queda vacia cuando la tiene. Como dos vacios no chocan entre si,
 * {@code uq_platform_tax_profiles_current} deja caber la historia entera y a la
 * vez impone <strong>una sola identidad fiscal vigente en toda la
 * tabla</strong> — no una por empresa, porque aqui no hay empresa: el
 * discriminador es la constante {@code 1} y no {@code company_id} como en 316 y
 * 364.
 *
 * <p>
 * <strong>Mapearla aqui romperia todos los {@code INSERT}.</strong> Hibernate
 * la incluiria en la sentencia y MySQL rechaza cualquier escritura sobre una
 * columna {@code GENERATED ALWAYS} —error 3105— <em>aunque el valor que se le
 * mande sea {@code NULL}</em>. Es la misma trampa que documentan
 * {@code CompanyBillingProfileJpaEntity} y
 * {@code DocumentWithholdingJpaEntity}. La regla no tiene excepciones: la
 * columna la calcula el motor y Java no la ve.
 *
 * <p>
 * <strong>Y no es un indice disperso</strong> —el aviso que el propio changeset
 * 367 repite—: MySQL no tiene indices parciales y el indice si contiene todas
 * las filas. Lo que se gana es unicidad condicional, no tamaño.
 *
 * <h2>Esta clase NO alcanza {@code CompanyJpaEntity}, y no es estetica</h2>
 *
 * <p>
 * La tabla es global y no tiene columna de empresa. El dia que alguien le
 * cuelgue un {@code @ManyToOne} a {@code companies} «para saber quien la
 * registro», las cuatro reglas duras de aislamiento de BE-COV
 * —{@code TENANT_DEFENSA_EN_PROFUNDIDAD},
 * {@code CARGA_POR_ID_ACOTADA_POR_EMPRESA},
 * {@code OPERACIONES_POR_ID_SIN_EMPRESA_SOLO_SYSTEM} y
 * {@code MUTACIONES_SQL_ACOTADAS_POR_EMPRESA}— se activan sobre la feature
 * entera y rompen el build. La unica asociacion que lleva es a
 * {@code EconomicActivityJpaEntity}, que tampoco alcanza a {@code companies}.
 *
 * <h2>La actividad economica va como asociacion, y es opcional</h2>
 *
 * <p>
 * {@code economic_activity_id} es <strong>nulable</strong> en 367, asi que el
 * {@code @JoinColumn} no lleva {@code nullable = false} y el {@code @ManyToOne}
 * es {@code optional}. Va como asociacion y no como escalar porque el codigo y
 * el nombre de la actividad salen en la ficha; y por ser {@code LAZY} necesita
 * {@code @EntityGraph} en cada consulta del repositorio, o listar el historico
 * dispara una consulta por fila.
 *
 * <h2>Con {@code @Version} y sin borrado</h2>
 *
 * <p>
 * La ficha vigente muta una vez —al cerrarse— y dos operadores de plataforma
 * sucediendola a la vez se pisarian sin ruido.
 *
 * <p>
 * <strong>No hay {@code @SQLDelete} ni {@code @SQLRestriction}</strong>, asi
 * que aqui tampoco existe la trampa de los dos parametros que documenta
 * {@code BORRADO_LOGICO_RESPETA_LA_VERSION}. La tabla <em>ni siquiera tiene
 * columna {@code enabled}</em>: el cierre de una identidad es {@code valid_to}
 * y no hay ninguna otra forma de baja.
 */
@Entity
@Table(name = "platform_tax_profiles")
public class PlatformTaxProfileJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 30)
    private PlatformDocumentType documentType;

    @Column(name = "document_id", nullable = false, length = 20)
    private String documentId;

    @Column(name = "verification_digit", length = 1)
    private String verificationDigit;

    @Column(name = "legal_name", nullable = false, length = 255)
    private String legalName;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_regime", nullable = false, length = 30)
    private PlatformTaxRegime taxRegime;

    @Column(name = "fiscal_email", nullable = false, length = 255)
    private String fiscalEmail;

    @Column(name = "commercial_name", length = 150)
    private String commercialName;

    /** Opcional: la columna es nulable y la asociacion tambien. */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "economic_activity_id")
    private EconomicActivityJpaEntity economicActivity;

    /**
     * Si <strong>Lumbre</strong> es autorretenedor.
     *
     * <p>
     * <strong>Sin {@code columnDefinition}, y nunca {@code TINYINT(1)}.</strong> El
     * proyecto fija {@code hibernate.type.preferred_boolean_jdbc_type: TINYINT}, y
     * el display width {@code (1)} hace que el driver reporte la columna a JDBC
     * como {@code Types.BIT}: la validacion de esquema muere con
     * {@code found [bit (Types#BIT)], but expecting [tinyint (Types#TINYINT)]} y se
     * lleva por delante el arranque de la aplicacion entera.
     */
    @Column(name = "is_self_withholder", nullable = false)
    private boolean selfWithholder;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    /**
     * Fin exclusivo de la vigencia. Nulo = vigente, y es lo que alimenta la columna
     * generada.
     */
    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected PlatformTaxProfileJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PlatformDocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(PlatformDocumentType documentType) {
        this.documentType = documentType;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getVerificationDigit() {
        return verificationDigit;
    }

    public void setVerificationDigit(String verificationDigit) {
        this.verificationDigit = verificationDigit;
    }

    public String getLegalName() {
        return legalName;
    }

    public void setLegalName(String legalName) {
        this.legalName = legalName;
    }

    public PlatformTaxRegime getTaxRegime() {
        return taxRegime;
    }

    public void setTaxRegime(PlatformTaxRegime taxRegime) {
        this.taxRegime = taxRegime;
    }

    public String getFiscalEmail() {
        return fiscalEmail;
    }

    public void setFiscalEmail(String fiscalEmail) {
        this.fiscalEmail = fiscalEmail;
    }

    public String getCommercialName() {
        return commercialName;
    }

    public void setCommercialName(String commercialName) {
        this.commercialName = commercialName;
    }

    public EconomicActivityJpaEntity getEconomicActivity() {
        return economicActivity;
    }

    public void setEconomicActivity(EconomicActivityJpaEntity economicActivity) {
        this.economicActivity = economicActivity;
    }

    public boolean isSelfWithholder() {
        return selfWithholder;
    }

    public void setSelfWithholder(boolean selfWithholder) {
        this.selfWithholder = selfWithholder;
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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
