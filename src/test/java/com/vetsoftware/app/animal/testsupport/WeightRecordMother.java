package com.vetsoftware.app.animal.testsupport;

import com.vetsoftware.app.animal.application.command.CreateWeightRecordCommand;
import com.vetsoftware.app.animal.domain.AnimalRef;
import com.vetsoftware.app.animal.domain.WeightRecord;
import com.vetsoftware.app.animal.domain.WeightSource;
import com.vetsoftware.app.animal.domain.WeightType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Fixtures de la serie temporal de peso. */
public final class WeightRecordMother {

    public static final Long RECORD_ID = 500L;
    public static final AnimalRef FIRULAIS = new AnimalRef(AnimalMother.ANIMAL_ID, "Firulais",
            "A-001");
    public static final LocalDate MEDIDO_EL = LocalDate.of(2026, 2, 1);
    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 2, 1, 9, 0);

    private WeightRecordMother() {
    }

    public static WeightRecord manual() {
        return manual(new BigDecimal("12.50"), MEDIDO_EL);
    }

    public static WeightRecord manual(BigDecimal valor, LocalDate medidoEl) {
        return new WeightRecord(RECORD_ID, FIRULAIS, valor, WeightType.KILOGRAMS, medidoEl,
                WeightSource.MANUAL, null, "control de rutina", AnimalMother.CLINICA, CREADO, true);
    }

    /** Registro derivado de un evento clinico: lleva sourceId obligatorio. */
    public static WeightRecord deConsulta(Long consultaId) {
        return new WeightRecord(RECORD_ID, FIRULAIS, new BigDecimal("13.00"), WeightType.KILOGRAMS,
                MEDIDO_EL, WeightSource.CONSULTATION, consultaId, null, AnimalMother.CLINICA,
                CREADO, true);
    }

    public static CreateWeightRecordCommand comandoCrear() {
        return new CreateWeightRecordCommand(AnimalMother.ANIMAL_ID, new BigDecimal("12.50"),
                WeightType.KILOGRAMS, MEDIDO_EL, "control de rutina", AnimalMother.COMPANY_ID);
    }
}
