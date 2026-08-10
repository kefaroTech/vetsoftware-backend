package com.vetsoftware.app.companytaxprofile.testsupport;

import com.vetsoftware.app.companytaxprofile.application.command.CreateCompanyTaxProfileCommand;
import com.vetsoftware.app.companytaxprofile.application.command.UpdateCompanyTaxProfileCommand;
import com.vetsoftware.app.companytaxprofile.domain.CompanyDocumentType;
import com.vetsoftware.app.companytaxprofile.domain.CompanyRef;
import com.vetsoftware.app.companytaxprofile.domain.CompanyTaxProfile;
import com.vetsoftware.app.companytaxprofile.domain.CompanyTaxProfileResponsibility;
import com.vetsoftware.app.companytaxprofile.domain.EconomicActivityRef;
import com.vetsoftware.app.companytaxprofile.domain.TaxRegime;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Fixtures del modulo companytaxprofile.
 *
 * <p>
 * Los perfiles se construyen con el constructor publico y no con
 * {@code CompanyTaxProfile.create(...)}: el factory pone
 * {@code LocalDateTime.now()} y haria no deterministas las aserciones sobre
 * {@code createdDate}.
 *
 * <p>
 * El DV de {@link #NIT} esta calculado con el modulo 11 de la DIAN
 * ({@code 900123456} → {@code 8}). Los comandos llevan a proposito un DV
 * <i>incorrecto</i> ({@link #DV_ENTRANTE_INCORRECTO}) para que los tests puedan
 * demostrar que el caso de uso lo recalcula e ignora lo que manda el cliente.
 */
public final class CompanyTaxProfileMother {

    public static final Long COMPANY_ID = 9L;
    public static final Long PROFILE_ID = 77L;
    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    public static final String NIT = "900123456";
    public static final String NIT_DV = "8";
    public static final String DV_ENTRANTE_INCORRECTO = "1";

    public static final String CEDULA = "1020304050";

    public static final String EMAIL_FISCAL = "facturacion@vetnorte.com";
    public static final String RAZON_SOCIAL = "Clinica Veterinaria Norte S.A.S.";
    public static final String NOMBRE_COMERCIAL = "Vet Norte";

    public static final CompanyRef CLINICA = new CompanyRef(COMPANY_ID, "Clinica Norte",
            "900123456-8");
    public static final EconomicActivityRef VETERINARIA = new EconomicActivityRef(5L, "7500",
            "Actividades veterinarias");
    public static final EconomicActivityRef COMERCIO = new EconomicActivityRef(6L, "4773",
            "Comercio al por menor de otros productos nuevos");

    public static final CompanyTaxProfileResponsibility O13 = new CompanyTaxProfileResponsibility(
            "O-13");
    public static final CompanyTaxProfileResponsibility O15 = new CompanyTaxProfileResponsibility(
            "O-15");

    private CompanyTaxProfileMother() {
    }

    /** Perfil con NIT, DV correcto, actividad economica y dos responsabilidades. */
    public static CompanyTaxProfile perfilNit() {
        return perfilNit(PROFILE_ID);
    }

    public static CompanyTaxProfile perfilNit(Long id) {
        return new CompanyTaxProfile(id, CLINICA, CompanyDocumentType.NIT, NIT, NIT_DV,
                RAZON_SOCIAL, TaxRegime.RESPONSABLE_IVA, EMAIL_FISCAL, NOMBRE_COMERCIAL,
                VETERINARIA, List.of(O13, O15), CREADO, true);
    }

    /** Persona natural: sin DV y sin actividad economica ni responsabilidades. */
    public static CompanyTaxProfile perfilCedula() {
        return new CompanyTaxProfile(PROFILE_ID, CLINICA, CompanyDocumentType.CEDULA_CIUDADANIA,
                CEDULA, null, RAZON_SOCIAL, TaxRegime.NO_RESPONSABLE_IVA, EMAIL_FISCAL, null, null,
                List.of(), CREADO, true);
    }

    public static CompanyTaxProfile perfilDeshabilitado() {
        return new CompanyTaxProfile(PROFILE_ID, CLINICA, CompanyDocumentType.NIT, NIT, NIT_DV,
                RAZON_SOCIAL, TaxRegime.RESPONSABLE_IVA, EMAIL_FISCAL, NOMBRE_COMERCIAL,
                VETERINARIA, List.of(O13), CREADO, false);
    }

    /** Comando de creacion coherente con las refs de arriba. */
    public static CreateCompanyTaxProfileCommand comandoCrear() {
        return new CreateCompanyTaxProfileCommand(CompanyDocumentType.NIT, NIT,
                DV_ENTRANTE_INCORRECTO, RAZON_SOCIAL, TaxRegime.RESPONSABLE_IVA, EMAIL_FISCAL,
                NOMBRE_COMERCIAL, VETERINARIA.id(), List.of("O-13", "O-15"), COMPANY_ID);
    }

    public static CreateCompanyTaxProfileCommand comandoCrearSinActividad() {
        return new CreateCompanyTaxProfileCommand(CompanyDocumentType.NIT, NIT,
                DV_ENTRANTE_INCORRECTO, RAZON_SOCIAL, TaxRegime.RESPONSABLE_IVA, EMAIL_FISCAL,
                NOMBRE_COMERCIAL, null, null, COMPANY_ID);
    }

    public static CreateCompanyTaxProfileCommand comandoCrearCedula() {
        return new CreateCompanyTaxProfileCommand(CompanyDocumentType.CEDULA_CIUDADANIA, CEDULA,
                DV_ENTRANTE_INCORRECTO, RAZON_SOCIAL, TaxRegime.NO_RESPONSABLE_IVA, EMAIL_FISCAL,
                null, null, List.of(), COMPANY_ID);
    }

    /**
     * Comando de actualizacion que cambia regimen, actividad y responsabilidades.
     */
    public static UpdateCompanyTaxProfileCommand comandoActualizar() {
        return new UpdateCompanyTaxProfileCommand(CompanyDocumentType.NIT, "830053800",
                DV_ENTRANTE_INCORRECTO, "Clinica Veterinaria Sur S.A.S.",
                TaxRegime.NO_RESPONSABLE_IVA, "contabilidad@vetsur.com", "Vet Sur", COMERCIO.id(),
                List.of("O-15"), COMPANY_ID);
    }

    public static UpdateCompanyTaxProfileCommand comandoActualizarSinActividad() {
        return new UpdateCompanyTaxProfileCommand(CompanyDocumentType.NIT, "830053800",
                DV_ENTRANTE_INCORRECTO, "Clinica Veterinaria Sur S.A.S.",
                TaxRegime.NO_RESPONSABLE_IVA, "contabilidad@vetsur.com", "Vet Sur", null, null,
                COMPANY_ID);
    }

    public static UpdateCompanyTaxProfileCommand comandoActualizarCedula() {
        return new UpdateCompanyTaxProfileCommand(CompanyDocumentType.CEDULA_EXTRANJERIA, CEDULA,
                DV_ENTRANTE_INCORRECTO, RAZON_SOCIAL, TaxRegime.NO_RESPONSABLE_IVA, EMAIL_FISCAL,
                null, null, null, COMPANY_ID);
    }
}
