package com.vetsoftware.app.servicecategory.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.servicecategory.application.dto.ServiceCategoryDto;
import com.vetsoftware.app.servicecategory.application.port.out.ServiceCategoryRepository;
import com.vetsoftware.app.servicecategory.testsupport.ServiceCategoryMother;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListServiceCategoriesService")
class ListServiceCategoriesServiceTest {

    @Mock
    private ServiceCategoryRepository repository;

    private ListServiceCategoriesService service;

    @BeforeEach
    void crearServicio() {
        service = new ListServiceCategoriesService(repository);
    }

    @Nested
    @DisplayName("listado")
    class Listado {

        @Test
        @DisplayName("mapea cada categoria de la empresa a su dto")
        void mapea_cada_categoria_de_la_empresa_a_su_dto() {
            when(repository.findAllByCompanyId(ServiceCategoryMother.COMPANY_ID))
                    .thenReturn(List.of(ServiceCategoryMother.activa()));

            List<ServiceCategoryDto> resultado = service
                    .listByCompany(ServiceCategoryMother.COMPANY_ID);

            assertThat(resultado).extracting(ServiceCategoryDto::id)
                    .containsExactly(ServiceCategoryMother.CATEGORY_ID);
        }

        @Test
        @DisplayName("una empresa sin categorias devuelve una lista vacia")
        void una_empresa_sin_categorias_devuelve_una_lista_vacia() {
            when(repository.findAllByCompanyId(ServiceCategoryMother.COMPANY_ID))
                    .thenReturn(List.of());

            List<ServiceCategoryDto> resultado = service
                    .listByCompany(ServiceCategoryMother.COMPANY_ID);

            assertThat(resultado).isEmpty();
        }
    }
}
