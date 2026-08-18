package com.vetsoftware.app.electronicdocument.infrastructure.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.inventory.application.command.RecordSaleCommand;
import com.vetsoftware.app.inventory.application.dto.StockConsumptionDto;
import com.vetsoftware.app.inventory.application.port.in.StockLedgerUseCase;
import com.vetsoftware.app.inventory.domain.StockReferenceType;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("LedgerInventoryAdapter — traduce la venta POS a movimientos de kardex")
class LedgerInventoryAdapterTest {

    @Mock
    private StockLedgerUseCase stockLedger;

    private LedgerInventoryAdapter adapter;

    @BeforeEach
    void montar() {
        adapter = new LedgerInventoryAdapter(stockLedger);
    }

    @Nested
    @DisplayName("recordPosSale")
    class RecordPosSale {

        @Test
        @DisplayName("suma las unidades realmente descontadas por lote, no lo pedido")
        void suma_las_unidades_descontadas_por_lote() {
            when(stockLedger.recordSale(any()))
                    .thenReturn(List.of(new StockConsumptionDto(1L, 3, new BigDecimal("1500")),
                            new StockConsumptionDto(2L, 2, new BigDecimal("1600"))));

            int discounted = adapter.recordPosSale(9L, 7L, 5L, 5, 100L, 4L);

            assertThat(discounted).isEqualTo(5);
            ArgumentCaptor<RecordSaleCommand> captor = ArgumentCaptor
                    .forClass(RecordSaleCommand.class);
            verify(stockLedger).recordSale(captor.capture());
            assertThat(captor.getValue()).isEqualTo(new RecordSaleCommand(9L, 7L, 5L, 5,
                    StockReferenceType.POS_DOCUMENT, 100L, 4L, true));
        }

        @Test
        @DisplayName("un ledger que trata la salida como duplicada devuelve cero, sin fingir exito")
        void ledger_duplicado_devuelve_cero() {
            when(stockLedger.recordSale(any())).thenReturn(List.of());

            int discounted = adapter.recordPosSale(9L, 7L, 5L, 5, 100L, 4L);

            assertThat(discounted).isZero();
        }
    }

    @Test
    @DisplayName("reversePosSale delega en el ledger con la referencia POS_DOCUMENT")
    void reverse_pos_sale_delega_con_referencia_pos_document() {
        adapter.reversePosSale(100L, 9L, 4L);

        verify(stockLedger).reverse(StockReferenceType.POS_DOCUMENT, 100L, 9L, 4L);
    }
}
