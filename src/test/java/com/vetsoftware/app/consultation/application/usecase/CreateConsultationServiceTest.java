package com.vetsoftware.app.consultation.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.consultation.application.dto.ConsultationDto;
import com.vetsoftware.app.consultation.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.consultation.application.port.out.AnimalWeightPort;
import com.vetsoftware.app.consultation.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.consultation.application.port.out.ConsultationRepository;
import com.vetsoftware.app.consultation.application.port.out.ConsultationTypeQueryPort;
import com.vetsoftware.app.consultation.domain.Consultation;
import com.vetsoftware.app.consultation.testsupport.ConsultationMother;
import java.math.BigDecimal;
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
@DisplayName("CreateConsultationService")
class CreateConsultationServiceTest {

    @Mock
    private ConsultationRepository repository;
    @Mock
    private ConsultationTypeQueryPort consultationTypeQueryPort;
    @Mock
    private AnimalQueryPort animalQueryPort;
    @Mock
    private CompanyQueryPort companyQueryPort;
    @Mock
    private AnimalWeightPort animalWeightPort;

    @InjectMocks
    private CreateConsultationService service;

    @Captor
    private ArgumentCaptor<Consultation> consultationCaptor;

    private void todasLasReferenciasExisten() {
        when(consultationTypeQueryPort.findById(ConsultationMother.CONSULTATION_TYPE_ID))
                .thenReturn(Optional.of(ConsultationMother.CONTROL));
        when(animalQueryPort.findByIdAndCompanyId(ConsultationMother.ANIMAL_ID,
                ConsultationMother.COMPANY_ID))
                .thenReturn(Optional.of(ConsultationMother.FIRULAIS));
        when(companyQueryPort.findById(ConsultationMother.COMPANY_ID))
                .thenReturn(Optional.of(ConsultationMother.CLINICA));
    }

    @Nested
    @DisplayName("creacion sin peso")
    class SinPeso {

        @Test
        @DisplayName("persiste la consulta con las referencias resueltas por los puertos")
        void persiste_la_consulta_con_las_referencias_resueltas() {
            todasLasReferenciasExisten();
            when(repository.save(any())).thenReturn(ConsultationMother.consultaValida());

            service.execute(ConsultationMother.comandoCrear());

            verify(repository).save(consultationCaptor.capture());
            Consultation guardada = consultationCaptor.getValue();
            // Lo que importa no es que se llamara a save, sino que se guardara ESTO: las
            // refs tienen que venir de los puertos, no de los ids del comando.
            assertThat(guardada.getConsultationType()).isEqualTo(ConsultationMother.CONTROL);
            assertThat(guardada.getAnimal()).isEqualTo(ConsultationMother.FIRULAIS);
            assertThat(guardada.getCompany()).isEqualTo(ConsultationMother.CLINICA);
            assertThat(guardada.getAnamnesis()).isEqualTo("Anamnesis del paciente");
            assertThat(guardada.getId()).isNull();
        }

        @Test
        @DisplayName("no registra ningun peso")
        void no_registra_ningun_peso() {
            todasLasReferenciasExisten();
            when(repository.save(any())).thenReturn(ConsultationMother.consultaValida());

            service.execute(ConsultationMother.comandoCrear());

            verifyNoInteractions(animalWeightPort);
        }

        @Test
        @DisplayName("devuelve el DTO de la consulta ya persistida, con su id")
        void devuelve_el_dto_de_la_consulta_ya_persistida() {
            todasLasReferenciasExisten();
            when(repository.save(any())).thenReturn(ConsultationMother.consultaValida());

            ConsultationDto dto = service.execute(ConsultationMother.comandoCrear());

            assertThat(dto.id()).isEqualTo(ConsultationMother.CONSULTATION_ID);
            assertThat(dto.anamnesis()).isEqualTo("Anamnesis del paciente");
        }

        @Test
        @DisplayName("construye el examen fisico a partir de las constantes vitales del comando")
        void construye_el_examen_fisico_del_comando() {
            todasLasReferenciasExisten();
            when(repository.save(any())).thenReturn(ConsultationMother.consultaValida());

            service.execute(ConsultationMother.comandoCrear());

            verify(repository).save(consultationCaptor.capture());
            assertThat(consultationCaptor.getValue().getPhysicalExam().temperature())
                    .isEqualByComparingTo("38.5");
            assertThat(consultationCaptor.getValue().getPhysicalExam().heartRate()).isEqualTo(90);
        }
    }

