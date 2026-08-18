package com.vetsoftware.app.procedureschedule.application.usecase;

import static com.vetsoftware.app.procedureschedule.testsupport.ProcedureScheduleMother.HOSPITALIZATION_ID;
import static com.vetsoftware.app.procedureschedule.testsupport.ProcedureScheduleMother.tomaPendiente;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.procedureschedule.application.dto.ProcedureScheduleDto;
import com.vetsoftware.app.procedureschedule.application.port.out.ProcedureScheduleRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListProcedureSchedulesByHospitalizationService")
class ListProcedureSchedulesByHospitalizationServiceTest {

    private static final Long COMPANY_ID = 9L;
    private static final Long OTRA_EMPRESA = 77L;

    @Mock
    private ProcedureScheduleRepository repository;
    @InjectMocks
    private ListProcedureSchedulesByHospitalizationService service;

    @Nested
    @DisplayName("Listado")
    class Listado {

        @Test
        @DisplayName("lista las ejecuciones de todas las ordenes de la hospitalizacion")
        void lista_las_ejecuciones_de_la_hospitalizacion() {
            when(repository.findByHospitalizationIdAndCompanyId(HOSPITALIZATION_ID, COMPANY_ID))
                    .thenReturn(List.of(tomaPendiente()));

            List<ProcedureScheduleDto> result = service.listByHospitalization(HOSPITALIZATION_ID,
                    COMPANY_ID);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("una hospitalizacion sin ejecuciones devuelve una lista vacia, no null")
        void sin_ejecuciones_devuelve_lista_vacia() {
            when(repository.findByHospitalizationIdAndCompanyId(HOSPITALIZATION_ID, COMPANY_ID))
                    .thenReturn(List.of());

            assertThat(service.listByHospitalization(HOSPITALIZATION_ID, COMPANY_ID)).isEmpty();
        }

        @Test
        @DisplayName("sin empresa (camino SYSTEM) lee sin acotar")
        void sin_empresa_lee_sin_acotar() {
            when(repository.findByHospitalizationId(HOSPITALIZATION_ID))
                    .thenReturn(List.of(tomaPendiente()));

            assertThat(service.listByHospitalization(HOSPITALIZATION_ID, null)).hasSize(1);

            verify(repository, never()).findByHospitalizationIdAndCompanyId(any(), any());
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        /**
         * El {@code hospitalizationId} lo escribe el cliente en la URL. Sin el filtro
         * por empresa este listado entregaba el plan de procedimientos completo del
         * paciente hospitalizado de otro tenant, que es historia clinica ajena.
         */
        @Test
        @DisplayName("la hospitalizacion de otra empresa no devuelve ninguna ejecucion")
        void la_hospitalizacion_de_otra_empresa_no_devuelve_nada() {
            when(repository.findByHospitalizationIdAndCompanyId(HOSPITALIZATION_ID, OTRA_EMPRESA))
                    .thenReturn(List.of());

            assertThat(service.listByHospitalization(HOSPITALIZATION_ID, OTRA_EMPRESA)).isEmpty();

            verify(repository, never()).findByHospitalizationId(any());
        }
    }
}
