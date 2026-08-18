package com.vetsoftware.app.laboratorytestfile.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.laboratorytest.infrastructure.persistence.LaboratoryTestJpaEntity;
import com.vetsoftware.app.laboratorytestfile.domain.LaboratoryTestFile;
import com.vetsoftware.app.laboratorytestfile.testsupport.LaboratoryTestFileMother;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El mapper es el unico punto que conoce dominio y entidad JPA a la vez. Las
 * entidades JPA de las features vecinas (empleado, examen de laboratorio) se
 * mockean: no tienen logica propia, son portadores de datos.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LaboratoryTestFileJpaMapper")
class LaboratoryTestFileJpaMapperTest {

    private final LaboratoryTestFileJpaMapper mapper = new LaboratoryTestFileJpaMapper();

    @Mock
    private EmployeeJpaEntity uploadedByEntity;
    @Mock
    private LaboratoryTestJpaEntity laboratoryTestEntity;

    @Nested
    @DisplayName("toJpa — dominio a entidad")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo escalar en su columna y engancha las dos asociaciones")
        void copia_cada_campo_y_engancha_las_asociaciones() {
            LaboratoryTestFile file = LaboratoryTestFileMother.archivoValido();

            LaboratoryTestFileJpaEntity entity = mapper.toJpa(file, uploadedByEntity,
                    laboratoryTestEntity);

            assertThat(entity.getId()).isEqualTo(file.getId());
            assertThat(entity.getStorageKey()).isEqualTo(file.getStorageKey());
            assertThat(entity.getBucket()).isEqualTo(file.getBucket());
            assertThat(entity.getOriginalFileName()).isEqualTo(file.getOriginalFileName());
            assertThat(entity.getContentType()).isEqualTo(file.getContentType());
            assertThat(entity.getSizeBytes()).isEqualTo(file.getSizeBytes());
            assertThat(entity.getETag()).isEqualTo(file.getETag());
            assertThat(entity.getCreatedDate()).isEqualTo(file.getCreatedDate());
            assertThat(entity.getUploadedBy()).isSameAs(uploadedByEntity);
            assertThat(entity.getLaboratoryTest()).isSameAs(laboratoryTestEntity);
        }
    }

    @Nested
    @DisplayName("toDomain con refs precargados — camino de escritura")
    class ToDomainConRefs {

        @Test
        @DisplayName("reconstruye el agregado sin tocar las asociaciones JPA")
        void reconstruye_el_agregado_sin_tocar_las_asociaciones() {
            LaboratoryTestFileJpaEntity entity = mapper.toJpa(
                    LaboratoryTestFileMother.archivoValido(), uploadedByEntity,
                    laboratoryTestEntity);

            LaboratoryTestFile file = mapper.toDomain(entity, LaboratoryTestFileMother.VETERINARIO,
                    LaboratoryTestFileMother.EXAMEN);

            assertThat(file.getId()).isEqualTo(LaboratoryTestFileMother.FILE_ID);
            assertThat(file.getUploadedBy()).isEqualTo(LaboratoryTestFileMother.VETERINARIO);
            assertThat(file.getLaboratoryTest()).isEqualTo(LaboratoryTestFileMother.EXAMEN);
        }

        @Test
        @DisplayName("la ida y vuelta dominio -> entidad -> dominio no pierde nada")
        void la_ida_y_vuelta_no_pierde_nada() {
            LaboratoryTestFile original = LaboratoryTestFileMother.archivoValido();

            LaboratoryTestFileJpaEntity entity = mapper.toJpa(original, uploadedByEntity,
                    laboratoryTestEntity);
            LaboratoryTestFile vuelta = mapper.toDomain(entity, original.getUploadedBy(),
                    original.getLaboratoryTest());

            assertThat(vuelta).usingRecursiveComparison().isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("toDomain desde las asociaciones — camino de lectura")
    class ToDomainDesdeAsociaciones {

        @Test
        @DisplayName("construye el empleado y el examen desde sus propias asociaciones hidratadas")
        void construye_los_refs_desde_sus_asociaciones() {
            when(uploadedByEntity.getId()).thenReturn(LaboratoryTestFileMother.EMPLOYEE_ID);
            when(uploadedByEntity.getEmployeeCode()).thenReturn("EMP-001");
            when(uploadedByEntity.getName()).thenReturn("Ana Ruiz");
            when(laboratoryTestEntity.getId())
                    .thenReturn(LaboratoryTestFileMother.LABORATORY_TEST_ID);
            when(laboratoryTestEntity.getDate()).thenReturn(LocalDate.of(2026, 1, 15));

            LaboratoryTestFileJpaEntity entity = mapper.toJpa(
                    LaboratoryTestFileMother.archivoValido(), uploadedByEntity,
                    laboratoryTestEntity);

            LaboratoryTestFile file = mapper.toDomain(entity);

            assertThat(file.getUploadedBy()).isEqualTo(LaboratoryTestFileMother.VETERINARIO);
            assertThat(file.getLaboratoryTest()).isEqualTo(LaboratoryTestFileMother.EXAMEN);
        }
    }
}
