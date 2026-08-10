package com.vetsoftware.app.laboratorytest.application.usecase;

import static com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother.BACTERIOLOGA;
import static com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother.BRANCH_ID;
import static com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother.CLINICA;
import static com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother.CONSULTA;
import static com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother.CREADO;
import static com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother.MICHI;
import static com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother.PROCESADO;
import static com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother.UROANALISIS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.laboratorytest.application.command.UpdateLaboratoryTestCommand;
import com.vetsoftware.app.laboratorytest.application.dto.LaboratoryTestDto;
import com.vetsoftware.app.laboratorytest.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.laboratorytest.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.laboratorytest.application.port.out.ConsultationQueryPort;
import com.vetsoftware.app.laboratorytest.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.laboratorytest.application.port.out.LaboratoryTestRepository;
import com.vetsoftware.app.laboratorytest.application.port.out.LaboratoryTestTypeQueryPort;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTest;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestNotFoundException;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestPriority;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestStatus;
import com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateLaboratoryTestService")
class UpdateLaboratoryTestServiceTest {

    private static final Long ID = LaboratoryTestMother.ID;
    private static final Long TEST_TYPE_ID = 8L;
    private static final Long ANIMAL_ID = 12L;
    private static final Long CONSULTATION_ID = 11L;
    private static final Long COMPANY_ID = 9L;
    private static final Long EMPLOYEE_ID = 3L;
    private static final LocalDate NUEVA_FECHA = LocalDate.of(2026, 4, 1);

    @Mock
    private LaboratoryTestRepository repository;
    @Mock
    private LaboratoryTestTypeQueryPort testTypeQueryPort;
    @Mock
    private AnimalQueryPort animalQueryPort;
    @Mock
    private ConsultationQueryPort consultationQueryPort;
    @Mock
    private CompanyQueryPort companyQueryPort;
    @Mock
    private EmployeeQueryPort employeeQueryPort;

    @InjectMocks
    private UpdateLaboratoryTestService service;

    @Captor
    private ArgumentCaptor<LaboratoryTest> guardada;

    private static UpdateLaboratoryTestCommand comando(Integer quantity, String prioridad,
            Long consultationId, Long processedById) {
        return new UpdateLaboratoryTestCommand(ID, NUEVA_FECHA, TEST_TYPE_ID, quantity,
                "Diagnostico revisado", prioridad, ANIMAL_ID, consultationId, COMPANY_ID,
                processedById, processedById == null ? null : PROCESADO);
    }

    private static UpdateLaboratoryTestCommand comandoCompleto() {
        return comando(3, "URGENTE", CONSULTATION_ID, EMPLOYEE_ID);
    }

    private void muestraExistente() {
        when(repository.findById(ID)).thenReturn(Optional.of(LaboratoryTestMother.validada()));
    }

    private void resolucionesBasicas() {
        when(testTypeQueryPort.findById(TEST_TYPE_ID)).thenReturn(Optional.of(UROANALISIS));
        when(animalQueryPort.findById(ANIMAL_ID)).thenReturn(Optional.of(MICHI));
        when(companyQueryPort.findById(COMPANY_ID)).thenReturn(Optional.of(CLINICA));
    }

    private void devuelveLoQueGuarda() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Nested
    @DisplayName("Edicion")
    class Edicion {

        @Test
        @DisplayName("guarda la muestra con los campos editables reemplazados")
        void guarda_la_muestra_con_los_campos_reemplazados() {
            muestraExistente();
            resolucionesBasicas();
            when(consultationQueryPort.findById(CONSULTATION_ID)).thenReturn(Optional.of(CONSULTA));
            when(employeeQueryPort.findById(EMPLOYEE_ID)).thenReturn(Optional.of(BACTERIOLOGA));
            devuelveLoQueGuarda();

            service.execute(comandoCompleto());

            verify(repository).save(guardada.capture());
            LaboratoryTest muestra = guardada.getValue();
            assertThat(muestra.getDate()).isEqualTo(NUEVA_FECHA);
            assertThat(muestra.getTestType()).isEqualTo(UROANALISIS);
            assertThat(muestra.getQuantity()).isEqualTo(3);
            assertThat(muestra.getDiagnosis()).isEqualTo("Diagnostico revisado");
            assertThat(muestra.getPrioridad()).isEqualTo(LaboratoryTestPriority.URGENTE);
            assertThat(muestra.getAnimal()).isEqualTo(MICHI);
            assertThat(muestra.getConsultation()).isEqualTo(CONSULTA);
            assertThat(muestra.getProcessedBy()).isEqualTo(BACTERIOLOGA);
            assertThat(muestra.getProcessedDate()).isEqualTo(PROCESADO);
        }

        @Test
        @DisplayName("la edicion no puede mover el estado, la sede ni la fecha de creacion")
        void la_edicion_no_puede_mover_el_estado_la_sede_ni_la_creacion() {
            muestraExistente();
            resolucionesBasicas();
            devuelveLoQueGuarda();

            service.execute(comando(3, "URGENTE", null, null));

            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue().getStatus()).isEqualTo(LaboratoryTestStatus.COMPLETED);
            assertThat(guardada.getValue().getBranchId()).isEqualTo(BRANCH_ID);
            assertThat(guardada.getValue().getCreatedDate()).isEqualTo(CREADO);
        }

