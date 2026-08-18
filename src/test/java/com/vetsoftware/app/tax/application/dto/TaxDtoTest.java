package com.vetsoftware.app.tax.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.tax.domain.Tax;
import com.vetsoftware.app.tax.testsupport.TaxMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * La rama {@code company == null} de {@code from(...)} no tiene test: el
 * constructor de {@link Tax} exige {@code company} no nulo (ver
 * {@code TaxTest.Validaciones}), asi que hoy es defensiva e inalcanzable desde
 * dominio real. Forzarla exigiria un doble de {@code Tax}, prohibido por la
 * convencion de tests. Hueco declarado, no test vacio.
 */
@DisplayName("TaxDto")
class TaxDtoTest {

    @Nested
    @DisplayName("from")
    class From {

        @Test
        @DisplayName("copia cada campo del impuesto, incluida la empresa")
        void copia_cada_campo_del_impuesto() {
            Tax tax = TaxMother.inactivo();

            TaxDto dto = TaxDto.from(tax);

            assertThat(dto.id()).isEqualTo(tax.getId());
            assertThat(dto.name()).isEqualTo(tax.getName());
            assertThat(dto.percentage()).isEqualByComparingTo(tax.getPercentage());
            assertThat(dto.taxScheme()).isEqualTo(tax.getTaxScheme());
            assertThat(dto.company().id()).isEqualTo(tax.getCompany().id());
            assertThat(dto.createdDate()).isEqualTo(tax.getCreatedDate());
            assertThat(dto.updatedDate()).isEqualTo(tax.getUpdatedDate());
            assertThat(dto.updatedBy()).isEqualTo(tax.getUpdatedBy());
            assertThat(dto.version()).isEqualTo(tax.getVersion());
            assertThat(dto.enabled()).isEqualTo(tax.isEnabled());
        }
    }
}
