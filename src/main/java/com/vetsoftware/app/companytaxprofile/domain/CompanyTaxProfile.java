package com.vetsoftware.app.companytaxprofile.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * La ficha fiscal de la empresa: con que identidad emite sus documentos.
 *
 * <h2>No se edita en sitio: se sucede</h2>
 *
 * <p>
 * <strong>Reescribir la fila cambiaria hacia atras con que identidad se emitio
 * una factura de hace un ano.</strong> {@code electronic_documents} congela
 * seis campos del emisor —tipo y numero de documento, DV, razon social, regimen
 * y correo— desde los changesets 121/136, pero <em>no</em> el nombre comercial,
 * la actividad economica ni las responsabilidades fiscales, y esos tres si
 * viajan en el documento fiscal y en su representacion grafica. Antes del
 * changeset 364 se leian siempre de la fila viva, asi que cambiar el RUT hoy
 * reescribia lo que decia la factura de entonces.
 *
 * <p>
 * Por eso un cambio <strong>cierra</strong> la ficha vigente con
 * {@link #closeOn(LocalDate)} y <strong>abre otra</strong> con {@link #open},
 * las dos en una sola transaccion. Es el mismo molde que
 * {@code company_billing_profiles} (changeset 316), copiado y no reinventado.
 *
 * <h2>La vigencia es un intervalo semiabierto</h2>
 *
 * <p>
 * {@code [validFrom, validTo)}. La sucesora arranca el mismo dia en que la
 * anterior se cierra, asi que la historia queda cubierta entera sin hueco y sin
 * solape, y en cualquier fecha hay exactamente una ficha aplicable.
 * {@code validTo} nulo significa «vigente», y es lo que alimenta la columna
 * generada {@code current_profile_marker} que sostiene, con
 * {@code uq_company_tax_profiles_current}, que una empresa no pueda tener dos
 * fichas vigentes a la vez.
 *
 * <p>
 * <strong>{@code validTo} y {@code enabled} no son lo mismo.</strong> El cierre
 * de una ficha es {@code validTo}; {@code enabled} sigue siendo la baja logica
 * heredada del esquema. Confundirlos dejaria el historico con un hueco justo
 * donde vive la explicacion de una factura.
 */
public class CompanyTaxProfile {
    private static final String EMAIL_REGEX = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";

    private Long id;
    private CompanyRef company;
    private CompanyDocumentType companyDocumentType;
    private String companyDocumentId;
    private String companyDocumentVerificationDigit;
    private String legalName;
    private TaxRegime taxRegime;
    private String fiscalEmail;
    private String commercialName;
    private EconomicActivityRef economicActivity;
    private List<CompanyTaxProfileResponsibility> responsibilities;

    /** Desde cuando rige esta ficha, inclusive. Nunca nulo. */
    private final LocalDate validFrom;

    /** Fin de la vigencia, exclusivo. Nulo si y solo si la ficha es la vigente. */
    private LocalDate validTo;

    private final LocalDateTime createdDate;
    private Long version;
    private boolean enabled;

    /**
     * Constructor sin vigencia explicita: la ficha <strong>nace vigente</strong> y
     * su {@code validFrom} es el dia de creacion.
     *
     * <p>
     * Es la forma que usan los fixtures y los caminos que no orquestan sucesion. El
     * camino de persistencia siempre pasa por el constructor completo, porque la
     * fila de la base trae sus dos fechas.
     *
     * <p>
     * <strong>La vigencia espeja a {@code createdDate} y no se inventa con un
     * {@code now()}</strong> ({@code RELOJ_INYECTADO_EN_VEZ_DE_NOW}): sin fecha de
     * creacion la ficha se queda tambien sin {@code validFrom}, y las dos columnas
     * son {@code NOT NULL}, asi que un perfil asi no llega a la base por dos
     * motivos en vez de por uno. Quien necesite fijar el dia usa {@link #open} con
     * el reloj inyectado de su caso de uso.
     */
    public CompanyTaxProfile(Long id, CompanyRef company, CompanyDocumentType companyDocumentType,
            String companyDocumentId, String companyDocumentVerificationDigit, String legalName,
            TaxRegime taxRegime, String fiscalEmail, String commercialName,
            EconomicActivityRef economicActivity,
            List<CompanyTaxProfileResponsibility> responsibilities, LocalDateTime createdDate,
            Long version, boolean enabled) {
        this(id, company, companyDocumentType, companyDocumentId, companyDocumentVerificationDigit,
                legalName, taxRegime, fiscalEmail, commercialName, economicActivity,
                responsibilities, createdDate == null ? null : createdDate.toLocalDate(), null,
                createdDate, version, enabled);
    }

    public CompanyTaxProfile(Long id, CompanyRef company, CompanyDocumentType companyDocumentType,
            String companyDocumentId, String companyDocumentVerificationDigit, String legalName,
            TaxRegime taxRegime, String fiscalEmail, String commercialName,
            EconomicActivityRef economicActivity,
            List<CompanyTaxProfileResponsibility> responsibilities, LocalDate validFrom,
            LocalDate validTo, LocalDateTime createdDate, Long version, boolean enabled) {
        validate(company, companyDocumentType, companyDocumentId, companyDocumentVerificationDigit,
                legalName, taxRegime, fiscalEmail);
        validateValidity(validFrom, validTo);
        this.id = id;
        this.company = company;
        this.companyDocumentType = companyDocumentType;
        this.companyDocumentId = companyDocumentId;
        this.companyDocumentVerificationDigit = (companyDocumentVerificationDigit == null
                || companyDocumentVerificationDigit.isBlank())
                        ? null
                        : companyDocumentVerificationDigit;
        this.legalName = legalName;
        this.taxRegime = taxRegime;
        this.fiscalEmail = fiscalEmail;
        this.commercialName = commercialName;
        this.economicActivity = economicActivity;
        this.responsibilities = responsibilities == null
                ? List.of()
                : List.copyOf(responsibilities);
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.createdDate = createdDate;
        this.version = version;
        this.enabled = enabled;
    }

    public static CompanyTaxProfile create(CompanyRef company,
            CompanyDocumentType companyDocumentType, String companyDocumentId,
            String companyDocumentVerificationDigit, String legalName, TaxRegime taxRegime,
            String fiscalEmail, String commercialName, EconomicActivityRef economicActivity,
            List<CompanyTaxProfileResponsibility> responsibilities) {
        LocalDateTime ahora = LocalDateTime.now();
        return open(company, companyDocumentType, companyDocumentId,
                companyDocumentVerificationDigit, legalName, taxRegime, fiscalEmail, commercialName,
                economicActivity, responsibilities, ahora.toLocalDate(), ahora);
    }

    /**
     * Ficha nueva: nace <strong>vigente</strong> —{@code validTo} nulo, que es lo
     * que hace valer la columna generada— y sin id.
     *
     * <p>
     * Es la unica puerta de entrada y sirve tanto para la primera ficha de una
     * empresa como para la sucesora de una que se acaba de cerrar. No hay una
     * factoria «sucesora» aparte a proposito: la sucesora no es una ficha de
     * segunda clase, es una ficha entera con sus datos completos y su propia fecha
     * de inicio. Lo que las encadena es el cierre de la anterior, y eso lo orquesta
     * el caso de uso dentro de una transaccion.
     *
     * @param validFrom
     *            desde cuando rige. En la sucesion es la misma fecha con la que se
     *            cerro la anterior, porque el intervalo es semiabierto
     * @param createdDate
     *            del reloj inyectado del caso de uso
     */
    public static CompanyTaxProfile open(CompanyRef company,
            CompanyDocumentType companyDocumentType, String companyDocumentId,
            String companyDocumentVerificationDigit, String legalName, TaxRegime taxRegime,
            String fiscalEmail, String commercialName, EconomicActivityRef economicActivity,
            List<CompanyTaxProfileResponsibility> responsibilities, LocalDate validFrom,
            LocalDateTime createdDate) {
        return new CompanyTaxProfile(null, company, companyDocumentType, companyDocumentId,
                companyDocumentVerificationDigit, legalName, taxRegime, fiscalEmail, commercialName,
                economicActivity, responsibilities, validFrom, null, createdDate, null, true);
    }

    /**
     * Cierra la ficha para que otra pueda sucederla.
     *
     * <p>
     * <strong>La fecha es la del comienzo de la sucesora</strong>, no la del ultimo
     * dia de esta: el intervalo es semiabierto, asi que esta ficha cubre hasta la
     * vispera y la nueva rige desde {@code effectiveFrom} inclusive. Escribirlo al
     * reves —cerrar en la vispera— dejaria el ultimo dia sin ficha aplicable o,
     * peor, con dos.
     *
     * <p>
     * <strong>La sucesion en el mismo dia no es representable</strong>, y no se
     * adelanta al dia siguiente por cuenta propia:
     * {@code chk_company_tax_profiles_validity} exige {@code valid_to > valid_from}
     * estricto, y esa fecha es la que decide con que ficha se emitio un documento
     * del intervalo. Inventarla aqui seria decidir en silencio a que identidad
     * fiscal pertenece una factura.
     */
    public void closeOn(LocalDate effectiveFrom) {
        if (validFrom == null)
            throw new IllegalArgumentException("El perfil fiscal " + id
                    + " no tiene fecha de inicio de vigencia: no se puede suceder");
        if (!isCurrent())
            throw new IllegalArgumentException("El perfil fiscal " + id
                    + " ya esta cerrado desde el " + validTo + ": no se puede volver a suceder");
        if (effectiveFrom == null)
            throw new IllegalArgumentException("effectiveFrom is required");
        if (!effectiveFrom.isAfter(validFrom))
            throw new IllegalArgumentException("El perfil fiscal vigente de la empresa "
                    + (company == null ? null : company.id()) + " rige desde el " + validFrom
                    + ", asi que su sucesor no puede empezar antes del " + validFrom.plusDays(1)
                    + " (pedido: " + effectiveFrom + ")");
        this.validTo = effectiveFrom;
    }

    /** La ficha que rige hoy: la unica de la empresa sin fecha de cierre. */
    public boolean isCurrent() {
        return validTo == null;
    }

    /**
     * <strong>{@code validFrom} nulo se tolera y {@code validTo} sin el
     * no.</strong> Un perfil sin fecha de inicio es el mismo caso que uno sin
     * {@code createdDate}: no es persistible -las dos columnas son
     * {@code NOT NULL}- y el motor lo para. Un perfil <em>cerrado</em> sin fecha de
     * inicio, en cambio, es un intervalo que no significa nada, y ese si se para
     * aqui.
     */
    private static void validateValidity(LocalDate validFrom, LocalDate validTo) {
        if (validTo == null)
            return;
        if (validFrom == null)
            throw new IllegalArgumentException("validTo requires validFrom");
        if (!validTo.isAfter(validFrom))
            throw new IllegalArgumentException(
                    "validTo must be after validFrom (chk_company_tax_profiles_validity)");
    }

    private static void validate(CompanyRef company, CompanyDocumentType companyDocumentType,
            String companyDocumentId, String companyDocumentVerificationDigit, String legalName,
            TaxRegime taxRegime, String fiscalEmail) {
        if (company == null)
            throw new IllegalArgumentException("company is required");
        if (companyDocumentType == null)
            throw new IllegalArgumentException("companyDocumentType is required");
        if (taxRegime == null)
            throw new IllegalArgumentException("taxRegime is required");

        if (companyDocumentId == null || companyDocumentId.isBlank())
            throw new IllegalArgumentException("companyDocumentId is required");
        if (companyDocumentId.length() > 20)
            throw new IllegalArgumentException("companyDocumentId must be 20 chars or less");

        if (legalName == null || legalName.isBlank())
            throw new IllegalArgumentException("legalName is required");
        if (legalName.length() > 255)
            throw new IllegalArgumentException("legalName must be 255 chars or less");

        if (fiscalEmail == null || fiscalEmail.isBlank())
            throw new IllegalArgumentException("fiscalEmail is required");
        if (fiscalEmail.length() > 255)
            throw new IllegalArgumentException("fiscalEmail must be 255 chars or less");
        if (!fiscalEmail.matches(EMAIL_REGEX))
            throw new IllegalArgumentException("fiscalEmail must be a valid email");

        boolean hasVerificationDigit = companyDocumentVerificationDigit != null
                && !companyDocumentVerificationDigit.isBlank();
        if (companyDocumentType == CompanyDocumentType.NIT) {
            if (!hasVerificationDigit)
                throw new IllegalArgumentException(
                        "companyDocumentVerificationDigit is required when document type is NIT");
            if (!companyDocumentVerificationDigit.matches("[0-9]"))
                throw new IllegalArgumentException(
                        "companyDocumentVerificationDigit must be a single digit 0-9");
        } else if (hasVerificationDigit) {
            throw new IllegalArgumentException(
                    "companyDocumentVerificationDigit is only allowed when document type is NIT");
        }
    }

    public Long getId() {
        return id;
    }

    public CompanyRef getCompany() {
        return company;
    }

    public CompanyDocumentType getCompanyDocumentType() {
        return companyDocumentType;
    }

    public String getCompanyDocumentId() {
        return companyDocumentId;
    }

    public String getCompanyDocumentVerificationDigit() {
        return companyDocumentVerificationDigit;
    }

    public String getLegalName() {
        return legalName;
    }

    public TaxRegime getTaxRegime() {
        return taxRegime;
    }

    public String getFiscalEmail() {
        return fiscalEmail;
    }

    public String getCommercialName() {
        return commercialName;
    }

    public EconomicActivityRef getEconomicActivity() {
        return economicActivity;
    }

    public List<CompanyTaxProfileResponsibility> getResponsibilities() {
        return responsibilities;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Long getVersion() {
        return version;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }
}
