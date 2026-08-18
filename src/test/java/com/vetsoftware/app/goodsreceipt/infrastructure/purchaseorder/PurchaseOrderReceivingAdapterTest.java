package com.vetsoftware.app.goodsreceipt.infrastructure.purchaseorder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.vetsoftware.app.goodsreceipt.application.port.out.ReceivedLine;
import com.vetsoftware.app.purchaseorder.application.command.ApplyReceiptCommand;
import com.vetsoftware.app.purchaseorder.application.command.ReceivedPurchaseOrderLine;
import com.vetsoftware.app.purchaseorder.application.port.in.ReceivePurchaseOrderUseCase;
import java.util.List;
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
@DisplayName("PurchaseOrderReceivingAdapter")
class PurchaseOrderReceivingAdapterTest {

    private static final Long PURCHASE_ORDER_ID = 88L;
    private static final Long COMPANY_ID = 9L;
    private static final Long ACTOR_ID = 77L;

    @Mock
    private ReceivePurchaseOrderUseCase receivePurchaseOrder;

    @InjectMocks
    private PurchaseOrderReceivingAdapter adapter;

    @Captor
    private ArgumentCaptor<ApplyReceiptCommand> commandCaptor;

    @Nested
    @DisplayName("applyReceipt")
    class ApplyReceipt {

        @Test
        @DisplayName("traduce las lineas recibidas a lineas de orden de compra")
        void traduce_las_lineas_recibidas() {
            adapter.applyReceipt(PURCHASE_ORDER_ID, COMPANY_ID,
                    List.of(new ReceivedLine(900L, 4), new ReceivedLine(901L, 2)), ACTOR_ID);

            verify(receivePurchaseOrder).applyReceipt(commandCaptor.capture());
            ApplyReceiptCommand command = commandCaptor.getValue();
            assertThat(command.purchaseOrderId()).isEqualTo(PURCHASE_ORDER_ID);
            assertThat(command.companyId()).isEqualTo(COMPANY_ID);
            assertThat(command.actorId()).isEqualTo(ACTOR_ID);
            assertThat(command.lines()).containsExactly(new ReceivedPurchaseOrderLine(900L, 4),
                    new ReceivedPurchaseOrderLine(901L, 2));
        }
    }

    @Nested
    @DisplayName("revertReceipt")
    class RevertReceipt {

        @Test
        @DisplayName("traduce las lineas revertidas al mismo comando que aplicar")
        void traduce_las_lineas_revertidas() {
            adapter.revertReceipt(PURCHASE_ORDER_ID, COMPANY_ID, List.of(new ReceivedLine(900L, 4)),
                    ACTOR_ID);

            verify(receivePurchaseOrder).revertReceipt(commandCaptor.capture());
            assertThat(commandCaptor.getValue().lines())
                    .containsExactly(new ReceivedPurchaseOrderLine(900L, 4));
        }
    }
}
