package com.vetsoftware.app.productcategory.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.productcategory.application.port.out.ProductCategoryRepository;
import com.vetsoftware.app.productcategory.application.port.out.ProductChildrenQueryPort;
import com.vetsoftware.app.productcategory.domain.ProductCategory;
import com.vetsoftware.app.productcategory.domain.ProductCategoryHasActiveChildrenException;
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
@DisplayName("DeleteProductCategoryService")
class DeleteProductCategoryServiceTest {

    @Mock
    private ProductCategoryRepository repository;
    @Mock
    private ProductChildrenQueryPort productChildrenQueryPort;

    private DeleteProductCategoryService service;

    @BeforeEach
    void crearServicio() {
        service = new DeleteProductCategoryService(repository, productChildrenQueryPort);
    }

    @Nested
    @DisplayName("borrado")
    class Borrado {

        @Test
        @DisplayName("borra la categoria cuando no tiene productos hijos activos")
        void borra_la_categoria_sin_hijos_activos() {
            ProductCategory existente = ProductCategoryMother.activa();
            when(repository.findByIdAndCompanyId(ProductCategoryMother.CATEGORY_ID,
                    ProductCategoryMother.COMPANY_ID)).thenReturn(Optional.of(existente));
            when(productChildrenQueryPort
                    .existsActiveByProductCategoryId(ProductCategoryMother.CATEGORY_ID))
                    .thenReturn(false);

            service.execute(ProductCategoryMother.CATEGORY_ID, ProductCategoryMother.COMPANY_ID);

            verify(repository).delete(ProductCategoryMother.CATEGORY_ID);
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("no consulta hijos ni borra si la categoria no existe")
        void no_consulta_hijos_ni_borra_si_la_categoria_no_existe() {
            when(repository.findByIdAndCompanyId(ProductCategoryMother.CATEGORY_ID,
                    ProductCategoryMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(ProductCategoryMother.CATEGORY_ID,
                    ProductCategoryMother.COMPANY_ID))
                    .isInstanceOf(ProductCategoryNotFoundException.class).hasMessageContaining(
                            "ProductCategory not found: " + ProductCategoryMother.CATEGORY_ID);

            verifyNoInteractions(productChildrenQueryPort);
            verify(repository, never()).delete(org.mockito.ArgumentMatchers.any());
        }

        @Test
        @DisplayName("no borra si tiene productos hijos activos")
        void no_borra_si_tiene_productos_hijos_activos() {
            ProductCategory existente = ProductCategoryMother.activa();
            when(repository.findByIdAndCompanyId(ProductCategoryMother.CATEGORY_ID,
                    ProductCategoryMother.COMPANY_ID)).thenReturn(Optional.of(existente));
            when(productChildrenQueryPort
                    .existsActiveByProductCategoryId(ProductCategoryMother.CATEGORY_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.execute(ProductCategoryMother.CATEGORY_ID,
                    ProductCategoryMother.COMPANY_ID))
                    .isInstanceOf(ProductCategoryHasActiveChildrenException.class)
                    .hasMessageContaining("" + ProductCategoryMother.CATEGORY_ID)
                    .hasMessageContaining("product");

            verify(repository, never()).delete(org.mockito.ArgumentMatchers.any());
        }
    }
}
