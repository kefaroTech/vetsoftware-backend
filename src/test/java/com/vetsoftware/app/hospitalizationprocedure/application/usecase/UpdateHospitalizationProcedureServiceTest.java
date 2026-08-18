package com.vetsoftware.app.hospitalizationprocedure.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.hospitalizationprocedure.application.command.UpdateHospitalizationProcedureCommand;
import com.vetsoftware.app.hospitalizationprocedure.application.dto.HospitalizationProcedureDto;
import com.vetsoftware.app.hospitalizationprocedure.application.port.out.HospitalizationProcedureRepository;
import com.vetsoftware.app.hospitalizationprocedure.domain.DurationMeasure;
import com.vetsoftware.app.hospitalizationprocedure.domain.Frequency;
import com.vetsoftware.app.hospitalizationprocedure.domain.GuidelineType;
import com.vetsoftware.app.hospitalizationprocedure.domain.HospitalizationProcedure;
import com.vetsoftware.app.hospitalizationprocedure.domain.HospitalizationProcedureNotFoundException;
import com.vetsoftware.app.hospitalizationprocedure.testsupport.HospitalizationProcedureMother;
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
@DisplayName("UpdateHospitalizationProcedureService")
class UpdateHospitalizationProcedureServiceTest {

    @Mock
    private HospitalizationProcedureRepository repository;

    @InjectMocks
    private UpdateHospitalizationProcedureService service;

    @Captor
    private ArgumentCaptor<HospitalizationProcedure> captor;

    private void ordenExiste() {
        when(repository.findByIdAndCompanyId(HospitalizationProcedureMother.PROCEDURE_ID,
                HospitalizationProcedureMother.COMPANY_ID))
                .thenReturn(Optional.of(HospitalizationProcedureMother.activo()));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Nested
    @DisplayName("actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("actualiza los campos y persiste sobre la orden encontrada")
        void actualiza_los_campos_y_persiste() {
            ordenExiste();

            HospitalizationProcedureDto dto = service
                    .execute(HospitalizationProcedureMother.comandoActualizar());

            verify(repository).save(captor.capture());
            HospitalizationProcedure guardado = captor.getValue();
            assertThat(guardado.getName()).isEqualTo("Curacion actualizada");
            assertThat(guardado.getDose()).isEqualTo("Suero fisiologico");
            assertThat(guardado.getFrequency()).isEqualTo(Frequency.EVERY_12H);
            assertThat(guardado.getGuidelineType()).isEqualTo(GuidelineType.FIXED);
            assertThat(guardado.getDurationMeasure()).isEqualTo(DurationMeasure.DOSES);
            assertThat(guardado.getDurationQuantity()).isEqualTo(3);
            assertThat(guardado.getId()).isEqualTo(HospitalizationProcedureMother.PROCEDURE_ID);
            assertThat(guardado.getHospitalization())
                    .isEqualTo(HospitalizationProcedureMother.HOSPITALIZACION);
            assertThat(dto.name()).isEqualTo("Curacion actualizada");
        }

        @Test
        @DisplayName("planificacion nula se mapea a null en los tres enums")
        void planificacion_nula_se_mapea_a_null() {
            ordenExiste();
            UpdateHospitalizationProcedureCommand comando = new UpdateHospitalizationProcedureCommand(
                    HospitalizationProcedureMother.PROCEDURE_ID, "Curacion actualizada", null, null,
                    null, null, null, null, null, null, HospitalizationProcedureMother.COMPANY_ID);

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
            UpdateHospitalizationProcedureCommand comando = new UpdateHospitalizationProcedureCommand(
                    HospitalizationProcedureMother.PROCEDURE_ID, "Curacion actualizada", null,
                    "   ", "   ", "   ", null, null, null, null,
                    HospitalizationProcedureMother.COMPANY_ID);

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
            when(repository.findByIdAndCompanyId(HospitalizationProcedureMother.PROCEDURE_ID,
                    HospitalizationProcedureMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(HospitalizationProcedureMother.comandoActualizar()))
                    .isInstanceOf(HospitalizationProcedureNotFoundException.class)
                    .hasMessageContaining("HospitalizationProcedure not found: "
                            + HospitalizationProcedureMother.PROCEDURE_ID);

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("invariantes del dominio propagadas")
    class InvariantesDelDominio {

        @Test
        @DisplayName("un nombre vacio no llega a persistirse")
        void un_nombre_vacio_no_llega_a_persistirse() {
            when(repository.findByIdAndCompanyId(HospitalizationProcedureMother.PROCEDURE_ID,
                    HospitalizationProcedureMother.COMPANY_ID))
                    .thenReturn(Optional.of(HospitalizationProcedureMother.activo()));
            UpdateHospitalizationProcedureCommand comando = new UpdateHospitalizationProcedureCommand(
                    HospitalizationProcedureMother.PROCEDURE_ID, "   ", "Suero", "EVERY_12H",
                    "FIXED", "DOSES", 3, LocalDate.of(2026, 3, 2), LocalTime.of(9, 0), "Notas",
                    HospitalizationProcedureMother.COMPANY_ID);

            assertThatThrownBy(() -> service.execute(comando))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name is required");

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        /**
         * El comando lleva ahora el {@code companyId} que pone el controller desde el
         * contexto, y el servicio lo usa para leer. Para el tenant equivocado la orden
         * no existe: 404 y ni una escritura. Antes se leia con {@code findById(id)} a
         * secas y cualquier empleado con {@code hospitalization.update} actualizaba la
         * orden de otra empresa adivinando el id.
         */
        @Test
        @DisplayName("una orden de otra empresa no se actualiza: 404 y no persiste nada")
        void una_orden_de_otra_empresa_no_se_actualiza() {
            when(repository.findByIdAndCompanyId(HospitalizationProcedureMother.PROCEDURE_ID,
                    HospitalizationProcedureMother.OTRA_COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(HospitalizationProcedureMother
                    .comandoActualizar(HospitalizationProcedureMother.OTRA_COMPANY_ID)))
                    .isInstanceOf(HospitalizationProcedureNotFoundException.class)
                    .hasMessageContaining("HospitalizationProcedure not found: "
                            + HospitalizationProcedureMother.PROCEDURE_ID);

            verify(repository, never()).save(any());
        }
    }
}
