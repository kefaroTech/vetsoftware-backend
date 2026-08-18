package com.vetsoftware.app.diagnosticimaging.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.diagnosticimaging.application.command.UpdateDiagnosticImagingCommand;
import com.vetsoftware.app.diagnosticimaging.application.dto.DiagnosticImagingDto;
import com.vetsoftware.app.diagnosticimaging.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.diagnosticimaging.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.diagnosticimaging.application.port.out.ConsultationQueryPort;
import com.vetsoftware.app.diagnosticimaging.application.port.out.DiagnosticImagingRepository;
import com.vetsoftware.app.diagnosticimaging.application.port.out.DiagnosticImagingTypeQueryPort;
import com.vetsoftware.app.diagnosticimaging.domain.DiagnosticImagingNotFoundException;
import com.vetsoftware.app.diagnosticimaging.testsupport.DiagnosticImagingMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateDiagnosticImagingService")
class UpdateDiagnosticImagingServiceTest {

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
    private UpdateDiagnosticImagingService service;

    private void laImagenExiste() {
        when(repository.findByIdAndCompanyId(DiagnosticImagingMother.IMAGING_ID,
                DiagnosticImagingMother.COMPANY_ID))
                .thenReturn(Optional.of(DiagnosticImagingMother.persistida()));
    }

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("actualiza la imagen con las referencias resueltas por los puertos")
        void actualiza_la_imagen_con_las_referencias_resueltas() {
            laImagenExiste();
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
            when(repository.save(any())).thenReturn(DiagnosticImagingMother.persistida());

            DiagnosticImagingDto dto = service.execute(DiagnosticImagingMother.comandoActualizar());

            assertThat(dto.id()).isEqualTo(DiagnosticImagingMother.IMAGING_ID);
        }

