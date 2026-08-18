package com.vetsoftware.app.servicecategory.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.servicecategory.application.dto.ServiceCategoryDto;
import com.vetsoftware.app.servicecategory.application.port.out.ServiceCategoryRepository;
import com.vetsoftware.app.servicecategory.domain.ServiceCategory;
import com.vetsoftware.app.servicecategory.domain.ServiceCategoryNotFoundException;
import com.vetsoftware.app.servicecategory.testsupport.ServiceCategoryMother;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReactivateServiceCategoryService")
class ReactivateServiceCategoryServiceTest {

    @Mock
    private ServiceCategoryRepository repository;

    private ReactivateServiceCategoryService service;

    @BeforeEach
    void crearServicio() {
        service = new ReactivateServiceCategoryService(repository);
    }

    @Nested
    @DisplayName("reactivacion")
    class Reactivacion {

        @Test
        @DisplayName("reactiva y devuelve la categoria releida")
        void reactiva_y_devuelve_la_categoria_releida() {
            ServiceCategory reactivada = ServiceCategoryMother.activa();
            when(repository.reactivate(ServiceCategoryMother.CATEGORY_ID,
                    ServiceCategoryMother.COMPANY_ID)).thenReturn(1);
            when(repository.findByIdAndCompanyId(ServiceCategoryMother.CATEGORY_ID,
                    ServiceCategoryMother.COMPANY_ID)).thenReturn(Optional.of(reactivada));

            ServiceCategoryDto dto = service.execute(ServiceCategoryMother.CATEGORY_ID,
                    ServiceCategoryMother.COMPANY_ID);

            assertThat(dto.id()).isEqualTo(ServiceCategoryMother.CATEGORY_ID);
            assertThat(dto.enabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("ninguna fila afectada no vuelve a leer y lanza no encontrada")
        void ninguna_fila_afectada_no_vuelve_a_leer() {
            when(repository.reactivate(ServiceCategoryMother.CATEGORY_ID,
                    ServiceCategoryMother.COMPANY_ID)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(ServiceCategoryMother.CATEGORY_ID,
                    ServiceCategoryMother.COMPANY_ID))
                    .isInstanceOf(ServiceCategoryNotFoundException.class).hasMessageContaining(
                            "ServiceCategory not found: " + ServiceCategoryMother.CATEGORY_ID);

            verify(repository, never()).findByIdAndCompanyId(anyLong(), anyLong());
        }

        @Test
        @DisplayName("una fila reactivada pero ilocalizable tambien lanza no encontrada")
        void una_fila_reactivada_pero_ilocalizable_tambien_lanza_no_encontrada() {
            when(repository.reactivate(ServiceCategoryMother.CATEGORY_ID,
                    ServiceCategoryMother.COMPANY_ID)).thenReturn(1);
            when(repository.findByIdAndCompanyId(ServiceCategoryMother.CATEGORY_ID,
                    ServiceCategoryMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(ServiceCategoryMother.CATEGORY_ID,
                    ServiceCategoryMother.COMPANY_ID))
                    .isInstanceOf(ServiceCategoryNotFoundException.class).hasMessageContaining(
                            "ServiceCategory not found: " + ServiceCategoryMother.CATEGORY_ID);
        }
    }
}
