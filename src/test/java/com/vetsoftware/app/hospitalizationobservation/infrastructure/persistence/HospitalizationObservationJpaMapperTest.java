package com.vetsoftware.app.hospitalizationobservation.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.hospitalization.infrastructure.persistence.HospitalizationJpaEntity;
import com.vetsoftware.app.hospitalizationobservation.domain.HospitalizationObservation;
import com.vetsoftware.app.hospitalizationobservation.testsupport.HospitalizationObservationMother;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El mapper es el unico punto que conoce dominio y entidad JPA a la vez. Las
 * entidades JPA de las features vecinas (hospitalizacion, empleado) se mockean:
 * no tienen logica propia, son portadores de datos.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HospitalizationObservationJpaMapper")
class HospitalizationObservationJpaMapperTest {

    private final HospitalizationObservationJpaMapper mapper = new HospitalizationObservationJpaMapper();

    @Mock
    private HospitalizationJpaEntity hospitalizationEntity;
    @Mock
    private EmployeeJpaEntity createdByEntity;

    @Nested
    @DisplayName("toJpa — dominio a entidad")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo escalar en su columna y engancha las dos asociaciones")
        void copia_cada_campo_y_engancha_las_asociaciones() {
            HospitalizationObservation observation = HospitalizationObservationMother
                    .observacionValida();

            HospitalizationObservationJpaEntity entity = mapper.toJpa(observation,
                    hospitalizationEntity, createdByEntity);

            assertThat(entity.getId()).isEqualTo(observation.getId());
            assertThat(entity.getDescription()).isEqualTo(observation.getDescription());
            assertThat(entity.getCreatedDate()).isEqualTo(observation.getCreatedDate());
            assertThat(entity.isEnabled()).isEqualTo(observation.isEnabled());
            assertThat(entity.getHospitalization()).isSameAs(hospitalizationEntity);
            assertThat(entity.getCreatedBy()).isSameAs(createdByEntity);
        }
    }

    @Nested
    @DisplayName("toDomain con refs precargados — camino de escritura")
    class ToDomainConRefs {

        @Test
        @DisplayName("reconstruye el agregado sin tocar las asociaciones JPA")
        void reconstruye_el_agregado_sin_tocar_las_asociaciones() {
            HospitalizationObservationJpaEntity entity = mapper.toJpa(
                    HospitalizationObservationMother.observacionValida(), hospitalizationEntity,
                    createdByEntity);

            HospitalizationObservation observation = mapper.toDomain(entity,
                    HospitalizationObservationMother.HOSPITALIZACION,
                    HospitalizationObservationMother.VETERINARIO);

            assertThat(observation.getId())
                    .isEqualTo(HospitalizationObservationMother.OBSERVATION_ID);
            assertThat(observation.getHospitalization())
                    .isEqualTo(HospitalizationObservationMother.HOSPITALIZACION);
            assertThat(observation.getCreatedBy())
                    .isEqualTo(HospitalizationObservationMother.VETERINARIO);
        }

        @Test
        @DisplayName("la ida y vuelta dominio -> entidad -> dominio no pierde nada")
        void la_ida_y_vuelta_no_pierde_nada() {
            HospitalizationObservation original = HospitalizationObservationMother
                    .observacionValida();

            HospitalizationObservationJpaEntity entity = mapper.toJpa(original,
                    hospitalizationEntity, createdByEntity);
            HospitalizationObservation vuelta = mapper.toDomain(entity,
                    original.getHospitalization(), original.getCreatedBy());

            assertThat(vuelta).usingRecursiveComparison().isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("toDomain desde las asociaciones — camino de lectura")
    class ToDomainDesdeAsociaciones {

        @Test
        @DisplayName("construye la hospitalizacion y el empleado desde sus propias asociaciones hidratadas")
        void construye_los_refs_desde_sus_asociaciones() {
            when(hospitalizationEntity.getId())
                    .thenReturn(HospitalizationObservationMother.HOSPITALIZATION_ID);
            when(hospitalizationEntity.getDate()).thenReturn(LocalDate.of(2026, 3, 1));
            when(createdByEntity.getId()).thenReturn(HospitalizationObservationMother.EMPLOYEE_ID);
            when(createdByEntity.getEmployeeCode()).thenReturn("EMP-001");
            when(createdByEntity.getName()).thenReturn("Ana Ruiz");

            HospitalizationObservationJpaEntity entity = mapper.toJpa(
                    HospitalizationObservationMother.observacionValida(), hospitalizationEntity,
                    createdByEntity);

            HospitalizationObservation observation = mapper.toDomain(entity);

            assertThat(observation.getHospitalization())
                    .isEqualTo(HospitalizationObservationMother.HOSPITALIZACION);
            assertThat(observation.getCreatedBy())
                    .isEqualTo(HospitalizationObservationMother.VETERINARIO);
        }
    }
}