    @Nested
    @DisplayName("creacion con peso")
    class ConPeso {

        @Test
        @DisplayName("registra el peso capturado en la fecha de la consulta, con el id ya persistido")
        void registra_el_peso_capturado_en_la_fecha_de_la_consulta() {
            todasLasReferenciasExisten();
            when(repository.save(any())).thenReturn(ConsultationMother.consultaValida());

            service.execute(
                    ConsultationMother.comandoCrearConPeso(new BigDecimal("12.50"), "KILOGRAMS"));

            verify(animalWeightPort).recordConsultationWeight(ConsultationMother.ANIMAL_ID,
                    ConsultationMother.COMPANY_ID, new BigDecimal("12.50"), "KILOGRAMS",
                    ConsultationMother.FECHA, ConsultationMother.CONSULTATION_ID);
        }
    }

    @Nested
    @DisplayName("referencias que no existen")
    class ReferenciasInexistentes {

        @Test
        @DisplayName("tipo de consulta inexistente: no consulta los puertos siguientes ni persiste")
        void tipo_de_consulta_inexistente() {
            when(consultationTypeQueryPort.findById(ConsultationMother.CONSULTATION_TYPE_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(ConsultationMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ConsultationType not found: "
                            + ConsultationMother.CONSULTATION_TYPE_ID);

            verifyNoInteractions(animalQueryPort, companyQueryPort, repository, animalWeightPort);
        }

        @Test
        @DisplayName("animal inexistente en la empresa")
        void animal_inexistente() {
            when(consultationTypeQueryPort.findById(ConsultationMother.CONSULTATION_TYPE_ID))
                    .thenReturn(Optional.of(ConsultationMother.CONTROL));
            when(animalQueryPort.findByIdAndCompanyId(ConsultationMother.ANIMAL_ID,
                    ConsultationMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(ConsultationMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Animal not found: " + ConsultationMother.ANIMAL_ID);

            verifyNoInteractions(companyQueryPort, repository, animalWeightPort);
        }

        @Test
        @DisplayName("empresa inexistente")
        void empresa_inexistente() {
            when(consultationTypeQueryPort.findById(ConsultationMother.CONSULTATION_TYPE_ID))
                    .thenReturn(Optional.of(ConsultationMother.CONTROL));
            when(animalQueryPort.findByIdAndCompanyId(ConsultationMother.ANIMAL_ID,
                    ConsultationMother.COMPANY_ID))
                    .thenReturn(Optional.of(ConsultationMother.FIRULAIS));
            when(companyQueryPort.findById(ConsultationMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(ConsultationMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: " + ConsultationMother.COMPANY_ID);

            verifyNoInteractions(repository, animalWeightPort);
        }
    }

    @Nested
    @DisplayName("invariantes del dominio propagadas")
    class InvariantesDelDominio {

        @Test
        @DisplayName("una anamnesis vacia no llega a persistirse")
        void una_anamnesis_vacia_no_llega_a_persistirse() {
            todasLasReferenciasExisten();
            var comandoInvalido = new com.vetsoftware.app.consultation.application.command.CreateConsultationCommand(
                    ConsultationMother.FECHA, ConsultationMother.CONSULTATION_TYPE_ID, "  ", null,
                    null, null, ConsultationMother.ANIMAL_ID, ConsultationMother.COMPANY_ID, null,
                    null, null, null, null, null, null, null, null, null, null, null);

            assertThatThrownBy(() -> service.execute(comandoInvalido))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("anamnesis is required");

            verify(repository, never()).save(any());
            verifyNoInteractions(animalWeightPort);
        }

        @Test
        @DisplayName("una constante vital fuera de rango aborta antes de guardar")
        void una_constante_vital_fuera_de_rango_aborta_antes_de_guardar() {
            todasLasReferenciasExisten();
            var comandoInvalido = new com.vetsoftware.app.consultation.application.command.CreateConsultationCommand(
                    ConsultationMother.FECHA, ConsultationMother.CONSULTATION_TYPE_ID,
                    "Anamnesis del paciente", null, null, null, ConsultationMother.ANIMAL_ID,
                    ConsultationMother.COMPANY_ID, null, null, new BigDecimal("100"), null, null,
                    null, null, null, null, null, null, null);

            assertThatThrownBy(() -> service.execute(comandoInvalido))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("temperature must be between 0 and 60");

            verify(repository, never()).save(any());
        }
    }
}
