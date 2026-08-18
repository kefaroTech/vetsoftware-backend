package com.vetsoftware.app.baserolepermission.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UpdateBaseRolePermissionCommand")
class UpdateBaseRolePermissionCommandTest {

    @Test
    @DisplayName("conserva id, baseRoleId y basePermissionId")
    void conserva_id_base_role_id_y_base_permission_id() {
        UpdateBaseRolePermissionCommand command = new UpdateBaseRolePermissionCommand(100L, 1L,
                10L);

        assertThat(command.id()).isEqualTo(100L);
        assertThat(command.baseRoleId()).isEqualTo(1L);
        assertThat(command.basePermissionId()).isEqualTo(10L);
    }
}
