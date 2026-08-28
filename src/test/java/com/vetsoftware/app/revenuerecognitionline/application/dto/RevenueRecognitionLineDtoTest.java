package com.vetsoftware.app.revenuerecognitionline.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.revenuerecognitionline.domain.RevenueRecognitionLine;
import com.vetsoftware.app.revenuerecognitionline.testsupport.RevenueRecognitionLineMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El mapeo campo a campo se prueba entero a proposito: un intercambio entre dos
 * campos del mismo tipo ({@code periodKey}/{@code postingPeriod}) compila, pasa
 * cualquier test de "no es null", y solo se ve en pantalla.
 */
@DisplayName("RevenueRecognitionLineDto.from")
class RevenueRecognitionLineDtoTest {

    @Test
    @DisplayName("copia cada campo del renglon en su posicion, incluido companyId")
    void copia_cada_campo_del_renglon_en_su_posicion() {
        RevenueRecognitionLine line = RevenueRecognitionLineMother.renglon();

        RevenueRecognitionLineDto dto = RevenueRecognitionLineDto.from(line);

        assertThat(dto.id()).isEqualTo(line.getId());
        assertThat(dto.companyId()).isEqualTo(line.getCompanyId());
        assertThat(dto.chargeId()).isEqualTo(line.getChargeId());
        assertThat(dto.periodKey()).isEqualTo(line.getPeriodKey());
        assertThat(dto.postingPeriod()).isEqualTo(line.getPostingPeriod());
        assertThat(dto.recognizedAmount()).isEqualByComparingTo(line.getRecognizedAmount());
        assertThat(dto.method()).isEqualTo(line.getMethod());
        assertThat(dto.createdDate()).isEqualTo(line.getCreatedDate());
    }

    @Test
    @DisplayName("un renglon que compensa conserva el importe negativo en el DTO")
    void un_renglon_que_compensa_conserva_el_importe_negativo() {
        RevenueRecognitionLine compensacion = RevenueRecognitionLineMother.compensacion();

        RevenueRecognitionLineDto dto = RevenueRecognitionLineDto.from(compensacion);

        assertThat(dto.recognizedAmount().signum()).isNegative();
    }
}
