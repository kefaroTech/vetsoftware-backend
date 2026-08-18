package com.vetsoftware.app.medicament.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.medicament.domain.CompanyRef;
import com.vetsoftware.app.medicament.domain.Medicament;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MedicamentDto")
class MedicamentDtoTest {

    @Test
    @DisplayName("from() copia los campos de un medicamento general (company nula)")
    void from_copia_los_campos_de_un_medicamento_general() {
        Medicament medicamento = Medicament.create("Amoxicilina", "Antibiotico", null, true);

        MedicamentDto dto = MedicamentDto.from(medicamento);

        assertThat(dto.name()).isEqualTo("Amoxicilina");
        assertThat(dto.description()).isEqualTo("Antibiotico");
        assertThat(dto.company()).isNull();
        assertThat(dto.general()).isTrue();
        assertThat(dto.enabled()).isTrue();
        assertThat(dto.createdDate()).isEqualTo(medicamento.getCreatedDate());
    }

    @Test
    @DisplayName("from() mapea la empresa a CompanySummaryDto cuando el medicamento es propio")
    void from_mapea_la_empresa_cuando_es_propio() {
        CompanyRef company = new CompanyRef(9L, "Clinica Norte", "900123456");
        Medicament medicamento = Medicament.create("Suero", null, company, false);

        MedicamentDto dto = MedicamentDto.from(medicamento);

        assertThat(dto.company()).isEqualTo(CompanySummaryDto.from(company));
        assertThat(dto.general()).isFalse();
    }
}
