package com.vetsoftware.app.gatewaysettlement.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.gatewaysettlement.testsupport.GatewaySettlementMother;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Las invariantes del lote, que son el espejo en Java de los cuatro
 * {@code CHECK} del changeset 326.
 */
@DisplayName("GatewaySettlement — las invariantes del lote liquidado")
class GatewaySettlementTest {

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("un lote recien cargado nace sin factura del proveedor y sin entrada de banco")
        void un_lote_recien_cargado_nace_sin_soporte_y_sin_banco() {
            GatewaySettlement lote = GatewaySettlementMother.reciencargada();

            assertThat(lote.getId()).isNull();
            assertThat(lote.getProviderInvoiceRef()).isNull();
            assertThat(lote.getProviderTaxId()).isNull();
            assertThat(lote.hasProviderInvoice()).isFalse();
            assertThat(lote.getBankReceiptId()).isNull();
            assertThat(lote.getPaymentCount()).isEqualTo(37);
        }

        @Test
        @DisplayName("un lote sin cobros lo para chk_gateway_settlements_payment_count")
        void un_lote_sin_cobros_no_es_un_lote() {
            assertThatThrownBy(() -> GatewaySettlementMother.conCobros(0, "LOTE-VACIO"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("paymentCount");
        }

        @Test
        @DisplayName("una referencia con acento la rechaza el dominio, no el motor")
        void una_referencia_no_ascii_la_rechaza_el_dominio() {
            // La columna es CHARACTER SET ascii: el motor no trunca, RECHAZA con un
            // "Incorrect string value" que no dice de que fila viene. En una carga de
            // cien lotes eso es media tarde buscando cual.
            assertThatThrownBy(() -> GatewaySettlementMother.conReferencia("LOTE-MARZO-Nº1"))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("ASCII");
        }
    }

    @Nested
    @DisplayName("Los cinco importes")
    class LosCincoImportes {

        @Test
        @DisplayName("la liquidacion separa la comision de su impuesto")
        void la_liquidacion_separa_la_comision_de_su_impuesto() {
            // R-TAX-25 del documento maestro. Si el servicio de la pasarela resulta
            // excluido, ese impuesto no es descontable y se vuelve mayor valor del
            // gasto: sumado dentro de la comision ya no hay forma de saberlo.
            SettlementAmounts importes = GatewaySettlementMother.importes();

            assertThat(importes.fee()).isEqualByComparingTo("373524.00");
            assertThat(importes.feeTax()).isEqualByComparingTo("70969.56");
            assertThat(importes.fee()).isNotEqualByComparingTo(importes.feeTax());
        }

        @Test
        @DisplayName("el informe de margen incluye el gmf de cada liquidacion")
        void el_informe_de_margen_incluye_el_gmf_de_cada_liquidacion() {
            // R-TAX-24. El cuatro por mil vive en su propia columna y entra en el coste
            // total de cobrar: 408.000 al ano que sin esta suma no aparecen en ningun
            // informe de margen.
            SettlementAmounts importes = GatewaySettlementMother.importes();

            assertThat(importes.gmf()).isEqualByComparingTo("46423.10");
            assertThat(importes.totalCost()).isEqualByComparingTo("490916.66");
        }

        @Test
        @DisplayName("un neto que no cuadra con los otros cuatro lo para la identidad")
        void un_neto_que_no_cuadra_lo_para_la_identidad() {
            // Espejo de chk_gateway_settlements_net. Sin esta invariante el desajuste
            // saldria como un error del driver a mitad de una carga, sin decir que fila.
            assertThatThrownBy(() -> new SettlementAmounts(new BigDecimal("12450800.00"),
                    new BigDecimal("373524.00"), new BigDecimal("70969.56"),
                    new BigDecimal("46423.10"), new BigDecimal("11959883.35")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("11959883.34").hasMessageContaining("net");
        }

        @Test
        @DisplayName("un lote sin comision entra: el CHECK es fee >= 0, no fee > 0")
        void un_lote_sin_comision_entra() {
            // Una renegociacion o una promocion del proveedor dejan la comision en cero.
            // Un @Positive de mas aqui rechazaria una liquidacion perfectamente real.
            assertThatCode(() -> new SettlementAmounts(new BigDecimal("1000.00"), BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("1000.00")))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("un tercer decimal lo para el dominio: la base lo redondearia en silencio")
        void un_tercer_decimal_lo_para_el_dominio() {
            // DECIMAL(19,2) no es un error para MySQL: redondea y sigue. Un centavo
            // perdido asi rompe la identidad del neto y el cuadre no cierra sin que
            // nadie sepa por que.
            assertThatThrownBy(() -> new SettlementAmounts(new BigDecimal("1000.005"),
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("1000.005")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("2 decimals");
        }

        @Test
        @DisplayName("un neto no positivo NO se puede representar hoy: lo prohibe el CHECK")
        void un_neto_no_positivo_no_se_puede_representar() {
            // Congela una limitacion conocida del esquema, no una preferencia: un lote
            // con un contracargo que se lleva por delante el abono entero (neto <= 0) es
            // inexpresable mientras chk_gateway_settlements_amounts exija net > 0.
            // Admitirlo aqui no lo haria guardable: lo convertiria en un 500 del driver.
            // Si el negocio necesita ese caso, la salida es un changeset.
            assertThatThrownBy(() -> new SettlementAmounts(new BigDecimal("1000.00"),
                    new BigDecimal("1000.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("net");
        }
    }

    @Nested
    @DisplayName("El soporte del gasto")
    class ElSoporteDelGasto {

        @Test
        @DisplayName("una liquidacion sin provider_invoice_ref dispara la vigilancia de soportes")
        void una_liquidacion_sin_provider_invoice_ref_dispara_la_vigilancia_de_soportes() {
            // R-TAX-26. Sin el soporte, cinco millones al ano de gasto y casi un millon
            // de impuesto quedan expuestos a que los rechacen.
            assertThat(GatewaySettlementMother.reciencargada().hasProviderInvoice()).isFalse();
            assertThat(GatewaySettlementMother.conciliada(1L, 5L).hasProviderInvoice()).isTrue();
        }

        @Test
        @DisplayName("la factura y el NIT se escriben juntos")
        void la_factura_y_el_nit_se_escriben_juntos() {
            GatewaySettlement lote = GatewaySettlementMother.reciencargada();

            lote.attachProviderInvoice(GatewaySettlementMother.FACTURA_DEL_PROVEEDOR,
                    GatewaySettlementMother.NIT_DEL_PROVEEDOR);

            assertThat(lote.getProviderInvoiceRef())
                    .isEqualTo(GatewaySettlementMother.FACTURA_DEL_PROVEEDOR);
            assertThat(lote.getProviderTaxId())
                    .isEqualTo(GatewaySettlementMother.NIT_DEL_PROVEEDOR);
        }

        @Test
        @DisplayName("la factura sin el NIT no entra: el CHECK es un bicondicional")
        void la_factura_sin_el_nit_no_entra() {
            // Sin el NIT no se puede armar el reporte anual de terceros, asi que media
            // factura no es media ayuda: es una fila que la base rechaza.
            GatewaySettlement lote = GatewaySettlementMother.reciencargada();

            assertThatThrownBy(() -> lote.attachProviderInvoice("FE-1", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("providerTaxId");
        }

        @Test
        @DisplayName("un soporte ya escrito no se sobrescribe")
        void un_soporte_ya_escrito_no_se_sobrescribe() {
            // Cambiarlo no es corregir: el numero viejo desaparece sin quedar en ningun
            // sitio y el reporte de terceros deja de cuadrar con lo declarado.
            GatewaySettlement lote = GatewaySettlementMother.conciliada(70L, 5L);

            assertThatThrownBy(() -> lote.attachProviderInvoice("FE-OTRA", "800111222-3"))
                    .isInstanceOf(ProviderInvoiceAlreadyAttachedException.class)
                    .hasMessageContaining(GatewaySettlementMother.FACTURA_DEL_PROVEEDOR);
        }
    }

    @Nested
    @DisplayName("La entrada de banco")
    class LaEntradaDeBanco {

        @Test
        @DisplayName("atar el lote al extracto cierra la conciliacion")
        void atar_el_lote_al_extracto_cierra_la_conciliacion() {
            GatewaySettlement lote = GatewaySettlementMother.reciencargada();

            lote.linkBankReceipt(8750L);

            assertThat(lote.getBankReceiptId()).isEqualTo(8750L);
        }

        @Test
        @DisplayName("un lote ya atado no se reata a otra entrada")
        void un_lote_ya_atado_no_se_reata() {
            // Reatarlo deja la primera entrada cuadrada contra nada, y ese descuadre no
            // lo denuncia ninguna constraint: la FK sigue siendo valida.
            GatewaySettlement lote = GatewaySettlementMother.conciliada(70L, 8750L);

            assertThatThrownBy(() -> lote.linkBankReceipt(8751L))
                    .isInstanceOf(BankReceiptAlreadyLinkedException.class)
                    .hasMessageContaining("8750");
        }
    }

    @Nested
    @DisplayName("El contraste de cobros")
    class ElContrasteDeCobros {

        @Test
        @DisplayName("declarados y enlazados iguales: el lote cuadra")
        void declarados_y_enlazados_iguales_cuadran() {
            PaymentCountReconciliation contraste = GatewaySettlementMother.reciencargada()
                    .reconcileWith(37L);

            assertThat(contraste.isBalanced()).isTrue();
            assertThat(contraste.difference()).isZero();
        }

        @Test
        @DisplayName("declara 37 y hay 36: la diferencia es positiva y hay un pago perdido")
        void declara_37_y_hay_36() {
            PaymentCountReconciliation contraste = GatewaySettlementMother.reciencargada()
                    .reconcileWith(36L);

            assertThat(contraste.isBalanced()).isFalse();
            assertThat(contraste.difference()).isEqualTo(1L);
        }

        @Test
        @DisplayName("hay mas enlazados que declarados: la diferencia sale negativa")
        void hay_mas_enlazados_que_declarados() {
            // El otro signo es otro defecto: se ato un cobro que no es de este lote, o
            // la pasarela reenvio la liquidacion con otra cuenta. El dinero del lote no
            // cubre los cobros atados.
            PaymentCountReconciliation contraste = GatewaySettlementMother.reciencargada()
                    .reconcileWith(39L);

            assertThat(contraste.difference()).isEqualTo(-2L);
        }
    }
}
