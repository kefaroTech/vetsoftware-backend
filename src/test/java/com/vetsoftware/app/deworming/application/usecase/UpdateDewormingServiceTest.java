package com.vetsoftware.app.deworming.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.deworming.application.command.UpdateDewormingCommand;
import com.vetsoftware.app.deworming.application.dto.DewormingDto;
import com.vetsoftware.app.deworming.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.deworming.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.deworming.application.port.out.ConsultationQueryPort;
import com.vetsoftware.app.deworming.application.port.out.DewormingRepository;
import com.vetsoftware.app.deworming.domain.Deworming;
import com.vetsoftware.app.deworming.domain.DewormingNotFoundException;
import com.vetsoftware.app.deworming.domain.DewormingType;
import com.vetsoftware.app.deworming.testsupport.DewormingMother;
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
@DisplayName("UpdateDewormingService")
class UpdateDewormingServiceTest {

    @Mock
    private DewormingRepository repository;
    @Mock
    private AnimalQueryPort animalQueryPort;
    @Mock
    private ConsultationQueryPort consultationQueryPort;
    @Mock
    private CompanyQueryPort companyQueryPort;

    @InjectMocks
    private UpdateDewormingService service;

    @Captor
    private ArgumentCaptor<Deworming> capturador;

    /**
     * El comando de la Mother lleva {@code COMPANY_ID}, asi que el servicio carga
     * por el finder acotado: cargar por id a secas era la apropiacion que esta
     * campaña cierra.
     */
    private void desparasitacionExistente() {
        when(repository.findByIdAndCompanyId(DewormingMother.DEWORMING_ID,
                DewormingMother.COMPANY_ID))
                .thenReturn(Optional.of(DewormingMother.desparasitacionValida()));
    }

