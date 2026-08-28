package com.vetsoftware.app.platformtaxprofile.testsupport;

import com.vetsoftware.app.platformtaxprofile.application.command.OpenPlatformTaxProfileCommand;
import com.vetsoftware.app.platformtaxprofile.application.command.SucceedPlatformTaxProfileCommand;
import com.vetsoftware.app.platformtaxprofile.domain.EconomicActivityRef;
import com.vetsoftware.app.platformtaxprofile.domain.PlatformDocumentType;
import com.vetsoftware.app.platformtaxprofile.domain.PlatformTaxProfile;
import com.vetsoftware.app.platformtaxprofile.domain.PlatformTaxRegime;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Fixtures del modulo platformtaxprofile.
 *
 * <p>
 * Se construyen con el constructor publico y no con
 * {@code PlatformTaxProfile.open(...)}: el factory pone el reloj del caso de
 * uso y aqui interesa una {@code createdDate} fija y afirmable.
 */
public final class PlatformTaxProfileMother {

    public static final Long PROFILE_ID = 700L;

    public static final EconomicActivityRef ACTIVIDAD = new EconomicActivityRef(11L, "6201",
            "Desarrollo de software");

    public static final LocalDate VALID_FROM = LocalDate.of(2026, 1, 1);
    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 1, 8, 0);

    private PlatformTaxProfileMother() {
    }

    /** La vigente por defecto: NIT con digito, con actividad economica. */
    public static PlatformTaxProfile vigente() {
        return new PlatformTaxProfile(PROFILE_ID, PlatformDocumentType.NIT, "900123456", "7",
                "VetSoftware SAS", PlatformTaxRegime.RESPONSABLE_IVA, "facturacion@vetsoftware.com",
                "VetSoftware", ACTIVIDAD, true, VALID_FROM, null, CREADO, 0L);
    }

    /** Vigente sin actividad economica: el caso opcional. */
    public static PlatformTaxProfile vigenteSinActividad() {
        return new PlatformTaxProfile(PROFILE_ID, PlatformDocumentType.NIT, "900123456", "7",
                "VetSoftware SAS", PlatformTaxRegime.RESPONSABLE_IVA, "facturacion@vetsoftware.com",
                "VetSoftware", null, true, VALID_FROM, null, CREADO, 0L);
    }

    /** Cerrada en {@code validTo}, con hueco para que una sucesora la relève. */
    public static PlatformTaxProfile cerrada(LocalDate validTo) {
        return new PlatformTaxProfile(PROFILE_ID, PlatformDocumentType.NIT, "900123456", "7",
                "VetSoftware SAS", PlatformTaxRegime.RESPONSABLE_IVA, "facturacion@vetsoftware.com",
                "VetSoftware", ACTIVIDAD, true, VALID_FROM, validTo, CREADO, 0L);
    }

    public static OpenPlatformTaxProfileCommand comandoAbrir() {
        return new OpenPlatformTaxProfileCommand(PlatformDocumentType.NIT, "900123456", "7",
                "VetSoftware SAS", PlatformTaxRegime.RESPONSABLE_IVA, "facturacion@vetsoftware.com",
                "VetSoftware", ACTIVIDAD.id(), true, VALID_FROM);
    }

    public static OpenPlatformTaxProfileCommand comandoAbrirSinActividad() {
        return new OpenPlatformTaxProfileCommand(PlatformDocumentType.NIT, "900123456", "7",
                "VetSoftware SAS", PlatformTaxRegime.RESPONSABLE_IVA, "facturacion@vetsoftware.com",
                "VetSoftware", null, true, VALID_FROM);
    }

    public static SucceedPlatformTaxProfileCommand comandoSuceder(LocalDate effectiveFrom) {
        return new SucceedPlatformTaxProfileCommand(PlatformDocumentType.NIT, "900999888", "1",
                "VetSoftware SAS BIC", PlatformTaxRegime.RESPONSABLE_IVA,
                "facturacion@vetsoftware.com", "VetSoftware", ACTIVIDAD.id(), true, effectiveFrom);
    }
}
