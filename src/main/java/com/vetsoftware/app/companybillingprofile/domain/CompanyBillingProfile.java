package com.vetsoftware.app.companybillingprofile.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * A quien se le factura: la ficha del tercero que recibe las facturas de Lumbre
 * por la suscripcion de una clinica.
 *
 * <h2>Esta ficha NO se edita: se sucede</h2>
 *
 * <p>
 * <strong>No hay {@code update(...)} en esta clase, y su ausencia es el diseño
 * entero.</strong> Ana atiende como Spa Ana Pet y factura a Inversiones Pet
 * SAS; el dia que cambia de sociedad, cambiar el NIT <em>en la fila</em>
 * reescribiria hacia atras a quien se le emitieron las facturas del año pasado.
 * Una factura tiene que seguir diciendo a quien se emitio, asi que el cambio de
 * datos <strong>cierra</strong> la ficha vigente poniendole
 * {@link #closeOn(LocalDate)} una fecha de fin y <strong>abre otra</strong> con
 * {@link #open}, las dos en una sola transaccion. Por eso tampoco hay
 * {@code PUT} en el controller ni comando de actualizacion.
 *
 * <p>
 * <strong>Tampoco se borra.</strong> No hay {@code delete}, ni baja logica, ni
 * {@code @SQLDelete}. El cierre de una ficha es {@code valid_to}, nunca
 * {@code enabled = false}: son dos cosas distintas y confundirlas dejaria el
 * historico con un hueco justo donde vive la explicacion de una factura.
 *
 * <h2>La vigencia es un intervalo semiabierto</h2>
 *
 * <p>
 * {@code [valid_from, valid_to)}. La sucesora arranca el mismo dia en que la
 * anterior se cierra, asi que la historia queda cubierta entera sin hueco y sin
 * solape, y en cualquier fecha hay exactamente una ficha aplicable.
 * {@code valid_to} nulo significa «vigente», y es lo que alimenta la columna
 * generada {@code current_profile_marker} que sostiene, con su indice unico,
 * que una empresa no pueda tener dos fichas vigentes a la vez.
 *
 * <p>
 * <strong>El corolario incomodo va escrito en
 * {@link BillingProfileSuccessionNotAfterCurrentException}</strong>: como el
 * {@code CHECK} del esquema es {@code valid_to > valid_from} <em>estricto</em>,
 * una ficha abierta hoy no se puede suceder hoy.
 *
 * <h2>El nombre va partido en cuatro, no en uno</h2>
 *
 * <p>
 * La informacion exogena anual exige primer nombre, otros nombres, primer
 * apellido y segundo apellido <strong>por separado</strong> cuando el tercero
 * es persona natural. Guardarlo junto obliga a partirlo despues a mano, cliente
 * por cliente, y eso no se rellena hacia atras. Cual de los dos juegos de
 * columnas se usa lo decide {@link PersonKind}, y las dos mitades de esa regla
 * —lo que tiene que estar y lo que tiene que faltar— son el espejo de
 * {@code chk_company_billing_profiles_name_shape}.
 *
 * <h2>{@code withholdingAgent} es del CLIENTE</h2>
 *
 * <p>
 * Guarda si <strong>el cliente</strong> es agente de retencion. Que Lumbre sea
 * autorretenedor es dato propio y vive en
 * {@code platform_tax_profiles.is_self_withholder} (changeset 367): la norma
 * dice que las dos condiciones <em>coexisten</em>, asi que deducir una de la
 * otra seria falso. Las tarifas que se espera que aplique tampoco viven aqui
 * —van en {@code withholding_rate_rules}, que es global— porque son datos que
 * se leen para calcular dinero y la base tiene que poder comprobar sus rangos.
 *
 * <p>
 * <strong>Correccion de un dato que este javadoc afirmaba y era falso.</strong>
 * Hasta el changeset 367 esta clase decia que la autorretencion propia vivia en
 * {@code platform_billing_config}. Se verifico y no era cierto: esa tabla (255)
 * es un singleton de nueve columnas —lista de precios por defecto, dias de
 * gracia, dias de prueba, dia de facturacion, plazo de pago, proveedor externo—
 * y ninguna es razon social, identificador fiscal ni marca de autorretencion.
 * Era una decision de modelado apoyada en una columna que nadie habia creado.
 * {@code 316_create_company_billing_profiles.xml} repite la misma afirmacion en
 * su comentario y <b>no se corrige</b>: es un changeset ya aplicado y editarlo
 * romperia su checksum.
 *
 * <p>
 * <strong>La tabla nueva existe pero todavia no tiene ninguna fila</strong>, y
 * es deliberado: no habia razon social ni NIT reales de Lumbre y no se
 * inventaron, porque una identidad fiscal inventada acaba impresa en la factura
 * de cada cliente. Sembrar la primera es una decision del dueño.
 *
 * <h2>Con {@code version}</h2>
 *
 * <p>
 * La ficha vigente muta —una vez, al cerrarse— y dos administradores de la
 * misma empresa sucediendola a la vez se pisarian sin ruido. El bloqueo
 * optimista para al segundo.
 */
public class CompanyBillingProfile {

    private static final int MAX_TAX_ID_LENGTH = 50;
    private static final int MAX_LEGAL_NAME_LENGTH = 255;
    private static final int MAX_PERSON_NAME_LENGTH = 80;
    private static final int MAX_ADDRESS_LENGTH = 255;
    private static final int MAX_BILLING_EMAIL_LENGTH = 160;
    private static final int MAX_ASCII_CODE_POINT = 127;

    /**
     * Espejo exacto de {@code billing_email LIKE '%_@_%._%'}: al menos un caracter
     * antes de la arroba, al menos uno despues, un punto mas adelante y al menos un
     * caracter tras el punto. <strong>No es una validacion de correo</strong> —no
     * pretende serlo, y escribir aqui algo mas estricto que la base haria que el
     * dominio rechazara direcciones que la tabla si admite—; es la misma criba
     * minima, escrita en el sitio donde el mensaje de error puede nombrar el campo.
     */
    private static final Pattern BILLING_EMAIL_SHAPE = Pattern.compile("^.+@.+\\..+$");

    private final Long id;

    /** El tenant. Escalar y no companion VO: la empresa es la dueña, no un dato. */
    private final Long companyId;

    private final PersonKind personKind;
    private final TaxIdKind taxIdKind;

    /**
     * El documento, tal cual. La columna es {@code ascii_bin}: se compara byte a
     * byte y no admite un solo caracter fuera de ASCII.
     */
    private final String taxId;

    /** Digito de verificacion. Solo el NIT lo tiene, y la tabla lo deja nulo. */
    private final String verificationDigit;

    private final String legalName;
    private final String firstName;
    private final String middleName;
    private final String lastName;
    private final String secondLastName;

    private final String address;
    private final CityRef city;
    private final String billingEmail;
    private final TaxRegime taxRegime;

    /**
     * Si el CLIENTE es agente de retencion. Nada que ver con la autorretencion
     * propia.
     */
    private final boolean withholdingAgent;

    private final LocalDate validFrom;

    /** Fin de la vigencia, exclusivo. Nulo si y solo si la ficha es la vigente. */
    private LocalDate validTo;

    private final LocalDateTime createdDate;
    private final Long version;

    public CompanyBillingProfile(Long id, Long companyId, PersonKind personKind,
            TaxIdKind taxIdKind, String taxId, String verificationDigit, String legalName,
            String firstName, String middleName, String lastName, String secondLastName,
            String address, CityRef city, String billingEmail, TaxRegime taxRegime,
            boolean withholdingAgent, LocalDate validFrom, LocalDate validTo,
            LocalDateTime createdDate, Long version) {
        validate(companyId, personKind, taxIdKind, taxId, verificationDigit, legalName, firstName,
                middleName, lastName, secondLastName, address, city, billingEmail, taxRegime,
                validFrom, validTo);
        this.id = id;
        this.companyId = companyId;
        this.personKind = personKind;
        this.taxIdKind = taxIdKind;
        this.taxId = taxId;
        this.verificationDigit = verificationDigit;
        this.legalName = legalName;
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.secondLastName = secondLastName;
        this.address = address;
        this.city = city;
        this.billingEmail = billingEmail;
        this.taxRegime = taxRegime;
        this.withholdingAgent = withholdingAgent;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.createdDate = createdDate;
        this.version = version;
    }

    /**
     * Ficha nueva: nace <strong>vigente</strong> —{@code validTo} nulo, que es lo
     * que hace valer la columna generada— y sin id.
     *
     * <p>
     * Es la unica puerta de entrada, y sirve tanto para la primera ficha de una
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
     *            del reloj inyectado del caso de uso, nunca de un
     *            {@code LocalDateTime.now()} pelado
     */
    public static CompanyBillingProfile open(Long companyId, PersonKind personKind,
            TaxIdKind taxIdKind, String taxId, String verificationDigit, String legalName,
            String firstName, String middleName, String lastName, String secondLastName,
            String address, CityRef city, String billingEmail, TaxRegime taxRegime,
            boolean withholdingAgent, LocalDate validFrom, LocalDateTime createdDate) {
        return new CompanyBillingProfile(null, companyId, personKind, taxIdKind, taxId,
                verificationDigit, legalName, firstName, middleName, lastName, secondLastName,
                address, city, billingEmail, taxRegime, withholdingAgent, validFrom, null,
                createdDate, null);
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
     * @throws CompanyBillingProfileAlreadyClosedException
     *             si ya tenia fecha de cierre
     * @throws BillingProfileSuccessionNotAfterCurrentException
     *             si la fecha no es estrictamente posterior a {@code validFrom}:
     *             {@code chk_company_billing_profiles_validity} no lo admite y
     *             {@code uq_company_billing_profiles_validity} tampoco dejaria
     *             entrar a la sucesora
     */
    public void closeOn(LocalDate effectiveFrom) {
        if (!isCurrent())
            throw new CompanyBillingProfileAlreadyClosedException(id, validTo);
        if (effectiveFrom == null)
            throw new IllegalArgumentException("effectiveFrom is required");
        if (!effectiveFrom.isAfter(validFrom))
            throw new BillingProfileSuccessionNotAfterCurrentException(companyId, validFrom,
                    effectiveFrom);
        this.validTo = effectiveFrom;
    }

    /** La ficha que rige hoy: la unica de la empresa sin fecha de cierre. */
    public boolean isCurrent() {
        return validTo == null;
    }

    /**
     * <strong>No hay {@code displayName()} que componga el nombre en una
     * cadena.</strong> Los cuatro campos estan partidos porque la informacion
     * exogena los exige por separado; ofrecer aqui el atajo que los junta invita a
     * guardarlo junto mas adelante, que es justo lo que esta tabla existe para
     * evitar. Quien tenga que pintarlo lo compone en su capa.
     */
    private static void validate(Long companyId, PersonKind personKind, TaxIdKind taxIdKind,
            String taxId, String verificationDigit, String legalName, String firstName,
            String middleName, String lastName, String secondLastName, String address, CityRef city,
            String billingEmail, TaxRegime taxRegime, LocalDate validFrom, LocalDate validTo) {
        if (companyId == null)
            throw new IllegalArgumentException("companyId is required");
        if (personKind == null)
            throw new IllegalArgumentException("personKind is required");
        if (taxIdKind == null)
            throw new IllegalArgumentException("taxIdKind is required");
        validateTaxId(taxId);
        validateVerificationDigit(verificationDigit);
        validateNameShape(personKind, legalName, firstName, middleName, lastName, secondLastName);
        if (address == null || address.isBlank())
            throw new IllegalArgumentException("address is required");
        if (address.length() > MAX_ADDRESS_LENGTH)
            throw new IllegalArgumentException("address must be 255 chars or less");
        if (city == null)
            throw new IllegalArgumentException("city is required");
        validateBillingEmail(billingEmail);
        if (taxRegime == null)
            throw new IllegalArgumentException("taxRegime is required");
        validateValidity(validFrom, validTo);
    }

    /**
     * La columna es {@code VARCHAR(50) CHARACTER SET ascii COLLATE ascii_bin}. Un
     * caracter fuera de ASCII no lo trunca la base: lo <em>rechaza</em> con un
     * {@code Incorrect string value} que no dice de que fila ni de que campo viene.
     * Comprobarlo aqui convierte ese error del driver en un mensaje que nombra el
     * campo, y ademas deja escrito que la comparacion posterior es exacta —dos
     * documentos que solo difieren en mayusculas son dos documentos distintos—.
     */
    private static void validateTaxId(String taxId) {
        if (taxId == null || taxId.isBlank())
            throw new IllegalArgumentException("taxId is required");
        if (taxId.length() > MAX_TAX_ID_LENGTH)
            throw new IllegalArgumentException("taxId must be 50 chars or less");
        if (taxId.chars().anyMatch(codePoint -> codePoint > MAX_ASCII_CODE_POINT))
            throw new IllegalArgumentException("taxId must be ASCII");
    }

    /**
     * <strong>Mas estricto que la columna, y a proposito.</strong> El esquema solo
     * dice {@code VARCHAR(1)} y no tiene {@code CHECK}, pero el digito de
     * verificacion del NIT sale del modulo 11 y siempre es una cifra del 0 al 9;
     * una letra ahi no es un dato raro, es un dato imposible que viajaria intacto
     * hasta la informacion exogena.
     *
     * <p>
     * <strong>Lo que NO se hace aqui es recalcularlo.</strong>
     * {@code companytaxprofile} si lo calcula, pero ese digito es el de la propia
     * clinica y su algoritmo vive en aquella feature; importarlo romperia el
     * vertical slicing. Aqui el documento es el del cliente y lo que llega es lo
     * que el cliente declara.
     */
    private static void validateVerificationDigit(String verificationDigit) {
        if (verificationDigit == null)
            return;
        if (verificationDigit.length() != 1)
            throw new IllegalArgumentException("verificationDigit must be a single character");
        if (!Character.isDigit(verificationDigit.charAt(0)))
            throw new IllegalArgumentException("verificationDigit must be a digit");
    }

    /**
     * Espejo de {@code chk_company_billing_profiles_name_shape}, con sus dos ramas
     * y —esto es lo que se olvida— con las dos mitades de cada rama.
     *
     * <p>
     * {@code LEGAL} exige {@code legal_name} <em>y</em> que los cuatro campos de
     * persona natural esten vacios; {@code NATURAL} exige nombre y primer apellido
     * <em>y</em> que {@code legal_name} este vacio. Comprobar solo lo que tiene que
     * estar dejaria entrar una sociedad que ademas trae apellidos —basura que la
     * base rechaza con un error que no nombra ningun campo— y, peor, una ficha
     * ambigua sobre que juego de columnas hay que reportar.
     *
     * <p>
     * {@code middleName} y {@code secondLastName} son opcionales en la rama
     * natural: la constraint no los exige y hay personas que no los tienen.
     */
    private static void validateNameShape(PersonKind personKind, String legalName, String firstName,
            String middleName, String lastName, String secondLastName) {
        if (personKind == PersonKind.LEGAL) {
            requirePresent("legalName", legalName, MAX_LEGAL_NAME_LENGTH);
            requireAbsent("firstName", firstName);
            requireAbsent("middleName", middleName);
            requireAbsent("lastName", lastName);
            requireAbsent("secondLastName", secondLastName);
            return;
        }
        requireAbsent("legalName", legalName);
        requirePresent("firstName", firstName, MAX_PERSON_NAME_LENGTH);
        requirePresent("lastName", lastName, MAX_PERSON_NAME_LENGTH);
        requireLength("middleName", middleName, MAX_PERSON_NAME_LENGTH);
        requireLength("secondLastName", secondLastName, MAX_PERSON_NAME_LENGTH);
    }

    private static void requirePresent(String field, String value, int maxLength) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException(field + " is required for this person kind");
        requireLength(field, value, maxLength);
    }

    private static void requireAbsent(String field, String value) {
        if (value != null)
            throw new IllegalArgumentException(field + " must be absent for this person kind");
    }

    private static void requireLength(String field, String value, int maxLength) {
        if (value != null && value.length() > maxLength)
            throw new IllegalArgumentException(field + " must be " + maxLength + " chars or less");
    }

    private static void validateBillingEmail(String billingEmail) {
        if (billingEmail == null || billingEmail.isBlank())
            throw new IllegalArgumentException("billingEmail is required");
        if (billingEmail.length() > MAX_BILLING_EMAIL_LENGTH)
            throw new IllegalArgumentException("billingEmail must be 160 chars or less");
        if (!BILLING_EMAIL_SHAPE.matcher(billingEmail).matches())
            throw new IllegalArgumentException("billingEmail must look like an email address");
    }

    /**
     * Espejo de {@code chk_company_billing_profiles_validity}, con el {@code >}
     * estricto que trae la constraint. Un {@code >=} aqui dejaria pasar una ficha
     * de duracion cero que la base rechaza, y la diferencia entre los dos signos es
     * exactamente lo que hace que la sucesion en el mismo dia no sea representable.
     */
    private static void validateValidity(LocalDate validFrom, LocalDate validTo) {
        if (validFrom == null)
            throw new IllegalArgumentException("validFrom is required");
        if (validTo != null && !validTo.isAfter(validFrom))
            throw new IllegalArgumentException("validTo must be after validFrom");
    }

    public Long getId() {
        return id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public PersonKind getPersonKind() {
        return personKind;
    }

    public TaxIdKind getTaxIdKind() {
        return taxIdKind;
    }

    public String getTaxId() {
        return taxId;
    }

    public String getVerificationDigit() {
        return verificationDigit;
    }

    public String getLegalName() {
        return legalName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getSecondLastName() {
        return secondLastName;
    }

    public String getAddress() {
        return address;
    }

    public CityRef getCity() {
        return city;
    }

    public String getBillingEmail() {
        return billingEmail;
    }

    public TaxRegime getTaxRegime() {
        return taxRegime;
    }

    public boolean isWithholdingAgent() {
        return withholdingAgent;
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
}
