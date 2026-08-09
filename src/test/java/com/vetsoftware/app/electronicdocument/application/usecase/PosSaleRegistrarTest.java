package com.vetsoftware.app.electronicdocument.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.electronicdocument.application.command.RegisterPosSaleCommand;
import com.vetsoftware.app.electronicdocument.application.command.RegisterPosSaleCommand.SaleLine;
import com.vetsoftware.app.electronicdocument.application.command.SaleLineKind;
import com.vetsoftware.app.electronicdocument.application.port.out.CashPort;
import com.vetsoftware.app.electronicdocument.application.port.out.InventoryLedgerPort;
import com.vetsoftware.app.electronicdocument.domain.CustomerSnapshot;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentLine;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import com.vetsoftware.app.electronicdocument.domain.IssuerSnapshot;
import com.vetsoftware.app.electronicdocument.domain.PaymentForm;
import com.vetsoftware.app.electronicdocument.domain.StockDiscountMismatchException;
import com.vetsoftware.app.electronicdocument.domain.TaxCategory;
import com.vetsoftware.app.electronicdocument.domain.TaxScheme;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * BE-01 — el invariante que faltaba: lo vendido y lo descontado del kardex
 * tienen que coincidir.
 *
 * <p>
 * El defecto original no fallaba nada: el ledger descartaba en silencio las
 * salidas que consideraba duplicadas y el inventario se iba sobrestimando
 * documento a documento. Estos tests fijan que ese silencio ya no existe.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PosSaleRegistrar — descuento de inventario de la venta POS")
class PosSaleRegistrarTest {

    private static final Long COMPANY = 9L;
    private static final Long BRANCH = 7L;
    private static final Long EMPLOYEE = 4L;
    private static final Long CHAMPU = 100L;
    private static final Long COLLAR = 200L;

    @Mock
    private PosSaleDocumentBuilder documentBuilder;
    @Mock
    private InventoryLedgerPort inventoryLedger;
    @Mock
    private CashPort cashPort;

    @InjectMocks
    private PosSaleRegistrar registrar;

    @Captor
    private ArgumentCaptor<Long> productCaptor;
    @Captor
    private ArgumentCaptor<Integer> quantityCaptor;

    private static SaleLine producto(Long productId, String cantidad) {
        return new SaleLine(SaleLineKind.PRODUCT, productId, "producto " + productId,
                new BigDecimal(cantidad), new BigDecimal("10000"));
    }

    private static SaleLine servicio() {
        return new SaleLine(SaleLineKind.SERVICE, 900L, "consulta", BigDecimal.ONE,
                new BigDecimal("50000"));
    }

    private static RegisterPosSaleCommand comando(SaleLine... lines) {
        return new RegisterPosSaleCommand(COMPANY, ElectronicDocumentType.FE_VENTA, true, null,
                List.of(lines), List.of(), "req-1", EMPLOYEE, BRANCH);
    }

    /** Documento sin pagos: aisla el descuento de stock del registro de caja. */
    private static ElectronicDocument documento() {
        return ElectronicDocument.createPending(
                COMPANY, null, ElectronicDocumentType.FE_VENTA, new IssuerSnapshot("NIT",
                        "900123456", "7", "Vet SAS", "RESPONSABLE", "vet@x.co", List.of("O-13")),
                CustomerSnapshot.finalConsumer(),
                List.of(new ElectronicDocumentLine(null, 1, "producto", BigDecimal.ONE, "94",
                        new BigDecimal("10000"), new BigDecimal("10000"), TaxCategory.GRAVADO,
                        TaxScheme.IVA, new BigDecimal("19"), new BigDecimal("1900"),
                        new BigDecimal("11900"))),
                List.of(), PaymentForm.CONTADO, false, null, null, null, new BigDecimal("49799"),
                "req-1", EMPLOYEE, BRANCH);
    }

    private void ventaConstruida() {
        when(documentBuilder.build(any())).thenReturn(documento());
    }

    /** El ledger descuenta exactamente lo que se le pide. */
    private void ledgerFiel() {
        when(inventoryLedger.recordPosSale(anyLong(), any(), anyLong(), anyInt(), any(), any()))
                .thenAnswer(inv -> inv.getArgument(3));
    }

    @Nested
    @DisplayName("agrupacion por producto")
    class Agrupacion {

        @Test
        @DisplayName("dos lineas del mismo producto se descuentan como una sola salida sumada")
        void dos_lineas_del_mismo_producto_se_suman() {
            // El ledger es idempotente por (documento, producto): sin agrupar, la
            // segunda linea se descartaba como duplicada y se cobraban dos unidades
            // descontando una.
            ventaConstruida();
            ledgerFiel();

            registrar.registerPending(comando(producto(CHAMPU, "1"), producto(CHAMPU, "1")));

            verify(inventoryLedger).recordPosSale(eq(COMPANY), any(), productCaptor.capture(),
                    quantityCaptor.capture(), any(), eq(EMPLOYEE));
            assertThat(productCaptor.getValue()).isEqualTo(CHAMPU);
            assertThat(quantityCaptor.getValue()).isEqualTo(2);
        }

