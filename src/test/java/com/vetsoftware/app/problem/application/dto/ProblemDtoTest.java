package com.vetsoftware.app.problem.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.problem.domain.Problem;
import com.vetsoftware.app.problem.domain.ProblemStatus;
import com.vetsoftware.app.problem.testsupport.ProblemMother;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El mapeo campo a campo se prueba entero a proposito: un intercambio entre dos
 * campos del mismo tipo —{@code onsetDate}/{@code resolvedDate}— compila, pasa
 * cualquier test de "no es null", y solo se ve en pantalla.
 */
@DisplayName("ProblemDto.from")
class ProblemDtoTest {

    @Test
    @DisplayName("copia cada campo del agregado en su posicion")
    void copia_cada_campo_del_agregado_en_su_posicion() {
        Problem problem = ProblemMother.activo();

        ProblemDto dto = ProblemDto.from(problem);

        assertThat(dto.id()).isEqualTo(ProblemMother.PROBLEM_ID);
        assertThat(dto.animalId()).isEqualTo(ProblemMother.ANIMAL_ID);
        assertThat(dto.animalName()).isEqualTo("Firulais");
        assertThat(dto.description()).isEqualTo("Dermatitis alergica");
        assertThat(dto.status()).isEqualTo(ProblemStatus.ACTIVE);
        assertThat(dto.onsetDate()).isEqualTo(ProblemMother.INICIO);
        assertThat(dto.resolvedDate()).isNull();
        assertThat(dto.notes()).isEqualTo("Revisar en dos semanas");
        assertThat(dto.createdDate()).isEqualTo(ProblemMother.CREADO);
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("propaga la resolucion con su fecha, sin cruzarla con onsetDate")
    void propaga_la_resolucion_con_su_fecha() {
        LocalDate fechaResolucion = LocalDate.of(2026, 2, 1);

        ProblemDto dto = ProblemDto.from(ProblemMother.resuelto(fechaResolucion));

        assertThat(dto.status()).isEqualTo(ProblemStatus.RESOLVED);
        assertThat(dto.onsetDate()).isEqualTo(ProblemMother.INICIO);
        assertThat(dto.resolvedDate()).isEqualTo(fechaResolucion);
    }

    @Test
    @DisplayName("propaga el problema deshabilitado")
    void propaga_el_problema_deshabilitado() {
        assertThat(ProblemDto.from(ProblemMother.deshabilitado()).enabled()).isFalse();
    }
}
