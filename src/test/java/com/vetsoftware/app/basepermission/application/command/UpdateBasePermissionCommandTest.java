package com.vetsoftware.app.basepermission.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UpdateBasePermissionCommand")
class UpdateBasePermissionCommandTest {

    @Test
    @DisplayName("conserva id, name, code y subModuleId")
    void conserva_id_name_code_y_sub_module_id() {
        UpdateBasePermissionCommand command = new UpdateBasePermissionCommand(2L, "Editar factura",
                "INVOICE_UPDATE", 1L);

        assertThat(command.id()).isEqualTo(2L);
        assertThat(command.name()).isEqualTo("Editar factura");
        assertThat(command.code()).isEqualTo("INVOICE_UPDATE");
        assertThat(command.subModuleId()).isEqualTo(1L);
    }
}
