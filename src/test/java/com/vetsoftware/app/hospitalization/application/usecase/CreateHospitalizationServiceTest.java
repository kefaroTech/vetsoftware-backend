package com.vetsoftware.app.hospitalization.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.hospitalization.application.command.CreateHospitalizationCommand;
import com.vetsoftware.app.hospitalization.application.dto.HospitalizationDto;
import com.vetsoftware.app.hospitalization.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.hospitalization.application.port.out.AnimalWeightPort;
import com.vetsoftware.app.hospitalization.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.hospitalization.application.port.out.ConsultationQueryPort;
import com.vetsoftware.app.hospitalization.application.port.out.HospitalizationRepository;
import com.vetsoftware.app.hospitalization.domain.Hospitalization;
import com.vetsoftware.app.hospitalization.domain.HospitalizationType;
import com.vetsoftware.app.hospitalization.domain.ReasonLeaving;
import com.vetsoftware.app.hospitalization.testsupport.HospitalizationMother;
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
@DisplayName("CreateHospitalizationService")
class CreateHospitalizationServiceTest {

    @Mock
    private HospitalizationRepository repository;
    @Mock
    private AnimalQueryPort animalQueryPort;
    @Mock
    private ConsultationQueryPort consultationQueryPort;
    @Mock
    private CompanyQueryPort companyQueryPort;
    @Mock
    private AnimalWeightPort animalWeightPort;

    @InjectMocks
    private CreateHospitalizationService service;

    @Captor
    private ArgumentCaptor<Hospitalization> guardada;

    private void todasLasReferenciasExisten() {
        when(animalQueryPort.findById(HospitalizationMother.ANIMAL_ID))
                .thenReturn(Optional.of(HospitalizationMother.FIRULAIS));
        when(consultationQueryPort.findById(HospitalizationMother.CONSULTATION_ID))
                .thenReturn(Optional.of(HospitalizationMother.CONSULTA));
        when(companyQueryPort.findById(HospitalizationMother.COMPANY_ID))
                .thenReturn(Optional.of(HospitalizationMother.CLINICA));
    }

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("persiste la hospitalizacion con las referencias resueltas por los puertos")
        void persiste_con_las_referencias_resueltas() {
            todasLasReferenciasExisten();
            when(repository.save(any())).thenReturn(HospitalizationMother.internado());

            service.execute(HospitalizationMother.comandoCrear());

            verify(repository).save(guardada.capture());
            Hospitalization nueva = guardada.getValue();
            // Las refs tienen que venir de los puertos, no de los ids sueltos del comando.
            assertThat(nueva.getAnimal()).isEqualTo(HospitalizationMother.FIRULAIS);
            assertThat(nueva.getConsultation()).isEqualTo(HospitalizationMother.CONSULTA);
            assertThat(nueva.getCompany()).isEqualTo(HospitalizationMother.CLINICA);
            assertThat(nueva.getId()).isNull();
            assertThat(nueva.isEnabled()).isTrue();
            assertThat(nueva.getDate()).isEqualTo(HospitalizationMother.FECHA);
            assertThat(nueva.getStartDate()).isEqualTo(HospitalizationMother.INICIO);
            assertThat(nueva.getEndDate()).isEqualTo(HospitalizationMother.FIN);
            assertThat(nueva.getType()).isEqualTo(HospitalizationType.HOSPITALIZATION);
            assertThat(nueva.getReasonLeaving()).isEqualTo(ReasonLeaving.MEDICAL_DISCHARGE);
            assertThat(nueva.getReason()).isEqualTo("Gastroenteritis aguda");
            assertThat(nueva.getObservations()).isEqualTo("Sin complicaciones");
        }

        @Test
        @DisplayName("devuelve el DTO de la entidad ya persistida, con su id")
        void devuelve_el_dto_de_la_entidad_persistida() {
            todasLasReferenciasExisten();
            when(repository.save(any())).thenReturn(HospitalizationMother.internado());

            HospitalizationDto dto = service.execute(HospitalizationMother.comandoCrear());

            assertThat(dto.id()).isEqualTo(HospitalizationMother.HOSPITALIZATION_ID);
            assertThat(dto.animal().name()).isEqualTo("Firulais");
            assertThat(dto.company().id()).isEqualTo(HospitalizationMother.COMPANY_ID);
        }

