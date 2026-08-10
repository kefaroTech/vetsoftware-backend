package com.vetsoftware.app.owner.testsupport;

import com.vetsoftware.app.owner.application.command.CreateOwnerCommand;
import com.vetsoftware.app.owner.application.command.UpdateOwnerCommand;
import com.vetsoftware.app.owner.domain.CityRef;
import com.vetsoftware.app.owner.domain.CompanyRef;
import com.vetsoftware.app.owner.domain.FiscalResponsibility;
import com.vetsoftware.app.owner.domain.Owner;
import com.vetsoftware.app.owner.domain.OwnerDocumentType;
import com.vetsoftware.app.owner.domain.PersonType;
import com.vetsoftware.app.owner.domain.TaxRegime;
import java.time.LocalDateTime;

/**
 * Fixtures del modulo owner.
 *
 * <p>
 * Se construyen con el constructor publico y no con {@code Owner.create(...)}:
 * el factory pone {@code LocalDateTime.now()} y haria no deterministas las
 * aserciones sobre {@code createdDate}.
 */
public final class OwnerMother {

    public static final Long OWNER_ID = 100L;
    public static final Long JURIDICA_ID = 101L;
    public static final Long COMPANY_ID = 9L;
    public static final Long CITY_ID = 5L;
    public static final Long OTRA_CITY_ID = 6L;

    public static final CityRef BOGOTA = new CityRef(CITY_ID, "Bogota");
    public static final CityRef CALI = new CityRef(OTRA_CITY_ID, "Cali");
    public static final CompanyRef CLINICA = new CompanyRef(COMPANY_ID, "Clinica Norte",
            "NIT-900123456");

    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    private OwnerMother() {
    }

    /**
     * Persona natural, habilitada, sin digito de verificacion. El caso por defecto.
     */
    public static Owner personaNatural() {
        return personaNatural(OWNER_ID);
    }

    public static Owner personaNatural(Long id) {
        return new Owner(id, "Ana Ruiz", "ana@vet.com", "1020304050",
                OwnerDocumentType.CEDULA_CIUDADANIA, PersonType.NATURAL, null, null,
                "Calle 1 # 2-3", "3001112233", BOGOTA, CLINICA, false, TaxRegime.NO_RESPONSABLE_IVA,
                FiscalResponsibility.NO_APLICA, CREADO, true);
    }

    /**
     * Persona juridica: NIT con digito de verificacion y razon social obligatoria.
     */
    public static Owner personaJuridica() {
        return new Owner(JURIDICA_ID, "Veterinaria Sur", "contacto@sur.com", "900123456",
                OwnerDocumentType.NIT, PersonType.JURIDICA, "7", "Veterinaria Sur S.A.S.",
                "Avenida 3 # 40-50", "6041234567", BOGOTA, CLINICA, true, TaxRegime.RESPONSABLE_IVA,
                FiscalResponsibility.GRAN_CONTRIBUYENTE, CREADO, true);
    }

    public static Owner deshabilitado() {
        return new Owner(OWNER_ID, "Ana Ruiz", "ana@vet.com", "1020304050",
                OwnerDocumentType.CEDULA_CIUDADANIA, PersonType.NATURAL, null, null,
                "Calle 1 # 2-3", "3001112233", BOGOTA, CLINICA, false, TaxRegime.NO_RESPONSABLE_IVA,
                FiscalResponsibility.NO_APLICA, CREADO, false);
    }

    /** Comando de creacion coherente con la persona natural de arriba. */
    public static CreateOwnerCommand comandoCrear() {
        return comandoCrear(TaxRegime.NO_RESPONSABLE_IVA, FiscalResponsibility.NO_APLICA);
    }

    /**
     * Igual que {@link #comandoCrear()} pero con los campos fiscales a eleccion.
     */
    public static CreateOwnerCommand comandoCrear(TaxRegime taxRegime,
            FiscalResponsibility fiscalResponsibility) {
        return new CreateOwnerCommand("Ana Ruiz", "ana@vet.com", "1020304050",
                OwnerDocumentType.CEDULA_CIUDADANIA, PersonType.NATURAL, null, null,
                "Calle 1 # 2-3", "3001112233", CITY_ID, COMPANY_ID, false, taxRegime,
                fiscalResponsibility);
    }

    /**
     * Comando de creacion de una persona juridica (NIT + digito + razon social).
     */
    public static CreateOwnerCommand comandoCrearJuridica(TaxRegime taxRegime,
            FiscalResponsibility fiscalResponsibility) {
        return new CreateOwnerCommand("Veterinaria Sur", "contacto@sur.com", "900123456",
                OwnerDocumentType.NIT, PersonType.JURIDICA, "7", "Veterinaria Sur S.A.S.",
                "Avenida 3 # 40-50", "6041234567", CITY_ID, COMPANY_ID, true, taxRegime,
                fiscalResponsibility);
    }

    /** Comando de creacion de una persona natural identificada con NIT. */
    public static CreateOwnerCommand comandoCrearNaturalConNit(TaxRegime taxRegime,
            FiscalResponsibility fiscalResponsibility) {
        return new CreateOwnerCommand("Ana Ruiz", "ana@vet.com", "900123456", OwnerDocumentType.NIT,
                PersonType.NATURAL, "7", null, "Calle 1 # 2-3", "3001112233", CITY_ID, COMPANY_ID,
                false, taxRegime, fiscalResponsibility);
    }

    /** Comando de actualizacion que cambia todos los campos editables. */
    public static UpdateOwnerCommand comandoActualizar() {
        return comandoActualizar(TaxRegime.RESPONSABLE_IVA, FiscalResponsibility.AUTORRETENEDOR);
    }

    public static UpdateOwnerCommand comandoActualizar(TaxRegime taxRegime,
            FiscalResponsibility fiscalResponsibility) {
        return new UpdateOwnerCommand(OWNER_ID, "Ana Maria Ruiz", "anamaria@vet.com", "9988776655",
                OwnerDocumentType.CEDULA_EXTRANJERIA, PersonType.NATURAL, null, null,
                "Carrera 9 # 8-7", "3005556677", OTRA_CITY_ID, COMPANY_ID, true, taxRegime,
                fiscalResponsibility);
    }
}
