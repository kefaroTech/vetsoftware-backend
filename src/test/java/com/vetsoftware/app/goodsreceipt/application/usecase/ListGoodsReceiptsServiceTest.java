package com.vetsoftware.app.goodsreceipt.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.goodsreceipt.application.dto.GoodsReceiptDto;
import com.vetsoftware.app.goodsreceipt.application.port.out.GoodsReceiptRepository;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceiptStatus;
import com.vetsoftware.app.goodsreceipt.testsupport.GoodsReceiptMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListGoodsReceiptsService")
class ListGoodsReceiptsServiceTest {

    @Mock
    private GoodsReceiptRepository repository;

    @InjectMocks
    private ListGoodsReceiptsService service;

    @Nested
    @DisplayName("Listado")
    class Listado {

        @Test
        @DisplayName("mapea cada recepcion de la empresa a su DTO")
        void mapea_cada_recepcion_a_su_dto() {
            when(repository.findAllByCompanyId(GoodsReceiptMother.COMPANY_ID)).thenReturn(
                    List.of(GoodsReceiptMother.enBorrador(), GoodsReceiptMother.confirmada()));

            List<GoodsReceiptDto> dtos = service.listByCompany(GoodsReceiptMother.COMPANY_ID);

            assertThat(dtos).extracting(GoodsReceiptDto::status)
                    .containsExactly(GoodsReceiptStatus.DRAFT, GoodsReceiptStatus.CONFIRMED);
        }

        @Test
        @DisplayName("una empresa sin recepciones devuelve una lista vacia")
        void una_empresa_sin_recepciones_devuelve_lista_vacia() {
            when(repository.findAllByCompanyId(GoodsReceiptMother.COMPANY_ID))
                    .thenReturn(List.of());

            assertThat(service.listByCompany(GoodsReceiptMother.COMPANY_ID)).isEmpty();
        }
    }
}
