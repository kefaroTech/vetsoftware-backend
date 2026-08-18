package com.vetsoftware.app.submodule.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UpdateSubModuleCommand")
class UpdateSubModuleCommandTest {

    @Test
    @DisplayName("conserva id, name, code y moduleId")
    void conserva_id_name_code_y_module_id() {
        UpdateSubModuleCommand command = new UpdateSubModuleCommand(100L, "Reportes", "REP", 1L);

        assertThat(command.id()).isEqualTo(100L);
        assertThat(command.name()).isEqualTo("Reportes");
        assertThat(command.code()).isEqualTo("REP");
        assertThat(command.moduleId()).isEqualTo(1L);
    }
}
