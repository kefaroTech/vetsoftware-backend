package com.vetsoftware.app.laboratorytest.application.usecase;

import static com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother.BACTERIOLOGA;
import static com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother.BRANCH_ID;
import static com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother.CLINICA;
import static com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother.CONSULTA;
import static com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother.FECHA;
import static com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother.FIRULAIS;
import static com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother.HEMOGRAMA;
import static com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother.PROCESADO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.laboratorytest.application.command.CreateLaboratoryTestCommand;
import com.vetsoftware.app.laboratorytest.application.dto.LaboratoryTestDto;
import com.vetsoftware.app.laboratorytest.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.laboratorytest.application.port.out.BranchQueryPort;
import com.vetsoftware.app.laboratorytest.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.laboratorytest.application.port.out.ConsultationQueryPort;
import com.vetsoftware.app.laboratorytest.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.laboratorytest.application.port.out.LaboratoryTestRepository;
import com.vetsoftware.app.laboratorytest.application.port.out.LaboratoryTestTypeQueryPort;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTest;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestPriority;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestStatus;
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
@DisplayName("CreateLaboratoryTestService")
class CreateLaboratoryTestServiceTest {

    private static final Long TEST_TYPE_ID = 4L;
    private static final Long ANIMAL_ID = 7L;
    private static final Long CONSULTATION_ID = 11L;
    private static final Long COMPANY_ID = 9L;
    private static final Long EMPLOYEE_ID = 3L;

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
    @Mock
    private BranchQueryPort branchQueryPort;

    @InjectMocks
    private CreateLaboratoryTestService service;

    @Captor
    private ArgumentCaptor<LaboratoryTest> guardada;

    private static CreateLaboratoryTestCommand comando(String status, String prioridad,
            Long consultationId, Long branchId, Long processedById) {
        return new CreateLaboratoryTestCommand(FECHA, TEST_TYPE_ID, 2, "Sospecha de anemia", status,
                prioridad, ANIMAL_ID, consultationId, COMPANY_ID, branchId, processedById,
                processedById == null ? null : PROCESADO);
    }

    private static CreateLaboratoryTestCommand comandoCompleto() {
        return comando("PENDING_PROCESSING", "URGENTE", CONSULTATION_ID, BRANCH_ID, EMPLOYEE_ID);
    }

    private void resolucionesBasicas() {
        when(testTypeQueryPort.findAvailableByIdAndCompanyId(TEST_TYPE_ID, COMPANY_ID))
                .thenReturn(Optional.of(HEMOGRAMA));
        when(animalQueryPort.findByIdAndCompanyId(ANIMAL_ID, COMPANY_ID))
                .thenReturn(Optional.of(FIRULAIS));
        when(companyQueryPort.findById(COMPANY_ID)).thenReturn(Optional.of(CLINICA));
    }

    private void devuelveLoQueGuarda() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Nested
    @DisplayName("Alta")
    class Alta {

        @Test
        @DisplayName("persiste la muestra con las referencias resueltas por cada puerto")
        void persiste_la_muestra_con_las_referencias_resueltas() {
            resolucionesBasicas();
            when(consultationQueryPort.findByIdAndCompanyId(CONSULTATION_ID, COMPANY_ID))
                    .thenReturn(Optional.of(CONSULTA));
            when(employeeQueryPort.findById(EMPLOYEE_ID)).thenReturn(Optional.of(BACTERIOLOGA));
            when(branchQueryPort.findActiveIdByIdAndCompanyId(BRANCH_ID, COMPANY_ID))
                    .thenReturn(Optional.of(BRANCH_ID));
            devuelveLoQueGuarda();

            service.execute(comandoCompleto());

            verify(repository).save(guardada.capture());
            LaboratoryTest muestra = guardada.getValue();
            assertThat(muestra.getDate()).isEqualTo(FECHA);
            assertThat(muestra.getTestType()).isEqualTo(HEMOGRAMA);
            assertThat(muestra.getQuantity()).isEqualTo(2);
            assertThat(muestra.getDiagnosis()).isEqualTo("Sospecha de anemia");
            assertThat(muestra.getAnimal()).isEqualTo(FIRULAIS);
            assertThat(muestra.getConsultation()).isEqualTo(CONSULTA);
            assertThat(muestra.getCompany()).isEqualTo(CLINICA);
            assertThat(muestra.getBranchId()).isEqualTo(BRANCH_ID);
            assertThat(muestra.getProcessedBy()).isEqualTo(BACTERIOLOGA);
            assertThat(muestra.getProcessedDate()).isEqualTo(PROCESADO);
            assertThat(muestra.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("devuelve el DTO de lo que quedo persistido")
        void devuelve_el_dto_de_lo_persistido() {
            resolucionesBasicas();
            when(consultationQueryPort.findByIdAndCompanyId(CONSULTATION_ID, COMPANY_ID))
                    .thenReturn(Optional.of(CONSULTA));
            when(employeeQueryPort.findById(EMPLOYEE_ID)).thenReturn(Optional.of(BACTERIOLOGA));
            when(branchQueryPort.findActiveIdByIdAndCompanyId(BRANCH_ID, COMPANY_ID))
                    .thenReturn(Optional.of(BRANCH_ID));
            devuelveLoQueGuarda();

            LaboratoryTestDto dto = service.execute(comandoCompleto());

            assertThat(dto.status()).isEqualTo("PENDING_PROCESSING");
            assertThat(dto.prioridad()).isEqualTo("URGENTE");
            assertThat(dto.animal().name()).isEqualTo("Firulais");
            assertThat(dto.processedBy().employeeCode()).isEqualTo("EMP-003");
        }

        @Test
        @DisplayName("sin consulta ni procesador no consulta esos puertos")
        void sin_consulta_ni_procesador_no_consulta_esos_puertos() {
            resolucionesBasicas();
            when(branchQueryPort.findActiveIdByIdAndCompanyId(BRANCH_ID, COMPANY_ID))
                    .thenReturn(Optional.of(BRANCH_ID));
            devuelveLoQueGuarda();

            service.execute(comando("PENDING_COLLECTION", "NORMAL", null, BRANCH_ID, null));

            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue().getConsultation()).isNull();
            assertThat(guardada.getValue().getProcessedBy()).isNull();
            verifyNoInteractions(consultationQueryPort, employeeQueryPort);
        }
    }

