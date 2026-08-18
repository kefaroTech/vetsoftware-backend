package com.vetsoftware.app.productcategory.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CreateProductCategoryCommand — portador de datos")
class CreateProductCategoryCommandTest {

    @Test
    @DisplayName("conserva cada campo en su sitio")
    void conserva_cada_campo_en_su_sitio() {
        CreateProductCategoryCommand command = new CreateProductCategoryCommand("Medicamentos",
                "Categoria de medicamentos", 9L);

        assertThat(command.name()).isEqualTo("Medicamentos");
        assertThat(command.description()).isEqualTo("Categoria de medicamentos");
        assertThat(command.companyId()).isEqualTo(9L);
    }
}
