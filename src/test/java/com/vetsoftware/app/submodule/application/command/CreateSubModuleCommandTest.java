package com.vetsoftware.app.submodule.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CreateSubModuleCommand")
class CreateSubModuleCommandTest {

    @Test
    @DisplayName("conserva name, code y moduleId")
    void conserva_name_code_y_module_id() {
        CreateSubModuleCommand command = new CreateSubModuleCommand("Reportes", "REP", 1L);

        assertThat(command.name()).isEqualTo("Reportes");
        assertThat(command.code()).isEqualTo("REP");
        assertThat(command.moduleId()).isEqualTo(1L);
    }
}