    @Nested
    @DisplayName("Valores por defecto del comando")
    class ValoresPorDefecto {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        @DisplayName("sin estado explicito la muestra nace pendiente de toma")
        void sin_estado_explicito_nace_pendiente_de_toma(String estado) {
            resolucionesBasicas();
            when(branchQueryPort.findActiveIdByIdAndCompanyId(BRANCH_ID, COMPANY_ID))
                    .thenReturn(Optional.of(BRANCH_ID));
            devuelveLoQueGuarda();

            service.execute(comando(estado, "NORMAL", null, BRANCH_ID, null));

            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue().getStatus())
                    .isEqualTo(LaboratoryTestStatus.PENDING_COLLECTION);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        @DisplayName("sin prioridad explicita la muestra nace normal")
        void sin_prioridad_explicita_nace_normal(String prioridad) {
            resolucionesBasicas();
            when(branchQueryPort.findActiveIdByIdAndCompanyId(BRANCH_ID, COMPANY_ID))
                    .thenReturn(Optional.of(BRANCH_ID));
            devuelveLoQueGuarda();

            service.execute(comando("PENDING_COLLECTION", prioridad, null, BRANCH_ID, null));

            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue().getPrioridad()).isEqualTo(LaboratoryTestPriority.NORMAL);
        }

        @Test
        @DisplayName("acepta estado y prioridad en minusculas")
        void acepta_estado_y_prioridad_en_minusculas() {
            resolucionesBasicas();
            when(branchQueryPort.findActiveIdByIdAndCompanyId(BRANCH_ID, COMPANY_ID))
                    .thenReturn(Optional.of(BRANCH_ID));
            devuelveLoQueGuarda();

            service.execute(comando("pending_processing", "urgente", null, BRANCH_ID, null));

            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue().getStatus())
                    .isEqualTo(LaboratoryTestStatus.PENDING_PROCESSING);
            assertThat(guardada.getValue().getPrioridad())
                    .isEqualTo(LaboratoryTestPriority.URGENTE);
        }

