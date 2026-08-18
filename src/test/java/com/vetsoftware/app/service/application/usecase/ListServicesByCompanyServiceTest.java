package com.vetsoftware.app.service.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.service.application.dto.ServiceDto;
import com.vetsoftware.app.service.application.port.out.ServiceRepository;
import com.vetsoftware.app.service.testsupport.ServiceMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListServicesByCompanyService")
class ListServicesByCompanyServiceTest {

    @Mock
    private ServiceRepository repository;

    @InjectMocks
    private ListServicesByCompanyService service;

    @Nested
    @DisplayName("listByCompany")
    class ListByCompany {

        @Test
        @DisplayName("mapea los servicios activos de la empresa")
        void mapea_los_servicios_activos_de_la_empresa() {
            when(repository.findAllByCompanyId(ServiceMother.COMPANY_ID))
                    .thenReturn(List.of(ServiceMother.consultaGeneral()));

            List<ServiceDto> resultado = service.listByCompany(ServiceMother.COMPANY_ID);

            assertThat(resultado).extracting(ServiceDto::id)
                    .containsExactly(ServiceMother.SERVICE_ID);
        }

        @Test
        @DisplayName("una empresa sin servicios devuelve lista vacia")
        void empresa_sin_servicios_devuelve_lista_vacia() {
            when(repository.findAllByCompanyId(ServiceMother.COMPANY_ID)).thenReturn(List.of());

            assertThat(service.listByCompany(ServiceMother.COMPANY_ID)).isEmpty();
        }
    }

    @Nested
    @DisplayName("listDisabledByCompany")
    class ListDisabledByCompany {

        @Test
        @DisplayName("mapea los servicios pausados de la empresa")
        void mapea_los_servicios_pausados_de_la_empresa() {
            when(repository.findAllDisabledByCompanyId(ServiceMother.COMPANY_ID))
                    .thenReturn(List.of(ServiceMother.exenta()));

            List<ServiceDto> resultado = service.listDisabledByCompany(ServiceMother.COMPANY_ID);

            assertThat(resultado).extracting(ServiceDto::id).containsExactly(2L);
        }
    }
}
