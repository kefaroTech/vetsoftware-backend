package com.vetsoftware.app.hospitalizationmedication.testsupport;

import com.vetsoftware.app.hospitalizationmedication.application.command.CreateHospitalizationMedicationCommand;
import com.vetsoftware.app.hospitalizationmedication.application.command.SuspendHospitalizationMedicationCommand;
import com.vetsoftware.app.hospitalizationmedication.application.command.UpdateHospitalizationMedicationCommand;
import com.vetsoftware.app.hospitalizationmedication.domain.DurationMeasure;
import com.vetsoftware.app.hospitalizationmedication.domain.EmployeeRef;
import com.vetsoftware.app.hospitalizationmedication.domain.Frequency;
import com.vetsoftware.app.hospitalizationmedication.domain.GuidelineType;
import com.vetsoftware.app.hospitalizationmedication.domain.HospitalizationMedication;
import com.vetsoftware.app.hospitalizationmedication.domain.HospitalizationRef;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Fixtures de la feature hospitalizationmedication.
 *
 * <p>
 * Se construyen con el constructor publico y no con
 * {@code HospitalizationMedication.create(...)}: el factory pone
 * {@code LocalDateTime.now()} y haria no deterministas las aserciones sobre
 * {@code createdDate}.
 */
public final class HospitalizationMedicationMother {

    public static final Long MEDICATION_ID = 500L;
    public static final Long COMPANY_ID = 9L;
    /** Empresa ajena: el tenant contra el que se prueba el aislamiento. */
    public static final Long OTRA_COMPANY_ID = 77L;
    public static final Long HOSPITALIZATION_ID = 20L;
    public static final Long EMPLOYEE_ID = 4L;
    public static final Long OTHER_EMPLOYEE_ID = 5L;

    public static final HospitalizationRef HOSPITALIZACION = new HospitalizationRef(
            HOSPITALIZATION_ID, LocalDate.of(2026, 3, 1));
    public static final EmployeeRef CREADO_POR = new EmployeeRef(EMPLOYEE_ID, "EMP-001",
            "Ana Ruiz");
    public static final EmployeeRef SUSPENDIDO_POR = new EmployeeRef(OTHER_EMPLOYEE_ID, "EMP-002",
            "Luis Paz");

    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 3, 1, 8, 0);
    public static final LocalDateTime SUSPENDIDO_EL = LocalDateTime.of(2026, 3, 2, 9, 0);

    private HospitalizationMedicationMother() {
    }

    /** Orden activa, con plan completo. El caso por defecto. */
    public static HospitalizationMedication activo() {
        return activo(MEDICATION_ID);
    }

    public static HospitalizationMedication activo(Long id) {
        return new HospitalizationMedication(id, "Amoxicilina 500mg", "1 tableta",
                Frequency.EVERY_8H, GuidelineType.INTERVAL, DurationMeasure.DAYS, 5,
                LocalDate.of(2026, 3, 1), LocalTime.of(8, 0), "Administrar con alimento",
                HOSPITALIZACION, CREADO_POR, CREADO, null, true, null, null);
    }

    /** Orden ya suspendida por un segundo empleado. */
    public static HospitalizationMedication suspendido() {
        return new HospitalizationMedication(MEDICATION_ID, "Amoxicilina 500mg", "1 tableta",
                Frequency.EVERY_8H, GuidelineType.INTERVAL, DurationMeasure.DAYS, 5,
                LocalDate.of(2026, 3, 1), LocalTime.of(8, 0), "Administrar con alimento",
                HOSPITALIZACION, CREADO_POR, CREADO, null, true, SUSPENDIDO_EL, SUSPENDIDO_POR);
    }

    public static HospitalizationMedication deshabilitado() {
        return new HospitalizationMedication(MEDICATION_ID, "Amoxicilina 500mg", "1 tableta",
                Frequency.EVERY_8H, GuidelineType.INTERVAL, DurationMeasure.DAYS, 5,
                LocalDate.of(2026, 3, 1), LocalTime.of(8, 0), "Administrar con alimento",
                HOSPITALIZACION, CREADO_POR, CREADO, null, false, null, null);
    }

    /**
     * Sin ninguno de los campos opcionales de planificacion: solo lo obligatorio.
     */
    public static HospitalizationMedication sinDetallesOpcionales() {
        return new HospitalizationMedication(MEDICATION_ID, "Amoxicilina 500mg", null, null, null,
                null, null, null, null, null, HOSPITALIZACION, CREADO_POR, CREADO, null, true, null,
                null);
    }

    public static CreateHospitalizationMedicationCommand comandoCrear() {
        return new CreateHospitalizationMedicationCommand("Amoxicilina 500mg", "1 tableta",
                "EVERY_8H", "INTERVAL", "DAYS", 5, LocalDate.of(2026, 3, 1), LocalTime.of(8, 0),
                "Administrar con alimento", HOSPITALIZATION_ID, EMPLOYEE_ID);
    }

    public static UpdateHospitalizationMedicationCommand comandoActualizar() {
        return comandoActualizar(COMPANY_ID);
    }

    /** El mismo comando dirigido a otra empresa: el caso de fuga entre tenants. */
    public static UpdateHospitalizationMedicationCommand comandoActualizar(Long companyId) {
        return new UpdateHospitalizationMedicationCommand(MEDICATION_ID, "Amoxicilina 1g",
                "2 tabletas", "EVERY_12H", "FIXED", "DOSES", 3, LocalDate.of(2026, 3, 2),
                LocalTime.of(9, 0), "Notas actualizadas", companyId);
    }

    public static SuspendHospitalizationMedicationCommand comandoSuspender() {
        return comandoSuspender(COMPANY_ID);
    }

    /** El mismo comando dirigido a otra empresa: el caso de fuga entre tenants. */
    public static SuspendHospitalizationMedicationCommand comandoSuspender(Long companyId) {
        return new SuspendHospitalizationMedicationCommand(MEDICATION_ID, OTHER_EMPLOYEE_ID,
                companyId);
    }
}
