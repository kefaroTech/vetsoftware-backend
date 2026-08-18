package com.vetsoftware.app.clinicalhistory.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ClinicalEventDetail — detalle rico de un evento clínico")
class ClinicalEventDetailTest {

    @Test
    @DisplayName("of(...) construye el detalle sin tablas")
    void of_construye_sin_tablas() {
        List<DetailField> campos = List.of(new DetailField("Diagnóstico", "Sano"));

        ClinicalEventDetail detalle = ClinicalEventDetail.of("Consulta general", campos);

        assertThat(detalle.title()).isEqualTo("Consulta general");
        assertThat(detalle.fields()).isEqualTo(campos);
        assertThat(detalle.tables()).isEmpty();
    }

    @Test
    @DisplayName("el constructor completo sí acepta tablas")
    void constructor_completo_acepta_tablas() {
        List<DetailField> campos = List.of(new DetailField("Motivo", "Control"));
        List<DetailTable> tablas = List.of(new DetailTable("Medicamentos", List.of("Nombre"),
                List.of(List.of("Amoxicilina"))));

        ClinicalEventDetail detalle = new ClinicalEventDetail("Hospitalización", campos, tablas);

        assertThat(detalle.tables()).hasSize(1);
        assertThat(detalle.tables().get(0).title()).isEqualTo("Medicamentos");
    }
}
