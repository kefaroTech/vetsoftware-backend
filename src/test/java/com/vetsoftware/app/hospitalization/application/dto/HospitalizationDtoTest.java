package com.vetsoftware.app.hospitalization.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.hospitalization.domain.Hospitalization;
import com.vetsoftware.app.hospitalization.domain.HospitalizationType;
import com.vetsoftware.app.hospitalization.domain.ReasonLeaving;
import com.vetsoftware.app.hospitalization.testsupport.HospitalizationMother;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("DTOs de salida de hospitalization")
class HospitalizationDtoTest {

    @Nested
    @DisplayName("HospitalizationDto.from")
    class DesdeElAgregado {

        @Test
        @DisplayName("copia campo por campo el agregado completo")
        void copia_campo_por_campo_el_agregado_completo() {
            HospitalizationDto dto = HospitalizationDto.from(HospitalizationMother.internado());

            assertThat(dto.id()).isEqualTo(HospitalizationMother.HOSPITALIZATION_ID);
            assertThat(dto.date()).isEqualTo(HospitalizationMother.FECHA);
            assertThat(dto.startDate()).isEqualTo(HospitalizationMother.INICIO);
            assertThat(dto.endDate()).isEqualTo(HospitalizationMother.FIN);
            assertThat(dto.type()).isEqualTo(HospitalizationType.HOSPITALIZATION);
            assertThat(dto.reasonLeaving()).isEqualTo(ReasonLeaving.MEDICAL_DISCHARGE);
            assertThat(dto.reason()).isEqualTo("Gastroenteritis aguda");
            assertThat(dto.observations()).isEqualTo("Sin complicaciones");
            assertThat(dto.animal()).isEqualTo(
                    new AnimalSummaryDto(HospitalizationMother.ANIMAL_ID, "Firulais", "A-001"));
            assertThat(dto.consultation()).isEqualTo(new ConsultationSummaryDto(
                    HospitalizationMother.CONSULTATION_ID, HospitalizationMother.CONSULTA.date()));
            assertThat(dto.company()).isEqualTo(new CompanySummaryDto(
                    HospitalizationMother.COMPANY_ID, "Clinica Vet", "900123456"));
            assertThat(dto.createdDate()).isEqualTo(HospitalizationMother.CREADO);
            assertThat(dto.enabled()).isTrue();
        }

        @Test
        @DisplayName("sin consulta asociada el sumario de consulta viaja nulo, no revienta")
        void sin_consulta_el_sumario_viaja_nulo() {
            HospitalizationDto dto = HospitalizationDto
                    .from(HospitalizationMother.ambulatorioSinConsulta());

            assertThat(dto.consultation()).isNull();
            assertThat(dto.type()).isEqualTo(HospitalizationType.OUTPATIENT);
            assertThat(dto.reasonLeaving()).isNull();
            assertThat(dto.endDate()).isNull();
        }

        @Test
        @DisplayName("refleja el estado deshabilitado del agregado")
        void refleja_el_estado_deshabilitado() {
            Hospitalization deshabilitado = HospitalizationMother.deshabilitado();

            assertThat(HospitalizationDto.from(deshabilitado).enabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("sumarios de referencias externas")
    class Sumarios {

        @Test
        @DisplayName("AnimalSummaryDto.from copia id, nombre y codigo")
        void animal_summary_copia_los_tres_campos() {
            AnimalSummaryDto dto = AnimalSummaryDto.from(HospitalizationMother.FIRULAIS);

            assertThat(dto.id()).isEqualTo(HospitalizationMother.ANIMAL_ID);
            assertThat(dto.name()).isEqualTo("Firulais");
            assertThat(dto.code()).isEqualTo("A-001");
        }

        @Test
        @DisplayName("ConsultationSummaryDto.from copia id y fecha")
        void consultation_summary_copia_id_y_fecha() {
            ConsultationSummaryDto dto = ConsultationSummaryDto
                    .from(HospitalizationMother.CONSULTA);

            assertThat(dto.id()).isEqualTo(HospitalizationMother.CONSULTATION_ID);
            assertThat(dto.date()).isEqualTo(HospitalizationMother.CONSULTA.date());
        }

        @Test
        @DisplayName("CompanySummaryDto.from copia id, nombre e identificador")
        void company_summary_copia_los_tres_campos() {
            CompanySummaryDto dto = CompanySummaryDto.from(HospitalizationMother.CLINICA);

            assertThat(dto.id()).isEqualTo(HospitalizationMother.COMPANY_ID);
            assertThat(dto.name()).isEqualTo("Clinica Vet");
            assertThat(dto.identifier()).isEqualTo("900123456");
        }
    }

    @Nested
    @DisplayName("PageResult")
    class Paginacion {

        @Test
        @DisplayName("map transforma el contenido y conserva los metadatos de la pagina")
        void map_transforma_el_contenido_y_conserva_los_metadatos() {
            PageResult<Hospitalization> pagina = new PageResult<>(
                    List.of(HospitalizationMother.internado()), 2, 20, 41L, 3);

            PageResult<HospitalizationDto> mapeada = pagina.map(HospitalizationDto::from);

            assertThat(mapeada.content()).hasSize(1);
            assertThat(mapeada.content().get(0).id())
                    .isEqualTo(HospitalizationMother.HOSPITALIZATION_ID);
            assertThat(mapeada.page()).isEqualTo(2);
            assertThat(mapeada.pageSize()).isEqualTo(20);
            assertThat(mapeada.totalElements()).isEqualTo(41L);
            assertThat(mapeada.totalPages()).isEqualTo(3);
        }

        @Test
        @DisplayName("map sobre una pagina vacia devuelve contenido vacio sin tocar la funcion")
        void map_sobre_pagina_vacia() {
            PageResult<Hospitalization> vacia = new PageResult<>(List.of(), 0, 20, 0L, 0);

            Function<Hospitalization, String> nuncaLlamada = h -> {
                throw new AssertionError("no deberia mapearse ningun elemento");
            };

            assertThat(vacia.map(nuncaLlamada).content()).isEmpty();
            assertThat(vacia.map(nuncaLlamada).totalElements()).isZero();
        }
    }
}
