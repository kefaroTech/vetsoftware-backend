package com.vetsoftware.app.companybillingprofile.testsupport;

import com.vetsoftware.app.companybillingprofile.domain.CityRef;
import com.vetsoftware.app.companybillingprofile.domain.CompanyBillingProfile;
import com.vetsoftware.app.companybillingprofile.domain.PersonKind;
import com.vetsoftware.app.companybillingprofile.domain.TaxIdKind;
import com.vetsoftware.app.companybillingprofile.domain.TaxRegime;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Fichas de facturacion listas para usar.
 *
 * <p>
 * <b>Las cuatro fechas son deliberadamente distintas entre si</b> —cuando se
 * creo la fila, desde cuando rige, desde cuando rige la sucesora y hasta cuando
 * rigio la primera—, para que cruzar dos columnas en un mapper, en un command o
 * en una response haga caer la asercion. Con la misma fecha en todas, no
 * caeria.
 *
 * <p>
 * <b>Los ids apuntan a lo que siembra {@code SchemaSeed}</b> ({@code 900} para
 * la empresa y la ciudad, {@code 901} para la empresa ajena) para que el mismo
 * fixture sirva en los tests puros y en la rodaja de persistencia.
 */
public final class CompanyBillingProfileMother {

    public static final Long COMPANY_ID = 900L;
    public static final Long OTRA_COMPANY_ID = 901L;

    public static final CityRef MEDELLIN = new CityRef(900L, "Medellin");

    public static final String NIT = "900123456";
    public static final String DIGITO_VERIFICACION = "7";
    public static final String RAZON_SOCIAL = "Inversiones Pet SAS";

    public static final String PRIMER_NOMBRE = "Ana";
    public static final String OTROS_NOMBRES = "Maria";
    public static final String PRIMER_APELLIDO = "Ruiz";
    public static final String SEGUNDO_APELLIDO = "Cardona";
    public static final String CEDULA = "43215678";

    public static final String DIRECCION = "Calle 10 # 43-51 oficina 704";
    public static final String CORREO = "facturacion@inversionespet.com.co";

    /** Desde cuando rige la primera ficha. */
    public static final LocalDate RIGE_DESDE = LocalDate.of(2026, 1, 15);

    /** Desde cuando rige la sucesora, y con lo que se cierra la primera. */
    public static final LocalDate SUCEDE_DESDE = LocalDate.of(2026, 4, 1);

    public static final LocalDateTime CREADA_EL = LocalDateTime.of(2026, 1, 12, 9, 30, 15);
    public static final LocalDateTime SUCESORA_CREADA_EL = LocalDateTime.of(2026, 3, 28, 17, 5, 40);

    private CompanyBillingProfileMother() {
    }

    /** Sociedad, ficha vigente y sin persistir. El caso mas comun. */
    public static CompanyBillingProfile sociedad() {
        return sociedadDe(COMPANY_ID, RIGE_DESDE);
    }

    public static CompanyBillingProfile sociedadDe(Long companyId, LocalDate validFrom) {
        return CompanyBillingProfile.open(companyId, PersonKind.LEGAL, TaxIdKind.NIT, NIT,
                DIGITO_VERIFICACION, RAZON_SOCIAL, null, null, null, null, DIRECCION, MEDELLIN,
                CORREO, TaxRegime.COMMON, true, validFrom, CREADA_EL);
    }

    /**
     * Persona natural con los cuatro campos de nombre poblados: el caso que la
     * informacion exogena obliga a guardar partido.
     */
    public static CompanyBillingProfile personaNatural() {
        return CompanyBillingProfile.open(COMPANY_ID, PersonKind.NATURAL, TaxIdKind.CC, CEDULA,
                null, null, PRIMER_NOMBRE, OTROS_NOMBRES, PRIMER_APELLIDO, SEGUNDO_APELLIDO,
                DIRECCION, MEDELLIN, CORREO, TaxRegime.NOT_RESPONSIBLE_VAT, false, RIGE_DESDE,
                CREADA_EL);
    }

