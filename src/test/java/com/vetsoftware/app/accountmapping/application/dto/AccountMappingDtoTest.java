package com.vetsoftware.app.accountmapping.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.accountmapping.domain.AccountMapping;
import com.vetsoftware.app.accountmapping.testsupport.AccountMappingMother;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AccountMappingDto")
class AccountMappingDtoTest {

    @Test
    @DisplayName("from copia cada campo del dominio, campo por campo, para un mapeo sin afinado")
    void from_copia_cada_campo_sin_afinado() {
        AccountMapping mapping = AccountMappingMother.mapeoBancoAbierto();

        AccountMappingDto dto = AccountMappingDto.from(mapping);

        assertThat(dto.id()).isEqualTo(mapping.getId());
        assertThat(dto.mappingKind()).isEqualTo(mapping.getMappingKind());
        assertThat(dto.mappingKey()).isEqualTo(mapping.getMappingKey());
        assertThat(dto.catalogItemId()).isEqualTo(mapping.getCatalogItemId());
        assertThat(dto.chargeType()).isEqualTo(mapping.getChargeType());
        assertThat(dto.taxTreatment()).isEqualTo(mapping.getTaxTreatment());
        assertThat(dto.debitAccountCode()).isEqualTo(mapping.getDebitAccountCode());
        assertThat(dto.creditAccountCode()).isEqualTo(mapping.getCreditAccountCode());
        assertThat(dto.deferredAccountCode()).isEqualTo(mapping.getDeferredAccountCode());
        assertThat(dto.validFrom()).isEqualTo(mapping.getValidFrom());
        assertThat(dto.validTo()).isEqualTo(mapping.getValidTo());
        assertThat(dto.createdDate()).isEqualTo(mapping.getCreatedDate());
        assertThat(dto.enabled()).isEqualTo(mapping.isEnabled());
    }

    @Test
    @DisplayName("from propaga el articulo, el cargo y el tratamiento fiscal de un mapeo refinable")
    void from_propaga_el_afinado_completo() {
        AccountMapping mapping = AccountMappingMother.mapeoIngresoAbierto();

        AccountMappingDto dto = AccountMappingDto.from(mapping);

        assertThat(dto.catalogItemId()).isEqualTo(AccountMappingMother.CATALOG_ITEM_ID);
        assertThat(dto.chargeType()).isEqualTo("CONSULTA");
        assertThat(dto.taxTreatment()).isEqualTo("GRAVADO");
        assertThat(dto.deferredAccountCode()).isEqualTo(AccountMappingMother.DEFERRED_CODE);
    }

    @Test
    @DisplayName("from propaga la fecha de fin de un mapeo cerrado")
    void from_propaga_la_fecha_de_fin() {
        AccountMapping cerrado = AccountMappingMother.mapeoBancoCerrado(LocalDate.of(2026, 6, 1));

        AccountMappingDto dto = AccountMappingDto.from(cerrado);

        assertThat(dto.validTo()).isEqualTo(LocalDate.of(2026, 6, 1));
    }
}
