package com.vetsoftware.app.service.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.service.application.port.out.ServiceRepository;
import com.vetsoftware.app.service.domain.ServiceNotFoundException;
import com.vetsoftware.app.service.testsupport.ServiceMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteServiceService")
class DeleteServiceServiceTest {

    @Mock
    private ServiceRepository repository;

    @InjectMocks
    private DeleteServiceService service;

    @Nested
    @DisplayName("eliminacion")
    class Eliminacion {

        @Test
        @DisplayName("elimina el servicio cuando existe en la empresa")
        void elimina_el_servicio_cuando_existe() {
            when(repository.findByIdAndCompanyId(ServiceMother.SERVICE_ID,
                    ServiceMother.COMPANY_ID))
                    .thenReturn(Optional.of(ServiceMother.consultaGeneral()));

            service.execute(ServiceMother.SERVICE_ID, ServiceMother.COMPANY_ID);

            verify(repository).delete(ServiceMother.SERVICE_ID);
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("un servicio inexistente en la empresa no se borra")
        void servicio_inexistente_no_se_borra() {
            when(repository.findByIdAndCompanyId(ServiceMother.SERVICE_ID,
                    ServiceMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(ServiceMother.SERVICE_ID, ServiceMother.COMPANY_ID))
                    .isInstanceOf(ServiceNotFoundException.class)
                    .hasMessageContaining("Service not found: " + ServiceMother.SERVICE_ID);

            verify(repository, never()).delete(org.mockito.ArgumentMatchers.any());
        }
    }
}
