package com.vetsoftware.app.goodsreceipt.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.goodsreceipt.application.port.out.GoodsReceiptRepository;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceiptNotFoundException;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceiptStatus;
import com.vetsoftware.app.goodsreceipt.domain.InvalidGoodsReceiptStatusTransitionException;
import com.vetsoftware.app.goodsreceipt.testsupport.GoodsReceiptMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteGoodsReceiptService")
class DeleteGoodsReceiptServiceTest {

    @Mock
    private GoodsReceiptRepository repository;

    @InjectMocks
    private DeleteGoodsReceiptService service;

    @Test
    @DisplayName("borra la recepcion cuando sigue en DRAFT")
    void borra_la_recepcion_en_borrador() {
        when(repository.findByIdAndCompanyId(GoodsReceiptMother.RECEIPT_ID,
                GoodsReceiptMother.COMPANY_ID))
                .thenReturn(Optional.of(GoodsReceiptMother.enBorrador()));

        service.execute(GoodsReceiptMother.RECEIPT_ID, GoodsReceiptMother.COMPANY_ID);

        verify(repository).delete(GoodsReceiptMother.RECEIPT_ID, GoodsReceiptMother.COMPANY_ID);
    }

    @Test
    @DisplayName("no borra nada si la recepcion es de otra empresa")
    void recepcion_de_otra_empresa() {
        when(repository.findByIdAndCompanyId(GoodsReceiptMother.RECEIPT_ID,
                GoodsReceiptMother.COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> service.execute(GoodsReceiptMother.RECEIPT_ID, GoodsReceiptMother.COMPANY_ID))
                .isInstanceOf(GoodsReceiptNotFoundException.class)
                .hasMessageContaining("Goods receipt not found: " + GoodsReceiptMother.RECEIPT_ID);

        verify(repository, never()).delete(anyLong(), anyLong());
    }

    @ParameterizedTest(name = "estado {0}")
    @EnumSource(value = GoodsReceiptStatus.class, names = "DRAFT", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("no borra una recepcion que ya movio inventario")
    void no_borra_fuera_de_borrador(GoodsReceiptStatus status) {
        when(repository.findByIdAndCompanyId(GoodsReceiptMother.RECEIPT_ID,
                GoodsReceiptMother.COMPANY_ID))
                .thenReturn(Optional.of(GoodsReceiptMother.conEstado(status)));

        assertThatThrownBy(
                () -> service.execute(GoodsReceiptMother.RECEIPT_ID, GoodsReceiptMother.COMPANY_ID))
                .isInstanceOf(InvalidGoodsReceiptStatusTransitionException.class)
                .hasMessageContaining("Only DRAFT goods receipts can be deleted")
                .hasMessageContaining(status.name());

        verify(repository, never()).delete(anyLong(), anyLong());
    }
}
