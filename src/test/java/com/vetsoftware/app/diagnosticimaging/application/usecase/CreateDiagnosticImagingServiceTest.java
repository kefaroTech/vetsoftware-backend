package com.vetsoftware.app.diagnosticimaging.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.diagnosticimaging.application.command.CreateDiagnosticImagingCommand;
import com.vetsoftware.app.diagnosticimaging.application.dto.DiagnosticImagingDto;
import com.vetsoftware.app.diagnosticimaging.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.diagnosticimaging.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.diagnosticimaging.application.port.out.ConsultationQueryPort;
import com.vetsoftware.app.diagnosticimaging.application.port.out.DiagnosticImagingRepository;
import com.vetsoftware.app.diagnosticimaging.application.port.out.DiagnosticImagingTypeQueryPort;
import com.vetsoftware.app.diagnosticimaging.domain.DiagnosticImaging;
import com.vetsoftware.app.diagnosticimaging.testsupport.DiagnosticImagingMother;
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
@DisplayName("CreateDiagnosticImagingService")
class CreateDiagnosticImagingServiceTest {

    @Mock
    private DiagnosticImagingRepository repository;
    @Mock
    private DiagnosticImagingTypeQueryPort diagnosticImagingTypeQueryPort;
    @Mock
    private AnimalQueryPort animalQueryPort;
    @Mock
    private ConsultationQueryPort consultationQueryPort;
    @Mock
    private CompanyQueryPort companyQueryPort;

    @InjectMocks
    private CreateDiagnosticImagingService service;

    @Captor
    private ArgumentCaptor<DiagnosticImaging> captor;

    private void todasLasReferenciasExisten() {
        when(diagnosticImagingTypeQueryPort.findAvailableByIdAndCompanyId(
                DiagnosticImagingMother.TYPE_ID, DiagnosticImagingMother.COMPANY_ID))
                .thenReturn(Optional.of(DiagnosticImagingMother.TIPO));
        when(animalQueryPort.findByIdAndCompanyId(DiagnosticImagingMother.ANIMAL_ID,
                DiagnosticImagingMother.COMPANY_ID))
                .thenReturn(Optional.of(DiagnosticImagingMother.MASCOTA));
        when(consultationQueryPort.findByIdAndCompanyId(DiagnosticImagingMother.CONSULTATION_ID,
                DiagnosticImagingMother.COMPANY_ID))
                .thenReturn(Optional.of(DiagnosticImagingMother.CONSULTA));
        when(companyQueryPort.findById(DiagnosticImagingMother.COMPANY_ID))
                .thenReturn(Optional.of(DiagnosticImagingMother.EMPRESA));
    }

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("persiste la imagen con las referencias resueltas por los puertos")
        void persiste_la_imagen_con_las_referencias_resueltas() {
            todasLasReferenciasExisten();
            when(repository.save(any())).thenReturn(DiagnosticImagingMother.persistida());

            service.execute(DiagnosticImagingMother.comandoCrear());

            verify(repository).save(captor.capture());
            DiagnosticImaging guardada = captor.getValue();
            assertThat(guardada.getDiagnosticImagingType()).isEqualTo(DiagnosticImagingMother.TIPO);
            assertThat(guardada.getAnimal()).isEqualTo(DiagnosticImagingMother.MASCOTA);
            assertThat(guardada.getConsultation()).isEqualTo(DiagnosticImagingMother.CONSULTA);
            assertThat(guardada.getCompany()).isEqualTo(DiagnosticImagingMother.EMPRESA);
            assertThat(guardada.getId()).isNull();
        }

        @Test
        @DisplayName("devuelve el DTO de la imagen ya persistida")
        void devuelve_el_dto_de_la_imagen_persistida() {
            todasLasReferenciasExisten();
            when(repository.save(any())).thenReturn(DiagnosticImagingMother.persistida());

            DiagnosticImagingDto dto = service.execute(DiagnosticImagingMother.comandoCrear());

            assertThat(dto.id()).isEqualTo(DiagnosticImagingMother.IMAGING_ID);
            assertThat(dto.diagnosis()).isEqualTo("Displasia leve");
        }