    @Nested
    @DisplayName("actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("actualiza la desparasitacion con las referencias resueltas por los puertos")
        void actualiza_la_desparasitacion_con_las_referencias_resueltas() {
            desparasitacionExistente();
            when(animalQueryPort.findByIdAndCompanyId(DewormingMother.MICHI.id(),
                    DewormingMother.COMPANY_ID)).thenReturn(Optional.of(DewormingMother.MICHI));
            when(consultationQueryPort.findByIdAndCompanyId(DewormingMother.CONSULTATION_ID,
                    DewormingMother.COMPANY_ID)).thenReturn(Optional.of(DewormingMother.CONSULTA));
            when(companyQueryPort.findById(DewormingMother.COMPANY_ID))
                    .thenReturn(Optional.of(DewormingMother.CLINICA));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(DewormingMother.comandoActualizar());

            verify(repository).save(capturador.capture());
            Deworming actualizada = capturador.getValue();
            assertThat(actualizada.getAnimal()).isEqualTo(DewormingMother.MICHI);
            assertThat(actualizada.getProduct()).isEqualTo("Bravecto");
            assertThat(actualizada.getType()).isEqualTo(DewormingType.MIX);
            assertThat(actualizada.getId()).isEqualTo(DewormingMother.DEWORMING_ID);
        }

        @Test
        @DisplayName("con consultationId null no consulta el puerto de consultas")
        void con_consultation_id_null_no_consulta_el_puerto() {
            desparasitacionExistente();
            when(animalQueryPort.findByIdAndCompanyId(DewormingMother.MICHI.id(),
                    DewormingMother.COMPANY_ID)).thenReturn(Optional.of(DewormingMother.MICHI));
            when(companyQueryPort.findById(DewormingMother.COMPANY_ID))
                    .thenReturn(Optional.of(DewormingMother.CLINICA));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(DewormingMother.comandoActualizarSinConsulta());

            verifyNoInteractions(consultationQueryPort);
            verify(repository).save(capturador.capture());
            assertThat(capturador.getValue().getConsultation()).isNull();
        }

        @Test
        @DisplayName("devuelve el DTO actualizado")
        void devuelve_el_dto_actualizado() {
            desparasitacionExistente();
            when(animalQueryPort.findByIdAndCompanyId(DewormingMother.MICHI.id(),
                    DewormingMother.COMPANY_ID)).thenReturn(Optional.of(DewormingMother.MICHI));
            when(consultationQueryPort.findByIdAndCompanyId(DewormingMother.CONSULTATION_ID,
                    DewormingMother.COMPANY_ID)).thenReturn(Optional.of(DewormingMother.CONSULTA));
            when(companyQueryPort.findById(DewormingMother.COMPANY_ID))
                    .thenReturn(Optional.of(DewormingMother.CLINICA));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DewormingDto dto = service.execute(DewormingMother.comandoActualizar());

            assertThat(dto.product()).isEqualTo("Bravecto");
            assertThat(dto.id()).isEqualTo(DewormingMother.DEWORMING_ID);
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("desparasitacion inexistente: no consulta ningun otro puerto ni persiste")
        void desparasitacion_inexistente() {
            when(repository.findByIdAndCompanyId(DewormingMother.DEWORMING_ID,
                    DewormingMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(DewormingMother.comandoActualizar()))
                    .isInstanceOf(DewormingNotFoundException.class)
                    .hasMessageContaining("Deworming not found: " + DewormingMother.DEWORMING_ID);

            verifyNoInteractions(animalQueryPort, consultationQueryPort, companyQueryPort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("animal inexistente")
        void animal_inexistente() {
            desparasitacionExistente();
            when(animalQueryPort.findByIdAndCompanyId(DewormingMother.MICHI.id(),
                    DewormingMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(DewormingMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Animal not found: " + DewormingMother.MICHI.id());

            verifyNoInteractions(consultationQueryPort, companyQueryPort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("consulta inexistente")
        void consulta_inexistente() {
            desparasitacionExistente();
            when(animalQueryPort.findByIdAndCompanyId(DewormingMother.MICHI.id(),
                    DewormingMother.COMPANY_ID)).thenReturn(Optional.of(DewormingMother.MICHI));
            when(consultationQueryPort.findByIdAndCompanyId(DewormingMother.CONSULTATION_ID,
                    DewormingMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(DewormingMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Consultation not found: " + DewormingMother.CONSULTATION_ID);

            verifyNoInteractions(companyQueryPort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("empresa inexistente")
        void empresa_inexistente() {
            desparasitacionExistente();
            when(animalQueryPort.findByIdAndCompanyId(DewormingMother.MICHI.id(),
                    DewormingMother.COMPANY_ID)).thenReturn(Optional.of(DewormingMother.MICHI));
            when(consultationQueryPort.findByIdAndCompanyId(DewormingMother.CONSULTATION_ID,
                    DewormingMother.COMPANY_ID)).thenReturn(Optional.of(DewormingMother.CONSULTA));
            when(companyQueryPort.findById(DewormingMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(DewormingMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: " + DewormingMother.COMPANY_ID);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("un producto invalido no llega a persistirse")
        void un_producto_invalido_no_llega_a_persistirse() {
            desparasitacionExistente();
            when(animalQueryPort.findByIdAndCompanyId(DewormingMother.MICHI.id(),
                    DewormingMother.COMPANY_ID)).thenReturn(Optional.of(DewormingMother.MICHI));
            when(consultationQueryPort.findByIdAndCompanyId(DewormingMother.CONSULTATION_ID,
                    DewormingMother.COMPANY_ID)).thenReturn(Optional.of(DewormingMother.CONSULTA));
            when(companyQueryPort.findById(DewormingMother.COMPANY_ID))
                    .thenReturn(Optional.of(DewormingMother.CLINICA));
            UpdateDewormingCommand comando = new UpdateDewormingCommand(
                    DewormingMother.DEWORMING_ID, DewormingMother.FECHA, null, DewormingType.MIX,
                    "   ", "1 comprimido", null, null, DewormingMother.MICHI.id(),
                    DewormingMother.CONSULTATION_ID, DewormingMother.COMPANY_ID);

            assertThatThrownBy(() -> service.execute(comando))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("product is required");

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        private static final Long OTRA_EMPRESA = 77L;

        /**
         * El {@code isMyCompany(#command.companyId)} del puerto solo prueba que el
         * llamante declara SU empresa. Cargando por id a secas el efecto no era un
         * rechazo sino una apropiacion: la desparasitacion del paciente ajeno se
         * reescribia con {@code company} = la del atacante. Por eso el corte esta en la
         * carga y no en la anotacion.
         */
        @Test
        @DisplayName("la desparasitacion de otra empresa no se carga ni se guarda")
        void la_desparasitacion_de_otra_empresa_no_se_guarda() {
            UpdateDewormingCommand ajeno = new UpdateDewormingCommand(DewormingMother.DEWORMING_ID,
                    DewormingMother.FECHA, null, DewormingType.MIX, "Bravecto", "1 comprimido",
                    null, null, DewormingMother.MICHI.id(), DewormingMother.CONSULTATION_ID,
                    OTRA_EMPRESA);
            when(repository.findByIdAndCompanyId(DewormingMother.DEWORMING_ID, OTRA_EMPRESA))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(ajeno))
                    .isInstanceOf(DewormingNotFoundException.class)
                    .hasMessageContaining("Deworming not found: " + DewormingMother.DEWORMING_ID);

            verifyNoInteractions(animalQueryPort, consultationQueryPort, companyQueryPort);
            verify(repository, never()).findById(any());
            verify(repository, never()).save(any());
        }

        /**
         * La cuarta forma del defecto, la que sobrevive a las otras tres: con la carga
         * propia ya acotada, esta desparasitacion no se puede robar — pero si se podia
         * <b>reapuntar</b> al animal de otro tenant, porque la referencia se resolvia
         * con un {@code findById} pelado. El resultado era una desparasitacion de mi
         * empresa dentro de la historia clinica de la vecina.
         */
        @Test
        @DisplayName("un animal de otra empresa no resuelve: la fila propia no se reapunta")
        void un_animal_de_otra_empresa_no_reapunta_la_fila() {
            desparasitacionExistente();
            when(animalQueryPort.findByIdAndCompanyId(DewormingMother.MICHI.id(),
                    DewormingMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(DewormingMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Animal not found: " + DewormingMother.MICHI.id());

            verifyNoInteractions(consultationQueryPort, companyQueryPort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("una consulta de otra empresa no resuelve: la fila propia no se reapunta")
        void una_consulta_de_otra_empresa_no_reapunta_la_fila() {
            desparasitacionExistente();
            when(animalQueryPort.findByIdAndCompanyId(DewormingMother.MICHI.id(),
                    DewormingMother.COMPANY_ID)).thenReturn(Optional.of(DewormingMother.MICHI));
            when(consultationQueryPort.findByIdAndCompanyId(DewormingMother.CONSULTATION_ID,
                    DewormingMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(DewormingMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Consultation not found: " + DewormingMother.CONSULTATION_ID);

            verifyNoInteractions(companyQueryPort);
            verify(repository, never()).save(any());
        }
    }
}
