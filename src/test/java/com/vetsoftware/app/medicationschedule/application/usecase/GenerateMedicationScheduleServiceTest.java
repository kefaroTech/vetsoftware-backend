package com.vetsoftware.app.medicationschedule.application.usecase;

import static com.vetsoftware.app.medicationschedule.testsupport.MedicationScheduleMother.EMPLEADO;
import static com.vetsoftware.app.medicationschedule.testsupport.MedicationScheduleMother.MEDICATION_ID;
import static com.vetsoftware.app.medicationschedule.testsupport.MedicationScheduleMother.PRIMERA_TOMA;
import static com.vetsoftware.app.medicationschedule.testsupport.MedicationScheduleMother.cada8hDurante3Dias;
import static com.vetsoftware.app.medicationschedule.testsupport.MedicationScheduleMother.tomaAplicada;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.medicationschedule.application.command.GenerateMedicationScheduleCommand;
import com.vetsoftware.app.medicationschedule.application.dto.MedicationScheduleDto;
import com.vetsoftware.app.medicationschedule.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.medicationschedule.application.port.out.HospitalizationMedicationQueryPort;
import com.vetsoftware.app.medicationschedule.application.port.out.MedicationScheduleRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("GenerateMedicationScheduleService")
class GenerateMedicationScheduleServiceTest {

    private static final Long COMPANY_ID = 9L;

    @Mock
    private MedicationScheduleRepository repository;
    @Mock
    private HospitalizationMedicationQueryPort medicationQueryPort;
    @Mock
    private EmployeeQueryPort employeeQueryPort;
    @InjectMocks
    private GenerateMedicationScheduleService service;

    private static GenerateMedicationScheduleCommand comando() {
        return new GenerateMedicationScheduleCommand(MEDICATION_ID, EMPLEADO.id(), COMPANY_ID);
    }

    @Nested
    @DisplayName("Alta nueva — sin tomas aplicadas todavia")
    class AltaNueva {

        @Test
        @DisplayName("regenera el calendario completo: deshabilita todo y guarda las nueve tomas")
        void regenera_el_calendario_completo() {
            when(medicationQueryPort.findByIdAndCompanyId(MEDICATION_ID, COMPANY_ID))
                    .thenReturn(Optional.of(cada8hDurante3Dias()));
            when(employeeQueryPort.findById(EMPLEADO.id())).thenReturn(Optional.of(EMPLEADO));
            when(repository.findByHospitalizationMedicationIdAndCompanyId(MEDICATION_ID,
                    COMPANY_ID)).thenReturn(List.of());
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            List<MedicationScheduleDto> result = service.execute(comando());

            verify(repository).disableByHospitalizationMedicationId(MEDICATION_ID, COMPANY_ID);
            verify(repository, never()).disablePendingByHospitalizationMedicationId(any(), any());
            verify(repository, times(9)).save(any());
            assertThat(result).hasSize(9);
        }
    }

    @Nested
    @DisplayName("Con tomas ya aplicadas — regeneracion parcial")
    class ConAplicadas {

        @Test
        @DisplayName("conserva las aplicadas y solo reconstruye las pendientes")
        void conserva_las_aplicadas_y_reconstruye_solo_las_pendientes() {
            var aplicada = tomaAplicada(700L, PRIMERA_TOMA, PRIMERA_TOMA);
            when(medicationQueryPort.findByIdAndCompanyId(MEDICATION_ID, COMPANY_ID))
                    .thenReturn(Optional.of(cada8hDurante3Dias()));
            when(employeeQueryPort.findById(EMPLEADO.id())).thenReturn(Optional.of(EMPLEADO));
            when(repository.findByHospitalizationMedicationIdAndCompanyId(MEDICATION_ID,
                    COMPANY_ID)).thenReturn(List.of(aplicada));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            List<MedicationScheduleDto> result = service.execute(comando());

            verify(repository).disablePendingByHospitalizationMedicationId(MEDICATION_ID,
                    COMPANY_ID);
            verify(repository, never()).disableByHospitalizationMedicationId(any(), any());
            // 9 tomas totales - 1 aplicada = 8 pendientes regeneradas + la aplicada = 9.
            verify(repository, times(8)).save(any());
            assertThat(result).hasSize(9);
            assertThat(result).extracting(MedicationScheduleDto::id).contains(700L);
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("una orden inexistente lanza excepcion y no toca el repositorio ni el empleado")
        void orden_inexistente_lanza_excepcion() {
            when(medicationQueryPort.findByIdAndCompanyId(MEDICATION_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comando()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Hospitalization medication not found: " + MEDICATION_ID);

            verifyNoInteractions(repository, employeeQueryPort);
        }

        @Test
        @DisplayName("un empleado inexistente lanza excepcion y no toca el repositorio")
        void empleado_inexistente_lanza_excepcion() {
            when(medicationQueryPort.findByIdAndCompanyId(MEDICATION_ID, COMPANY_ID))
                    .thenReturn(Optional.of(cada8hDurante3Dias()));
            when(employeeQueryPort.findById(EMPLEADO.id())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comando()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Employee not found: " + EMPLEADO.id());

            verifyNoInteractions(repository);
        }
    }
}