    /**
     * Ya persistida: con id y con version, que es lo que ve un {@code findById}.
     */
    public static CompanyBillingProfile persistida(Long id) {
        return persistida(id, COMPANY_ID, RIGE_DESDE, null);
    }

    /**
     * Ficha persistida a medida. {@code validTo} nulo la deja vigente; con valor,
     * la deja en el historico.
     */
    public static CompanyBillingProfile persistida(Long id, Long companyId, LocalDate validFrom,
            LocalDate validTo) {
        return new CompanyBillingProfile(id, companyId, PersonKind.LEGAL, TaxIdKind.NIT, NIT,
                DIGITO_VERIFICACION, RAZON_SOCIAL, null, null, null, null, DIRECCION, MEDELLIN,
                CORREO, TaxRegime.COMMON, true, validFrom, validTo, CREADA_EL, 0L);
    }

    /**
     * Ficha con la combinacion de nombres que se le pida, para recorrer la matriz
     * de {@code chk_company_billing_profiles_name_shape}.
     *
     * <p>
     * El ternario del tipo de documento vive aqui, en el andamio: la convencion
     * prohibe la logica dentro del cuerpo de un test, no dentro del fixture que lo
     * alimenta.
     */
    public static CompanyBillingProfile conNombres(PersonKind personKind, String legalName,
            String firstName, String middleName, String lastName, String secondLastName) {
        return CompanyBillingProfile.open(COMPANY_ID, personKind,
                personKind == PersonKind.LEGAL ? TaxIdKind.NIT : TaxIdKind.CC,
                personKind == PersonKind.LEGAL ? NIT : CEDULA, null, legalName, firstName,
                middleName, lastName, secondLastName, DIRECCION, MEDELLIN, CORREO, TaxRegime.COMMON,
                false, RIGE_DESDE, CREADA_EL);
    }

    public static CompanyBillingProfile conCorreo(String billingEmail) {
        return CompanyBillingProfile.open(COMPANY_ID, PersonKind.LEGAL, TaxIdKind.NIT, NIT, null,
                RAZON_SOCIAL, null, null, null, null, DIRECCION, MEDELLIN, billingEmail,
                TaxRegime.COMMON, false, RIGE_DESDE, CREADA_EL);
    }

    public static CompanyBillingProfile conDocumento(String taxId) {
        return CompanyBillingProfile.open(COMPANY_ID, PersonKind.LEGAL, TaxIdKind.NIT, taxId, null,
                RAZON_SOCIAL, null, null, null, null, DIRECCION, MEDELLIN, CORREO, TaxRegime.COMMON,
                false, RIGE_DESDE, CREADA_EL);
    }

    public static CompanyBillingProfile conDigitoDeVerificacion(String verificationDigit) {
        return CompanyBillingProfile.open(COMPANY_ID, PersonKind.LEGAL, TaxIdKind.NIT, NIT,
                verificationDigit, RAZON_SOCIAL, null, null, null, null, DIRECCION, MEDELLIN,
                CORREO, TaxRegime.COMMON, false, RIGE_DESDE, CREADA_EL);
    }

    public static CompanyBillingProfile conDireccion(String address) {
        return CompanyBillingProfile.open(COMPANY_ID, PersonKind.LEGAL, TaxIdKind.NIT, NIT, null,
                RAZON_SOCIAL, null, null, null, null, address, MEDELLIN, CORREO, TaxRegime.COMMON,
                false, RIGE_DESDE, CREADA_EL);
    }

    /** Ficha construida con la vigencia que se le pida, incluida una imposible. */
    public static CompanyBillingProfile conVigencia(LocalDate validFrom, LocalDate validTo) {
        return new CompanyBillingProfile(null, COMPANY_ID, PersonKind.LEGAL, TaxIdKind.NIT, NIT,
                null, RAZON_SOCIAL, null, null, null, null, DIRECCION, MEDELLIN, CORREO,
                TaxRegime.COMMON, false, validFrom, validTo, CREADA_EL, null);
    }

    private static final String RELLENO = "x".repeat(300);

    /** Cadena de la longitud pedida, para los topes de columna. */
    public static String cadenaDe(int longitud) {
        return RELLENO.substring(0, longitud);
    }
}
