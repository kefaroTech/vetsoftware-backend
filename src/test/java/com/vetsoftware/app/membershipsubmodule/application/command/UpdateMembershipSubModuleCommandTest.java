package com.vetsoftware.app.membershipsubmodule.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UpdateMembershipSubModuleCommand")
class UpdateMembershipSubModuleCommandTest {

    @Test
    @DisplayName("conserva id, membershipId y subModuleId")
    void conserva_id_membership_id_y_sub_module_id() {
        UpdateMembershipSubModuleCommand command = new UpdateMembershipSubModuleCommand(500L, 900L,
                980L);

        assertThat(command.id()).isEqualTo(500L);
        assertThat(command.membershipId()).isEqualTo(900L);
        assertThat(command.subModuleId()).isEqualTo(980L);
    }
}
