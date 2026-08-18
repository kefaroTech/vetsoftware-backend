package com.vetsoftware.app.promotion.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.promotion.application.port.out.PromotionRepository;
import com.vetsoftware.app.promotion.domain.PromotionNotFoundException;
import com.vetsoftware.app.promotion.testsupport.PromotionMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeletePromotionService — borrado logico de una promocion")
class DeletePromotionServiceTest {

    @Mock
    private PromotionRepository repository;

    @InjectMocks
    private DeletePromotionService service;

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("borra la promocion encontrada en la empresa")
        void borra_la_promocion_encontrada() {
            when(repository.findByIdAndCompanyId(PromotionMother.PROMOTION_ID,
                    PromotionMother.COMPANY_ID)).thenReturn(Optional.of(PromotionMother.activa()));

            service.execute(PromotionMother.PROMOTION_ID, PromotionMother.COMPANY_ID);

            verify(repository).delete(PromotionMother.PROMOTION_ID);
        }

        @Test
        @DisplayName("sin empresa (SYSTEM) la lectura previa no se acota")
        void sin_empresa_la_lectura_no_se_acota() {
            when(repository.findById(PromotionMother.PROMOTION_ID))
                    .thenReturn(Optional.of(PromotionMother.activa()));

            service.execute(PromotionMother.PROMOTION_ID, null);

            verify(repository).delete(PromotionMother.PROMOTION_ID);
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("una promocion inexistente no llega a borrarse")
        void promocion_inexistente_no_llega_a_borrarse() {
            when(repository.findByIdAndCompanyId(99L, PromotionMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(99L, PromotionMother.COMPANY_ID))
                    .isInstanceOf(PromotionNotFoundException.class)
                    .hasMessageContaining("Promotion not found: 99");

            verify(repository, never()).delete(org.mockito.ArgumentMatchers.any());
        }
    }

    @Nested
    @DisplayName("aislamiento entre empresas")
    class Tenancy {

        @Test
        @DisplayName("la promocion de OTRA empresa es 404 y no se borra")
        void promocion_de_otra_empresa_es_not_found_y_no_borra() {
            when(repository.findByIdAndCompanyId(PromotionMother.PROMOTION_ID,
                    PromotionMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(PromotionMother.PROMOTION_ID, PromotionMother.COMPANY_ID))
                    .isInstanceOf(PromotionNotFoundException.class);

            verify(repository, never()).delete(org.mockito.ArgumentMatchers.any());
            verify(repository, never()).findById(org.mockito.ArgumentMatchers.any());
        }
    }
}
