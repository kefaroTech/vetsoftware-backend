package com.vetsoftware.app.servicecategory.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.servicecategory.application.port.out.ServiceCategoryRepository;
import com.vetsoftware.app.servicecategory.application.port.out.ServiceChildrenQueryPort;
import com.vetsoftware.app.servicecategory.domain.ServiceCategory;
import com.vetsoftware.app.servicecategory.domain.ServiceCategoryHasActiveChildrenException;
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
@DisplayName("DeleteServiceCategoryService")
class DeleteServiceCategoryServiceTest {

    @Mock
    private ServiceCategoryRepository repository;
    @Mock
    private ServiceChildrenQueryPort serviceChildrenQueryPort;

    private DeleteServiceCategoryService service;

    @BeforeEach
    void crearServicio() {
        service = new DeleteServiceCategoryService(repository, serviceChildrenQueryPort);
    }

    @Nested
    @DisplayName("borrado")
    class Borrado {

        @Test
        @DisplayName("borra la categoria cuando no tiene servicios hijos activos")
        void borra_la_categoria_sin_hijos_activos() {
            ServiceCategory existente = ServiceCategoryMother.activa();
            when(repository.findByIdAndCompanyId(ServiceCategoryMother.CATEGORY_ID,
                    ServiceCategoryMother.COMPANY_ID)).thenReturn(Optional.of(existente));
            when(serviceChildrenQueryPort
                    .existsActiveByServiceCategoryId(ServiceCategoryMother.CATEGORY_ID))
                    .thenReturn(false);

            service.execute(ServiceCategoryMother.CATEGORY_ID, ServiceCategoryMother.COMPANY_ID);

            verify(repository).delete(ServiceCategoryMother.CATEGORY_ID);
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("no consulta hijos ni borra si la categoria no existe")
        void no_consulta_hijos_ni_borra_si_la_categoria_no_existe() {
            when(repository.findByIdAndCompanyId(ServiceCategoryMother.CATEGORY_ID,
                    ServiceCategoryMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(ServiceCategoryMother.CATEGORY_ID,
                    ServiceCategoryMother.COMPANY_ID))
                    .isInstanceOf(ServiceCategoryNotFoundException.class).hasMessageContaining(
                            "ServiceCategory not found: " + ServiceCategoryMother.CATEGORY_ID);

            verifyNoInteractions(serviceChildrenQueryPort);
            verify(repository, never()).delete(any());
        }

        @Test
        @DisplayName("no borra si tiene servicios hijos activos")
        void no_borra_si_tiene_servicios_hijos_activos() {
            ServiceCategory existente = ServiceCategoryMother.activa();
            when(repository.findByIdAndCompanyId(ServiceCategoryMother.CATEGORY_ID,
                    ServiceCategoryMother.COMPANY_ID)).thenReturn(Optional.of(existente));
            when(serviceChildrenQueryPort
                    .existsActiveByServiceCategoryId(ServiceCategoryMother.CATEGORY_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.execute(ServiceCategoryMother.CATEGORY_ID,
                    ServiceCategoryMother.COMPANY_ID))
                    .isInstanceOf(ServiceCategoryHasActiveChildrenException.class)
                    .hasMessageContaining("" + ServiceCategoryMother.CATEGORY_ID)
                    .hasMessageContaining("service");

            verify(repository, never()).delete(any());
        }
    }
}