        @Test
        @DisplayName("sin consultationId no consulta el puerto de consultas y guarda null")
        void sin_consultation_id_no_consulta_el_puerto() {
            when(animalQueryPort.findById(HospitalizationMother.ANIMAL_ID))
                    .thenReturn(Optional.of(HospitalizationMother.FIRULAIS));
            when(companyQueryPort.findById(HospitalizationMother.COMPANY_ID))
                    .thenReturn(Optional.of(HospitalizationMother.CLINICA));
            when(repository.save(any())).thenReturn(HospitalizationMother.ambulatorioSinConsulta());

            service.execute(HospitalizationMother.comandoCrearSinConsulta());

            verifyNoInteractions(consultationQueryPort);
            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue().getConsultation()).isNull();
        }
    }

    @Nested
    @DisplayName("peso opcional al ingreso")
    class PesoAlIngreso {

        @Test
        @DisplayName("sin peso en el comando no se toca la serie temporal del animal")
        void sin_peso_no_se_toca_la_serie_temporal() {
            todasLasReferenciasExisten();
            when(repository.save(any())).thenReturn(HospitalizationMother.internado());

            service.execute(HospitalizationMother.comandoCrear());

            verifyNoInteractions(animalWeightPort);
        }

        @Test
        @DisplayName("con peso registra el punto de peso contra la hospitalizacion ya guardada")
        void con_peso_registra_el_punto_de_peso() {
            todasLasReferenciasExisten();
            when(repository.save(any())).thenReturn(HospitalizationMother.internado());

            service.execute(HospitalizationMother.comandoCrearConPeso(new BigDecimal("12.50"),
                    "KILOGRAMS"));

            // El id que viaja al puerto de peso tiene que ser el de la entidad guardada:
            // si se tomara la de entrada, seria null y el punto quedaria huerfano.
            verify(animalWeightPort).recordHospitalizationWeight(HospitalizationMother.ANIMAL_ID,
                    HospitalizationMother.COMPANY_ID, new BigDecimal("12.50"), "KILOGRAMS",
                    HospitalizationMother.FECHA, HospitalizationMother.HOSPITALIZATION_ID);
        }

        @Test
        @DisplayName("con peso y sin unidad delega la unidad preferida al paquete animal")
        void con_peso_y_sin_unidad_viaja_null() {
            todasLasReferenciasExisten();
            when(repository.save(any())).thenReturn(HospitalizationMother.internado());

            service.execute(HospitalizationMother.comandoCrearConPeso(new BigDecimal("3"), null));

            verify(animalWeightPort).recordHospitalizationWeight(HospitalizationMother.ANIMAL_ID,
                    HospitalizationMother.COMPANY_ID, new BigDecimal("3"), null,
                    HospitalizationMother.FECHA, HospitalizationMother.HOSPITALIZATION_ID);
        }
    }

    @Nested
    @DisplayName("referencias que no existen")
    class ReferenciasInexistentes {

        @Test
        @DisplayName("animal inexistente: no consulta los puertos siguientes ni persiste")
        void animal_inexistente() {
            when(animalQueryPort.findById(HospitalizationMother.ANIMAL_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(HospitalizationMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Animal not found: " + HospitalizationMother.ANIMAL_ID);

            verifyNoInteractions(consultationQueryPort, companyQueryPort, repository,
                    animalWeightPort);
        }

        @Test
        @DisplayName("consulta inexistente: aborta antes de resolver la empresa")
        void consulta_inexistente() {
            when(animalQueryPort.findById(HospitalizationMother.ANIMAL_ID))
                    .thenReturn(Optional.of(HospitalizationMother.FIRULAIS));
            when(consultationQueryPort.findById(HospitalizationMother.CONSULTATION_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(HospitalizationMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Consultation not found: " + HospitalizationMother.CONSULTATION_ID);

            verifyNoInteractions(companyQueryPort, repository, animalWeightPort);
        }

        @Test
        @DisplayName("empresa inexistente: cero escrituras")
        void empresa_inexistente() {
            when(animalQueryPort.findById(HospitalizationMother.ANIMAL_ID))
                    .thenReturn(Optional.of(HospitalizationMother.FIRULAIS));
            when(consultationQueryPort.findById(HospitalizationMother.CONSULTATION_ID))
                    .thenReturn(Optional.of(HospitalizationMother.CONSULTA));
            when(companyQueryPort.findById(HospitalizationMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(HospitalizationMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: " + HospitalizationMother.COMPANY_ID);

            verifyNoInteractions(repository, animalWeightPort);
        }
    }

    @Nested
    @DisplayName("invariantes del dominio propagadas")
    class InvariantesDelDominio {

        @Test
        @DisplayName("un motivo en blanco no llega a persistirse")
        void un_motivo_en_blanco_no_llega_a_persistirse() {
            todasLasReferenciasExisten();
            CreateHospitalizationCommand comando = new CreateHospitalizationCommand(
                    HospitalizationMother.FECHA, HospitalizationMother.INICIO,
                    HospitalizationMother.FIN, HospitalizationType.HOSPITALIZATION, null, "   ",
                    null, HospitalizationMother.ANIMAL_ID, HospitalizationMother.CONSULTATION_ID,
                    HospitalizationMother.COMPANY_ID, null, null);

            assertThatThrownBy(() -> service.execute(comando))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reason is required");

            verify(repository, never()).save(any());
            verifyNoInteractions(animalWeightPort);
        }

        @Test
        @DisplayName("una fecha de alta anterior al ingreso no llega a persistirse")
        void fecha_de_alta_anterior_al_ingreso_no_persiste() {
            todasLasReferenciasExisten();
            CreateHospitalizationCommand comando = new CreateHospitalizationCommand(
                    HospitalizationMother.FECHA, java.time.LocalDate.of(2026, 3, 5),
                    java.time.LocalDate.of(2026, 3, 4), HospitalizationType.HOSPITALIZATION, null,
                    "Motivo", null, HospitalizationMother.ANIMAL_ID,
                    HospitalizationMother.CONSULTATION_ID, HospitalizationMother.COMPANY_ID, null,
                    null);

            assertThatThrownBy(() -> service.execute(comando))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("endDate cannot be before startDate");

            verify(repository, never()).save(any());
        }
    }
}