        @Test
        @DisplayName("devuelve el DTO de la muestra ya editada")
        void devuelve_el_dto_de_la_muestra_editada() {
            muestraExistente();
            resolucionesBasicas();
            devuelveLoQueGuarda();

            LaboratoryTestDto dto = service.execute(comando(3, "NORMAL", null, null));

            assertThat(dto.testType().name()).isEqualTo("Uroanalisis");
            assertThat(dto.animal().name()).isEqualTo("Michi");
            assertThat(dto.prioridad()).isEqualTo("NORMAL");
            assertThat(dto.consultation()).isNull();
            assertThat(dto.processedBy()).isNull();
        }

        @Test
        @DisplayName("quitar la consulta y el procesador los borra del agregado")
        void quitar_la_consulta_y_el_procesador_los_borra() {
            muestraExistente();
            resolucionesBasicas();
            devuelveLoQueGuarda();

            service.execute(comando(1, "NORMAL", null, null));

            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue().getConsultation()).isNull();
            assertThat(guardada.getValue().getProcessedBy()).isNull();
            assertThat(guardada.getValue().getProcessedDate()).isNull();
            verifyNoInteractions(consultationQueryPort, employeeQueryPort);
        }
    }

    @Nested
    @DisplayName("Prioridad")
    class Prioridad {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        @DisplayName("sin prioridad en el comando se conserva la que ya tenia la muestra")
        void sin_prioridad_se_conserva_la_actual(String prioridad) {
            muestraExistente();
            resolucionesBasicas();
            devuelveLoQueGuarda();

            service.execute(comando(1, prioridad, null, null));

            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue().getPrioridad())
                    .isEqualTo(LaboratoryTestPriority.URGENTE);
        }

        @Test
        @DisplayName("acepta la prioridad en minusculas")
        void acepta_la_prioridad_en_minusculas() {
            muestraExistente();
            resolucionesBasicas();
            devuelveLoQueGuarda();

            service.execute(comando(1, "normal", null, null));

            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue().getPrioridad()).isEqualTo(LaboratoryTestPriority.NORMAL);
        }

        @Test
        @DisplayName("una prioridad inexistente no llega a la base")
        void una_prioridad_inexistente_no_llega_a_la_base() {
            muestraExistente();
            resolucionesBasicas();

            assertThatThrownBy(() -> service.execute(comando(1, "CRITICA", null, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No enum constant");

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Rechazos — ninguno escribe")
    class Rechazos {

        @Test
        @DisplayName("una muestra inexistente corta antes de resolver referencias")
        void una_muestra_inexistente_corta_antes() {
            when(repository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoCompleto()))
                    .isInstanceOf(LaboratoryTestNotFoundException.class)
                    .hasMessageContaining("LaboratoryTest not found: 42");

            verify(repository, never()).save(any());
            verifyNoInteractions(testTypeQueryPort, animalQueryPort, consultationQueryPort,
                    companyQueryPort, employeeQueryPort);
        }

        @Test
        @DisplayName("un tipo de examen inexistente no edita la muestra")
        void un_tipo_de_examen_inexistente_no_edita() {
            muestraExistente();
            when(testTypeQueryPort.findById(TEST_TYPE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoCompleto()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("LaboratoryTestType not found: 8");

            verify(repository, never()).save(any());
            verifyNoInteractions(animalQueryPort, consultationQueryPort, companyQueryPort,
                    employeeQueryPort);
        }

        @Test
        @DisplayName("un animal inexistente no edita la muestra")
        void un_animal_inexistente_no_edita() {
            muestraExistente();
            when(testTypeQueryPort.findById(TEST_TYPE_ID)).thenReturn(Optional.of(UROANALISIS));
            when(animalQueryPort.findById(ANIMAL_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoCompleto()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Animal not found: 12");

            verify(repository, never()).save(any());
            verifyNoInteractions(consultationQueryPort, companyQueryPort, employeeQueryPort);
        }

        @Test
        @DisplayName("una consulta inexistente no edita la muestra")
        void una_consulta_inexistente_no_edita() {
            muestraExistente();
            when(testTypeQueryPort.findById(TEST_TYPE_ID)).thenReturn(Optional.of(UROANALISIS));
            when(animalQueryPort.findById(ANIMAL_ID)).thenReturn(Optional.of(MICHI));
            when(consultationQueryPort.findById(CONSULTATION_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoCompleto()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Consultation not found: 11");

            verify(repository, never()).save(any());
            verifyNoInteractions(companyQueryPort, employeeQueryPort);
        }

        @Test
        @DisplayName("una empresa inexistente no edita la muestra")
        void una_empresa_inexistente_no_edita() {
            muestraExistente();
            when(testTypeQueryPort.findById(TEST_TYPE_ID)).thenReturn(Optional.of(UROANALISIS));
            when(animalQueryPort.findById(ANIMAL_ID)).thenReturn(Optional.of(MICHI));
            when(companyQueryPort.findById(COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comando(1, "NORMAL", null, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: 9");

            verify(repository, never()).save(any());
            verifyNoInteractions(employeeQueryPort);
        }

        @Test
        @DisplayName("un empleado inexistente no edita la muestra")
        void un_empleado_inexistente_no_edita() {
            muestraExistente();
            resolucionesBasicas();
            when(employeeQueryPort.findById(EMPLOYEE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comando(1, "NORMAL", null, EMPLOYEE_ID)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Employee not found: 3");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("una cantidad invalida la rechaza el dominio y nada se guarda")
        void una_cantidad_invalida_la_rechaza_el_dominio() {
            muestraExistente();
            resolucionesBasicas();

            assertThatThrownBy(() -> service.execute(comando(0, "NORMAL", null, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("quantity must be at least 1");

            verify(repository, never()).save(any());
        }
    }
}
