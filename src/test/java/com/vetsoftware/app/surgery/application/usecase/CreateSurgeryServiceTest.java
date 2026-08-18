package com.vetsoftware.app.surgery.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.surgery.application.command.CreateSurgeryCommand;
import com.vetsoftware.app.surgery.application.dto.SurgeryDto;
import com.vetsoftware.app.surgery.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.surgery.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.surgery.application.port.out.ConsultationQueryPort;
import com.vetsoftware.app.surgery.application.port.out.SurgeryRepository;
import com.vetsoftware.app.surgery.application.port.out.SurgeryTypeQueryPort;
import com.vetsoftware.app.surgery.domain.Surgery;
import com.vetsoftware.app.surgery.testsupport.SurgeryMother;
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
@DisplayName("CreateSurgeryService")
class CreateSurgeryServiceTest {

    @Mock
    private SurgeryRepository repository;
    @Mock
    private SurgeryTypeQueryPort surgeryTypeQueryPort;
    @Mock
    private AnimalQueryPort animalQueryPort;
    @Mock
    private ConsultationQueryPort consultationQueryPort;
    @Mock
    private CompanyQueryPort companyQueryPort;

    @InjectMocks
    private CreateSurgeryService service;

    @Captor
    private ArgumentCaptor<Surgery> surgeryCaptor;

    private void tipoAnimalYEmpresaExisten() {
        when(surgeryTypeQueryPort.findAvailableByIdAndCompanyId(SurgeryMother.SURGERY_TYPE_ID,
                SurgeryMother.COMPANY_ID))
                .thenReturn(Optional.of(SurgeryMother.OVARIOHISTERECTOMIA));
        when(animalQueryPort.findByIdAndCompanyId(SurgeryMother.ANIMAL_ID,
                SurgeryMother.COMPANY_ID)).thenReturn(Optional.of(SurgeryMother.FIRULAIS));
        when(companyQueryPort.findById(SurgeryMother.COMPANY_ID))
                .thenReturn(Optional.of(SurgeryMother.CLINICA));
    }

    @Nested
    @DisplayName("creacion con consulta asociada")
    class ConConsulta {

        @Test
        @DisplayName("persiste la cirugia con las referencias resueltas por los puertos")
        void persiste_la_cirugia_con_las_referencias_resueltas() {
            tipoAnimalYEmpresaExisten();
            when(consultationQueryPort.findByIdAndCompanyId(SurgeryMother.CONSULTATION_ID,
                    SurgeryMother.COMPANY_ID))
                    .thenReturn(Optional.of(SurgeryMother.CONSULTA_PREVIA));
            when(repository.save(any())).thenReturn(SurgeryMother.cirugiaValida());

            service.execute(SurgeryMother.comandoCrear());

            verify(repository).save(surgeryCaptor.capture());
            Surgery guardada = surgeryCaptor.getValue();
            // Lo que importa no es que se llamara a save, sino que se guardara ESTO: las
            // refs
            // tienen que venir de los puertos, no de los ids del comando.
            assertThat(guardada.getSurgeryType()).isEqualTo(SurgeryMother.OVARIOHISTERECTOMIA);
            assertThat(guardada.getAnimal()).isEqualTo(SurgeryMother.FIRULAIS);
            assertThat(guardada.getConsultation()).isEqualTo(SurgeryMother.CONSULTA_PREVIA);
            assertThat(guardada.getCompany()).isEqualTo(SurgeryMother.CLINICA);
            assertThat(guardada.getId()).isNull();
        }

        @Test
        @DisplayName("devuelve el DTO de la cirugia ya persistida, con su id")
        void devuelve_el_dto_de_la_cirugia_ya_persistida() {
            tipoAnimalYEmpresaExisten();
            when(consultationQueryPort.findByIdAndCompanyId(SurgeryMother.CONSULTATION_ID,
                    SurgeryMother.COMPANY_ID))
                    .thenReturn(Optional.of(SurgeryMother.CONSULTA_PREVIA));
            when(repository.save(any())).thenReturn(SurgeryMother.cirugiaValida());

            SurgeryDto dto = service.execute(SurgeryMother.comandoCrear());

            assertThat(dto.id()).isEqualTo(SurgeryMother.SURGERY_ID);
            assertThat(dto.description()).isEqualTo("Ovariohisterectomia electiva");
        }
    }

    @Nested
    @DisplayName("creacion sin consulta asociada")
    class SinConsulta {

        @Test
        @DisplayName("no consulta el puerto de consultas")
        void no_consulta_el_puerto_de_consultas() {
            tipoAnimalYEmpresaExisten();
            when(repository.save(any())).thenReturn(SurgeryMother.cirugiaSinConsulta());

            service.execute(SurgeryMother.comandoCrearSinConsulta());

            verifyNoInteractions(consultationQueryPort);
        }

