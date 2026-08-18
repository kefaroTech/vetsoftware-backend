package com.vetsoftware.app.surgery.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.surgery.domain.Surgery;
import com.vetsoftware.app.surgery.testsupport.SurgeryMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El mapeo campo a campo se prueba entero a proposito: un intercambio entre dos
 * campos del mismo tipo compila, pasa cualquier test de "no es null", y solo se
 * ve en pantalla.
 */
@DisplayName("SurgeryDto.from")
class SurgeryDtoTest {

    @Test
    @DisplayName("copia cada campo escalar del agregado en su posicion")
    void copia_cada_campo_escalar_del_agregado_en_su_posicion() {
        Surgery surgery = SurgeryMother.cirugiaValida();

        SurgeryDto dto = SurgeryDto.from(surgery);

        assertThat(dto.id()).isEqualTo(SurgeryMother.SURGERY_ID);
        assertThat(dto.date()).isEqualTo(SurgeryMother.FECHA);
        assertThat(dto.description()).isEqualTo("Ovariohisterectomia electiva");
        assertThat(dto.medicament()).isEqualTo("Ketamina 10mg");
        assertThat(dto.observations()).isEqualTo("Recuperacion normal");
        assertThat(dto.complications()).isNull();
        assertThat(dto.createdDate()).isEqualTo(SurgeryMother.CREADO);
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("el estado viaja como el nombre del enum")
    void el_estado_viaja_como_el_nombre_del_enum() {
        SurgeryDto dto = SurgeryDto.from(SurgeryMother.cirugiaValida());

        assertThat(dto.status()).isEqualTo("PROGRAMADA");
    }

    @Test
    @DisplayName("aplana las referencias en summaries sin perder campos")
    void aplana_las_referencias_en_summaries() {
        SurgeryDto dto = SurgeryDto.from(SurgeryMother.cirugiaValida());

        assertThat(dto.surgeryType())
                .isEqualTo(SurgeryTypeSummaryDto.from(SurgeryMother.OVARIOHISTERECTOMIA));
        assertThat(dto.animal()).isEqualTo(AnimalSummaryDto.from(SurgeryMother.FIRULAIS));
        assertThat(dto.consultation())
                .isEqualTo(ConsultationSummaryDto.from(SurgeryMother.CONSULTA_PREVIA));
        assertThat(dto.company()).isEqualTo(CompanySummaryDto.from(SurgeryMother.CLINICA));
    }

    @Test
    @DisplayName("sin consulta asociada el summary viaja en null")
    void sin_consulta_asociada_el_summary_viaja_en_null() {
        SurgeryDto dto = SurgeryDto.from(SurgeryMother.cirugiaSinConsulta());

        assertThat(dto.consultation()).isNull();
    }

    @Test
    @DisplayName("propaga la cirugia deshabilitada")
    void propaga_la_cirugia_deshabilitada() {
        assertThat(SurgeryDto.from(SurgeryMother.cirugiaDeshabilitada()).enabled()).isFalse();
    }
}