        @Test
        @DisplayName("una consulta ausente en el comando no consulta el puerto de consulta")
        void consulta_ausente_no_consulta_el_puerto() {
            when(diagnosticImagingTypeQueryPort.findAvailableByIdAndCompanyId(
                    DiagnosticImagingMother.TYPE_ID, DiagnosticImagingMother.COMPANY_ID))
                    .thenReturn(Optional.of(DiagnosticImagingMother.TIPO));
            when(animalQueryPort.findByIdAndCompanyId(DiagnosticImagingMother.ANIMAL_ID,
                    DiagnosticImagingMother.COMPANY_ID))
                    .thenReturn(Optional.of(DiagnosticImagingMother.MASCOTA));
            when(companyQueryPort.findById(DiagnosticImagingMother.COMPANY_ID))
                    .thenReturn(Optional.of(DiagnosticImagingMother.EMPRESA));
            when(repository.save(any())).thenReturn(DiagnosticImagingMother.sinConsulta());
            CreateDiagnosticImagingCommand comando = new CreateDiagnosticImagingCommand(
                    DiagnosticImagingMother.FECHA, DiagnosticImagingMother.TYPE_ID,
                    "Cojera pata trasera", "Radiografia de cadera", "Displasia leve",
                    "Control en 30 dias", DiagnosticImagingMother.ANIMAL_ID, null,
                    DiagnosticImagingMother.COMPANY_ID);

            service.execute(comando);

            verifyNoInteractions(consultationQueryPort);
        }
    }

    @Nested
    @DisplayName("referencias que no existen")
    class ReferenciasInexistentes {

        @Test
        @DisplayName("tipo de imagen inexistente: no consulta los puertos siguientes ni persiste")
        void tipo_inexistente() {
            when(diagnosticImagingTypeQueryPort.findAvailableByIdAndCompanyId(
                    DiagnosticImagingMother.TYPE_ID, DiagnosticImagingMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(DiagnosticImagingMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "DiagnosticImagingType not found: " + DiagnosticImagingMother.TYPE_ID);

            verifyNoInteractions(animalQueryPort, consultationQueryPort, companyQueryPort,
                    repository);
        }

        @Test
        @DisplayName("animal inexistente")
        void animal_inexistente() {
            when(diagnosticImagingTypeQueryPort.findAvailableByIdAndCompanyId(
                    DiagnosticImagingMother.TYPE_ID, DiagnosticImagingMother.COMPANY_ID))
                    .thenReturn(Optional.of(DiagnosticImagingMother.TIPO));
            when(animalQueryPort.findByIdAndCompanyId(DiagnosticImagingMother.ANIMAL_ID,
                    DiagnosticImagingMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(DiagnosticImagingMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Animal not found: " + DiagnosticImagingMother.ANIMAL_ID);

            verifyNoInteractions(consultationQueryPort, companyQueryPort, repository);
        }

        @Test
        @DisplayName("consulta inexistente")
        void consulta_inexistente() {
            when(diagnosticImagingTypeQueryPort.findAvailableByIdAndCompanyId(
                    DiagnosticImagingMother.TYPE_ID, DiagnosticImagingMother.COMPANY_ID))
                    .thenReturn(Optional.of(DiagnosticImagingMother.TIPO));
            when(animalQueryPort.findByIdAndCompanyId(DiagnosticImagingMother.ANIMAL_ID,
                    DiagnosticImagingMother.COMPANY_ID))
                    .thenReturn(Optional.of(DiagnosticImagingMother.MASCOTA));
            when(consultationQueryPort.findByIdAndCompanyId(DiagnosticImagingMother.CONSULTATION_ID,
                    DiagnosticImagingMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(DiagnosticImagingMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Consultation not found: " + DiagnosticImagingMother.CONSULTATION_ID);

            verifyNoInteractions(companyQueryPort, repository);
        }

        @Test
        @DisplayName("empresa inexistente")
        void empresa_inexistente() {
            when(diagnosticImagingTypeQueryPort.findAvailableByIdAndCompanyId(
                    DiagnosticImagingMother.TYPE_ID, DiagnosticImagingMother.COMPANY_ID))
                    .thenReturn(Optional.of(DiagnosticImagingMother.TIPO));
            when(animalQueryPort.findByIdAndCompanyId(DiagnosticImagingMother.ANIMAL_ID,
                    DiagnosticImagingMother.COMPANY_ID))
                    .thenReturn(Optional.of(DiagnosticImagingMother.MASCOTA));
            when(consultationQueryPort.findByIdAndCompanyId(DiagnosticImagingMother.CONSULTATION_ID,
                    DiagnosticImagingMother.COMPANY_ID))
                    .thenReturn(Optional.of(DiagnosticImagingMother.CONSULTA));
            when(companyQueryPort.findById(DiagnosticImagingMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(DiagnosticImagingMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Company not found: " + DiagnosticImagingMother.COMPANY_ID);

            verifyNoInteractions(repository);
        }
    }
}
