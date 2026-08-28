package com.vetsoftware.app.companybillingprofile.infrastructure.persistence;

import com.vetsoftware.app.city.infrastructure.persistence.CityJpaEntity;
import com.vetsoftware.app.companybillingprofile.domain.PersonKind;
import com.vetsoftware.app.companybillingprofile.domain.TaxIdKind;
import com.vetsoftware.app.companybillingprofile.domain.TaxRegime;
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
 * {@code company_billing_profiles} — a quien se le factura.
 *
 * <h2>{@code current_profile_marker} NO se mapea, y es lo primero que hay que
 * saber de esta clase</h2>
 *
 * <p>
 * La columna existe en la tabla y es
 * {@code BIGINT GENERATED ALWAYS AS (CASE WHEN valid_to IS NULL THEN company_id
 * ELSE NULL END) STORED}: vale la empresa mientras la ficha no tenga fecha de
 * cierre y queda vacia cuando la tiene. Como dos vacios no chocan entre si,
 * {@code uq_company_billing_profiles_current} deja caber la historia entera y a
 * la vez impone <strong>una sola ficha vigente por empresa</strong>.
 *
 * <p>
 * <strong>Mapearla aqui romperia todos los {@code INSERT}.</strong> Hibernate
 * la incluiria en la sentencia y MySQL rechaza cualquier escritura sobre una
 * columna {@code GENERATED ALWAYS} —error 3105— <em>aunque el valor que se le
 * mande sea {@code NULL}</em>. Es la misma trampa que documenta
 * {@code DocumentWithholdingJpaEntity} con {@code municipality_key} y la que
 * obliga a {@code SchemaSeed} a no nombrar {@code subscriptions.active_marker}.
 * La regla no tiene excepciones: la columna la calcula el motor y Java no la
 * ve.
 *
 * <p>
 * Y no es un indice disperso: MySQL no tiene indices parciales y si indexa los
 * vacios. Lo que se gana es unicidad condicional, no tamaño.
 *
 * <h2>La empresa va como escalar y el municipio como asociacion</h2>
 *
 * <p>
 * {@code company_id} es el <strong>tenant</strong>, no un dato de la ficha: no
 * hay nada que leer de {@code companies} para pintar una factura, y un
 * {@code @ManyToOne} hacia alli obligaria a hidratar la empresa entera en cada
 * lectura. {@code city_id} si es un dato —el municipio aparece en la direccion
 * de facturacion—, asi que va como asociacion {@code LAZY} con
 * {@code @EntityGraph} en cada consulta del repositorio: sin el, listar el
 * historico dispara una consulta por fila.
 *
 * <h2>Con {@code @Version} y sin borrado logico</h2>
 *
 * <p>
 * La ficha vigente muta una vez —al cerrarse— y dos administradores sucediendo
 * a la vez se pisarian sin ruido.
 *
 * <p>
 * <strong>No hay {@code @SQLDelete} ni {@code @SQLRestriction}</strong>, asi
 * que aqui tampoco existe la trampa de los dos parametros que documenta
 * {@code BORRADO_LOGICO_RESPETA_LA_VERSION}. La columna {@code enabled} esta en
 * la tabla por convencion del esquema y vale siempre {@code true}: el cierre de
 * una ficha es {@code valid_to} y nunca {@code enabled = false}. Por eso no
 * viaja al dominio — tener las dos bajas visibles en el modelo es la forma
 * segura de que alguien acabe usando la que no es.
 */
@Entity
@Table(name = "company_billing_profiles")
public class CompanyBillingProfileJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "person_kind", nullable = false, length = 20)
    private PersonKind personKind;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_id_kind", nullable = false, length = 30)
    private TaxIdKind taxIdKind;

    /**
     * <strong>Sin {@code columnDefinition}.</strong> El juego de caracteres
     * {@code ascii} y la colacion {@code ascii_bin} los fija el changeset 316 con
     * un {@code MODIFY COLUMN}; declararlos otra vez aqui seria duplicar la
     * decision en dos sitios que pueden divergir, y el que manda es el esquema.
     */
    @Column(name = "tax_id", nullable = false, length = 50)
    private String taxId;

    @Column(name = "verification_digit", length = 1)
    private String verificationDigit;

    @Column(name = "legal_name", length = 255)
    private String legalName;

    @Column(name = "first_name", length = 80)
    private String firstName;

    @Column(name = "middle_name", length = 80)
    private String middleName;

    @Column(name = "last_name", length = 80)
    private String lastName;

    @Column(name = "second_last_name", length = 80)
    private String secondLastName;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", nullable = false)
    private CityJpaEntity city;

    @Column(name = "billing_email", nullable = false, length = 160)
    private String billingEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_regime", nullable = false, length = 30)
    private TaxRegime taxRegime;

    /**
     * Si el <strong>cliente</strong> es agente de retencion.
     *
     * <p>
     * <strong>Sin {@code columnDefinition}, y nunca {@code TINYINT(1)}.</strong> El
     * proyecto fija {@code hibernate.type.preferred_boolean_jdbc_type: TINYINT}, y
     * el display width {@code (1)} hace que el driver reporte la columna a JDBC
     * como {@code Types.BIT}: la validacion de esquema muere con
     * {@code found [bit (Types#BIT)], but expecting [tinyint (Types#TINYINT)]} y se
     * lleva por delante el arranque de la aplicacion entera.
     */
    @Column(name = "withholding_agent", nullable = false)
    private boolean withholdingAgent;

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

    /**
     * Siempre {@code true}. No hay camino de baja logica en esta feature y por eso
     * no hay {@code setter}: el cierre de una ficha es {@code valid_to}.
     */
    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected CompanyBillingProfileJpaEntity() {
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

    public PersonKind getPersonKind() {
        return personKind;
    }

    public void setPersonKind(PersonKind personKind) {
        this.personKind = personKind;
    }

    public TaxIdKind getTaxIdKind() {
        return taxIdKind;
    }

    public void setTaxIdKind(TaxIdKind taxIdKind) {
        this.taxIdKind = taxIdKind;
    }

    public String getTaxId() {
        return taxId;
    }

    public void setTaxId(String taxId) {
        this.taxId = taxId;
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

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getSecondLastName() {
        return secondLastName;
    }

    public void setSecondLastName(String secondLastName) {
        this.secondLastName = secondLastName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public CityJpaEntity getCity() {
        return city;
    }

    public void setCity(CityJpaEntity city) {
        this.city = city;
    }

    public String getBillingEmail() {
        return billingEmail;
    }

    public void setBillingEmail(String billingEmail) {
        this.billingEmail = billingEmail;
    }

    public TaxRegime getTaxRegime() {
        return taxRegime;
    }

    public void setTaxRegime(TaxRegime taxRegime) {
        this.taxRegime = taxRegime;
    }

    public boolean isWithholdingAgent() {
        return withholdingAgent;
    }

    public void setWithholdingAgent(boolean withholdingAgent) {
        this.withholdingAgent = withholdingAgent;
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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
