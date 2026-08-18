package com.vetsoftware.app.supplierinvoice.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.supplierinvoice.domain.SupplierRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SupplierSummaryDto")
class SupplierSummaryDtoTest {

    @Test
    @DisplayName("from mapea id, nombre y NIT del proveedor")
    void from_mapea_id_nombre_y_nit() {
        SupplierSummaryDto dto = SupplierSummaryDto
                .from(new SupplierRef(7L, "Distribuidora Sur", "800111222"));

        assertThat(dto.id()).isEqualTo(7L);
        assertThat(dto.name()).isEqualTo("Distribuidora Sur");
        assertThat(dto.taxId()).isEqualTo("800111222");
    }
}
