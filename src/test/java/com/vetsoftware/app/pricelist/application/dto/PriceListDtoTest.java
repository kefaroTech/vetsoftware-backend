package com.vetsoftware.app.pricelist.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.pricelist.domain.PriceList;
import com.vetsoftware.app.pricelist.domain.PriceListStatus;
import com.vetsoftware.app.pricelist.testsupport.PriceListMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PriceListDto")
class PriceListDtoTest {

    @Test
    @DisplayName("copia campo por campo una lista publicada, firma incluida")
    void copia_campo_por_campo() {
        PriceList lista = PriceListMother.publicada();

        PriceListDto dto = PriceListDto.from(lista);

        assertThat(dto.id()).isEqualTo(lista.getId());
        assertThat(dto.code()).isEqualTo(lista.getCode());
        assertThat(dto.name()).isEqualTo(lista.getName());
        assertThat(dto.currency()).isEqualTo(lista.getCurrency());
        assertThat(dto.validFrom()).isEqualTo(lista.getValidFrom());
        assertThat(dto.validTo()).isEqualTo(lista.getValidTo());
        assertThat(dto.status()).isEqualTo(PriceListStatus.PUBLISHED);
        assertThat(dto.publishedAt()).isEqualTo(lista.getPublishedAt());
        assertThat(dto.publishedBySystemUserId()).isEqualTo(lista.getPublishedBySystemUserId());
        assertThat(dto.createdDate()).isEqualTo(lista.getCreatedDate());
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("un borrador sale sin firma, no con una firma inventada")
    void el_borrador_sale_sin_firma() {
        PriceListDto dto = PriceListDto.from(PriceListMother.borrador());

        assertThat(dto.status()).isEqualTo(PriceListStatus.DRAFT);
        assertThat(dto.publishedAt()).isNull();
        assertThat(dto.publishedBySystemUserId()).isNull();
    }
}
