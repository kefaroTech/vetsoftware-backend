package com.vetsoftware.app.promotion.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.promotion.domain.Promotion;
import com.vetsoftware.app.promotion.testsupport.PromotionMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PromotionDto — proyeccion de Promotion")
class PromotionDtoTest {

    @Nested
    @DisplayName("from")
    class From {

        @Test
        @DisplayName("copia cada campo del dominio, campo por campo")
        void copia_cada_campo_del_dominio() {
            Promotion promotion = PromotionMother.activa();

            PromotionDto dto = PromotionDto.from(promotion);

            assertThat(dto.id()).isEqualTo(promotion.getId());
            assertThat(dto.name()).isEqualTo(promotion.getName());
            assertThat(dto.promotionType()).isEqualTo(promotion.getPromotionType());
            assertThat(dto.applicationType()).isEqualTo(promotion.getApplicationType());
            assertThat(dto.applicationItem()).isEqualTo(promotion.getApplicationItem());
            assertThat(dto.valueType()).isEqualTo(promotion.getValueType());
            assertThat(dto.value()).isEqualByComparingTo(promotion.getValue());
            assertThat(dto.startDate()).isEqualTo(promotion.getStartDate());
            assertThat(dto.endDate()).isEqualTo(promotion.getEndDate());
            assertThat(dto.promotionStatus()).isEqualTo(promotion.getPromotionStatus());
            assertThat(dto.createdDate()).isEqualTo(promotion.getCreatedDate());
            assertThat(dto.enabled()).isEqualTo(promotion.isEnabled());
        }

        @Test
        @DisplayName("con empresa presente mapea el resumen de la empresa")
        void con_empresa_presente_mapea_el_resumen() {
            Promotion promotion = PromotionMother.activa();

            PromotionDto dto = PromotionDto.from(promotion);

            assertThat(dto.company()).isEqualTo(CompanySummaryDto.from(PromotionMother.CLINICA));
        }

        // NOTA: la rama "promotion.getCompany() == null" de PromotionDto.from no es
        // alcanzable por un camino legitimo: Promotion.validate() exige company != null
        // en TODO constructor (incluido el compacto), asi que nunca existe un Promotion
        // real con company null que llegue hasta aqui. Es codigo defensivo muerto, no
        // un
        // caso de negocio; forzarlo requeriria reflection para saltarse el invariante
        // del
        // dominio, que es precisamente lo que la convencion prohibe. Ver informe final.
    }
}
