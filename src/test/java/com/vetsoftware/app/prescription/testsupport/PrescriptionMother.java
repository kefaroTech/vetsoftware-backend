package com.vetsoftware.app.prescription.testsupport;

import com.vetsoftware.app.prescription.application.command.CreatePrescriptionCommand;
import com.vetsoftware.app.prescription.application.command.UpdatePrescriptionCommand;
import com.vetsoftware.app.prescription.domain.AnimalRef;
import com.vetsoftware.app.prescription.domain.CompanyRef;
import com.vetsoftware.app.prescription.domain.ConsultationRef;
import com.vetsoftware.app.prescription.domain.MedicamentRef;
import com.vetsoftware.app.prescription.domain.Prescription;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class PrescriptionMother {

    public static final Long COMPANY_ID = 500L;
    public static final Long ANIMAL_ID = 501L;
    public static final Long CONSULTATION_ID = 502L;
    public static final Long PRESCRIPTION_ID = 503L;
    public static final Long MEDICAMENT_ID = 504L;

    public static final LocalDate FECHA = LocalDate.of(2026, 1, 10);
    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 10, 9, 0);

    public static final CompanyRef CLINICA = new CompanyRef(COMPANY_ID, "Veterinaria Test",
            "900123456");
    public static final CompanyRef OTRA_CLINICA = new CompanyRef(999L, "Veterinaria ajena",
            "900654321");
    public static final AnimalRef MASCOTA = new AnimalRef(ANIMAL_ID, "Firulais", "A-001");
    public static final ConsultationRef CONSULTA = new ConsultationRef(CONSULTATION_ID, FECHA);
    public static final MedicamentRef MEDICAMENTO = new MedicamentRef(MEDICAMENT_ID,
            "Amoxicilina 500mg", "Tableta", 2.0, "Cada 12 horas por 7 dias", "Con alimento");

    private PrescriptionMother() {
    }

    public static Prescription valida() {
        return Prescription.create(FECHA, "Otitis externa", "Control en 7 dias", MASCOTA, CONSULTA,
                CLINICA);
    }

    public static Prescription persistida() {
        return new Prescription(PRESCRIPTION_ID, FECHA, "Otitis externa", "Control en 7 dias",
                MASCOTA, CONSULTA, CLINICA, CREADO, true);
    }

    public static CreatePrescriptionCommand comandoCrear() {
        return new CreatePrescriptionCommand(FECHA, "Otitis externa", "Control en 7 dias",
                ANIMAL_ID, CONSULTATION_ID, COMPANY_ID);
    }

    public static UpdatePrescriptionCommand comandoActualizar() {
        return new UpdatePrescriptionCommand(PRESCRIPTION_ID, FECHA, "Otitis externa",
                "Control en 7 dias", ANIMAL_ID, CONSULTATION_ID, COMPANY_ID);
    }

    public static List<MedicamentRef> unMedicamento() {
        return List.of(MEDICAMENTO);
    }
}
