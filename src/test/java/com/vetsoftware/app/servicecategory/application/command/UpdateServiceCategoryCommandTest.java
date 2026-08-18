package com.vetsoftware.app.servicecategory.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UpdateServiceCategoryCommand — portador de datos")
class UpdateServiceCategoryCommandTest {

    @Test
    @DisplayName("conserva cada campo en su sitio")
    void conserva_cada_campo_en_su_sitio() {
        UpdateServiceCategoryCommand command = new UpdateServiceCategoryCommand(70L, "Cirugias",
                "Categoria de cirugias", 9L, 4L, 3L);

        assertThat(command.id()).isEqualTo(70L);
        assertThat(command.name()).isEqualTo("Cirugias");
        assertThat(command.description()).isEqualTo("Categoria de cirugias");
        assertThat(command.companyId()).isEqualTo(9L);
        assertThat(command.updatedBy()).isEqualTo(4L);
        assertThat(command.version()).isEqualTo(3L);
    }
}
