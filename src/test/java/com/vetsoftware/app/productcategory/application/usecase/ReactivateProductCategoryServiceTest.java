package com.vetsoftware.app.productcategory.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
@DisplayName("ReactivateProductCategoryService")
class ReactivateProductCategoryServiceTest {

    @Mock
    private ProductCategoryRepository repository;

    private ReactivateProductCategoryService service;

    @BeforeEach
    void crearServicio() {
        service = new ReactivateProductCategoryService(repository);
    }

    @Nested
    @DisplayName("reactivacion")
    class Reactivacion {

        @Test
        @DisplayName("reactiva y devuelve la categoria releida")
        void reactiva_y_devuelve_la_categoria_releida() {
            ProductCategory reactivada = ProductCategoryMother.activa();
            when(repository.reactivate(ProductCategoryMother.CATEGORY_ID,
                    ProductCategoryMother.COMPANY_ID)).thenReturn(1);
            when(repository.findByIdAndCompanyId(ProductCategoryMother.CATEGORY_ID,
                    ProductCategoryMother.COMPANY_ID)).thenReturn(Optional.of(reactivada));

            ProductCategoryDto dto = service.execute(ProductCategoryMother.CATEGORY_ID,
                    ProductCategoryMother.COMPANY_ID);

            assertThat(dto.id()).isEqualTo(ProductCategoryMother.CATEGORY_ID);
            assertThat(dto.enabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("ninguna fila afectada no vuelve a leer y lanza no encontrada")
        void ninguna_fila_afectada_no_vuelve_a_leer() {
            when(repository.reactivate(ProductCategoryMother.CATEGORY_ID,
                    ProductCategoryMother.COMPANY_ID)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(ProductCategoryMother.CATEGORY_ID,
                    ProductCategoryMother.COMPANY_ID))
                    .isInstanceOf(ProductCategoryNotFoundException.class).hasMessageContaining(
                            "ProductCategory not found: " + ProductCategoryMother.CATEGORY_ID);

            verify(repository, never()).findByIdAndCompanyId(org.mockito.ArgumentMatchers.anyLong(),
                    org.mockito.ArgumentMatchers.anyLong());
        }

        @Test
        @DisplayName("una fila reactivada pero ilocalizable tambien lanza no encontrada")
        void una_fila_reactivada_pero_ilocalizable_tambien_lanza_no_encontrada() {
            when(repository.reactivate(ProductCategoryMother.CATEGORY_ID,
                    ProductCategoryMother.COMPANY_ID)).thenReturn(1);
            when(repository.findByIdAndCompanyId(ProductCategoryMother.CATEGORY_ID,
                    ProductCategoryMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(ProductCategoryMother.CATEGORY_ID,
                    ProductCategoryMother.COMPANY_ID))
                    .isInstanceOf(ProductCategoryNotFoundException.class).hasMessageContaining(
                            "ProductCategory not found: " + ProductCategoryMother.CATEGORY_ID);
        }
    }
}