        @Test
        @DisplayName("un estado inexistente no llega a la base")
        void un_estado_inexistente_no_llega_a_la_base() {
            resolucionesBasicas();

            assertThatThrownBy(
                    () -> service.execute(comando("ARCHIVED", "NORMAL", null, BRANCH_ID, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No enum constant");

            verifyNoInteractions(repository, branchQueryPort);
        }

        @Test
        @DisplayName("una prioridad inexistente no llega a la base")
        void una_prioridad_inexistente_no_llega_a_la_base() {
            resolucionesBasicas();

            assertThatThrownBy(() -> service
                    .execute(comando("PENDING_COLLECTION", "CRITICA", null, BRANCH_ID, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No enum constant");

            verifyNoInteractions(repository, branchQueryPort);
        }

        @Test
        @DisplayName("un estado inicial ya avanzado lo rechaza el dominio, no el service")
        void un_estado_inicial_ya_avanzado_lo_rechaza_el_dominio() {
            resolucionesBasicas();
            when(branchQueryPort.findActiveIdByIdAndCompanyId(BRANCH_ID, COMPANY_ID))
                    .thenReturn(Optional.of(BRANCH_ID));

            assertThatThrownBy(
                    () -> service.execute(comando("COMPLETED", "NORMAL", null, BRANCH_ID, null)))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "initial status must be PENDING_COLLECTION or PENDING_PROCESSING");

            verifyNoInteractions(repository);
        }
    }

    @Nested
    @DisplayName("Sede de la muestra")
    class Sede {

        @Test
        @DisplayName("sin sede pedida usa la sede activa por defecto de la empresa")
        void sin_sede_pedida_usa_la_sede_por_defecto() {
            resolucionesBasicas();
            when(branchQueryPort.findDefaultActiveIdByCompanyId(COMPANY_ID))
                    .thenReturn(Optional.of(77L));
            devuelveLoQueGuarda();

            service.execute(comando("PENDING_COLLECTION", "NORMAL", null, null, null));

            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue().getBranchId()).isEqualTo(77L);
        }

        @Test
        @DisplayName("una sede inactiva o de otra empresa no crea la muestra")
        void una_sede_inactiva_o_ajena_no_crea_la_muestra() {
            resolucionesBasicas();
            when(branchQueryPort.findActiveIdByIdAndCompanyId(BRANCH_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service
                    .execute(comando("PENDING_COLLECTION", "NORMAL", null, BRANCH_ID, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Branch is not active or not found: 5");

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("una empresa sin ninguna sede activa no puede recibir muestras")
        void una_empresa_sin_sede_activa_no_puede_recibir_muestras() {
            resolucionesBasicas();
            when(branchQueryPort.findDefaultActiveIdByCompanyId(COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service
                    .execute(comando("PENDING_COLLECTION", "NORMAL", null, null, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company has no active branch: 9");

            verifyNoInteractions(repository);
        }
    }

    @Nested
    @DisplayName("Referencias inexistentes — ninguna escribe")
    class ReferenciasInexistentes {

        @Test
        @DisplayName("un tipo de examen inexistente corta antes de mirar el animal")
        void un_tipo_de_examen_inexistente_corta_antes() {
            when(testTypeQueryPort.findAvailableByIdAndCompanyId(TEST_TYPE_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoCompleto()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("LaboratoryTestType not found: 4");

            verifyNoInteractions(repository, animalQueryPort, consultationQueryPort,
                    companyQueryPort, employeeQueryPort, branchQueryPort);
        }

        @Test
        @DisplayName("un animal inexistente no crea la muestra")
        void un_animal_inexistente_no_crea_la_muestra() {
            when(testTypeQueryPort.findAvailableByIdAndCompanyId(TEST_TYPE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(HEMOGRAMA));
            when(animalQueryPort.findByIdAndCompanyId(ANIMAL_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoCompleto()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Animal not found: 7");

            verifyNoInteractions(repository, consultationQueryPort, companyQueryPort,
                    employeeQueryPort, branchQueryPort);
        }

        @Test
        @DisplayName("una consulta inexistente no crea la muestra")
        void una_consulta_inexistente_no_crea_la_muestra() {
            when(testTypeQueryPort.findAvailableByIdAndCompanyId(TEST_TYPE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(HEMOGRAMA));
            when(animalQueryPort.findByIdAndCompanyId(ANIMAL_ID, COMPANY_ID))
                    .thenReturn(Optional.of(FIRULAIS));
            when(consultationQueryPort.findByIdAndCompanyId(CONSULTATION_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoCompleto()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Consultation not found: 11");

            verifyNoInteractions(repository, companyQueryPort, employeeQueryPort, branchQueryPort);
        }

        @Test
        @DisplayName("una empresa inexistente no crea la muestra")
        void una_empresa_inexistente_no_crea_la_muestra() {
            when(testTypeQueryPort.findAvailableByIdAndCompanyId(TEST_TYPE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(HEMOGRAMA));
            when(animalQueryPort.findByIdAndCompanyId(ANIMAL_ID, COMPANY_ID))
                    .thenReturn(Optional.of(FIRULAIS));
            when(companyQueryPort.findById(COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service
                    .execute(comando("PENDING_COLLECTION", "NORMAL", null, BRANCH_ID, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: 9");

            verifyNoInteractions(repository, employeeQueryPort, branchQueryPort);
        }

        @Test
        @DisplayName("un empleado inexistente no crea la muestra")
        void un_empleado_inexistente_no_crea_la_muestra() {
            resolucionesBasicas();
            when(employeeQueryPort.findById(EMPLOYEE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service
                    .execute(comando("PENDING_COLLECTION", "NORMAL", null, BRANCH_ID, EMPLOYEE_ID)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Employee not found: 3");

            verifyNoInteractions(repository, branchQueryPort);
        }
    }
}
