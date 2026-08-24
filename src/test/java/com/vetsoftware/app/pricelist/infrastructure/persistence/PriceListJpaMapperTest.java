package com.vetsoftware.app.pricelist.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.pricelist.domain.PriceList;
import com.vetsoftware.app.pricelist.domain.PriceListStatus;
import com.vetsoftware.app.pricelist.testsupport.PriceListMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("PriceListJpaMapper — ida y vuelta dominio/entidad")
class PriceListJpaMapperTest {

    private final PriceListJpaMapper mapper = new PriceListJpaMapper();

    @Test
    @DisplayName("el viaje de ida y vuelta no pierde ni un campo de una lista publicada")
    void ida_y_vuelta_sin_perdidas() {
        PriceList original = PriceListMother.publicada();

        PriceList vuelta = mapper.toDomain(mapper.toJpa(original));

        assertThat(vuelta.getId()).isEqualTo(original.getId());
        assertThat(vuelta.getCode()).isEqualTo(original.getCode());
        assertThat(vuelta.getName()).isEqualTo(original.getName());
        assertThat(vuelta.getCurrency()).isEqualTo(original.getCurrency());
        assertThat(vuelta.getValidFrom()).isEqualTo(original.getValidFrom());
        assertThat(vuelta.getValidTo()).isEqualTo(original.getValidTo());
        assertThat(vuelta.getStatus()).isEqualTo(original.getStatus());
        assertThat(vuelta.getPublishedAt()).isEqualTo(original.getPublishedAt());
        assertThat(vuelta.getPublishedBySystemUserId())
                .isEqualTo(original.getPublishedBySystemUserId());
        assertThat(vuelta.getCreatedDate()).isEqualTo(original.getCreatedDate());
        assertThat(vuelta.getVersion()).isEqualTo(original.getVersion());
        assertThat(vuelta.isEnabled()).isEqualTo(original.isEnabled());
    }

    @ParameterizedTest
    @EnumSource(PriceListStatus.class)
    @DisplayName("cada estado sobrevive al viaje, incluido el que decide la inmutabilidad")
    void cada_estado_sobrevive(PriceListStatus estado) {
        PriceList original = PriceListMother.enEstado(estado);

        assertThat(mapper.toDomain(mapper.toJpa(original)).getStatus()).isEqualTo(estado);
    }

    @Test
    @DisplayName("una lista nueva viaja con id y version nulos, que es lo que espera IDENTITY")
    void lista_nueva_viaja_sin_id() {
        PriceListJpaEntity entity = mapper.toJpa(PriceListMother.nuevoBorrador());

        assertThat(entity.getId()).isNull();
        assertThat(entity.getVersion()).isNull();
        assertThat(entity.isEnabled()).isTrue();
        assertThat(entity.getStatus()).isEqualTo(PriceListStatus.DRAFT);
    }

    @Test
    @DisplayName("un valid_to vacío sigue vacío: es la marca de la lista vigente")
    void valid_to_vacio_sigue_vacio() {
        PriceListJpaEntity entity = mapper.toJpa(PriceListMother.borrador());

        assertThat(entity.getValidTo()).isNull();
        assertThat(mapper.toDomain(entity).getValidTo()).isNull();
    }
}
