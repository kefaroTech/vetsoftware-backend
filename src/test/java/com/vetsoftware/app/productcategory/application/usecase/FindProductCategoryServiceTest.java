package com.vetsoftware.app.productcategory.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.productcategory.application.dto.ProductCategoryDto;
import com.vetsoftware.app.productcategory.application.port.out.ProductCategoryRepository;
import com.vetsoftware.app.productcategory.domain.ProductCategory;
import com.vetsoftware.app.productcategory.domain.ProductCategoryNotFoundException;
import com.vetsoftware.app.productcategory.testsupport.ProductCategoryMother;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindProductCategoryService")
class FindProductCategoryServiceTest {

    @Mock
    private ProductCategoryRepository repository;

    private FindProductCategoryService service;

    @BeforeEach
    void crearServicio() {
        service = new FindProductCategoryService(repository);
    }

    @Nested
    @DisplayName("busqueda")
    class Busqueda {

        @Test
        @DisplayName("devuelve la categoria de la empresa del contexto")
        void devuelve_la_categoria_de_la_empresa_del_contexto() {
            ProductCategory categoria = ProductCategoryMother.activa();
            when(repository.findByIdAndCompanyId(ProductCategoryMother.CATEGORY_ID,
                    ProductCategoryMother.COMPANY_ID)).thenReturn(Optional.of(categoria));

            ProductCategoryDto dto = service.findById(ProductCategoryMother.CATEGORY_ID,
                    ProductCategoryMother.COMPANY_ID);

            assertThat(dto.id()).isEqualTo(ProductCategoryMother.CATEGORY_ID);
            assertThat(dto.name()).isEqualTo("Medicamentos");
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("lanza no encontrada si no existe en la empresa")
        void lanza_no_encontrada_si_no_existe_en_la_empresa() {
            when(repository.findByIdAndCompanyId(ProductCategoryMother.CATEGORY_ID,
                    ProductCategoryMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(ProductCategoryMother.CATEGORY_ID,
                    ProductCategoryMother.COMPANY_ID))
                    .isInstanceOf(ProductCategoryNotFoundException.class).hasMessageContaining(
                            "ProductCategory not found: " + ProductCategoryMother.CATEGORY_ID);
        }
    }
}
