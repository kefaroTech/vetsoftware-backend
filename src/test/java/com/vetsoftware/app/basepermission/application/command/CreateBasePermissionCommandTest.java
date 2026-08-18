package com.vetsoftware.app.basepermission.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CreateBasePermissionCommand")
class CreateBasePermissionCommandTest {

    @Test
    @DisplayName("conserva name, code y subModuleId")
    void conserva_name_code_y_sub_module_id() {
        CreateBasePermissionCommand command = new CreateBasePermissionCommand("Crear factura",
                "INVOICE_CREATE", 1L);

        assertThat(command.name()).isEqualTo("Crear factura");
        assertThat(command.code()).isEqualTo("INVOICE_CREATE");
        assertThat(command.subModuleId()).isEqualTo(1L);
    }
}
