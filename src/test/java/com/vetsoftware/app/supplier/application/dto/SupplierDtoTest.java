package com.vetsoftware.app.supplier.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.supplier.domain.CompanyRef;
import com.vetsoftware.app.supplier.domain.Supplier;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SupplierDto.from — mapeo campo por campo")
class SupplierDtoTest {

    private static final CompanyRef CLINICA = new CompanyRef(10L, "Clinica Norte", "900123456");
    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);
    private static final LocalDateTime ACTUALIZADO = LocalDateTime.of(2026, 2, 1, 9, 0);

    @Test
    @DisplayName("mapea cada campo del proveedor completo, incluida la empresa resumida")
    void mapea_cada_campo_del_proveedor_completo() {
        Supplier supplier = new Supplier(55L, "Distribuidora Sur", "901555444-1", "Marta Gil",
                "3001234567", "compras@sur.test", "Calle 10 # 5-20", 30, "Entrega los martes",
                CLINICA, CREADO, ACTUALIZADO, 99L, 3L, true);

        SupplierDto dto = SupplierDto.from(supplier);

        assertThat(dto.id()).isEqualTo(55L);
        assertThat(dto.name()).isEqualTo("Distribuidora Sur");
        assertThat(dto.taxId()).isEqualTo("901555444-1");
        assertThat(dto.contactName()).isEqualTo("Marta Gil");
        assertThat(dto.phone()).isEqualTo("3001234567");
        assertThat(dto.email()).isEqualTo("compras@sur.test");
        assertThat(dto.address()).isEqualTo("Calle 10 # 5-20");
        assertThat(dto.paymentTermsDays()).isEqualTo(30);
        assertThat(dto.notes()).isEqualTo("Entrega los martes");
        assertThat(dto.company())
                .isEqualTo(new CompanySummaryDto(10L, "Clinica Norte", "900123456"));
        assertThat(dto.createdDate()).isEqualTo(CREADO);
        assertThat(dto.updatedDate()).isEqualTo(ACTUALIZADO);
        assertThat(dto.updatedBy()).isEqualTo(99L);
        assertThat(dto.version()).isEqualTo(3L);
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("los campos opcionales en null en el dominio quedan null en el dto")
    void los_campos_opcionales_en_null_quedan_null() {
        Supplier supplier = new Supplier(56L, "Insumos Norte", null, null, null, null, null, null,
                null, CLINICA, CREADO, null, null, null, false);

        SupplierDto dto = SupplierDto.from(supplier);

        assertThat(dto.taxId()).isNull();
        assertThat(dto.contactName()).isNull();
        assertThat(dto.phone()).isNull();
        assertThat(dto.email()).isNull();
        assertThat(dto.address()).isNull();
        assertThat(dto.paymentTermsDays()).isNull();
        assertThat(dto.notes()).isNull();
        assertThat(dto.updatedDate()).isNull();
        assertThat(dto.updatedBy()).isNull();
        assertThat(dto.version()).isNull();
        assertThat(dto.enabled()).isFalse();
    }
}
