package com.vetsoftware.app.medicationschedule.application.usecase;

import static com.vetsoftware.app.medicationschedule.testsupport.MedicationScheduleMother.MEDICATION_ID;
import static com.vetsoftware.app.medicationschedule.testsupport.MedicationScheduleMother.SCHEDULE_ID;
import static com.vetsoftware.app.medicationschedule.testsupport.MedicationScheduleMother.cada8hDurante3Dias;
import static com.vetsoftware.app.medicationschedule.testsupport.MedicationScheduleMother.tomaPendiente;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.medicationschedule.application.command.ApplyMedicationScheduleCommand;
import com.vetsoftware.app.medicationschedule.application.dto.MedicationScheduleDto;
import com.vetsoftware.app.medicationschedule.application.port.out.HospitalizationMedicationQueryPort;
import com.vetsoftware.app.medicationschedule.application.port.out.MedicationScheduleRepository;
import com.vetsoftware.app.medicationschedule.domain.AppliedStatus;
import com.vetsoftware.app.medicationschedule.domain.MedicationSchedule;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApplyMedicationScheduleService")
class ApplyMedicationScheduleServiceTest {

    private static final Long COMPANY_ID = 9L;
    private static final Long OTRA_EMPRESA = 77L;

    @Mock
    private MedicationScheduleRepository repository;
    @Mock
    private HospitalizationMedicationQueryPort medicationQueryPort;
    @InjectMocks
    private ApplyMedicationScheduleService service;

    @Nested
    @DisplayName("Aplicacion")
    class Aplicacion {

        @Test
        @DisplayName("marca la toma como aplicada y devuelve el plan actualizado de la orden")
        void marca_la_toma_como_aplicada_y_devuelve_el_plan() {
            MedicationSchedule toma = tomaPendiente();
            when(repository.findById(SCHEDULE_ID)).thenReturn(Optional.of(toma));
            when(medicationQueryPort.findByIdAndCompanyId(MEDICATION_ID, COMPANY_ID))
                    .thenReturn(Optional.of(cada8hDurante3Dias()));
            when(repository.findByHospitalizationMedicationIdAndCompanyId(MEDICATION_ID,
                    COMPANY_ID)).thenReturn(List.of(toma));

            List<MedicationScheduleDto> result = service
                    .execute(new ApplyMedicationScheduleCommand(SCHEDULE_ID, COMPANY_ID));

            ArgumentCaptor<MedicationSchedule> captor = ArgumentCaptor
                    .forClass(MedicationSchedule.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getAppliedStatus()).isEqualTo(AppliedStatus.APPLIED);
            assertThat(captor.getValue().getRealDateTime()).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().id()).isEqualTo(SCHEDULE_ID);
        }

        @Test
        @DisplayName("sin empresa (camino SYSTEM) aplica sin consultar la orden")
        void sin_empresa_aplica_sin_consultar_la_orden() {
            MedicationSchedule toma = tomaPendiente();
            when(repository.findById(SCHEDULE_ID)).thenReturn(Optional.of(toma));
            when(repository.findByHospitalizationMedicationId(MEDICATION_ID))
                    .thenReturn(List.of(toma));

            service.execute(new ApplyMedicationScheduleCommand(SCHEDULE_ID, null));

            verifyNoInteractions(medicationQueryPort);
            verify(repository).save(any());
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("una toma inexistente lanza excepcion y no escribe nada")
        void una_toma_inexistente_lanza_excepcion_y_no_escribe() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(new ApplyMedicationScheduleCommand(999L, COMPANY_ID)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Medication schedule not found: 999");

            verify(repository, never()).save(any());
            verifyNoInteractions(medicationQueryPort);
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        /**
         * La toma no tiene empresa propia: la unica prueba de propiedad es que la orden
         * de medicacion padre pertenezca al tenant. Si no, el servicio tiene que frenar
         * ANTES de marcar nada — una toma aplicada falsea la hoja de medicacion de un
         * paciente ajeno y no se puede deshacer desde la UI.
         */
        @Test
        @DisplayName("la toma de un paciente de otra empresa no se marca como aplicada")
        void la_toma_de_otra_empresa_no_se_marca_como_aplicada() {
            when(repository.findById(SCHEDULE_ID)).thenReturn(Optional.of(tomaPendiente()));
            when(medicationQueryPort.findByIdAndCompanyId(MEDICATION_ID, OTRA_EMPRESA))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service
                    .execute(new ApplyMedicationScheduleCommand(SCHEDULE_ID, OTRA_EMPRESA)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Medication schedule not found: " + SCHEDULE_ID);

            verify(repository, never()).save(any());
            verify(repository, never()).findByHospitalizationMedicationId(any());
            verify(repository, never()).findByHospitalizationMedicationIdAndCompanyId(any(), any());
        }
    }
}
