package com.vetsoftware.app.animalalert.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.animalalert.domain.AlertSeverity;
import com.vetsoftware.app.animalalert.domain.AlertType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Los commands de animalalert son records sin invariantes propias: lo unico que
 * hay que garantizar es que cada componente llega a su posicion, que es justo
 * lo que un intercambio de dos campos del mismo tipo no rompe en compilacion.
 */
@DisplayName("Commands de animalalert")
class AnimalAlertCommandsTest {

    @Nested
    @DisplayName("CreateAnimalAlertCommand")
    class Crear {

        @Test
        @DisplayName("expone cada componente en su posicion")
        void expone_cada_componente_en_su_posicion() {
            CreateAnimalAlertCommand command = new CreateAnimalAlertCommand(100L, AlertType.ALLERGY,
                    "Alergia a la penicilina", AlertSeverity.HIGH, 9L);

            assertThat(command.animalId()).isEqualTo(100L);
            assertThat(command.type()).isEqualTo(AlertType.ALLERGY);
            assertThat(command.description()).isEqualTo("Alergia a la penicilina");
            assertThat(command.severity()).isEqualTo(AlertSeverity.HIGH);
            assertThat(command.companyId()).isEqualTo(9L);
        }
    }

    @Nested
    @DisplayName("UpdateAnimalAlertCommand")
    class Actualizar {

        @Test
        @DisplayName("expone cada componente en su posicion")
        void expone_cada_componente_en_su_posicion() {
            UpdateAnimalAlertCommand command = new UpdateAnimalAlertCommand(500L,
                    AlertType.BEHAVIOR, "Agresivo con extranos", AlertSeverity.MEDIUM, 9L);

            assertThat(command.id()).isEqualTo(500L);
            assertThat(command.type()).isEqualTo(AlertType.BEHAVIOR);
            assertThat(command.description()).isEqualTo("Agresivo con extranos");
            assertThat(command.severity()).isEqualTo(AlertSeverity.MEDIUM);
            assertThat(command.companyId()).isEqualTo(9L);
        }
    }
}
