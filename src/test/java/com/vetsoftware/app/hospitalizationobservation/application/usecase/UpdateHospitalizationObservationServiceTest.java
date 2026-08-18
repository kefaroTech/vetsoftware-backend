package com.vetsoftware.app.hospitalizationobservation.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.hospitalizationobservation.application.dto.HospitalizationObservationDto;
import com.vetsoftware.app.hospitalizationobservation.application.port.out.HospitalizationObservationRepository;
import com.vetsoftware.app.hospitalizationobservation.domain.HospitalizationObservation;
import com.vetsoftware.app.hospitalizationobservation.testsupport.HospitalizationObservationMother;
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
@DisplayName("UpdateHospitalizationObservationService")
class UpdateHospitalizationObservationServiceTest {

    @Mock
    private HospitalizationObservationRepository repository;

    @InjectMocks
    private UpdateHospitalizationObservationService service;

    @Captor
    private ArgumentCaptor<HospitalizationObservation> observationCaptor;

    @Nested
    @DisplayName("actualizacion valida")
    class ActualizacionValida {

        @Test
        @DisplayName("actualiza la descripcion de la observacion encontrada y la persiste")
        void actualiza_la_descripcion_y_persiste() {
            HospitalizationObservation existente = HospitalizationObservationMother
                    .observacionValida();
            when(repository.findByIdAndCompanyId(HospitalizationObservationMother.OBSERVATION_ID,
                    HospitalizationObservationMother.COMPANY_ID))
                    .thenReturn(Optional.of(existente));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            HospitalizationObservationDto dto = service
                    .execute(HospitalizationObservationMother.comandoActualizar());

            verify(repository).save(observationCaptor.capture());
            assertThat(observationCaptor.getValue().getDescription())
                    .isEqualTo("Descripcion actualizada");
            assertThat(dto.description()).isEqualTo("Descripcion actualizada");
        }
    }

    @Nested
    @DisplayName("observacion inexistente")
    class ObservacionInexistente {

        @Test
        @DisplayName("no persiste nada")
        void no_persiste_nada() {
            when(repository.findByIdAndCompanyId(HospitalizationObservationMother.OBSERVATION_ID,
                    HospitalizationObservationMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(HospitalizationObservationMother.comandoActualizar()))
                    .isInstanceOf(
                            com.vetsoftware.app.hospitalizationobservation.domain.HospitalizationObservationNotFoundException.class)
                    .hasMessageContaining(
                            String.valueOf(HospitalizationObservationMother.OBSERVATION_ID));

            verify(repository, never()).save(any());
        }
    }

    /**
     * El {@code @PreAuthorize} solo prueba que el atacante declara SU propia
     * empresa; mientras la carga fue {@code findById(command.id())} el gate era
     * vacuo y la observacion clinica de otra empresa se editaba adivinando el id.
     */
    @Nested
    @DisplayName("aislamiento multi-tenant")
    class Tenencia {

        @Test
        @DisplayName("una observacion de otra empresa no se actualiza: 404 y no persiste nada")
        void una_observacion_de_otra_empresa_no_se_actualiza() {
            when(repository.findByIdAndCompanyId(HospitalizationObservationMother.OBSERVATION_ID,
                    HospitalizationObservationMother.OTRA_COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(HospitalizationObservationMother
                    .comandoActualizar(HospitalizationObservationMother.OTRA_COMPANY_ID)))
                    .isInstanceOf(
                            com.vetsoftware.app.hospitalizationobservation.domain.HospitalizationObservationNotFoundException.class)
                    .hasMessageContaining(
                            String.valueOf(HospitalizationObservationMother.OBSERVATION_ID));

            verify(repository, never()).save(any());
        }
    }
}
