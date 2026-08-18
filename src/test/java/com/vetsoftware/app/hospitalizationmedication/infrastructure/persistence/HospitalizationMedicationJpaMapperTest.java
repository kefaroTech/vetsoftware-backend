package com.vetsoftware.app.hospitalizationmedication.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.hospitalization.infrastructure.persistence.HospitalizationJpaEntity;
import com.vetsoftware.app.hospitalizationmedication.domain.DurationMeasure;
import com.vetsoftware.app.hospitalizationmedication.domain.EmployeeRef;
import com.vetsoftware.app.hospitalizationmedication.domain.Frequency;
import com.vetsoftware.app.hospitalizationmedication.domain.GuidelineType;
import com.vetsoftware.app.hospitalizationmedication.domain.HospitalizationMedication;
import com.vetsoftware.app.hospitalizationmedication.testsupport.HospitalizationMedicationMother;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El mapper es el unico punto que conoce dominio y entidad JPA a la vez, asi
 * que un campo cruzado aqui no lo detecta ninguna otra capa.
 *
 * <p>
 * Las entidades JPA de las features vecinas (hospitalizacion, empleado) se
 * mockean: no tienen logica propia, son portadores de datos, y mockearlas no
 * oculta comportamiento.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HospitalizationMedicationJpaMapper")
class HospitalizationMedicationJpaMapperTest {

    private final HospitalizationMedicationJpaMapper mapper = new HospitalizationMedicationJpaMapper();

    @Mock
    private HospitalizationJpaEntity hospitalizationEntity;
    @Mock
    private EmployeeJpaEntity createdByEntity;
    @Mock
    private EmployeeJpaEntity suspensionByEntity;

    private HospitalizationMedicationJpaEntity entidadCompleta() {
        HospitalizationMedicationJpaEntity entity = new HospitalizationMedicationJpaEntity();
        entity.setId(HospitalizationMedicationMother.MEDICATION_ID);
        entity.setName("Amoxicilina 500mg");
        entity.setDose("1 tableta");
        entity.setFrequency("EVERY_8H");
        entity.setGuidelineType("INTERVAL");
        entity.setDurationMeasure("DAYS");
        entity.setDurationQuantity(5);
        entity.setStartDate(LocalDate.of(2026, 3, 1));
        entity.setStartTime(LocalTime.of(8, 0));
        entity.setNotes("Notas");
        entity.setCreatedDate(HospitalizationMedicationMother.CREADO);
        entity.setEnabled(true);
        return entity;
    }

