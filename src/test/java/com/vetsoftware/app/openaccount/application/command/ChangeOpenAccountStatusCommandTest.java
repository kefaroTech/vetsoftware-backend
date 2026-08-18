package com.vetsoftware.app.openaccount.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ChangeOpenAccountStatusCommand")
class ChangeOpenAccountStatusCommandTest {

    @Test
    @DisplayName("conserva cada campo, incluidos los opcionales de cierre")
    void conserva_cada_campo() {
        ChangeOpenAccountStatusCommand command = new ChangeOpenAccountStatusCommand(100L, "CLOSE",
                4L, "Incobrable", 9L, "FE_VENTA", true, 3L);

        assertThat(command.id()).isEqualTo(100L);
        assertThat(command.status()).isEqualTo("CLOSE");
        assertThat(command.closedById()).isEqualTo(4L);
        assertThat(command.reason()).isEqualTo("Incobrable");
        assertThat(command.companyId()).isEqualTo(9L);
        assertThat(command.documentType()).isEqualTo("FE_VENTA");
        assertThat(command.finalConsumer()).isTrue();
        assertThat(command.expectedVersion()).isEqualTo(3L);
    }

    @Test
    @DisplayName("admite reason, documentType y expectedVersion nulos")
    void admite_campos_opcionales_nulos() {
        ChangeOpenAccountStatusCommand command = new ChangeOpenAccountStatusCommand(100L, "CLOSE",
                4L, null, 9L, null, false, null);

        assertThat(command.reason()).isNull();
        assertThat(command.documentType()).isNull();
        assertThat(command.expectedVersion()).isNull();
    }
}
