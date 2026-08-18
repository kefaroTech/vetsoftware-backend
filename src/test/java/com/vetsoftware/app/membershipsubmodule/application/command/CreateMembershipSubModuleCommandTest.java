package com.vetsoftware.app.membershipsubmodule.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CreateMembershipSubModuleCommand")
class CreateMembershipSubModuleCommandTest {

    @Test
    @DisplayName("conserva membershipId y subModuleId")
    void conserva_membership_id_y_sub_module_id() {
        CreateMembershipSubModuleCommand command = new CreateMembershipSubModuleCommand(900L, 980L);

        assertThat(command.membershipId()).isEqualTo(900L);
        assertThat(command.subModuleId()).isEqualTo(980L);
    }
}