    @Nested
    @DisplayName("toJpa — dominio a entidad")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo escalar en su columna")
        void copia_cada_campo_escalar_en_su_columna() {
            HospitalizationMedication medication = HospitalizationMedicationMother.activo();

            HospitalizationMedicationJpaEntity entity = mapper.toJpa(medication,
                    hospitalizationEntity, createdByEntity, null);

            assertThat(entity.getId()).isEqualTo(HospitalizationMedicationMother.MEDICATION_ID);
            assertThat(entity.getName()).isEqualTo("Amoxicilina 500mg");
            assertThat(entity.getDose()).isEqualTo("1 tableta");
            assertThat(entity.getFrequency()).isEqualTo("EVERY_8H");
            assertThat(entity.getGuidelineType()).isEqualTo("INTERVAL");
            assertThat(entity.getDurationMeasure()).isEqualTo("DAYS");
            assertThat(entity.getDurationQuantity()).isEqualTo(5);
            assertThat(entity.getStartDate()).isEqualTo(LocalDate.of(2026, 3, 1));
            assertThat(entity.getStartTime()).isEqualTo(LocalTime.of(8, 0));
            assertThat(entity.getNotes()).isEqualTo("Administrar con alimento");
            assertThat(entity.getCreatedDate()).isEqualTo(HospitalizationMedicationMother.CREADO);
            assertThat(entity.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("engancha las tres asociaciones en su slot")
        void engancha_las_tres_asociaciones_en_su_slot() {
            HospitalizationMedicationJpaEntity entity = mapper.toJpa(
                    HospitalizationMedicationMother.suspendido(), hospitalizationEntity,
                    createdByEntity, suspensionByEntity);

            assertThat(entity.getHospitalization()).isSameAs(hospitalizationEntity);
            assertThat(entity.getCreatedBy()).isSameAs(createdByEntity);
            assertThat(entity.getSuspensionBy()).isSameAs(suspensionByEntity);
        }

        @Test
        @DisplayName("sin suspension, la columna de suspensionBy queda null")
        void sin_suspension_la_columna_de_suspension_by_queda_null() {
            HospitalizationMedicationJpaEntity entity = mapper.toJpa(
                    HospitalizationMedicationMother.activo(), hospitalizationEntity,
                    createdByEntity, null);

            assertThat(entity.getSuspensionBy()).isNull();
            assertThat(entity.getSuspensionDate()).isNull();
        }

        @Test
        @DisplayName("los tres enums nulos se mapean a columnas null, no al literal NULL")
        void los_tres_enums_nulos_se_mapean_a_columnas_null() {
            HospitalizationMedicationJpaEntity entity = mapper.toJpa(
                    HospitalizationMedicationMother.sinDetallesOpcionales(), hospitalizationEntity,
                    createdByEntity, null);

            assertThat(entity.getFrequency()).isNull();
            assertThat(entity.getGuidelineType()).isNull();
            assertThat(entity.getDurationMeasure()).isNull();
        }
    }

    @Nested
    @DisplayName("toDomain con refs precargados — camino de escritura")
    class ToDomainConRefs {

        @Test
        @DisplayName("reconstruye el agregado sin tocar las asociaciones JPA")
        void reconstruye_el_agregado_sin_tocar_las_asociaciones() {
            // Este overload existe para no inicializar los proxies de getReferenceById:
            // si leyera entity.getHospitalization(), Hibernate lanzaria un SELECT extra.
            HospitalizationMedication medication = mapper.toDomain(entidadCompleta(),
                    HospitalizationMedicationMother.HOSPITALIZACION,
                    HospitalizationMedicationMother.CREADO_POR, null);

            assertThat(medication.getId()).isEqualTo(HospitalizationMedicationMother.MEDICATION_ID);
            assertThat(medication.getName()).isEqualTo("Amoxicilina 500mg");
            assertThat(medication.getFrequency()).isEqualTo(Frequency.EVERY_8H);
            assertThat(medication.getGuidelineType()).isEqualTo(GuidelineType.INTERVAL);
            assertThat(medication.getDurationMeasure()).isEqualTo(DurationMeasure.DAYS);
            assertThat(medication.getHospitalization())
                    .isEqualTo(HospitalizationMedicationMother.HOSPITALIZACION);
            assertThat(medication.getCreatedBy())
                    .isEqualTo(HospitalizationMedicationMother.CREADO_POR);
            assertThat(medication.getSuspensionBy()).isNull();
        }

        @Test
        @DisplayName("con suspensionByRef presente, tambien lo conserva")
        void con_suspension_by_ref_presente_tambien_lo_conserva() {
            HospitalizationMedication medication = mapper.toDomain(entidadCompleta(),
                    HospitalizationMedicationMother.HOSPITALIZACION,
                    HospitalizationMedicationMother.CREADO_POR,
                    HospitalizationMedicationMother.SUSPENDIDO_POR);

            assertThat(medication.getSuspensionBy())
                    .isEqualTo(HospitalizationMedicationMother.SUSPENDIDO_POR);
        }

        @Test
        @DisplayName("los tres enums null en la columna vuelven a null en el dominio")
        void los_tres_enums_null_en_la_columna_vuelven_a_null() {
            HospitalizationMedicationJpaEntity entity = entidadCompleta();
            entity.setFrequency(null);
            entity.setGuidelineType(null);
            entity.setDurationMeasure(null);

            HospitalizationMedication medication = mapper.toDomain(entity,
                    HospitalizationMedicationMother.HOSPITALIZACION,
                    HospitalizationMedicationMother.CREADO_POR, null);

            assertThat(medication.getFrequency()).isNull();
            assertThat(medication.getGuidelineType()).isNull();
            assertThat(medication.getDurationMeasure()).isNull();
        }

        @Test
        @DisplayName("la ida y vuelta dominio -> entidad -> dominio no pierde nada")
        void la_ida_y_vuelta_no_pierde_nada() {
            HospitalizationMedication original = HospitalizationMedicationMother.suspendido();

            HospitalizationMedicationJpaEntity entity = mapper.toJpa(original,
                    hospitalizationEntity, createdByEntity, suspensionByEntity);
            HospitalizationMedication vuelta = mapper.toDomain(entity,
                    original.getHospitalization(), original.getCreatedBy(),
                    original.getSuspensionBy());

            assertThat(vuelta).usingRecursiveComparison().isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("toDomain desde las asociaciones — camino de lectura")
    class ToDomainDesdeAsociaciones {

        @Test
        @DisplayName("construye la hospitalizacion y el creador desde sus propias asociaciones")
        void construye_los_refs_desde_sus_asociaciones() {
            when(hospitalizationEntity.getId())
                    .thenReturn(HospitalizationMedicationMother.HOSPITALIZATION_ID);
            when(hospitalizationEntity.getDate())
                    .thenReturn(HospitalizationMedicationMother.HOSPITALIZACION.date());
            when(createdByEntity.getId()).thenReturn(HospitalizationMedicationMother.EMPLOYEE_ID);
            when(createdByEntity.getEmployeeCode()).thenReturn("EMP-001");
            when(createdByEntity.getName()).thenReturn("Ana Ruiz");

            HospitalizationMedicationJpaEntity entity = entidadCompleta();
            entity.setHospitalization(hospitalizationEntity);
            entity.setCreatedBy(createdByEntity);
            entity.setSuspensionBy(null);

            HospitalizationMedication medication = mapper.toDomain(entity);

            assertThat(medication.getHospitalization())
                    .isEqualTo(HospitalizationMedicationMother.HOSPITALIZACION);
            assertThat(medication.getCreatedBy())
                    .isEqualTo(HospitalizationMedicationMother.CREADO_POR);
            assertThat(medication.getSuspensionBy()).isNull();
        }

        @Test
        @DisplayName("con suspensionBy presente, tambien construye ese ref")
        void con_suspension_by_presente_tambien_construye_ese_ref() {
            when(hospitalizationEntity.getId())
                    .thenReturn(HospitalizationMedicationMother.HOSPITALIZATION_ID);
            when(hospitalizationEntity.getDate())
                    .thenReturn(HospitalizationMedicationMother.HOSPITALIZACION.date());
            when(createdByEntity.getId()).thenReturn(HospitalizationMedicationMother.EMPLOYEE_ID);
            when(createdByEntity.getEmployeeCode()).thenReturn("EMP-001");
            when(createdByEntity.getName()).thenReturn("Ana Ruiz");
            when(suspensionByEntity.getId())
                    .thenReturn(HospitalizationMedicationMother.OTHER_EMPLOYEE_ID);
            when(suspensionByEntity.getEmployeeCode()).thenReturn("EMP-002");
            when(suspensionByEntity.getName()).thenReturn("Luis Paz");

            HospitalizationMedicationJpaEntity entity = entidadCompleta();
            entity.setHospitalization(hospitalizationEntity);
            entity.setCreatedBy(createdByEntity);
            entity.setSuspensionBy(suspensionByEntity);

            EmployeeRef suspensionBy = mapper.toDomain(entity).getSuspensionBy();

            assertThat(suspensionBy).isEqualTo(HospitalizationMedicationMother.SUSPENDIDO_POR);
        }
    }
}
