package com.vetsoftware.app.productcategory.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.productcategory.application.dto.ProductCategoryDto;
import com.vetsoftware.app.productcategory.application.port.out.ProductCategoryRepository;
import com.vetsoftware.app.productcategory.testsupport.ProductCategoryMother;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListProductCategoriesService")
class ListProductCategoriesServiceTest {

    @Mock
    private ProductCategoryRepository repository;

    private ListProductCategoriesService service;

    @BeforeEach
    void crearServicio() {
        service = new ListProductCategoriesService(repository);
    }

    @Nested
    @DisplayName("listado")
    class Listado {

        @Test
        @DisplayName("mapea cada categoria de la empresa a su dto")
        void mapea_cada_categoria_de_la_empresa_a_su_dto() {
            when(repository.findAllByCompanyId(ProductCategoryMother.COMPANY_ID))
                    .thenReturn(List.of(ProductCategoryMother.activa()));

            List<ProductCategoryDto> resultado = service
                    .listByCompany(ProductCategoryMother.COMPANY_ID);

            assertThat(resultado).extracting(ProductCategoryDto::id)
                    .containsExactly(ProductCategoryMother.CATEGORY_ID);
        }

        @Test
        @DisplayName("una empresa sin categorias devuelve una lista vacia")
        void una_empresa_sin_categorias_devuelve_una_lista_vacia() {
            when(repository.findAllByCompanyId(ProductCategoryMother.COMPANY_ID))
                    .thenReturn(List.of());

            List<ProductCategoryDto> resultado = service
                    .listByCompany(ProductCategoryMother.COMPANY_ID);

            assertThat(resultado).isEmpty();
        }
    }
}
