package com.vetsoftware.app.servicecategory.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CreateServiceCategoryCommand — portador de datos")
class CreateServiceCategoryCommandTest {

    @Test
    @DisplayName("conserva cada campo en su sitio")
    void conserva_cada_campo_en_su_sitio() {
        CreateServiceCategoryCommand command = new CreateServiceCategoryCommand("Consultas",
                "Categoria de consultas", 9L);

        assertThat(command.name()).isEqualTo("Consultas");
        assertThat(command.description()).isEqualTo("Categoria de consultas");
        assertThat(command.companyId()).isEqualTo(9L);
    }
}
