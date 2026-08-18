package com.vetsoftware.app.service.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.service.application.dto.ServiceDto;
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
@DisplayName("ReactivateServiceService")
class ReactivateServiceServiceTest {

    @Mock
    private ServiceRepository repository;

    @InjectMocks
    private ReactivateServiceService service;

    @Nested
    @DisplayName("reactivacion")
    class Reactivacion {

        @Test
        @DisplayName("reactiva y devuelve el servicio releido")
        void reactiva_y_devuelve_el_servicio_releido() {
            when(repository.reactivate(ServiceMother.SERVICE_ID, ServiceMother.COMPANY_ID))
                    .thenReturn(1);
            when(repository.findByIdAndCompanyId(ServiceMother.SERVICE_ID,
                    ServiceMother.COMPANY_ID))
                    .thenReturn(Optional.of(ServiceMother.consultaGeneral()));

            ServiceDto dto = service.execute(ServiceMother.SERVICE_ID, ServiceMother.COMPANY_ID);

            assertThat(dto.id()).isEqualTo(ServiceMother.SERVICE_ID);
            assertThat(dto.enabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("cero filas afectadas no relee el servicio")
        void cero_filas_no_relee() {
            when(repository.reactivate(ServiceMother.SERVICE_ID, ServiceMother.COMPANY_ID))
                    .thenReturn(0);

            assertThatThrownBy(
                    () -> service.execute(ServiceMother.SERVICE_ID, ServiceMother.COMPANY_ID))
                    .isInstanceOf(ServiceNotFoundException.class)
                    .hasMessageContaining("Service not found: " + ServiceMother.SERVICE_ID);

            verify(repository, never()).findByIdAndCompanyId(org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any());
        }

        @Test
        @DisplayName("filas afectadas pero relectura vacia tambien falla")
        void filas_afectadas_pero_relectura_vacia_falla() {
            when(repository.reactivate(ServiceMother.SERVICE_ID, ServiceMother.COMPANY_ID))
                    .thenReturn(1);
            when(repository.findByIdAndCompanyId(ServiceMother.SERVICE_ID,
                    ServiceMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(ServiceMother.SERVICE_ID, ServiceMother.COMPANY_ID))
                    .isInstanceOf(ServiceNotFoundException.class)
                    .hasMessageContaining("Service not found: " + ServiceMother.SERVICE_ID);
        }
    }
}
