package com.vetsoftware.app.medicationschedule.application.usecase;

import static com.vetsoftware.app.medicationschedule.testsupport.MedicationScheduleMother.HOSPITALIZATION_ID;
import static com.vetsoftware.app.medicationschedule.testsupport.MedicationScheduleMother.tomaPendiente;
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
@DisplayName("ListMedicationSchedulesByHospitalizationService")
class ListMedicationSchedulesByHospitalizationServiceTest {

    private static final Long COMPANY_ID = 9L;
    private static final Long OTRA_EMPRESA = 77L;

    @Mock
    private MedicationScheduleRepository repository;
    @InjectMocks
    private ListMedicationSchedulesByHospitalizationService service;

    @Nested
    @DisplayName("Listado")
    class Listado {

        @Test
        @DisplayName("lista las tomas de todas las ordenes de la hospitalizacion")
        void lista_las_tomas_de_la_hospitalizacion() {
            when(repository.findByHospitalizationIdAndCompanyId(HOSPITALIZATION_ID, COMPANY_ID))
                    .thenReturn(List.of(tomaPendiente()));

            List<MedicationScheduleDto> result = service.listByHospitalization(HOSPITALIZATION_ID,
                    COMPANY_ID);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("una hospitalizacion sin tomas devuelve una lista vacia, no null")
        void sin_tomas_devuelve_lista_vacia() {
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
         * por empresa este listado entregaba la hoja de medicacion completa —farmaco,
         * dosis, horario— del paciente hospitalizado de otro tenant, que es historia
         * clinica ajena.
         */
        @Test
        @DisplayName("la hospitalizacion de otra empresa no devuelve ninguna toma")
        void la_hospitalizacion_de_otra_empresa_no_devuelve_nada() {
            when(repository.findByHospitalizationIdAndCompanyId(HOSPITALIZATION_ID, OTRA_EMPRESA))
                    .thenReturn(List.of());

            assertThat(service.listByHospitalization(HOSPITALIZATION_ID, OTRA_EMPRESA)).isEmpty();

            verify(repository, never()).findByHospitalizationId(any());
        }
    }
}
