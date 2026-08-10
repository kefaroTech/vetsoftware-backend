package com.vetsoftware.app.product.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.product.application.dto.ProductDto;
import com.vetsoftware.app.product.application.port.out.ProductRepository;
import com.vetsoftware.app.product.testsupport.ProductMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListProductsService")
class ListProductsServiceTest {

    @Mock
    private ProductRepository repository;

    @InjectMocks
    private ListProductsService service;

    @Nested
    @DisplayName("listado activo por empresa")
    class Activos {

        @Test
        @DisplayName("proyecta a DTO cada producto de la empresa")
        void proyecta_cada_producto() {
            when(repository.findAllByCompanyId(ProductMother.COMPANY_ID))
                    .thenReturn(List.of(ProductMother.gravado(1L), ProductMother.gravado(2L)));

            List<ProductDto> dtos = service.listByCompany(ProductMother.COMPANY_ID);

            assertThat(dtos).hasSize(2).extracting(ProductDto::id).containsExactly(1L, 2L);
        }

        @Test
        @DisplayName("una empresa sin productos devuelve lista vacia, no null")
        void empresa_sin_productos() {
            when(repository.findAllByCompanyId(ProductMother.COMPANY_ID)).thenReturn(List.of());

            assertThat(service.listByCompany(ProductMother.COMPANY_ID)).isEmpty();
        }
    }

    @Nested
    @DisplayName("listado de pausados")
    class Pausados {

        @Test
        @DisplayName("devuelve los productos con enabled = false")
        void devuelve_los_pausados() {
            when(repository.findAllDisabledByCompanyId(ProductMother.COMPANY_ID))
                    .thenReturn(List.of(ProductMother.deshabilitado()));

            List<ProductDto> dtos = service.listDisabledByCompany(ProductMother.COMPANY_ID);

            assertThat(dtos).hasSize(1);
            assertThat(dtos.get(0).enabled()).isFalse();
        }

        @Test
        @DisplayName("no mezcla el listado de pausados con el de activos")
        void no_mezcla_los_listados() {
            when(repository.findAllDisabledByCompanyId(ProductMother.COMPANY_ID))
                    .thenReturn(List.of());

            assertThat(service.listDisabledByCompany(ProductMother.COMPANY_ID)).isEmpty();
        }
    }
}
