package com.vetsoftware.app.problem.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.problem.domain.ProblemStatus;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CreateProblemCommand")
class CreateProblemCommandTest {

    @Test
    @DisplayName("conserva cada campo en su posicion")
    void conserva_cada_campo_en_su_posicion() {
        LocalDate onset = LocalDate.of(2026, 1, 10);
        LocalDate resolved = LocalDate.of(2026, 2, 1);

        CreateProblemCommand command = new CreateProblemCommand(100L, "Dermatitis alergica",
                ProblemStatus.ACTIVE, onset, resolved, "Revisar en dos semanas", 9L);

        assertThat(command.animalId()).isEqualTo(100L);
        assertThat(command.description()).isEqualTo("Dermatitis alergica");
        assertThat(command.status()).isEqualTo(ProblemStatus.ACTIVE);
        assertThat(command.onsetDate()).isEqualTo(onset);
        assertThat(command.resolvedDate()).isEqualTo(resolved);
        assertThat(command.notes()).isEqualTo("Revisar en dos semanas");
        assertThat(command.companyId()).isEqualTo(9L);
    }
}
