package com.vetsoftware.app.promotion.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.promotion.application.dto.PromotionDto;
import com.vetsoftware.app.promotion.application.port.out.PromotionRepository;
import com.vetsoftware.app.promotion.testsupport.PromotionMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListPromotionsService — listado de promociones por empresa")
class ListPromotionsServiceTest {

    @Mock
    private PromotionRepository repository;

    @InjectMocks
    private ListPromotionsService service;

    @Test
    @DisplayName("mapea cada promocion de la empresa a su dto")
    void mapea_cada_promocion_de_la_empresa_a_su_dto() {
        when(repository.findAllByCompanyId(PromotionMother.COMPANY_ID))
                .thenReturn(List.of(PromotionMother.activa()));

        List<PromotionDto> dtos = service.listByCompany(PromotionMother.COMPANY_ID);

        assertThat(dtos).hasSize(1);
        assertThat(dtos.get(0).id()).isEqualTo(PromotionMother.PROMOTION_ID);
        assertThat(dtos.get(0).company().id()).isEqualTo(PromotionMother.COMPANY_ID);
    }

    @Test
    @DisplayName("una empresa sin promociones devuelve una lista vacia")
    void una_empresa_sin_promociones_devuelve_lista_vacia() {
        when(repository.findAllByCompanyId(PromotionMother.OTRA_COMPANY_ID)).thenReturn(List.of());

        assertThat(service.listByCompany(PromotionMother.OTRA_COMPANY_ID)).isEmpty();
    }
}
