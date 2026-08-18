package com.vetsoftware.app.baserolepermission.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CreateBaseRolePermissionCommand")
class CreateBaseRolePermissionCommandTest {

    @Test
    @DisplayName("conserva baseRoleId y basePermissionId")
    void conserva_base_role_id_y_base_permission_id() {
        CreateBaseRolePermissionCommand command = new CreateBaseRolePermissionCommand(1L, 10L);

        assertThat(command.baseRoleId()).isEqualTo(1L);
        assertThat(command.basePermissionId()).isEqualTo(10L);
    }
}
