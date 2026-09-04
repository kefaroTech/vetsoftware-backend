package com.vetsoftware.app.platformtaxprofile.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * <strong>Quien es Lumbre ante la DIAN</strong>: la razon social, el NIT, el
 * regimen y la marca de autorretenedor de la propia plataforma, con vigencia.
 *
 * <h2>Este dato no existia en ninguna tabla, y tres sitios afirmaban que
 * si</h2>
 *
 * <p>
 * {@code 316_create_company_billing_profiles.xml}, el javadoc de
 * {@code CompanyBillingProfile} y el de
 * {@code OpenCompanyBillingProfileCommand#withholdingAgent} decian por escrito
 * que «ser autorretenedor es dato propio de Lumbre y vive en
 * {@code platform_billing_config}». <strong>Verificado: era falso.</strong>
 * {@code platform_billing_config} (255) tiene nueve columnas —id, singleton,
 * lista de precios por defecto, dias de gracia, dias de prueba, dia de
 * facturacion, plazo de pago, proveedor externo, fecha de creacion y version— y
 * <em>ninguna</em> es razon social, identificador fiscal ni marca de
 * autorretencion. Era una decision de modelado apoyada en una columna que nadie
 * habia creado.
 *
 * <h2>Por que no bastaba con añadirle columnas a
 * {@code platform_billing_config}</h2>
 *
 * <p>
 * Dos motivos, y los dos vienen de la resolucion de facturacion electronica, no
 * de gusto de diseño:
 *
 * <ol>
 * <li><strong>Ser autorretenedor coexiste con que los clientes le retengan a
 * Lumbre</strong>; no se deduce lo uno de lo otro. Es un insumo del calculo de
 * la retencion esperada y tiene que poder leerse con la misma seriedad que
 * cualquier otro dato de dinero.
 * <li><strong>La razon social y el NIT van impresos en cada factura de cada
 * cliente</strong>, como fabricante del software. Si la razon social cambia,
 * las facturas de hace dos años tienen que seguir diciendo lo que decian
 * entonces. {@code platform_billing_config} es un singleton que <em>se
 * sobrescribe</em>, sin historia: una identidad fiscal que se sobrescribe rompe
 * esa garantia el dia que cambien de razon social o de regimen.
 * </ol>
 *
 * <h2>Esta ficha NO se edita: se sucede</h2>
 *
 * <p>
 * <strong>No hay {@code update(...)} en esta clase, y su ausencia es el diseño
 * entero</strong> —mismo molde que {@code CompanyBillingProfile} y que
 * {@code company_tax_profiles} tras el changeset 364—. El cambio de datos
 * <strong>cierra</strong> la ficha vigente con {@link #closeOn(LocalDate)} y
 * <strong>abre otra</strong> con {@link #open}, las dos en una sola
 * transaccion. Por eso tampoco hay {@code PUT} en el controller ni comando de
 * actualizacion.
 *
 * <p>
 * <strong>Tampoco se borra.</strong> No hay {@code delete}, ni baja logica, ni
 * {@code @SQLDelete} — la tabla ni siquiera tiene columna {@code enabled}. El
 * cierre es {@code valid_to}, y una identidad retirada del uso sigue siendo la
 * correcta para las facturas de su vigencia.
 *
 * <h2>Global: aqui no hay empresa, y el marcador lo demuestra</h2>
 *
 * <p>
 * {@code platform_tax_profiles} <strong>no tiene {@code company_id}</strong>:
 * hay una sola identidad fiscal para todo Lumbre. Por eso el marcador de
 * vigente es un <strong>discriminador fijo</strong> —{@code 1}— y no
 * {@code company_id} como en 316 y 364. La consecuencia practica es que
 * {@code uq_platform_tax_profiles_current} admite <em>una fila vigente en toda
 * la tabla</em>, no una por tenant.
 *
 * <p>
 * Esa ausencia es tambien lo que deja la feature fuera del alcance de las
 * cuatro reglas duras de BE-COV: {@code PlatformTaxProfileJpaEntity} no alcanza
 * {@code CompanyJpaEntity} por ninguna asociacion.
 *
 * <h2>La vigencia es un intervalo semiabierto</h2>
 *
 * <p>
 * {@code [valid_from, valid_to)}. La sucesora arranca el mismo dia en que la
 * anterior se cierra, asi que la historia queda cubierta entera sin hueco y sin
 * solape, y en cualquier fecha hay exactamente una ficha aplicable.
 * {@code valid_to} nulo significa «vigente», y es lo que alimenta la columna
 * generada {@code current_profile_marker}.
 *
 * <p>
 * <strong>El corolario incomodo va escrito en
 * {@link PlatformTaxProfileSuccessionNotAfterCurrentException}</strong>: como
 * el {@code CHECK} del esquema es {@code valid_to > valid_from}
 * <em>estricto</em>, una ficha abierta hoy no se puede suceder hoy.
 *
 * <h2>{@code selfWithholder} es el dato que faltaba</h2>
 *
 * <p>
 * No es deducible de nada mas. Que Lumbre sea autorretenedor y que sus clientes
 * le practiquen retencion son <em>dos hechos independientes que coexisten</em>,
 * asi que deducir uno del otro seria falso. Hoy <strong>ningun sitio del codigo
 * lo consume</strong> para el calculo de la retencion esperada: cablearlo es
 * una funcionalidad aparte.
 *
 * <h2>Con {@code version}</h2>
 *
 * <p>
 * La ficha vigente muta —una vez, al cerrarse— y dos operadores de plataforma
 * sucediendola a la vez se pisarian sin ruido. El bloqueo optimista para al
 * segundo.
 */
public class PlatformTaxProfile {

    private static final int MAX_DOCUMENT_ID_LENGTH = 20;
    private static final int MAX_LEGAL_NAME_LENGTH = 255;
    private static final int MAX_COMMERCIAL_NAME_LENGTH = 150;
    private static final int MAX_FISCAL_EMAIL_LENGTH = 255;
    private static final int MAX_ASCII_CODE_POINT = 127;

    /**
     * Criba minima de correo, no una validacion completa: al menos un caracter
     * antes de la arroba, al menos uno despues, un punto mas adelante y al menos un
     * caracter tras el punto. La columna no tiene {@code CHECK} —367 no le puso
     * ninguno—, asi que esto es toda la barandilla que hay; escribir algo mas
     * estricto haria que el dominio rechazara direcciones que la tabla si admite,
     * que es la forma de acabar sin poder registrar la identidad real.
     */
    private static final Pattern FISCAL_EMAIL_SHAPE = Pattern.compile("^.+@.+\\..+$");

    private final Long id;

    private final PlatformDocumentType documentType;

    /**
     * El documento, tal cual. Sin puntos ni guiones: lo que se imprime en la
     * factura y lo que viaja en el XML.
     */
    private final String documentId;

    /** Digito de verificacion. Solo el NIT lo tiene, y la tabla lo deja nulo. */
    private final String verificationDigit;

    private final String legalName;
    private final PlatformTaxRegime taxRegime;
    private final String fiscalEmail;

    /** El nombre comercial, si difiere de la razon social. Opcional. */
    private final String commercialName;

    /**
     * La actividad CIIU. <strong>Opcional</strong>: la columna
     * {@code economic_activity_id} es nulable en 367, asi que la identidad se puede
     * registrar antes de decidirla.
     */
    private final EconomicActivityRef economicActivity;

    /**
     * Si <strong>Lumbre</strong> es autorretenedor. Nada que ver con que sus
     * clientes sean agentes de retencion: coexisten.
     */
    private final boolean selfWithholder;

    private final LocalDate validFrom;

    /** Fin de la vigencia, exclusivo. Nulo si y solo si la ficha es la vigente. */
    private LocalDate validTo;

    private final LocalDateTime createdDate;
    private final Long version;

    public PlatformTaxProfile(Long id, PlatformDocumentType documentType, String documentId,
            String verificationDigit, String legalName, PlatformTaxRegime taxRegime,
            String fiscalEmail, String commercialName, EconomicActivityRef economicActivity,
            boolean selfWithholder, LocalDate validFrom, LocalDate validTo,
            LocalDateTime createdDate, Long version) {
        validate(documentType, documentId, verificationDigit, legalName, taxRegime, fiscalEmail,
                commercialName, validFrom, validTo, createdDate);
        this.id = id;
        this.documentType = documentType;
        this.documentId = documentId;
        this.verificationDigit = verificationDigit;
        this.legalName = legalName;
        this.taxRegime = taxRegime;
        this.fiscalEmail = fiscalEmail;
        this.commercialName = commercialName;
        this.economicActivity = economicActivity;
        this.selfWithholder = selfWithholder;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.createdDate = createdDate;
        this.version = version;
    }

    /**
     * Identidad fiscal nueva: nace <strong>vigente</strong> —{@code validTo} nulo,
     * que es lo que hace valer la columna generada— y sin id.
     *
     * <p>
     * Es la unica puerta de entrada, y sirve tanto para la primera ficha como para
     * la sucesora de una que se acaba de cerrar. No hay una factoria «sucesora»
     * aparte a proposito: la sucesora no es una ficha de segunda clase, es una
     * ficha entera con sus datos completos y su propia fecha de inicio. Lo que las
     * encadena es el cierre de la anterior, y eso lo orquesta el caso de uso dentro
     * de una transaccion.
     *
     * @param validFrom
     *            desde cuando rige. En la sucesion es la misma fecha con la que se
     *            cerro la anterior, porque el intervalo es semiabierto
     * @param createdDate
     *            del reloj inyectado del caso de uso, nunca de un
     *            {@code LocalDateTime.now()} pelado
     */
    public static PlatformTaxProfile open(PlatformDocumentType documentType, String documentId,
            String verificationDigit, String legalName, PlatformTaxRegime taxRegime,
            String fiscalEmail, String commercialName, EconomicActivityRef economicActivity,
            boolean selfWithholder, LocalDate validFrom, LocalDateTime createdDate) {
        return new PlatformTaxProfile(null, documentType, documentId, verificationDigit, legalName,
                taxRegime, fiscalEmail, commercialName, economicActivity, selfWithholder, validFrom,
                null, createdDate, null);
    }

    /**
     * Cierra la ficha para que otra pueda sucederla.
     *
     * <p>
     * <strong>La fecha es la del comienzo de la sucesora</strong>, no la del ultimo
     * dia de esta: el intervalo es semiabierto, asi que esta ficha cubre hasta la
     * vispera y la nueva rige desde {@code effectiveFrom} inclusive. Escribirlo al
     * reves —cerrar en la vispera— dejaria el ultimo dia sin identidad fiscal
     * aplicable o, peor, con dos.
     *
     * @throws PlatformTaxProfileAlreadyClosedException
     *             si ya tenia fecha de cierre
     * @throws PlatformTaxProfileSuccessionNotAfterCurrentException
     *             si la fecha no es estrictamente posterior a {@code validFrom}:
     *             {@code chk_platform_tax_profiles_validity} no lo admite y
     *             {@code uq_platform_tax_profiles_validity} tampoco dejaria entrar
     *             a la sucesora
     */
    public void closeOn(LocalDate effectiveFrom) {
        if (!isCurrent())
            throw new PlatformTaxProfileAlreadyClosedException(id, validTo);
        if (effectiveFrom == null)
            throw new IllegalArgumentException("effectiveFrom is required");
        if (!effectiveFrom.isAfter(validFrom))
            throw new PlatformTaxProfileSuccessionNotAfterCurrentException(validFrom,
                    effectiveFrom);
        this.validTo = effectiveFrom;
    }

    /**
     * La identidad que rige hoy: la unica de la tabla sin fecha de cierre. Que sea
     * «la unica» no lo sostiene este metodo sino
     * {@code uq_platform_tax_profiles_current} sobre la columna generada.
     */
    public boolean isCurrent() {
        return validTo == null;
    }

    /**
     * Si la identidad aplica en esa fecha: {@code validFrom <= on} y la vigencia
     * sigue abierta o termina despues.
     *
     * <p>
     * <strong>El limite superior es estricto y el inferior no.</strong> El dia
     * escrito en {@code validTo} es el primero en que la ficha <em>ya no</em>
     * aplica, de modo que la que se cierra y la que empieza ese mismo dia se
     * relevan sin dejar hueco ni pisarse. Un {@code >=} aqui haria que las dos
     * aplicaran a la vez durante un dia — y una factura emitida ese dia no sabria
     * que razon social imprimir.
     */
    public boolean isEffectiveOn(LocalDate on) {
        return !validFrom.isAfter(on) && (validTo == null || validTo.isAfter(on));
    }

    /**
     * El documento como se imprime: {@code 900123456-7} para un NIT con digito de
     * verificacion, el documento pelado en cualquier otro caso.
     */
    public String formattedDocumentId() {
        return verificationDigit == null ? documentId : documentId + "-" + verificationDigit;
    }

    private static void validate(PlatformDocumentType documentType, String documentId,
            String verificationDigit, String legalName, PlatformTaxRegime taxRegime,
            String fiscalEmail, String commercialName, LocalDate validFrom, LocalDate validTo,
            LocalDateTime createdDate) {
        if (documentType == null)
            throw new IllegalArgumentException("documentType is required");
        validateDocumentId(documentId);
        validateVerificationDigit(documentType, verificationDigit);
        validateLegalName(legalName);
        if (taxRegime == null)
            throw new IllegalArgumentException("taxRegime is required");
        validateFiscalEmail(fiscalEmail);
        if (commercialName != null && commercialName.length() > MAX_COMMERCIAL_NAME_LENGTH)
            throw new IllegalArgumentException("commercialName must be 150 chars or less");
        validateValidity(validFrom, validTo);
        if (createdDate == null)
            throw new IllegalArgumentException("createdDate is required");
    }

    /**
     * <strong>Se exige ASCII aunque la columna no lo pida.</strong> A diferencia de
     * {@code company_billing_profiles.tax_id}, aqui 367 dejo
     * {@code document_id VARCHAR(20)} con la colacion heredada, asi que la base
     * <em>aceptaria</em> un caracter raro. Pero este documento se imprime en la
     * factura de cada cliente y viaja en el XML de la DIAN: un guion largo pegado
     * al copiar de un PDF entraria sin quejas y saldria en cada factura hasta que
     * alguien lo notara.
     */
    private static void validateDocumentId(String documentId) {
        if (documentId == null || documentId.isBlank())
            throw new IllegalArgumentException("documentId is required");
        if (documentId.length() > MAX_DOCUMENT_ID_LENGTH)
            throw new IllegalArgumentException("documentId must be 20 chars or less");
        if (documentId.chars().anyMatch(codePoint -> codePoint > MAX_ASCII_CODE_POINT))
            throw new IllegalArgumentException("documentId must be ASCII");
    }

    /**
     * Espejo de nada: <strong>el esquema no tiene {@code CHECK} sobre esto</strong>
     * y la columna es un {@code VARCHAR(1)} pelado. Las dos mitades de la regla
     * viven aqui.
     *
     * <p>
     * El digito de verificacion sale del modulo 11 y siempre es una cifra del 0 al
     * 9; una letra ahi no es un dato raro, es un dato imposible que viajaria
     * intacto hasta la factura. Y solo el NIT lo tiene: un pasaporte con digito de
     * verificacion es basura que nadie sabria interpretar despues.
     *
     * <p>
     * <strong>Lo que NO se hace aqui es calcularlo.</strong>
     * {@code companytaxprofile} tiene el algoritmo del modulo 11 en
     * {@code NitVerificationDigit}, pero importarlo romperia el vertical slicing.
     * Aqui lo que llega es lo que declara quien registra la identidad, y ese numero
     * lo saca del RUT.
     */
    private static void validateVerificationDigit(PlatformDocumentType documentType,
            String verificationDigit) {
        if (verificationDigit == null)
            return;
        if (!documentType.hasVerificationDigit())
            throw new IllegalArgumentException(
                    "verificationDigit must be absent unless the document type is NIT");
        if (verificationDigit.length() != 1)
            throw new IllegalArgumentException("verificationDigit must be a single character");
        if (!Character.isDigit(verificationDigit.charAt(0)))
            throw new IllegalArgumentException("verificationDigit must be a digit");
    }

    private static void validateLegalName(String legalName) {
        if (legalName == null || legalName.isBlank())
            throw new IllegalArgumentException("legalName is required");
        if (legalName.length() > MAX_LEGAL_NAME_LENGTH)
            throw new IllegalArgumentException("legalName must be 255 chars or less");
    }

    private static void validateFiscalEmail(String fiscalEmail) {
        if (fiscalEmail == null || fiscalEmail.isBlank())
            throw new IllegalArgumentException("fiscalEmail is required");
        if (fiscalEmail.length() > MAX_FISCAL_EMAIL_LENGTH)
            throw new IllegalArgumentException("fiscalEmail must be 255 chars or less");
        if (!FISCAL_EMAIL_SHAPE.matcher(fiscalEmail).matches())
            throw new IllegalArgumentException("fiscalEmail must look like an email address");
    }

    /**
     * Espejo de {@code chk_platform_tax_profiles_validity}, con el {@code >}
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

    public PlatformDocumentType getDocumentType() {
        return documentType;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getVerificationDigit() {
        return verificationDigit;
    }

    public String getLegalName() {
        return legalName;
    }

    public PlatformTaxRegime getTaxRegime() {
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

    public boolean isSelfWithholder() {
        return selfWithholder;
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
