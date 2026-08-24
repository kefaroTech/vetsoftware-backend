package com.vetsoftware.app.submodule.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UpdateSubModuleCommand")
class UpdateSubModuleCommandTest {

    @Test
    @DisplayName("conserva id, name, code, moduleId y las dos banderas comerciales")
    void conserva_id_name_code_y_module_id() {
        UpdateSubModuleCommand command = new UpdateSubModuleCommand(100L, "Reportes", "REP", 1L,
                true, false);

        assertThat(command.id()).isEqualTo(100L);
        assertThat(command.name()).isEqualTo("Reportes");
        assertThat(command.code()).isEqualTo("REP");
        assertThat(command.moduleId()).isEqualTo(1L);
        assertThat(command.sellable()).isTrue();
        assertThat(command.readOnlyCapable()).isFalse();
    }
}
