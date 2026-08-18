package com.vetsoftware.app.service.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
@DisplayName("FindServiceService")
class FindServiceServiceTest {

    @Mock
    private ServiceRepository repository;

    @InjectMocks
    private FindServiceService service;

    @Nested
    @DisplayName("busqueda por id")
    class BusquedaPorId {

        @Test
        @DisplayName("devuelve el dto cuando el servicio existe en la empresa")
        void devuelve_el_dto_cuando_existe() {
            when(repository.findByIdAndCompanyId(ServiceMother.SERVICE_ID,
                    ServiceMother.COMPANY_ID))
                    .thenReturn(Optional.of(ServiceMother.consultaGeneral()));

            ServiceDto dto = service.findById(ServiceMother.SERVICE_ID, ServiceMother.COMPANY_ID);

            assertThat(dto.id()).isEqualTo(ServiceMother.SERVICE_ID);
            assertThat(dto.name()).isEqualTo("Consulta general");
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("un servicio de otra empresa o inexistente lanza ServiceNotFoundException")
        void servicio_inexistente_lanza_not_found() {
            when(repository.findByIdAndCompanyId(ServiceMother.SERVICE_ID,
                    ServiceMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.findById(ServiceMother.SERVICE_ID, ServiceMother.COMPANY_ID))
                    .isInstanceOf(ServiceNotFoundException.class)
                    .hasMessageContaining("Service not found: " + ServiceMother.SERVICE_ID);
        }
    }
}