        @Test
        @DisplayName("una consulta ausente en el comando no consulta el puerto de consulta")
        void consulta_ausente_no_consulta_el_puerto() {
            laImagenExiste();
            when(diagnosticImagingTypeQueryPort.findAvailableByIdAndCompanyId(
                    DiagnosticImagingMother.TYPE_ID, DiagnosticImagingMother.COMPANY_ID))
                    .thenReturn(Optional.of(DiagnosticImagingMother.TIPO));
            when(animalQueryPort.findByIdAndCompanyId(DiagnosticImagingMother.ANIMAL_ID,
                    DiagnosticImagingMother.COMPANY_ID))
                    .thenReturn(Optional.of(DiagnosticImagingMother.MASCOTA));
            when(companyQueryPort.findById(DiagnosticImagingMother.COMPANY_ID))
                    .thenReturn(Optional.of(DiagnosticImagingMother.EMPRESA));
            when(repository.save(any())).thenReturn(DiagnosticImagingMother.sinConsulta());
            UpdateDiagnosticImagingCommand comando = new UpdateDiagnosticImagingCommand(
                    DiagnosticImagingMother.IMAGING_ID, DiagnosticImagingMother.FECHA,
                    DiagnosticImagingMother.TYPE_ID, "Cojera pata trasera", "Radiografia de cadera",
                    "Displasia leve", "Control en 30 dias", DiagnosticImagingMother.ANIMAL_ID, null,
                    DiagnosticImagingMother.COMPANY_ID);

            service.execute(comando);

            verifyNoInteractions(consultationQueryPort);
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("una imagen que no existe lanza DiagnosticImagingNotFoundException")
        void imagen_inexistente() {
            when(repository.findByIdAndCompanyId(DiagnosticImagingMother.IMAGING_ID,
                    DiagnosticImagingMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(DiagnosticImagingMother.comandoActualizar()))
                    .isInstanceOf(DiagnosticImagingNotFoundException.class)
                    .hasMessageContaining(String.valueOf(DiagnosticImagingMother.IMAGING_ID));

            verifyNoInteractions(diagnosticImagingTypeQueryPort, animalQueryPort,
                    consultationQueryPort, companyQueryPort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("tipo de imagen inexistente")
        void tipo_inexistente() {
            laImagenExiste();
            when(diagnosticImagingTypeQueryPort.findAvailableByIdAndCompanyId(
                    DiagnosticImagingMother.TYPE_ID, DiagnosticImagingMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(DiagnosticImagingMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "DiagnosticImagingType not found: " + DiagnosticImagingMother.TYPE_ID);

            verifyNoInteractions(animalQueryPort, consultationQueryPort, companyQueryPort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("animal inexistente")
        void animal_inexistente() {
            laImagenExiste();
            when(diagnosticImagingTypeQueryPort.findAvailableByIdAndCompanyId(
                    DiagnosticImagingMother.TYPE_ID, DiagnosticImagingMother.COMPANY_ID))
                    .thenReturn(Optional.of(DiagnosticImagingMother.TIPO));
            when(animalQueryPort.findByIdAndCompanyId(DiagnosticImagingMother.ANIMAL_ID,
                    DiagnosticImagingMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(DiagnosticImagingMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Animal not found: " + DiagnosticImagingMother.ANIMAL_ID);

            verifyNoInteractions(consultationQueryPort, companyQueryPort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("consulta inexistente")
        void consulta_inexistente() {
            laImagenExiste();
            when(diagnosticImagingTypeQueryPort.findAvailableByIdAndCompanyId(
                    DiagnosticImagingMother.TYPE_ID, DiagnosticImagingMother.COMPANY_ID))
                    .thenReturn(Optional.of(DiagnosticImagingMother.TIPO));
            when(animalQueryPort.findByIdAndCompanyId(DiagnosticImagingMother.ANIMAL_ID,
                    DiagnosticImagingMother.COMPANY_ID))
                    .thenReturn(Optional.of(DiagnosticImagingMother.MASCOTA));
            when(consultationQueryPort.findByIdAndCompanyId(DiagnosticImagingMother.CONSULTATION_ID,
                    DiagnosticImagingMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(DiagnosticImagingMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Consultation not found: " + DiagnosticImagingMother.CONSULTATION_ID);

            verifyNoInteractions(companyQueryPort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("empresa inexistente")
        void empresa_inexistente() {
            laImagenExiste();
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

            assertThatThrownBy(() -> service.execute(DiagnosticImagingMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Company not found: " + DiagnosticImagingMother.COMPANY_ID);

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("aislamiento entre empresas")
    class Tenancy {

        /**
         * El {@code @authz.isMyCompany(#command.companyId)} del puerto solo prueba que
         * el actor declara SU empresa. Sin acotar la carga, el {@code update} posterior
         * reescribiria el company del estudio ajeno —diagnostico incluido—:
         * apropiacion, no rechazo.
         */
        @Test
        @DisplayName("un estudio de otra empresa es un 404 y no se guarda nada")
        void estudio_de_otra_empresa_no_se_apropia() {
            when(repository.findByIdAndCompanyId(DiagnosticImagingMother.IMAGING_ID,
                    DiagnosticImagingMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(DiagnosticImagingMother.comandoActualizar()))
                    .isInstanceOf(DiagnosticImagingNotFoundException.class)
                    .hasMessageContaining(String.valueOf(DiagnosticImagingMother.IMAGING_ID));

            verify(repository, never()).save(any());
            verify(repository, never()).findById(any());
            verifyNoInteractions(diagnosticImagingTypeQueryPort, animalQueryPort,
                    consultationQueryPort, companyQueryPort);
        }

        /**
         * La cuarta forma del defecto, y la que sobrevive a la de arriba: aqui el
         * estudio <b>si es mio</b> y lo ajeno es la <b>referencia</b>. Nadie puede
         * robarme la fila, pero si se podia colgarla del animal de otro tenant: una
         * imagen con su diagnostico en la historia clinica de la vecina. La referencia
         * se resolvia con {@code findById(animalId)}, sin empresa; el {@code verify} de
         * la empresa es la asercion que lo caza.
         */
        @Test
        @DisplayName("no puede reapuntar el estudio propio al animal de otra empresa")
        void no_puede_reapuntar_al_animal_de_otra_empresa() {
            laImagenExiste();
            when(diagnosticImagingTypeQueryPort.findAvailableByIdAndCompanyId(
                    DiagnosticImagingMother.TYPE_ID, DiagnosticImagingMother.COMPANY_ID))
                    .thenReturn(Optional.of(DiagnosticImagingMother.TIPO));
            when(animalQueryPort.findByIdAndCompanyId(DiagnosticImagingMother.ANIMAL_ID,
                    DiagnosticImagingMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(DiagnosticImagingMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Animal not found: " + DiagnosticImagingMother.ANIMAL_ID);

            verify(animalQueryPort).findByIdAndCompanyId(DiagnosticImagingMother.ANIMAL_ID,
                    DiagnosticImagingMother.COMPANY_ID);
            verify(repository, never()).save(any());
            verifyNoInteractions(consultationQueryPort, companyQueryPort);
        }

        /**
         * El catalogo de tipos mezcla filas generales con las privadas de cada empresa,
         * asi que la variante acotada es «general O mia»: el tipo general sigue
         * sirviendo y el privado del vecino deja de servir.
         */
        @Test
        @DisplayName("no puede reapuntar el estudio propio a un tipo privado de otra empresa")
        void no_puede_reapuntar_a_un_tipo_de_otra_empresa() {
            laImagenExiste();
            when(diagnosticImagingTypeQueryPort.findAvailableByIdAndCompanyId(
                    DiagnosticImagingMother.TYPE_ID, DiagnosticImagingMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(DiagnosticImagingMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "DiagnosticImagingType not found: " + DiagnosticImagingMother.TYPE_ID);

            verify(diagnosticImagingTypeQueryPort).findAvailableByIdAndCompanyId(
                    DiagnosticImagingMother.TYPE_ID, DiagnosticImagingMother.COMPANY_ID);
            verify(repository, never()).save(any());
            verifyNoInteractions(animalQueryPort, consultationQueryPort, companyQueryPort);
        }
    }
}
