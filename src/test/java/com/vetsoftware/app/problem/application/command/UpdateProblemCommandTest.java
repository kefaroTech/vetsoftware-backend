package com.vetsoftware.app.problem.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.problem.domain.ProblemStatus;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UpdateProblemCommand")
class UpdateProblemCommandTest {

    @Test
    @DisplayName("conserva cada campo en su posicion")
    void conserva_cada_campo_en_su_posicion() {
        LocalDate onset = LocalDate.of(2026, 1, 10);
        LocalDate resolved = LocalDate.of(2026, 2, 1);

        UpdateProblemCommand command = new UpdateProblemCommand(200L, "Resuelto tras tratamiento",
                ProblemStatus.RESOLVED, onset, resolved, "Sin recaidas", 9L);

        assertThat(command.id()).isEqualTo(200L);
        assertThat(command.description()).isEqualTo("Resuelto tras tratamiento");
        assertThat(command.status()).isEqualTo(ProblemStatus.RESOLVED);
        assertThat(command.onsetDate()).isEqualTo(onset);
        assertThat(command.resolvedDate()).isEqualTo(resolved);
        assertThat(command.notes()).isEqualTo("Sin recaidas");
        assertThat(command.companyId()).isEqualTo(9L);
    }
}
