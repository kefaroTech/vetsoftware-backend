package com.vetsoftware.app.submodule.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CreateSubModuleCommand")
class CreateSubModuleCommandTest {

    @Test
    @DisplayName("conserva name, code, moduleId y las dos banderas comerciales")
    void conserva_name_code_y_module_id() {
        CreateSubModuleCommand command = new CreateSubModuleCommand("Reportes", "REP", 1L, true,
                true);

        assertThat(command.name()).isEqualTo("Reportes");
        assertThat(command.code()).isEqualTo("REP");
        assertThat(command.moduleId()).isEqualTo(1L);
        assertThat(command.sellable()).isTrue();
        assertThat(command.readOnlyCapable()).isTrue();
    }

    @Test
    @DisplayName("las dos banderas viajan en falso sin cruzarse entre si")
    void las_dos_banderas_viajan_sin_cruzarse() {
        CreateSubModuleCommand soloVendible = new CreateSubModuleCommand("Reportes", "REP", 1L,
                true, false);

        assertThat(soloVendible.sellable()).isTrue();
        assertThat(soloVendible.readOnlyCapable()).isFalse();
    }
}