        @Test
        @DisplayName("productos distintos generan una salida cada uno")
        void productos_distintos_generan_una_salida_cada_uno() {
            ventaConstruida();
            ledgerFiel();

            registrar.registerPending(comando(producto(CHAMPU, "2"), producto(COLLAR, "3")));

            verify(inventoryLedger, Mockito.times(2)).recordPosSale(eq(COMPANY), any(),
                    productCaptor.capture(), quantityCaptor.capture(), any(), eq(EMPLOYEE));
            assertThat(productCaptor.getAllValues()).containsExactly(CHAMPU, COLLAR);
            assertThat(quantityCaptor.getAllValues()).containsExactly(2, 3);
        }

        @Test
        @DisplayName("las lineas que no son producto no tocan el kardex")
        void las_lineas_que_no_son_producto_no_tocan_el_kardex() {
            ventaConstruida();

            registrar.registerPending(comando(servicio()));

            verifyNoInteractions(inventoryLedger);
        }
    }

    @Nested
    @DisplayName("invariante vendido == descontado")
    class Invariante {

        @Test
        @DisplayName("si el ledger descuenta de menos, la venta entera se deshace")
        void si_el_ledger_descuenta_de_menos_la_venta_se_deshace() {
            ventaConstruida();
            // Exactamente lo que hacia BE-01: el ledger ignora la salida y devuelve
            // cero unidades sin lanzar nada.
            when(inventoryLedger.recordPosSale(anyLong(), any(), eq(CHAMPU), anyInt(), any(),
                    any())).thenReturn(0);

            assertThatThrownBy(() -> registrar.registerPending(comando(producto(CHAMPU, "2"))))
                    .isInstanceOf(StockDiscountMismatchException.class)
                    .hasMessageContaining("producto " + CHAMPU)
                    .hasMessageContaining("se esperaban 2 unidades y se descontaron 0");
        }

        @Test
        @DisplayName("y no llega a registrar el cobro en caja")
        void y_no_llega_a_registrar_el_cobro_en_caja() {
            ventaConstruida();
            when(inventoryLedger.recordPosSale(anyLong(), any(), eq(CHAMPU), anyInt(), any(),
                    any())).thenReturn(0);

            assertThatThrownBy(() -> registrar.registerPending(comando(producto(CHAMPU, "2"))))
                    .isInstanceOf(StockDiscountMismatchException.class);

            verify(cashPort, never()).registerSale(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("descontar de mas tambien es un descuadre")
        void descontar_de_mas_tambien_es_un_descuadre() {
            ventaConstruida();
            when(inventoryLedger.recordPosSale(anyLong(), any(), eq(CHAMPU), anyInt(), any(),
                    any())).thenReturn(5);

            assertThatThrownBy(() -> registrar.registerPending(comando(producto(CHAMPU, "2"))))
                    .isInstanceOf(StockDiscountMismatchException.class)
                    .hasMessageContaining("se descontaron 5");
        }

        @Test
        @DisplayName("el segundo producto se verifica aunque el primero cuadre")
        void el_segundo_producto_se_verifica_aunque_el_primero_cuadre() {
            ventaConstruida();
            when(inventoryLedger.recordPosSale(anyLong(), any(), eq(CHAMPU), anyInt(), any(),
                    any())).thenReturn(2);
            when(inventoryLedger.recordPosSale(anyLong(), any(), eq(COLLAR), anyInt(), any(),
                    any())).thenReturn(0);

            assertThatThrownBy(() -> registrar
                    .registerPending(comando(producto(CHAMPU, "2"), producto(COLLAR, "3"))))
                    .isInstanceOf(StockDiscountMismatchException.class)
                    .hasMessageContaining("producto " + COLLAR);
        }
    }

    @Nested
    @DisplayName("orden de la transaccion")
    class Orden {

        @Test
        @DisplayName("la caja se valida antes de mover inventario")
        void la_caja_se_valida_antes_de_mover_inventario() {
            // Una caja cerrada tiene que abortar la venta sin dejar rastro en el kardex.
            ventaConstruida();
            ledgerFiel();

            registrar.registerPending(comando(producto(CHAMPU, "1")));

            InOrder orden = Mockito.inOrder(cashPort, inventoryLedger);
            orden.verify(cashPort).requireOpenSession(COMPANY, BRANCH, EMPLOYEE);
            orden.verify(inventoryLedger).recordPosSale(eq(COMPANY), any(), eq(CHAMPU), eq(1),
                    any(), eq(EMPLOYEE));
        }
    }

    @Nested
    @DisplayName("cantidades")
    class Cantidades {

        @Test
        @DisplayName("una cantidad fraccionaria se rechaza antes de tocar el kardex")
        void una_cantidad_fraccionaria_se_rechaza() {
            ventaConstruida();

            assertThatThrownBy(() -> registrar.registerPending(comando(producto(CHAMPU, "1.5"))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("debe ser entera");

            verifyNoInteractions(inventoryLedger);
        }
    }
}
