package com.vetsoftware.app.deworming.testsupport;

import com.vetsoftware.app.deworming.application.command.CreateDewormingCommand;
import com.vetsoftware.app.deworming.application.command.UpdateDewormingCommand;
import com.vetsoftware.app.deworming.domain.AnimalRef;
import com.vetsoftware.app.deworming.domain.CompanyRef;
import com.vetsoftware.app.deworming.domain.ConsultationRef;
import com.vetsoftware.app.deworming.domain.Deworming;
import com.vetsoftware.app.deworming.domain.DewormingType;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Fixtures del modulo deworming.
 *
 * <p>
 * Se construyen con el constructor publico y no con
 * {@code Deworming.create(...)}: el factory pone {@code LocalDateTime.now()} y
 * haria no deterministas las aserciones sobre {@code createdDate}.
 */
public final class DewormingMother {

    public static final Long DEWORMING_ID = 300L;
    public static final Long COMPANY_ID = 9L;
    public static final Long ANIMAL_ID = 100L;
    public static final Long CONSULTATION_ID = 200L;

    public static final AnimalRef FIRULAIS = new AnimalRef(ANIMAL_ID, "Firulais", "A-001");
    public static final CompanyRef CLINICA = new CompanyRef(COMPANY_ID, "Clinica Norte", "NIT-900");
    public static final ConsultationRef CONSULTA = new ConsultationRef(CONSULTATION_ID,
            LocalDate.of(2026, 3, 1));

    public static final AnimalRef MICHI = new AnimalRef(101L, "Michi", "A-002");
    public static final CompanyRef OTRA_CLINICA = new CompanyRef(10L, "Clinica Sur", "NIT-901");

    public static final LocalDate FECHA = LocalDate.of(2026, 4, 1);
    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 4, 1, 9, 0);

    private DewormingMother() {
    }

    /**
     * Desparasitacion interna, con consulta asociada, habilitada. El caso por
     * defecto.
     */
    public static Deworming desparasitacionValida() {
        return desparasitacionValida(DEWORMING_ID);
    }

    public static Deworming desparasitacionValida(Long id) {
        return new Deworming(id, FECHA, FECHA.minusMonths(3), DewormingType.INTERNAL,
                "Drontal Plus", "1 tableta / 10kg", FECHA.plusMonths(3), "Sin reacciones adversas",
                FIRULAIS, CONSULTA, CLINICA, CREADO, null, true);
    }

    /** Desparasitacion sin consulta asociada — el path opcional del agregado. */
    public static Deworming sinConsulta() {
        return new Deworming(DEWORMING_ID, FECHA, null, DewormingType.EXTERNAL, "Frontline",
                "1 pipeta", null, null, FIRULAIS, null, CLINICA, CREADO, null, true);
    }

    public static Deworming deshabilitada() {
        return new Deworming(DEWORMING_ID, FECHA, FECHA.minusMonths(3), DewormingType.INTERNAL,
                "Drontal Plus", "1 tableta / 10kg", FECHA.plusMonths(3), "Sin reacciones adversas",
                FIRULAIS, CONSULTA, CLINICA, CREADO, null, false);
    }

    /**
     * Comando de creacion coherente con las refs de arriba, con consulta asociada.
     */
    public static CreateDewormingCommand comandoCrear() {
        return new CreateDewormingCommand(FECHA, FECHA.minusMonths(3), DewormingType.INTERNAL,
                "Drontal Plus", "1 tableta / 10kg", FECHA.plusMonths(3), "Sin reacciones adversas",
                ANIMAL_ID, CONSULTATION_ID, COMPANY_ID);
    }

    /** Comando de creacion sin consulta asociada. */
    public static CreateDewormingCommand comandoCrearSinConsulta() {
        return new CreateDewormingCommand(FECHA, null, DewormingType.EXTERNAL, "Frontline",
                "1 pipeta", null, null, ANIMAL_ID, null, COMPANY_ID);
    }

    /**
     * Comando de actualizacion que cambia el animal y el producto, con consulta
     * asociada.
     */
    public static UpdateDewormingCommand comandoActualizar() {
        return new UpdateDewormingCommand(DEWORMING_ID, FECHA, FECHA.minusMonths(2),
                DewormingType.MIX, "Bravecto", "1 comprimido", FECHA.plusMonths(4),
                "Control en un mes", MICHI.id(), CONSULTATION_ID, COMPANY_ID);
    }

    /** Comando de actualizacion sin consulta asociada. */
    public static UpdateDewormingCommand comandoActualizarSinConsulta() {
        return new UpdateDewormingCommand(DEWORMING_ID, FECHA, FECHA.minusMonths(2),
                DewormingType.MIX, "Bravecto", "1 comprimido", FECHA.plusMonths(4),
                "Control en un mes", MICHI.id(), null, COMPANY_ID);
    }
}
