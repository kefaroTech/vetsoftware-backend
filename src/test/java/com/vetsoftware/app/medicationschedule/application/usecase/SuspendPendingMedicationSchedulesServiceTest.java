package com.vetsoftware.app.medicationschedule.application.usecase;

import static com.vetsoftware.app.medicationschedule.testsupport.MedicationScheduleMother.MEDICATION_ID;
import static com.vetsoftware.app.medicationschedule.testsupport.MedicationScheduleMother.PRIMERA_TOMA;
import static com.vetsoftware.app.medicationschedule.testsupport.MedicationScheduleMother.tomaAplicada;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.medicationschedule.application.dto.MedicationScheduleDto;
import com.vetsoftware.app.medicationschedule.application.port.out.MedicationScheduleRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SuspendPendingMedicationSchedulesService")
class SuspendPendingMedicationSchedulesServiceTest {

    private static final Long COMPANY_ID = 9L;
    private static final Long OTRA_EMPRESA = 77L;

    @Mock
    private MedicationScheduleRepository repository;
    @InjectMocks
    private SuspendPendingMedicationSchedulesService service;

    @Nested
    @DisplayName("Suspension")
    class Suspension {

        @Test
        @DisplayName("deshabilita las pendientes y devuelve las aplicadas que quedan")
        void deshabilita_las_pendientes_y_devuelve_las_aplicadas() {
            var aplicada = tomaAplicada(500L, PRIMERA_TOMA, PRIMERA_TOMA);
            when(repository.findByHospitalizationMedicationIdAndCompanyId(MEDICATION_ID,
                    COMPANY_ID)).thenReturn(List.of(aplicada));

            List<MedicationScheduleDto> result = service.execute(MEDICATION_ID, COMPANY_ID);

            verify(repository).disablePendingByHospitalizationMedicationId(MEDICATION_ID,
                    COMPANY_ID);
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().appliedStatus()).isEqualTo("APPLIED");
        }

        @Test
        @DisplayName("sin ninguna aplicada devuelve una lista vacia, no null")
        void sin_aplicadas_devuelve_lista_vacia() {
            when(repository.findByHospitalizationMedicationIdAndCompanyId(MEDICATION_ID,
                    COMPANY_ID)).thenReturn(List.of());

            assertThat(service.execute(MEDICATION_ID, COMPANY_ID)).isEmpty();
        }

        @Test
        @DisplayName("sin empresa (camino SYSTEM) suspende sin acotar")
        void sin_empresa_suspende_sin_acotar() {
            when(repository.findByHospitalizationMedicationId(MEDICATION_ID)).thenReturn(List.of());

            service.execute(MEDICATION_ID, null);

            verify(repository).disablePendingByHospitalizationMedicationId(MEDICATION_ID);
            verify(repository, never()).disablePendingByHospitalizationMedicationId(any(), any());
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        /**
         * Aqui no hay lectura previa que valide la propiedad: el servicio escribe
         * primero y decide que devolver mirando lo que quedo vivo, asi que el
         * {@code AND company_id} del UPDATE es la unica barrera. Con la orden de otro
         * tenant el UPDATE acotado no toca ninguna fila —y nunca se emite el ancho, que
         * si se las habria suspendido todas.
         */
        @Test
        @DisplayName("la orden de otra empresa no suspende ninguna toma")
        void la_orden_de_otra_empresa_no_suspende_nada() {
            when(repository.findByHospitalizationMedicationIdAndCompanyId(MEDICATION_ID,
                    OTRA_EMPRESA)).thenReturn(List.of());

            assertThat(service.execute(MEDICATION_ID, OTRA_EMPRESA)).isEmpty();

            verify(repository).disablePendingByHospitalizationMedicationId(MEDICATION_ID,
                    OTRA_EMPRESA);
            verify(repository, never()).disablePendingByHospitalizationMedicationId(any());
            verify(repository, never()).findByHospitalizationMedicationId(any());
        }
    }
}
