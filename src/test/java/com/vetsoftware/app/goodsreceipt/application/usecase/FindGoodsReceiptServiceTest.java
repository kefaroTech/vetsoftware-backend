package com.vetsoftware.app.goodsreceipt.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.goodsreceipt.application.dto.GoodsReceiptDto;
import com.vetsoftware.app.goodsreceipt.application.port.out.GoodsReceiptRepository;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceiptNotFoundException;
import com.vetsoftware.app.goodsreceipt.testsupport.GoodsReceiptMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindGoodsReceiptService")
class FindGoodsReceiptServiceTest {

    @Mock
    private GoodsReceiptRepository repository;

    @InjectMocks
    private FindGoodsReceiptService service;

    @Nested
    @DisplayName("Consulta")
    class Consulta {

        @Test
        @DisplayName("devuelve el DTO de la recepcion cuando existe en la empresa")
        void devuelve_el_dto_de_la_recepcion() {
            when(repository.findByIdAndCompanyId(GoodsReceiptMother.RECEIPT_ID,
                    GoodsReceiptMother.COMPANY_ID))
                    .thenReturn(Optional.of(GoodsReceiptMother.enBorrador()));

            GoodsReceiptDto dto = service.findById(GoodsReceiptMother.RECEIPT_ID,
                    GoodsReceiptMother.COMPANY_ID);

            assertThat(dto.id()).isEqualTo(GoodsReceiptMother.RECEIPT_ID);
            assertThat(dto.company().id()).isEqualTo(GoodsReceiptMother.COMPANY_ID);
        }
    }

    @Nested
    @DisplayName("Fallos")
    class Fallos {

        @Test
        @DisplayName("lanza GoodsReceiptNotFoundException si no existe en la empresa")
        void lanza_excepcion_si_no_existe() {
            when(repository.findByIdAndCompanyId(GoodsReceiptMother.RECEIPT_ID,
                    GoodsReceiptMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(GoodsReceiptMother.RECEIPT_ID,
                    GoodsReceiptMother.COMPANY_ID))
                    .isInstanceOf(GoodsReceiptNotFoundException.class).hasMessageContaining(
                            "Goods receipt not found: " + GoodsReceiptMother.RECEIPT_ID);
        }
    }
}
