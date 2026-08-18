package com.vetsoftware.app.hospitalizationprocedure.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.hospitalizationprocedure.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.hospitalizationprocedure.application.port.out.HospitalizationProcedureRepository;
import com.vetsoftware.app.hospitalizationprocedure.domain.HospitalizationProcedure;
import com.vetsoftware.app.hospitalizationprocedure.domain.HospitalizationProcedureNotFoundException;
import com.vetsoftware.app.hospitalizationprocedure.testsupport.HospitalizationProcedureMother;
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
@DisplayName("SuspendHospitalizationProcedureService")
class SuspendHospitalizationProcedureServiceTest {

    @Mock
    private HospitalizationProcedureRepository repository;
    @Mock
    private EmployeeQueryPort employeeQueryPort;

    @InjectMocks
    private SuspendHospitalizationProcedureService service;

    @Captor
    private ArgumentCaptor<HospitalizationProcedure> captor;

    @Nested
    @DisplayName("suspension")
    class Suspension {

        @Test
        @DisplayName("suspende con el empleado resuelto por el puerto y persiste")
        void suspende_con_el_empleado_resuelto_y_persiste() {
            when(repository.findByIdAndCompanyId(HospitalizationProcedureMother.PROCEDURE_ID,
                    HospitalizationProcedureMother.COMPANY_ID))
                    .thenReturn(Optional.of(HospitalizationProcedureMother.activo()));
            when(employeeQueryPort.findById(HospitalizationProcedureMother.OTHER_EMPLOYEE_ID))
                    .thenReturn(Optional.of(HospitalizationProcedureMother.SUSPENDIDO_POR));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(HospitalizationProcedureMother.comandoSuspender());

            verify(repository).save(captor.capture());
            HospitalizationProcedure guardado = captor.getValue();
            assertThat(guardado.getSuspensionBy())
                    .isEqualTo(HospitalizationProcedureMother.SUSPENDIDO_POR);
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
            when(repository.findByIdAndCompanyId(HospitalizationProcedureMother.PROCEDURE_ID,
                    HospitalizationProcedureMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(HospitalizationProcedureMother.comandoSuspender()))
                    .isInstanceOf(HospitalizationProcedureNotFoundException.class)
                    .hasMessageContaining("HospitalizationProcedure not found: "
                            + HospitalizationProcedureMother.PROCEDURE_ID);

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
            when(repository.findByIdAndCompanyId(HospitalizationProcedureMother.PROCEDURE_ID,
                    HospitalizationProcedureMother.COMPANY_ID))
                    .thenReturn(Optional.of(HospitalizationProcedureMother.activo()));
            when(employeeQueryPort.findById(HospitalizationProcedureMother.OTHER_EMPLOYEE_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(HospitalizationProcedureMother.comandoSuspender()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Employee not found: "
                            + HospitalizationProcedureMother.OTHER_EMPLOYEE_ID);

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        /**
         * El comando lleva ahora el {@code companyId} del contexto y el servicio lee
         * acotado por el. Para el tenant equivocado la orden no existe: 404, sin
         * consultar el empleado ni escribir. Antes se leia con {@code findById(id)} a
         * secas y cualquier empleado con {@code hospitalization.update} suspendia la
         * orden de otra empresa adivinando el id.
         */
        @Test
        @DisplayName("una orden de otra empresa no se suspende: 404 y no escribe nada")
        void una_orden_de_otra_empresa_no_se_suspende() {
            when(repository.findByIdAndCompanyId(HospitalizationProcedureMother.PROCEDURE_ID,
                    HospitalizationProcedureMother.OTRA_COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(HospitalizationProcedureMother
                    .comandoSuspender(HospitalizationProcedureMother.OTRA_COMPANY_ID)))
                    .isInstanceOf(HospitalizationProcedureNotFoundException.class)
                    .hasMessageContaining("HospitalizationProcedure not found: "
                            + HospitalizationProcedureMother.PROCEDURE_ID);

            verifyNoInteractions(employeeQueryPort);
            verify(repository, never()).save(any());
        }
    }
}
