package com.vetsoftware.app.goodsreceipt.infrastructure.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.vetsoftware.app.inventory.application.command.RecordPurchaseCommand;
import com.vetsoftware.app.inventory.application.port.in.StockLedgerUseCase;
import com.vetsoftware.app.inventory.domain.StockReferenceType;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("LedgerInventoryAdapter (goodsreceipt)")
class LedgerInventoryAdapterTest {

    private static final Long COMPANY_ID = 9L;
    private static final Long BRANCH_ID = 4L;
    private static final Long PRODUCT_ID = 21L;
    private static final Long RECEIPT_ID = 500L;
    private static final Long ACTOR_ID = 77L;

    @Mock
    private StockLedgerUseCase stockLedger;

    @InjectMocks
    private LedgerInventoryAdapter adapter;

    @Captor
    private ArgumentCaptor<RecordPurchaseCommand> commandCaptor;

    @Nested
    @DisplayName("recordReceipt")
    class RecordReceipt {

        @Test
        @DisplayName("registra la entrada de compra con referencia GOODS_RECEIPT y el id de la recepcion")
        void registra_la_entrada_con_referencia_goods_receipt() {
            adapter.recordReceipt(COMPANY_ID, BRANCH_ID, PRODUCT_ID, "LOTE-A",
                    LocalDate.of(2027, 1, 31), 10, new BigDecimal("12.50"), RECEIPT_ID, ACTOR_ID);

            verify(stockLedger).recordPurchase(commandCaptor.capture());
            RecordPurchaseCommand command = commandCaptor.getValue();
            assertThat(command.companyId()).isEqualTo(COMPANY_ID);
            assertThat(command.branchId()).isEqualTo(BRANCH_ID);
            assertThat(command.productId()).isEqualTo(PRODUCT_ID);
            assertThat(command.lotNumber()).isEqualTo("LOTE-A");
            assertThat(command.quantity()).isEqualTo(10);
            assertThat(command.unitCost()).isEqualByComparingTo("12.50");
            assertThat(command.referenceType()).isEqualTo(StockReferenceType.GOODS_RECEIPT);
            assertThat(command.referenceId()).isEqualTo(RECEIPT_ID);
            assertThat(command.createdBy()).isEqualTo(ACTOR_ID);
        }
    }

    @Nested
    @DisplayName("reverseReceipt")
    class ReverseReceipt {

        @Test
        @DisplayName("revierte los movimientos de la recepcion con referencia GOODS_RECEIPT")
        void revierte_los_movimientos_de_la_recepcion() {
            adapter.reverseReceipt(RECEIPT_ID, COMPANY_ID, ACTOR_ID);

            verify(stockLedger).reverse(StockReferenceType.GOODS_RECEIPT, RECEIPT_ID, COMPANY_ID,
                    ACTOR_ID);
        }
    }
}
