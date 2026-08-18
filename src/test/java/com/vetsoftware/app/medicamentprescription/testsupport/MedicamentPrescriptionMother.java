package com.vetsoftware.app.medicamentprescription.testsupport;

import com.vetsoftware.app.medicamentprescription.application.command.CreateMedicamentPrescriptionCommand;
import com.vetsoftware.app.medicamentprescription.application.command.UpdateMedicamentPrescriptionCommand;
import com.vetsoftware.app.medicamentprescription.domain.MedicamentPrescription;
import com.vetsoftware.app.medicamentprescription.domain.MedicamentRef;
import com.vetsoftware.app.medicamentprescription.domain.PrescriptionRef;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Fixtures del modulo medicamentprescription.
 *
 * <p>
 * Se construyen con el constructor publico y no con {@code create(...)}: el
 * factory pone {@code LocalDateTime.now()} y haria no deterministas las
 * aserciones sobre {@code createdDate}.
 */
public final class MedicamentPrescriptionMother {

    public static final Long ID = 700L;
    public static final Long MEDICAMENT_ID = 701L;
    public static final Long PRESCRIPTION_ID = 702L;
    public static final Long COMPANY_ID = 703L;
    public static final Long OTRO_MEDICAMENT_ID = 705L;

    public static final LocalDate FECHA = LocalDate.of(2026, 1, 10);
    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 10, 9, 0);

    public static final MedicamentRef MEDICAMENTO = new MedicamentRef(MEDICAMENT_ID,
            "Amoxicilina 500mg");
    public static final MedicamentRef OTRO_MEDICAMENTO = new MedicamentRef(OTRO_MEDICAMENT_ID,
            "Ivermectina 1%");
    public static final PrescriptionRef RECETA = new PrescriptionRef(PRESCRIPTION_ID, FECHA);

    private MedicamentPrescriptionMother() {
    }

    /** Linea recien creada (sin id), habilitada. */
    public static MedicamentPrescription valida() {
        return MedicamentPrescription.create(MEDICAMENTO, "Tableta", 2.0,
                "Cada 12 horas por 7 dias", "Con alimento", RECETA);
    }

    /** Linea ya persistida, con id y fecha de creacion fijos. */
    public static MedicamentPrescription persistida() {
        return new MedicamentPrescription(ID, MEDICAMENTO, "Tableta", 2.0,
                "Cada 12 horas por 7 dias", "Con alimento", RECETA, CREADO, true);
    }

    public static MedicamentPrescription persistida(Long id) {
        return new MedicamentPrescription(id, MEDICAMENTO, "Tableta", 2.0,
                "Cada 12 horas por 7 dias", "Con alimento", RECETA, CREADO, true);
    }

    public static MedicamentPrescription deshabilitada() {
        return new MedicamentPrescription(ID, MEDICAMENTO, "Tableta", 2.0,
                "Cada 12 horas por 7 dias", "Con alimento", RECETA, CREADO, false);
    }

    public static CreateMedicamentPrescriptionCommand comandoCrear() {
        return comandoCrear(COMPANY_ID);
    }

    /**
     * Con {@code companyId} explicito para poder pedir el comando de otra empresa
     * —el caso que destapo la fuga— y el {@code null} del camino SYSTEM.
     */
    public static CreateMedicamentPrescriptionCommand comandoCrear(Long companyId) {
        return new CreateMedicamentPrescriptionCommand(MEDICAMENT_ID, "Tableta", 2.0,
                "Cada 12 horas por 7 dias", "Con alimento", PRESCRIPTION_ID, companyId);
    }

    public static UpdateMedicamentPrescriptionCommand comandoActualizar(Long companyId) {
        return new UpdateMedicamentPrescriptionCommand(ID, MEDICAMENT_ID, "Tableta", 2.0,
                "Cada 12 horas por 7 dias", "Con alimento", PRESCRIPTION_ID, companyId);
    }
}
