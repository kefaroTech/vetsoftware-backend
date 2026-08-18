package com.vetsoftware.app.hospitalizationobservation.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.hospitalizationobservation.application.dto.HospitalizationObservationDto;
import com.vetsoftware.app.hospitalizationobservation.application.port.out.HospitalizationObservationRepository;
import com.vetsoftware.app.hospitalizationobservation.domain.HospitalizationObservationNotFoundException;
import com.vetsoftware.app.hospitalizationobservation.testsupport.HospitalizationObservationMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReactivateHospitalizationObservationService")
class ReactivateHospitalizationObservationServiceTest {

    @Mock
    private HospitalizationObservationRepository repository;

    @InjectMocks
    private ReactivateHospitalizationObservationService service;

    @Nested
    @DisplayName("reactivacion valida")
    class ReactivacionValida {

        @Test
        @DisplayName("reactiva y devuelve el DTO de la observacion ya habilitada")
        void reactiva_y_devuelve_el_dto() {
            when(repository.reactivate(HospitalizationObservationMother.OBSERVATION_ID,
                    HospitalizationObservationMother.COMPANY_ID)).thenReturn(1);
            when(repository.findByIdAndCompanyId(HospitalizationObservationMother.OBSERVATION_ID,
                    HospitalizationObservationMother.COMPANY_ID))
                    .thenReturn(Optional.of(HospitalizationObservationMother.observacionValida()));

            HospitalizationObservationDto dto = service.execute(
                    HospitalizationObservationMother.OBSERVATION_ID,
                    HospitalizationObservationMother.COMPANY_ID);

            assertThat(dto.id()).isEqualTo(HospitalizationObservationMother.OBSERVATION_ID);
        }
    }

    @Nested
    @DisplayName("ninguna fila afectada")
    class NingunaFilaAfectada {

        @Test
        @DisplayName("reactivate en cero no relee la observacion y lanza NotFoundException")
        void reactivate_en_cero_no_relee() {
            when(repository.reactivate(HospitalizationObservationMother.OBSERVATION_ID,
                    HospitalizationObservationMother.COMPANY_ID)).thenReturn(0);

            assertThatThrownBy(
                    () -> service.execute(HospitalizationObservationMother.OBSERVATION_ID,
                            HospitalizationObservationMother.COMPANY_ID))
                    .isInstanceOf(HospitalizationObservationNotFoundException.class)
                    .hasMessageContaining(
                            String.valueOf(HospitalizationObservationMother.OBSERVATION_ID));

            verify(repository, never()).findByIdAndCompanyId(
                    HospitalizationObservationMother.OBSERVATION_ID,
                    HospitalizationObservationMother.COMPANY_ID);
        }
    }

    @Nested
    @DisplayName("estado inconsistente")
    class EstadoInconsistente {

        @Test
        @DisplayName("reactivate afecta una fila pero la relectura no la encuentra: igual lanza NotFoundException")
        void reactivate_afecta_fila_pero_la_relectura_no_la_encuentra() {
            when(repository.reactivate(HospitalizationObservationMother.OBSERVATION_ID,
                    HospitalizationObservationMother.COMPANY_ID)).thenReturn(1);
            when(repository.findByIdAndCompanyId(HospitalizationObservationMother.OBSERVATION_ID,
                    HospitalizationObservationMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(HospitalizationObservationMother.OBSERVATION_ID,
                            HospitalizationObservationMother.COMPANY_ID))
                    .isInstanceOf(HospitalizationObservationNotFoundException.class)
                    .hasMessageContaining(
                            String.valueOf(HospitalizationObservationMother.OBSERVATION_ID));
        }
    }

    /**
     * Antes de BE-COV el UPDATE nativo era {@code WHERE id = :id} a secas: en
     * reactivacion no hay lectura previa que valide la propiedad, asi que el SQL
     * era la unica barrera y no existia. Ahora el {@code EXISTS} contra la
     * hospitalizacion padre acota por empresa y cero filas afectadas sale como 404.
     */
    @Nested
    @DisplayName("aislamiento multi-tenant")
    class Tenencia {

        @Test
        @DisplayName("una observacion de otra empresa no se reactiva: 404 y no relee nada")
        void una_observacion_de_otra_empresa_no_se_reactiva() {
            when(repository.reactivate(HospitalizationObservationMother.OBSERVATION_ID,
                    HospitalizationObservationMother.OTRA_COMPANY_ID)).thenReturn(0);

            assertThatThrownBy(
                    () -> service.execute(HospitalizationObservationMother.OBSERVATION_ID,
                            HospitalizationObservationMother.OTRA_COMPANY_ID))
                    .isInstanceOf(HospitalizationObservationNotFoundException.class)
                    .hasMessageContaining(
                            String.valueOf(HospitalizationObservationMother.OBSERVATION_ID));

            verify(repository, never()).findByIdAndCompanyId(
                    HospitalizationObservationMother.OBSERVATION_ID,
                    HospitalizationObservationMother.OTRA_COMPANY_ID);
        }
    }
}
