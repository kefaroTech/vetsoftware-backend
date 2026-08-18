package com.vetsoftware.app.surgery.testsupport;

import com.vetsoftware.app.surgery.application.command.ChangeSurgeryStatusCommand;
import com.vetsoftware.app.surgery.application.command.CreateSurgeryCommand;
import com.vetsoftware.app.surgery.application.command.UpdateSurgeryCommand;
import com.vetsoftware.app.surgery.domain.AnimalRef;
import com.vetsoftware.app.surgery.domain.CompanyRef;
import com.vetsoftware.app.surgery.domain.ConsultationRef;
import com.vetsoftware.app.surgery.domain.Surgery;
import com.vetsoftware.app.surgery.domain.SurgeryStatus;
import com.vetsoftware.app.surgery.domain.SurgeryTypeRef;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Fixtures del modulo surgery.
 *
 * <p>
 * Se construyen con el constructor publico y no con
 * {@code Surgery.create(...)}: el factory pone {@code LocalDateTime.now()} y
 * haria no deterministas las aserciones sobre {@code createdDate}.
 */
public final class SurgeryMother {

    public static final Long SURGERY_ID = 300L;
    public static final Long COMPANY_ID = 9L;
    public static final Long ANIMAL_ID = 100L;
    public static final Long SURGERY_TYPE_ID = 5L;
    public static final Long CONSULTATION_ID = 200L;

    public static final SurgeryTypeRef OVARIOHISTERECTOMIA = new SurgeryTypeRef(SURGERY_TYPE_ID,
            "Ovariohisterectomia");
    public static final AnimalRef FIRULAIS = new AnimalRef(ANIMAL_ID, "Firulais", "A-001");
    public static final CompanyRef CLINICA = new CompanyRef(COMPANY_ID, "Clinica Norte", "NIT-900");
    public static final ConsultationRef CONSULTA_PREVIA = new ConsultationRef(CONSULTATION_ID,
            LocalDate.of(2026, 3, 9));

    public static final SurgeryTypeRef CASTRACION = new SurgeryTypeRef(6L, "Castracion");
    public static final AnimalRef MICHI = new AnimalRef(101L, "Michi", "A-002");
    public static final CompanyRef OTRA_CLINICA = new CompanyRef(10L, "Clinica Sur", "NIT-901");
    public static final ConsultationRef OTRA_CONSULTA = new ConsultationRef(201L,
            LocalDate.of(2026, 3, 12));

    public static final LocalDate FECHA = LocalDate.of(2026, 3, 10);
    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 3, 10, 9, 0);

    private SurgeryMother() {
    }

    public static Surgery cirugiaValida() {
        return cirugiaValida(SURGERY_ID);
    }

    public static Surgery cirugiaValida(Long id) {
        return new Surgery(id, FECHA, OVARIOHISTERECTOMIA, "Ovariohisterectomia electiva",
                "Ketamina 10mg", "Recuperacion normal", null, SurgeryStatus.PROGRAMADA, FIRULAIS,
                CONSULTA_PREVIA, CLINICA, CREADO, true);
    }

    public static Surgery cirugiaSinConsulta() {
        return new Surgery(SURGERY_ID, FECHA, OVARIOHISTERECTOMIA, "Ovariohisterectomia electiva",
                "Ketamina 10mg", "Recuperacion normal", null, SurgeryStatus.PROGRAMADA, FIRULAIS,
                null, CLINICA, CREADO, true);
    }

    public static Surgery cirugiaEnEstado(SurgeryStatus status) {
        return new Surgery(SURGERY_ID, FECHA, OVARIOHISTERECTOMIA, "Ovariohisterectomia electiva",
                "Ketamina 10mg", "Recuperacion normal", null, status, FIRULAIS, CONSULTA_PREVIA,
                CLINICA, CREADO, true);
    }

    public static Surgery cirugiaDeshabilitada() {
        return new Surgery(SURGERY_ID, FECHA, OVARIOHISTERECTOMIA, "Ovariohisterectomia electiva",
                "Ketamina 10mg", "Recuperacion normal", null, SurgeryStatus.PROGRAMADA, FIRULAIS,
                CONSULTA_PREVIA, CLINICA, CREADO, false);
    }

    /**
     * Comando de creacion coherente con las refs de arriba, con consulta asociada.
     */
    public static CreateSurgeryCommand comandoCrear() {
        return new CreateSurgeryCommand(FECHA, SURGERY_TYPE_ID, "Ovariohisterectomia electiva",
                "Ketamina 10mg", "Recuperacion normal", null, ANIMAL_ID, CONSULTATION_ID,
                COMPANY_ID);
    }

    public static CreateSurgeryCommand comandoCrearSinConsulta() {
        return new CreateSurgeryCommand(FECHA, SURGERY_TYPE_ID, "Ovariohisterectomia electiva",
                "Ketamina 10mg", "Recuperacion normal", null, ANIMAL_ID, null, COMPANY_ID);
    }

    /** Comando de actualizacion que cambia todas las referencias. */
    public static UpdateSurgeryCommand comandoActualizar() {
        return new UpdateSurgeryCommand(SURGERY_ID, FECHA.plusDays(5), CASTRACION.id(),
                "Castracion electiva", "Anestesia local", "Observaciones nuevas", "Sangrado leve",
                MICHI.id(), OTRA_CONSULTA.id(), COMPANY_ID);
    }

    public static ChangeSurgeryStatusCommand comandoCambiarEstado(String status) {
        return new ChangeSurgeryStatusCommand(SURGERY_ID, status, COMPANY_ID);
    }

    public static ChangeSurgeryStatusCommand comandoCambiarEstadoSinCompanyId(String status) {
        return new ChangeSurgeryStatusCommand(SURGERY_ID, status, null);
    }
}
