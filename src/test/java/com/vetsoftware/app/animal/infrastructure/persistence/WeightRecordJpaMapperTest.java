package com.vetsoftware.app.animal.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.animal.domain.WeightRecord;
import com.vetsoftware.app.animal.domain.WeightSource;
import com.vetsoftware.app.animal.domain.WeightType;
import com.vetsoftware.app.animal.testsupport.AnimalMother;
import com.vetsoftware.app.animal.testsupport.WeightRecordMother;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("WeightRecordJpaMapper")
class WeightRecordJpaMapperTest {

    private final WeightRecordJpaMapper mapper = new WeightRecordJpaMapper();

    @Mock
    private CompanyJpaEntity companyEntity;

    private AnimalJpaEntity animalEntity() {
        AnimalJpaEntity animal = new AnimalJpaEntity();
        animal.setId(AnimalMother.ANIMAL_ID);
        animal.setName("Firulais");
        animal.setCode("A-001");
        return animal;
    }

    @Nested
    @DisplayName("toJpa")
    class ToJpa {

        @Test
        @DisplayName("copia valor, unidad, fecha, origen y nota")
        void copia_valor_unidad_fecha_origen_y_nota() {
            WeightRecordJpaEntity entity = mapper.toJpa(WeightRecordMother.manual(), animalEntity(),
                    companyEntity);

            assertThat(entity.getId()).isEqualTo(WeightRecordMother.RECORD_ID);
            assertThat(entity.getValue()).isEqualByComparingTo("12.50");
            assertThat(entity.getUnit()).isEqualTo(WeightType.KILOGRAMS);
            assertThat(entity.getMeasuredAt()).isEqualTo(WeightRecordMother.MEDIDO_EL);
            assertThat(entity.getSource()).isEqualTo(WeightSource.MANUAL);
            assertThat(entity.getSourceId()).isNull();
            assertThat(entity.getNote()).isEqualTo("control de rutina");
            assertThat(entity.getCreatedDate()).isEqualTo(WeightRecordMother.CREADO);
            assertThat(entity.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("engancha animal y empresa en su slot")
        void engancha_animal_y_empresa_en_su_slot() {
            AnimalJpaEntity animal = animalEntity();

            WeightRecordJpaEntity entity = mapper.toJpa(WeightRecordMother.manual(), animal,
                    companyEntity);

            assertThat(entity.getAnimal()).isSameAs(animal);
            assertThat(entity.getCompany()).isSameAs(companyEntity);
        }

        @Test
        @DisplayName("conserva el sourceId de un registro clinico")
        void conserva_el_source_id_de_un_registro_clinico() {
            WeightRecordJpaEntity entity = mapper.toJpa(WeightRecordMother.deConsulta(88L),
                    animalEntity(), companyEntity);

            assertThat(entity.getSource()).isEqualTo(WeightSource.CONSULTATION);
            assertThat(entity.getSourceId()).isEqualTo(88L);
        }

        @Test
        @DisplayName("no altera la escala del valor")
        void no_altera_la_escala_del_valor() {
            WeightRecord record = WeightRecordMother.manual(new BigDecimal("12.50"),
                    WeightRecordMother.MEDIDO_EL);

            assertThat(
                    mapper.toJpa(record, animalEntity(), companyEntity).getValue().toPlainString())
                    .isEqualTo("12.50");
        }
    }

    @Nested
    @DisplayName("toDomain")
    class ToDomain {

        @Test
        @DisplayName("el camino de lectura arma el AnimalRef desde la asociacion")
        void el_camino_de_lectura_arma_el_animal_ref_desde_la_asociacion() {
            WeightRecordJpaEntity entity = mapper.toJpa(WeightRecordMother.manual(), animalEntity(),
                    companyEntity);
            entity.setCompany(companyJpaConDatos());

            WeightRecord record = mapper.toDomain(entity);

            assertThat(record.getAnimal().id()).isEqualTo(AnimalMother.ANIMAL_ID);
            assertThat(record.getAnimal().name()).isEqualTo("Firulais");
            assertThat(record.getAnimal().code()).isEqualTo("A-001");
            assertThat(record.getCompany()).isEqualTo(AnimalMother.CLINICA);
        }

        @Test
        @DisplayName("la ida y vuelta con refs precargados no pierde nada")
        void la_ida_y_vuelta_con_refs_precargados_no_pierde_nada() {
            WeightRecord original = WeightRecordMother.manual();

            WeightRecordJpaEntity entity = mapper.toJpa(original, animalEntity(), companyEntity);
            WeightRecord vuelta = mapper.toDomain(entity, original.getAnimal(),
                    original.getCompany());

            assertThat(vuelta).usingRecursiveComparison().isEqualTo(original);
        }

        private CompanyJpaEntity companyJpaConDatos() {
            CompanyJpaEntity company = org.mockito.Mockito.mock(CompanyJpaEntity.class);
            org.mockito.Mockito.when(company.getId()).thenReturn(AnimalMother.CLINICA.id());
            org.mockito.Mockito.when(company.getName()).thenReturn(AnimalMother.CLINICA.name());
            org.mockito.Mockito.when(company.getIdentifier())
                    .thenReturn(AnimalMother.CLINICA.identifier());
            return company;
        }
    }
}