        @Test
        @DisplayName("persiste la cirugia con la consulta en null")
        void persiste_la_cirugia_con_la_consulta_en_null() {
            tipoAnimalYEmpresaExisten();
            when(repository.save(any())).thenReturn(SurgeryMother.cirugiaSinConsulta());

            service.execute(SurgeryMother.comandoCrearSinConsulta());

            verify(repository).save(surgeryCaptor.capture());
            assertThat(surgeryCaptor.getValue().getConsultation()).isNull();
        }
    }

    @Nested
    @DisplayName("referencias que no existen")
    class ReferenciasInexistentes {

        @Test
        @DisplayName("tipo de cirugia inexistente: no consulta los puertos siguientes ni persiste")
        void tipo_de_cirugia_inexistente() {
            when(surgeryTypeQueryPort.findAvailableByIdAndCompanyId(SurgeryMother.SURGERY_TYPE_ID,
                    SurgeryMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(SurgeryMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "SurgeryType not found: " + SurgeryMother.SURGERY_TYPE_ID);

            verifyNoInteractions(animalQueryPort, consultationQueryPort, companyQueryPort,
                    repository);
        }

        @Test
        @DisplayName("animal inexistente")
        void animal_inexistente() {
            when(surgeryTypeQueryPort.findAvailableByIdAndCompanyId(SurgeryMother.SURGERY_TYPE_ID,
                    SurgeryMother.COMPANY_ID))
                    .thenReturn(Optional.of(SurgeryMother.OVARIOHISTERECTOMIA));
            when(animalQueryPort.findByIdAndCompanyId(SurgeryMother.ANIMAL_ID,
                    SurgeryMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(SurgeryMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Animal not found: " + SurgeryMother.ANIMAL_ID);

            verifyNoInteractions(consultationQueryPort, companyQueryPort, repository);
        }

        @Test
        @DisplayName("consulta inexistente cuando el comando trae consultationId")
        void consulta_inexistente() {
            when(surgeryTypeQueryPort.findAvailableByIdAndCompanyId(SurgeryMother.SURGERY_TYPE_ID,
                    SurgeryMother.COMPANY_ID))
                    .thenReturn(Optional.of(SurgeryMother.OVARIOHISTERECTOMIA));
            when(animalQueryPort.findByIdAndCompanyId(SurgeryMother.ANIMAL_ID,
                    SurgeryMother.COMPANY_ID)).thenReturn(Optional.of(SurgeryMother.FIRULAIS));
            when(consultationQueryPort.findByIdAndCompanyId(SurgeryMother.CONSULTATION_ID,
                    SurgeryMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(SurgeryMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Consultation not found: " + SurgeryMother.CONSULTATION_ID);

            verifyNoInteractions(companyQueryPort, repository);
        }

        @Test
        @DisplayName("empresa inexistente")
        void empresa_inexistente() {
            when(surgeryTypeQueryPort.findAvailableByIdAndCompanyId(SurgeryMother.SURGERY_TYPE_ID,
                    SurgeryMother.COMPANY_ID))
                    .thenReturn(Optional.of(SurgeryMother.OVARIOHISTERECTOMIA));
            when(animalQueryPort.findByIdAndCompanyId(SurgeryMother.ANIMAL_ID,
                    SurgeryMother.COMPANY_ID)).thenReturn(Optional.of(SurgeryMother.FIRULAIS));
            when(consultationQueryPort.findByIdAndCompanyId(SurgeryMother.CONSULTATION_ID,
                    SurgeryMother.COMPANY_ID))
                    .thenReturn(Optional.of(SurgeryMother.CONSULTA_PREVIA));
            when(companyQueryPort.findById(SurgeryMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(SurgeryMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: " + SurgeryMother.COMPANY_ID);

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("invariantes del dominio propagadas")
    class InvariantesDelDominio {

        @Test
        @DisplayName("una descripcion vacia no llega a persistirse")
        void una_descripcion_vacia_no_llega_a_persistirse() {
            tipoAnimalYEmpresaExisten();
            when(consultationQueryPort.findByIdAndCompanyId(SurgeryMother.CONSULTATION_ID,
                    SurgeryMother.COMPANY_ID))
                    .thenReturn(Optional.of(SurgeryMother.CONSULTA_PREVIA));
            CreateSurgeryCommand comandoInvalido = new CreateSurgeryCommand(SurgeryMother.FECHA,
                    SurgeryMother.SURGERY_TYPE_ID, "  ", null, null, null, SurgeryMother.ANIMAL_ID,
                    SurgeryMother.CONSULTATION_ID, SurgeryMother.COMPANY_ID);

            assertThatThrownBy(() -> service.execute(comandoInvalido))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("description is required");

            verify(repository, never()).save(any());
        }
    }
}
