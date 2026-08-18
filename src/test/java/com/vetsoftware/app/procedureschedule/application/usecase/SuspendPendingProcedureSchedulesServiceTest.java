package com.vetsoftware.app.procedureschedule.application.usecase;

import static com.vetsoftware.app.procedureschedule.testsupport.ProcedureScheduleMother.PRIMERA_TOMA;
import static com.vetsoftware.app.procedureschedule.testsupport.ProcedureScheduleMother.PROCEDURE_ID;
import static com.vetsoftware.app.procedureschedule.testsupport.ProcedureScheduleMother.tomaAplicada;
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
@DisplayName("SuspendPendingProcedureSchedulesService")
class SuspendPendingProcedureSchedulesServiceTest {

    private static final Long COMPANY_ID = 9L;
    private static final Long OTRA_EMPRESA = 77L;

    @Mock
    private ProcedureScheduleRepository repository;
    @InjectMocks
    private SuspendPendingProcedureSchedulesService service;

    @Nested
    @DisplayName("Suspension")
    class Suspension {

        @Test
        @DisplayName("deshabilita las pendientes y devuelve las aplicadas que quedan")
        void deshabilita_las_pendientes_y_devuelve_las_aplicadas() {
            var aplicada = tomaAplicada(500L, PRIMERA_TOMA, PRIMERA_TOMA);
            when(repository.findByHospitalizationProcedureIdAndCompanyId(PROCEDURE_ID, COMPANY_ID))
                    .thenReturn(List.of(aplicada));

            List<ProcedureScheduleDto> result = service.execute(PROCEDURE_ID, COMPANY_ID);

            verify(repository).disablePendingByHospitalizationProcedureId(PROCEDURE_ID, COMPANY_ID);
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().appliedStatus()).isEqualTo("APPLIED");
        }

        @Test
        @DisplayName("sin ninguna aplicada devuelve una lista vacia, no null")
        void sin_aplicadas_devuelve_lista_vacia() {
            when(repository.findByHospitalizationProcedureIdAndCompanyId(PROCEDURE_ID, COMPANY_ID))
                    .thenReturn(List.of());

            assertThat(service.execute(PROCEDURE_ID, COMPANY_ID)).isEmpty();
        }

        @Test
        @DisplayName("sin empresa (camino SYSTEM) suspende sin acotar")
        void sin_empresa_suspende_sin_acotar() {
            when(repository.findByHospitalizationProcedureId(PROCEDURE_ID)).thenReturn(List.of());

            service.execute(PROCEDURE_ID, null);

            verify(repository).disablePendingByHospitalizationProcedureId(PROCEDURE_ID);
            verify(repository, never()).disablePendingByHospitalizationProcedureId(any(), any());
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        /**
         * Aqui no hay lectura previa que valide la propiedad: el servicio escribe
         * primero y decide que devolver mirando lo que quedo vivo, asi que el
         * {@code AND company_id} del UPDATE es la unica barrera. Con el procedimiento
         * de otro tenant el UPDATE acotado no toca ninguna fila —y nunca se emite el
         * ancho, que si le habria suspendido el plan entero.
         */
        @Test
        @DisplayName("el procedimiento de otra empresa no suspende ninguna ejecucion")
        void el_procedimiento_de_otra_empresa_no_suspende_nada() {
            when(repository.findByHospitalizationProcedureIdAndCompanyId(PROCEDURE_ID,
                    OTRA_EMPRESA)).thenReturn(List.of());

            assertThat(service.execute(PROCEDURE_ID, OTRA_EMPRESA)).isEmpty();

            verify(repository).disablePendingByHospitalizationProcedureId(PROCEDURE_ID,
                    OTRA_EMPRESA);
            verify(repository, never()).disablePendingByHospitalizationProcedureId(any());
            verify(repository, never()).findByHospitalizationProcedureId(any());
        }
    }
}
