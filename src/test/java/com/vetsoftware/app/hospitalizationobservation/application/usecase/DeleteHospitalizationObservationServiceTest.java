package com.vetsoftware.app.hospitalizationobservation.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
@DisplayName("DeleteHospitalizationObservationService")
class DeleteHospitalizationObservationServiceTest {

    @Mock
    private HospitalizationObservationRepository repository;

    @InjectMocks
    private DeleteHospitalizationObservationService service;

    @Nested
    @DisplayName("borrado valido")
    class BorradoValido {

        @Test
        @DisplayName("borra la observacion encontrada")
        void borra_la_observacion_encontrada() {
            when(repository.findByIdAndCompanyId(HospitalizationObservationMother.OBSERVATION_ID,
                    HospitalizationObservationMother.COMPANY_ID))
                    .thenReturn(Optional.of(HospitalizationObservationMother.observacionValida()));

            service.execute(HospitalizationObservationMother.OBSERVATION_ID,
                    HospitalizationObservationMother.COMPANY_ID);

            verify(repository).delete(HospitalizationObservationMother.OBSERVATION_ID);
        }
    }

    @Nested
    @DisplayName("observacion inexistente")
    class ObservacionInexistente {

        @Test
        @DisplayName("no borra nada")
        void no_borra_nada() {
            when(repository.findByIdAndCompanyId(HospitalizationObservationMother.OBSERVATION_ID,
                    HospitalizationObservationMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(HospitalizationObservationMother.OBSERVATION_ID,
                            HospitalizationObservationMother.COMPANY_ID))
                    .isInstanceOf(HospitalizationObservationNotFoundException.class)
                    .hasMessageContaining(
                            String.valueOf(HospitalizationObservationMother.OBSERVATION_ID));

            verify(repository, never()).delete(HospitalizationObservationMother.OBSERVATION_ID);
        }
    }

    /**
     * Antes de BE-COV el puerto no recibia companyId y el servicio comprobaba la
     * existencia con {@code findById(id)}: cualquier empleado con
     * {@code hospitalization.delete} borraba la observacion de otra empresa
     * adivinando el id.
     */
    @Nested
    @DisplayName("aislamiento multi-tenant")
    class Tenencia {

        @Test
        @DisplayName("una observacion de otra empresa no se borra: 404 y el repositorio no escribe")
        void una_observacion_de_otra_empresa_no_se_borra() {
            when(repository.findByIdAndCompanyId(HospitalizationObservationMother.OBSERVATION_ID,
                    HospitalizationObservationMother.OTRA_COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(HospitalizationObservationMother.OBSERVATION_ID,
                            HospitalizationObservationMother.OTRA_COMPANY_ID))
                    .isInstanceOf(HospitalizationObservationNotFoundException.class)
                    .hasMessageContaining(
                            String.valueOf(HospitalizationObservationMother.OBSERVATION_ID));

            verify(repository, never()).delete(HospitalizationObservationMother.OBSERVATION_ID);
        }
    }
}
