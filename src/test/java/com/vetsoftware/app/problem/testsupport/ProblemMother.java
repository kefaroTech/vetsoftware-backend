package com.vetsoftware.app.problem.testsupport;

import com.vetsoftware.app.problem.application.command.CreateProblemCommand;
import com.vetsoftware.app.problem.application.command.UpdateProblemCommand;
import com.vetsoftware.app.problem.domain.AnimalRef;
import com.vetsoftware.app.problem.domain.CompanyRef;
import com.vetsoftware.app.problem.domain.Problem;
import com.vetsoftware.app.problem.domain.ProblemStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Fixtures del modulo problem.
 *
 * <p>
 * Se construyen con el constructor publico y no con
 * {@code Problem.create(...)}: el factory pone {@code LocalDateTime.now()} y
 * haria no deterministas las aserciones sobre {@code createdDate}.
 */
public final class ProblemMother {

    public static final Long PROBLEM_ID = 200L;
    public static final Long ANIMAL_ID = 100L;
    public static final Long COMPANY_ID = 9L;

    public static final AnimalRef FIRULAIS = new AnimalRef(ANIMAL_ID, "Firulais", "A-001");
    public static final CompanyRef CLINICA = new CompanyRef(COMPANY_ID, "Clinica Norte",
            "NIT-900123");

    public static final LocalDate INICIO = LocalDate.of(2026, 1, 10);
    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    private ProblemMother() {
    }

    /** Problema activo, habilitado, con notas. El caso por defecto. */
    public static Problem activo() {
        return activo(PROBLEM_ID);
    }

    public static Problem activo(Long id) {
        return new Problem(id, FIRULAIS, CLINICA, "Dermatitis alergica", ProblemStatus.ACTIVE,
                INICIO, null, "Revisar en dos semanas", CREADO, true);
    }

    public static Problem resuelto(LocalDate fechaResolucion) {
        return new Problem(PROBLEM_ID, FIRULAIS, CLINICA, "Dermatitis alergica",
                ProblemStatus.RESOLVED, INICIO, fechaResolucion, "Tratamiento completo", CREADO,
                true);
    }

    public static Problem deshabilitado() {
        return new Problem(PROBLEM_ID, FIRULAIS, CLINICA, "Dermatitis alergica",
                ProblemStatus.ACTIVE, INICIO, null, "Revisar en dos semanas", CREADO, false);
    }

    /** Comando de creacion coherente con las refs de arriba. */
    public static CreateProblemCommand comandoCrear() {
        return new CreateProblemCommand(ANIMAL_ID, "Dermatitis alergica", ProblemStatus.ACTIVE,
                INICIO, null, "Revisar en dos semanas", COMPANY_ID);
    }

    /** Comando de actualizacion que resuelve el problema. */
    public static UpdateProblemCommand comandoActualizar() {
        return new UpdateProblemCommand(PROBLEM_ID, "Resuelto tras tratamiento",
                ProblemStatus.RESOLVED, INICIO, LocalDate.of(2026, 2, 1), null, COMPANY_ID);
    }
}
