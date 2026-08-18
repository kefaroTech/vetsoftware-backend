package com.vetsoftware.app.productcategory.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UpdateProductCategoryCommand — portador de datos")
class UpdateProductCategoryCommandTest {

    @Test
    @DisplayName("conserva cada campo en su sitio")
    void conserva_cada_campo_en_su_sitio() {
        UpdateProductCategoryCommand command = new UpdateProductCategoryCommand(70L, "Insumos",
                "Categoria de insumos", 9L, 4L, 3L);

        assertThat(command.id()).isEqualTo(70L);
        assertThat(command.name()).isEqualTo("Insumos");
        assertThat(command.description()).isEqualTo("Categoria de insumos");
        assertThat(command.companyId()).isEqualTo(9L);
        assertThat(command.updatedBy()).isEqualTo(4L);
        assertThat(command.version()).isEqualTo(3L);
    }
}
