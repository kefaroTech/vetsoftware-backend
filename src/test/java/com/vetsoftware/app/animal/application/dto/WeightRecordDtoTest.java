package com.vetsoftware.app.animal.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.animal.domain.WeightRecord;
import com.vetsoftware.app.animal.domain.WeightSource;
import com.vetsoftware.app.animal.domain.WeightType;
import com.vetsoftware.app.animal.testsupport.AnimalMother;
import com.vetsoftware.app.animal.testsupport.WeightRecordMother;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("WeightRecordDto.from")
class WeightRecordDtoTest {

    @Test
    @DisplayName("aplana el AnimalRef en tres campos sueltos sin cruzarlos")
    void aplana_el_animal_ref_en_tres_campos_sueltos() {
        WeightRecordDto dto = WeightRecordDto.from(WeightRecordMother.manual());

        assertThat(dto.animalId()).isEqualTo(AnimalMother.ANIMAL_ID);
        assertThat(dto.animalName()).isEqualTo("Firulais");
        assertThat(dto.animalCode()).isEqualTo("A-001");
    }

    @Test
    @DisplayName("copia el resto de campos del registro manual")
    void copia_el_resto_de_campos_del_registro_manual() {
        WeightRecord record = WeightRecordMother.manual();

        WeightRecordDto dto = WeightRecordDto.from(record);

        assertThat(dto.id()).isEqualTo(WeightRecordMother.RECORD_ID);
        assertThat(dto.value()).isEqualByComparingTo("12.50");
        assertThat(dto.unit()).isEqualTo(WeightType.KILOGRAMS);
        assertThat(dto.measuredAt()).isEqualTo(WeightRecordMother.MEDIDO_EL);
        assertThat(dto.source()).isEqualTo(WeightSource.MANUAL);
        assertThat(dto.sourceId()).isNull();
        assertThat(dto.note()).isEqualTo("control de rutina");
        assertThat(dto.createdDate()).isEqualTo(WeightRecordMother.CREADO);
    }

    @Test
    @DisplayName("conserva el origen clinico y su sourceId")
    void conserva_el_origen_clinico_y_su_source_id() {
        WeightRecordDto dto = WeightRecordDto.from(WeightRecordMother.deConsulta(88L));

        assertThat(dto.source()).isEqualTo(WeightSource.CONSULTATION);
        assertThat(dto.sourceId()).isEqualTo(88L);
        assertThat(dto.note()).isNull();
    }

    @Test
    @DisplayName("no altera la escala del valor: 12.50 no se convierte en 12.5")
    void no_altera_la_escala_del_valor() {
        WeightRecord record = WeightRecordMother.manual(new BigDecimal("12.50"),
                WeightRecordMother.MEDIDO_EL);

        assertThat(WeightRecordDto.from(record).value().toPlainString()).isEqualTo("12.50");
    }
}
