package com.vetsoftware.app.hospitalizationmedication.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.hospitalizationmedication.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.hospitalizationmedication.application.port.out.HospitalizationMedicationRepository;
import com.vetsoftware.app.hospitalizationmedication.domain.HospitalizationMedication;
import com.vetsoftware.app.hospitalizationmedication.domain.HospitalizationMedicationNotFoundException;
import com.vetsoftware.app.hospitalizationmedication.testsupport.HospitalizationMedicationMother;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SuspendHospitalizationMedicationService")
class SuspendHospitalizationMedicationServiceTest {

    @Mock
    private HospitalizationMedicationRepository repository;
    @Mock
    private EmployeeQueryPort employeeQueryPort;

    @InjectMocks
    private SuspendHospitalizationMedicationService service;

    @Captor
    private ArgumentCaptor<HospitalizationMedication> captor;

    @Nested
    @DisplayName("suspension")
    class Suspension {

        @Test
        @DisplayName("suspende con el empleado resuelto por el puerto y persiste")
        void suspende_con_el_empleado_resuelto_y_persiste() {
            when(repository.findByIdAndCompanyId(HospitalizationMedicationMother.MEDICATION_ID,
                    HospitalizationMedicationMother.COMPANY_ID))
                    .thenReturn(Optional.of(HospitalizationMedicationMother.activo()));
            when(employeeQueryPort.findById(HospitalizationMedicationMother.OTHER_EMPLOYEE_ID))
                    .thenReturn(Optional.of(HospitalizationMedicationMother.SUSPENDIDO_POR));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(HospitalizationMedicationMother.comandoSuspender());

            verify(repository).save(captor.capture());
            HospitalizationMedication guardado = captor.getValue();
            assertThat(guardado.getSuspensionBy())
                    .isEqualTo(HospitalizationMedicationMother.SUSPENDIDO_POR);
            // El service llama a LocalDateTime.now() directamente: deuda registrada en
            // el CLAUDE.md, no hay Clock inyectable. La asercion es una ventana.
            assertThat(guardado.getSuspensionDate()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
        }
    }

    @Nested
    @DisplayName("orden inexistente")
    class OrdenInexistente {

        @Test
        @DisplayName("no consulta al empleado ni persiste")
        void no_consulta_al_empleado_ni_persiste() {
            when(repository.findByIdAndCompanyId(HospitalizationMedicationMother.MEDICATION_ID,
                    HospitalizationMedicationMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(HospitalizationMedicationMother.comandoSuspender()))
                    .isInstanceOf(HospitalizationMedicationNotFoundException.class)
                    .hasMessageContaining("HospitalizationMedication not found: "
                            + HospitalizationMedicationMother.MEDICATION_ID);

            verifyNoInteractions(employeeQueryPort);
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("empleado inexistente")
    class EmpleadoInexistente {

        @Test
        @DisplayName("no persiste la suspension")
        void no_persiste_la_suspension() {
            when(repository.findByIdAndCompanyId(HospitalizationMedicationMother.MEDICATION_ID,
                    HospitalizationMedicationMother.COMPANY_ID))
                    .thenReturn(Optional.of(HospitalizationMedicationMother.activo()));
            when(employeeQueryPort.findById(HospitalizationMedicationMother.OTHER_EMPLOYEE_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(HospitalizationMedicationMother.comandoSuspender()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Employee not found: "
                            + HospitalizationMedicationMother.OTHER_EMPLOYEE_ID);

            verify(repository, never()).save(any());
        }
    }

    /**
     * Antes de BE-COV la carga era {@code findById(command.id())} y el
     * {@code @PreAuthorize} no miraba la empresa: un empleado de A suspendia la
     * medicacion de un paciente hospitalizado en B adivinando el id — daño clinico
     * directo. La carga acotada convierte la orden ajena en un 404.
     */
    @Nested
    @DisplayName("aislamiento multi-tenant")
    class Tenencia {

        @Test
        @DisplayName("una orden de otra empresa no se suspende: 404 y no escribe nada")
        void una_orden_de_otra_empresa_no_se_suspende() {
            when(repository.findByIdAndCompanyId(HospitalizationMedicationMother.MEDICATION_ID,
                    HospitalizationMedicationMother.OTRA_COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(HospitalizationMedicationMother
                    .comandoSuspender(HospitalizationMedicationMother.OTRA_COMPANY_ID)))
                    .isInstanceOf(HospitalizationMedicationNotFoundException.class)
                    .hasMessageContaining("HospitalizationMedication not found: "
                            + HospitalizationMedicationMother.MEDICATION_ID);

            verifyNoInteractions(employeeQueryPort);
            verify(repository, never()).save(any());
        }
    }
}
