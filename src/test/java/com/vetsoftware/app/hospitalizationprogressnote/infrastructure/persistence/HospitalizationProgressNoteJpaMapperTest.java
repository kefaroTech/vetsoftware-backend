package com.vetsoftware.app.hospitalizationprogressnote.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.hospitalization.infrastructure.persistence.HospitalizationJpaEntity;
import com.vetsoftware.app.hospitalizationprogressnote.domain.HospitalizationProgressNote;
import com.vetsoftware.app.hospitalizationprogressnote.testsupport.HospitalizationProgressNoteMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El unico punto que conoce dominio y entidad JPA a la vez. Las entidades de
 * las otras features (hospitalization, employee) se mockean porque su
 * constructor sin argumentos es {@code protected} y no son instanciables desde
 * este paquete.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HospitalizationProgressNoteJpaMapper")
class HospitalizationProgressNoteJpaMapperTest {

    private final HospitalizationProgressNoteJpaMapper mapper = new HospitalizationProgressNoteJpaMapper();

    @Mock
    private HospitalizationJpaEntity hospitalizationEntity;
    @Mock
    private EmployeeJpaEntity employeeEntity;

    @Nested
    @DisplayName("toJpa — dominio a entidad")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo escalar y engancha las dos asociaciones")
        void copia_cada_campo_y_engancha_las_asociaciones() {
            HospitalizationProgressNote nota = HospitalizationProgressNoteMother.notaEvolucion();

            HospitalizationProgressNoteJpaEntity entity = mapper.toJpa(nota, hospitalizationEntity,
                    employeeEntity);

            assertThat(entity.getId()).isEqualTo(nota.getId());
            assertThat(entity.getDescription()).isEqualTo(nota.getDescription());
            assertThat(entity.getCreatedDate()).isEqualTo(nota.getCreatedDate());
            assertThat(entity.isEnabled()).isTrue();
            assertThat(entity.getHospitalization()).isSameAs(hospitalizationEntity);
            assertThat(entity.getCreatedBy()).isSameAs(employeeEntity);
        }

        @Test
        @DisplayName("conserva el estado deshabilitado")
        void conserva_el_estado_deshabilitado() {
            HospitalizationProgressNoteJpaEntity entity = mapper.toJpa(
                    HospitalizationProgressNoteMother.deshabilitada(), hospitalizationEntity,
                    employeeEntity);

            assertThat(entity.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("toDomain con refs precargados — camino de escritura")
    class ToDomainConRefs {

        @Test
        @DisplayName("reconstruye el agregado sin tocar las asociaciones JPA de la entidad")
        void reconstruye_el_agregado_sin_tocar_las_asociaciones() {
            // Este overload existe para no inicializar los proxies de getReferenceById:
            // si leyera entity.getHospitalization()/getCreatedBy(), Hibernate lanzaria un
            // SELECT extra. Por eso la entidad de origen se deja sin esas asociaciones.
            HospitalizationProgressNoteJpaEntity entity = new HospitalizationProgressNoteJpaEntity();
            entity.setId(HospitalizationProgressNoteMother.NOTE_ID);
            entity.setDescription("Paciente estable, buena respuesta al tratamiento");
            entity.setCreatedDate(HospitalizationProgressNoteMother.CREADO);
            entity.setEnabled(true);

            HospitalizationProgressNote nota = mapper.toDomain(entity,
                    HospitalizationProgressNoteMother.HOSPITALIZACION,
                    HospitalizationProgressNoteMother.VETERINARIO);

            assertThat(nota.getId()).isEqualTo(HospitalizationProgressNoteMother.NOTE_ID);
            assertThat(nota.getDescription())
                    .isEqualTo("Paciente estable, buena respuesta al tratamiento");
            assertThat(nota.getHospitalization())
                    .isEqualTo(HospitalizationProgressNoteMother.HOSPITALIZACION);
            assertThat(nota.getCreatedBy())
                    .isEqualTo(HospitalizationProgressNoteMother.VETERINARIO);
            assertThat(nota.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("la ida y vuelta dominio -> entidad -> dominio no pierde nada")
        void la_ida_y_vuelta_no_pierde_nada() {
            HospitalizationProgressNote original = HospitalizationProgressNoteMother
                    .notaEvolucion();

            HospitalizationProgressNoteJpaEntity entity = mapper.toJpa(original,
                    hospitalizationEntity, employeeEntity);
            HospitalizationProgressNote vuelta = mapper.toDomain(entity,
                    original.getHospitalization(), original.getCreatedBy());

            assertThat(vuelta).usingRecursiveComparison().isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("toDomain desde las asociaciones — camino de lectura")
    class ToDomainDesdeAsociaciones {

        @Test
        @DisplayName("construye cada companion VO desde su propia asociacion ya hidratada")
        void construye_cada_companion_vo_desde_su_asociacion() {
            when(hospitalizationEntity.getId())
                    .thenReturn(HospitalizationProgressNoteMother.HOSPITALIZACION.id());
            when(hospitalizationEntity.getDate())
                    .thenReturn(HospitalizationProgressNoteMother.HOSPITALIZACION.date());
            when(employeeEntity.getId())
                    .thenReturn(HospitalizationProgressNoteMother.VETERINARIO.id());
            when(employeeEntity.getEmployeeCode())
                    .thenReturn(HospitalizationProgressNoteMother.VETERINARIO.employeeCode());
            when(employeeEntity.getName())
                    .thenReturn(HospitalizationProgressNoteMother.VETERINARIO.name());

            HospitalizationProgressNoteJpaEntity entity = new HospitalizationProgressNoteJpaEntity();
            entity.setId(HospitalizationProgressNoteMother.NOTE_ID);
            entity.setDescription("Paciente estable, buena respuesta al tratamiento");
            entity.setHospitalization(hospitalizationEntity);
            entity.setCreatedBy(employeeEntity);
            entity.setCreatedDate(HospitalizationProgressNoteMother.CREADO);
            entity.setEnabled(true);

            HospitalizationProgressNote nota = mapper.toDomain(entity);

            assertThat(nota.getHospitalization())
                    .isEqualTo(HospitalizationProgressNoteMother.HOSPITALIZACION);
            assertThat(nota.getCreatedBy())
                    .isEqualTo(HospitalizationProgressNoteMother.VETERINARIO);
        }
    }
}
