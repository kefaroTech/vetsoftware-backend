package com.vetsoftware.app.hospitalizationmedication.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.hospitalizationmedication.application.command.UpdateHospitalizationMedicationCommand;
import com.vetsoftware.app.hospitalizationmedication.application.dto.HospitalizationMedicationDto;
import com.vetsoftware.app.hospitalizationmedication.application.port.out.HospitalizationMedicationRepository;
import com.vetsoftware.app.hospitalizationmedication.domain.DurationMeasure;
import com.vetsoftware.app.hospitalizationmedication.domain.Frequency;
import com.vetsoftware.app.hospitalizationmedication.domain.GuidelineType;
import com.vetsoftware.app.hospitalizationmedication.domain.HospitalizationMedication;
import com.vetsoftware.app.hospitalizationmedication.domain.HospitalizationMedicationNotFoundException;
import com.vetsoftware.app.hospitalizationmedication.testsupport.HospitalizationMedicationMother;
import java.time.LocalDate;
import java.time.LocalTime;
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
@DisplayName("UpdateHospitalizationMedicationService")
class UpdateHospitalizationMedicationServiceTest {

    @Mock
    private HospitalizationMedicationRepository repository;

    @InjectMocks
    private UpdateHospitalizationMedicationService service;

    @Captor
    private ArgumentCaptor<HospitalizationMedication> captor;

    private void ordenExiste() {
        when(repository.findByIdAndCompanyId(HospitalizationMedicationMother.MEDICATION_ID,
                HospitalizationMedicationMother.COMPANY_ID))
                .thenReturn(Optional.of(HospitalizationMedicationMother.activo()));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Nested
    @DisplayName("actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("actualiza los campos y persiste sobre la orden encontrada")
        void actualiza_los_campos_y_persiste() {
            ordenExiste();

            HospitalizationMedicationDto dto = service
                    .execute(HospitalizationMedicationMother.comandoActualizar());

            verify(repository).save(captor.capture());
            HospitalizationMedication guardado = captor.getValue();
            assertThat(guardado.getName()).isEqualTo("Amoxicilina 1g");
            assertThat(guardado.getDose()).isEqualTo("2 tabletas");
            assertThat(guardado.getFrequency()).isEqualTo(Frequency.EVERY_12H);
            assertThat(guardado.getGuidelineType()).isEqualTo(GuidelineType.FIXED);
            assertThat(guardado.getDurationMeasure()).isEqualTo(DurationMeasure.DOSES);
            assertThat(guardado.getDurationQuantity()).isEqualTo(3);
            assertThat(guardado.getId()).isEqualTo(HospitalizationMedicationMother.MEDICATION_ID);
            assertThat(guardado.getHospitalization())
                    .isEqualTo(HospitalizationMedicationMother.HOSPITALIZACION);
            assertThat(dto.name()).isEqualTo("Amoxicilina 1g");
        }

        @Test
        @DisplayName("planificacion nula se mapea a null en los tres enums")
        void planificacion_nula_se_mapea_a_null() {
            ordenExiste();
            UpdateHospitalizationMedicationCommand comando = new UpdateHospitalizationMedicationCommand(
                    HospitalizationMedicationMother.MEDICATION_ID, "Amoxicilina 1g", null, null,
                    null, null, null, null, null, null, HospitalizationMedicationMother.COMPANY_ID);

            service.execute(comando);

            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getFrequency()).isNull();
            assertThat(captor.getValue().getGuidelineType()).isNull();
            assertThat(captor.getValue().getDurationMeasure()).isNull();
        }

        @Test
        @DisplayName("planificacion en blanco tambien se mapea a null")
        void planificacion_en_blanco_se_mapea_a_null() {
            ordenExiste();
            UpdateHospitalizationMedicationCommand comando = new UpdateHospitalizationMedicationCommand(
                    HospitalizationMedicationMother.MEDICATION_ID, "Amoxicilina 1g", null, "   ",
                    "   ", "   ", null, null, null, null,
                    HospitalizationMedicationMother.COMPANY_ID);

            service.execute(comando);

            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getFrequency()).isNull();
            assertThat(captor.getValue().getGuidelineType()).isNull();
            assertThat(captor.getValue().getDurationMeasure()).isNull();
        }
    }

    @Nested
    @DisplayName("orden inexistente")
    class OrdenInexistente {

        @Test
        @DisplayName("no encontrada: lanza y no persiste nada")
        void no_encontrada_lanza_y_no_persiste() {
            when(repository.findByIdAndCompanyId(HospitalizationMedicationMother.MEDICATION_ID,
                    HospitalizationMedicationMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(HospitalizationMedicationMother.comandoActualizar()))
                    .isInstanceOf(HospitalizationMedicationNotFoundException.class)
                    .hasMessageContaining("HospitalizationMedication not found: "
                            + HospitalizationMedicationMother.MEDICATION_ID);

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("invariantes del dominio propagadas")
    class InvariantesDelDominio {

        @Test
        @DisplayName("un nombre vacio no llega a persistirse")
        void un_nombre_vacio_no_llega_a_persistirse() {
            when(repository.findByIdAndCompanyId(HospitalizationMedicationMother.MEDICATION_ID,
                    HospitalizationMedicationMother.COMPANY_ID))
                    .thenReturn(Optional.of(HospitalizationMedicationMother.activo()));
            UpdateHospitalizationMedicationCommand comando = new UpdateHospitalizationMedicationCommand(
                    HospitalizationMedicationMother.MEDICATION_ID, "   ", "2 tabletas", "EVERY_12H",
                    "FIXED", "DOSES", 3, LocalDate.of(2026, 3, 2), LocalTime.of(9, 0), "Notas",
                    HospitalizationMedicationMother.COMPANY_ID);

            assertThatThrownBy(() -> service.execute(comando))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name is required");

            verify(repository, never()).save(any());
        }
    }

    /**
     * El {@code @PreAuthorize} solo prueba que el atacante declara SU propia
     * empresa; mientras la carga fue {@code findById(command.id())} el gate era
     * vacuo y la orden de otra empresa se editaba adivinando el id. La carga
     * acotada la convierte en un 404.
     */
    @Nested
    @DisplayName("aislamiento multi-tenant")
    class Tenencia {

        @Test
        @DisplayName("una orden de otra empresa no se actualiza: 404 y no persiste nada")
        void una_orden_de_otra_empresa_no_se_actualiza() {
            when(repository.findByIdAndCompanyId(HospitalizationMedicationMother.MEDICATION_ID,
                    HospitalizationMedicationMother.OTRA_COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(HospitalizationMedicationMother
                    .comandoActualizar(HospitalizationMedicationMother.OTRA_COMPANY_ID)))
                    .isInstanceOf(HospitalizationMedicationNotFoundException.class)
                    .hasMessageContaining("HospitalizationMedication not found: "
                            + HospitalizationMedicationMother.MEDICATION_ID);

            verify(repository, never()).save(any());
        }
    }
}
