package com.vetsoftware.app.promotion.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.promotion.application.dto.PromotionDto;
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
@DisplayName("ReactivatePromotionService — reactivacion de una promocion deshabilitada")
class ReactivatePromotionServiceTest {

    @Mock
    private PromotionRepository repository;

    @InjectMocks
    private ReactivatePromotionService service;

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("reactiva y devuelve la promocion releida")
        void reactiva_y_devuelve_la_promocion_releida() {
            when(repository.reactivate(PromotionMother.PROMOTION_ID, PromotionMother.COMPANY_ID))
                    .thenReturn(1);
            when(repository.findByIdAndCompanyId(PromotionMother.PROMOTION_ID,
                    PromotionMother.COMPANY_ID)).thenReturn(Optional.of(PromotionMother.activa()));

            PromotionDto dto = service.execute(PromotionMother.PROMOTION_ID,
                    PromotionMother.COMPANY_ID);

            assertThat(dto.id()).isEqualTo(PromotionMother.PROMOTION_ID);
            assertThat(dto.enabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("cero filas afectadas es una promocion inexistente: no vuelve a leer")
        void cero_filas_afectadas_no_vuelve_a_leer() {
            when(repository.reactivate(99L, PromotionMother.COMPANY_ID)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(99L, PromotionMother.COMPANY_ID))
                    .isInstanceOf(PromotionNotFoundException.class)
                    .hasMessageContaining("Promotion not found: 99");

            verify(repository, never()).findByIdAndCompanyId(org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any());
        }
    }

    @Nested
    @DisplayName("aislamiento entre empresas")
    class Tenancy {

        @Test
        @DisplayName("la promocion de OTRA empresa es 404 y no se reactiva")
        void promocion_de_otra_empresa_es_not_found_y_no_escribe() {
            // El company_id viaja dentro del UPDATE: es la unica barrera, porque aqui no
            // hay lectura previa que valide la propiedad. Cero filas afectadas.
            when(repository.reactivate(PromotionMother.PROMOTION_ID, PromotionMother.COMPANY_ID))
                    .thenReturn(0);

            assertThatThrownBy(
                    () -> service.execute(PromotionMother.PROMOTION_ID, PromotionMother.COMPANY_ID))
                    .isInstanceOf(PromotionNotFoundException.class);

            verify(repository, never()).findByIdAndCompanyId(org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any());
            verify(repository, never()).findById(org.mockito.ArgumentMatchers.any());
            verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
        }
    }
}
