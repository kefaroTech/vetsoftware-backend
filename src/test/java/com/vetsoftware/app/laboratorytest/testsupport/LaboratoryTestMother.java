package com.vetsoftware.app.laboratorytest.testsupport;

import com.vetsoftware.app.laboratorytest.domain.AnimalRef;
import com.vetsoftware.app.laboratorytest.domain.CompanyRef;
import com.vetsoftware.app.laboratorytest.domain.ConsultationRef;
import com.vetsoftware.app.laboratorytest.domain.EmployeeRef;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTest;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestPriority;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestStatus;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestTypeRef;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Fixtures de la feature. Todo valor por defecto es valido: cada test cambia
 * solo el campo que esta ejerciendo, de forma que un fallo apunte al invariante
 * y no al fixture.
 */
public final class LaboratoryTestMother {

    public static final Long ID = 42L;
    public static final Long BRANCH_ID = 5L;
    public static final LocalDate FECHA = LocalDate.of(2026, 3, 15);
    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 3, 15, 8, 0);
    public static final LocalDateTime PROCESADO = LocalDateTime.of(2026, 3, 16, 9, 30);

    public static final LaboratoryTestTypeRef HEMOGRAMA = new LaboratoryTestTypeRef(4L,
            "Hemograma");
    public static final LaboratoryTestTypeRef UROANALISIS = new LaboratoryTestTypeRef(8L,
            "Uroanalisis");
    public static final AnimalRef FIRULAIS = new AnimalRef(7L, "Firulais", "A-001");
    public static final AnimalRef MICHI = new AnimalRef(12L, "Michi", "A-002");
    public static final ConsultationRef CONSULTA = new ConsultationRef(11L,
            LocalDate.of(2026, 3, 14));
    public static final CompanyRef CLINICA = new CompanyRef(9L, "Clinica Kefaro", "900123456");
    public static final EmployeeRef BACTERIOLOGA = new EmployeeRef(3L, "EMP-003", "Ana Ruiz");

    private LaboratoryTestMother() {
    }

    /** Muestra recien registrada: sin procesar y con consulta asociada. */
    public static LaboratoryTest pendienteDeToma() {
        return new LaboratoryTest(ID, FECHA, HEMOGRAMA, 1, "Sospecha de anemia",
                LaboratoryTestStatus.PENDING_COLLECTION, LaboratoryTestPriority.NORMAL, FIRULAIS,
                CONSULTA, CLINICA, BRANCH_ID, null, null, CREADO, null, true);
    }

    /** Muestra ya validada: lleva firma de quien la proceso y cuando. */
    public static LaboratoryTest validada() {
        return new LaboratoryTest(ID, FECHA, HEMOGRAMA, 2, "Anemia regenerativa",
                LaboratoryTestStatus.COMPLETED, LaboratoryTestPriority.URGENTE, FIRULAIS, CONSULTA,
                CLINICA, BRANCH_ID, BACTERIOLOGA, PROCESADO, CREADO, null, true);
    }

    /** Minima: sin consulta, sin diagnostico y sin procesador. */
    public static LaboratoryTest sinAsociacionesOpcionales() {
        return new LaboratoryTest(ID, FECHA, HEMOGRAMA, 1, null,
                LaboratoryTestStatus.PENDING_PROCESSING, LaboratoryTestPriority.NORMAL, FIRULAIS,
                null, CLINICA, BRANCH_ID, null, null, CREADO, null, true);
    }

    /** Deshabilitada por borrado logico. */
    public static LaboratoryTest deshabilitada() {
        return new LaboratoryTest(ID, FECHA, HEMOGRAMA, 1, "Sospecha de anemia",
                LaboratoryTestStatus.CANCELLED, LaboratoryTestPriority.NORMAL, FIRULAIS, CONSULTA,
                CLINICA, BRANCH_ID, null, null, CREADO, null, false);
    }
}
