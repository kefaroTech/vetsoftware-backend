package com.vetsoftware.app.electronicdocument.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Catalogos DIAN congelados en el documento. Los codigos NO son cosmeticos:
 * viajan literalmente en el XML UBL, asi que se fijan uno a uno. El recorrido
 * con {@code @EnumSource} es lo que detecta la constante nueva a la que se le
 * olvido el codigo.
 */
@DisplayName("Catalogos DIAN de la feature")
class DianCatalogEnumsTest {

    @Nested
    @DisplayName("conceptos de correccion")
    class ConceptosDeCorreccion {

        static Stream<Arguments> conceptosDeNotaCredito() {
            return Stream.of(Arguments.of(CreditNoteReason.DEVOLUCION, "1"),
                    Arguments.of(CreditNoteReason.ANULACION, "2"),
                    Arguments.of(CreditNoteReason.REBAJA, "3"),
                    Arguments.of(CreditNoteReason.AJUSTE_PRECIO, "4"),
                    Arguments.of(CreditNoteReason.OTROS, "5"));
        }

        @ParameterizedTest(name = "{0} → {1}")
        @MethodSource("conceptosDeNotaCredito")
        @DisplayName("el concepto de nota credito lleva su codigo del catalogo 2.5")
        void el_concepto_de_nota_credito_lleva_su_codigo(CreditNoteReason reason, String dianCode) {
            assertThat(reason.dianCode()).isEqualTo(dianCode);
        }

        static Stream<Arguments> conceptosDeNotaDebito() {
            return Stream.of(Arguments.of(DebitNoteReason.INTERESES, "1"),
                    Arguments.of(DebitNoteReason.GASTOS, "2"),
                    Arguments.of(DebitNoteReason.CAMBIO_VALOR, "3"),
                    Arguments.of(DebitNoteReason.OTROS, "4"));
        }

        @ParameterizedTest(name = "{0} → {1}")
        @MethodSource("conceptosDeNotaDebito")
        @DisplayName("el concepto de nota debito usa SU catalogo, distinto al de la nota credito")
        void el_concepto_de_nota_debito_usa_su_catalogo(DebitNoteReason reason, String dianCode) {
            assertThat(reason.dianCode()).isEqualTo(dianCode);
        }

        @ParameterizedTest
        @EnumSource(CreditNoteReason.class)
        @DisplayName("todo concepto de nota credito tiene descripcion para el XML")
        void todo_concepto_de_nota_credito_tiene_descripcion(CreditNoteReason reason) {
            assertThat(reason.description()).isNotBlank();
        }

        @ParameterizedTest
        @EnumSource(DebitNoteReason.class)
        @DisplayName("todo concepto de nota debito tiene descripcion para el XML")
        void todo_concepto_de_nota_debito_tiene_descripcion(DebitNoteReason reason) {
            assertThat(reason.description()).isNotBlank();
        }

        @Test
        @DisplayName("los codigos de nota credito no se repiten entre si")
        void los_codigos_de_nota_credito_no_se_repiten() {
            assertThat(Arrays.stream(CreditNoteReason.values()).map(CreditNoteReason::dianCode))
                    .doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("los codigos de nota debito no se repiten entre si")
        void los_codigos_de_nota_debito_no_se_repiten() {
            assertThat(Arrays.stream(DebitNoteReason.values()).map(DebitNoteReason::dianCode))
                    .doesNotHaveDuplicates();
        }
    }

    @Nested
    @DisplayName("medios y forma de pago")
    class MediosYFormaDePago {

        static Stream<Arguments> mediosDePago() {
            return Stream.of(Arguments.of(PaymentMeans.EFECTIVO, "10"),
                    Arguments.of(PaymentMeans.TRANSFERENCIA, "42"),
                    Arguments.of(PaymentMeans.TARJETA_DEBITO, "48"),
                    Arguments.of(PaymentMeans.TARJETA_CREDITO, "49"));
        }

        @ParameterizedTest(name = "{0} → {1}")
        @MethodSource("mediosDePago")
        @DisplayName("cada medio de pago lleva su codigo DIAN")
        void cada_medio_de_pago_lleva_su_codigo(PaymentMeans means, String dianCode) {
            assertThat(means.dianCode()).isEqualTo(dianCode);
        }

        @Test
        @DisplayName("los codigos de medio de pago son unicos")
        void los_codigos_de_medio_de_pago_son_unicos() {
            assertThat(Arrays.stream(PaymentMeans.values()).map(PaymentMeans::dianCode))
                    .doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("solo existe forma de pago CONTADO (codigo 1): no hay credito")
        void solo_existe_forma_de_pago_contado() {
            assertThat(PaymentForm.values()).containsExactly(PaymentForm.CONTADO);
            assertThat(PaymentForm.CONTADO.dianCode()).isEqualTo("1");
        }
    }

    @Nested
    @DisplayName("esquemas y categorias tributarias")
    class EsquemasTributarios {

        @Test
        @DisplayName("IVA es 01 e INC es 04, los dos unicos esquemas soportados")
        void iva_es_01_e_inc_es_04() {
            assertThat(TaxScheme.IVA.dianCode()).isEqualTo("01");
            assertThat(TaxScheme.INC.dianCode()).isEqualTo("04");
            assertThat(TaxScheme.values()).containsExactly(TaxScheme.IVA, TaxScheme.INC);
        }

        @Test
        @DisplayName("las cuatro clasificaciones tributarias del catalogo estan presentes")
        void las_cuatro_clasificaciones_estan_presentes() {
            assertThat(TaxCategory.values()).containsExactlyInAnyOrder(TaxCategory.GRAVADO,
                    TaxCategory.EXENTO, TaxCategory.EXCLUIDO, TaxCategory.INC);
        }

        @Test
        @DisplayName("los cuatro tipos de documento electronico del alcance estan presentes")
        void los_cuatro_tipos_de_documento_estan_presentes() {
            assertThat(ElectronicDocumentType.values()).containsExactlyInAnyOrder(
                    ElectronicDocumentType.FE_VENTA, ElectronicDocumentType.DOC_EQUIV_POS,
                    ElectronicDocumentType.NOTA_CREDITO, ElectronicDocumentType.NOTA_DEBITO);
        }

        @Test
        @DisplayName("los cinco estados DIAN del ciclo de vida estan presentes")
        void los_cinco_estados_dian_estan_presentes() {
            assertThat(DianStatus.values()).containsExactlyInAnyOrder(DianStatus.PENDIENTE,
                    DianStatus.VALIDADO, DianStatus.RECHAZADO, DianStatus.CONTINGENCIA,
                    DianStatus.NO_ELECTRONICO);
        }

        @Test
        @DisplayName("la bitacora distingue aceptado, rechazado, pendiente y error")
        void la_bitacora_distingue_los_cuatro_resultados() {
            assertThat(TransmissionResult.values()).containsExactlyInAnyOrder(
                    TransmissionResult.ACCEPTED, TransmissionResult.REJECTED,
                    TransmissionResult.PENDING, TransmissionResult.ERROR);
        }

        @Test
        @DisplayName("el webhook solo puede aceptar, rechazar o ignorar")
        void el_webhook_solo_acepta_rechaza_o_ignora() {
            assertThat(WebhookOutcome.values()).containsExactlyInAnyOrder(WebhookOutcome.ACCEPTED,
                    WebhookOutcome.REJECTED, WebhookOutcome.IGNORED);
        }
    }

    @Nested
    @DisplayName("regimen de IVA del adquiriente — parseo tolerante")
    class RegimenDeIva {

        @ParameterizedTest
        @EnumSource(TaxRegime.class)
        @DisplayName("recupera el regimen desde el nombre con el que se persistio")
        void recupera_el_regimen_desde_su_nombre(TaxRegime regime) {
            assertThat(TaxRegime.fromName(regime.name())).isEqualTo(regime);
        }

        @ParameterizedTest
        @EnumSource(TaxRegime.class)
        @DisplayName("tolera espacios alrededor del nombre persistido")
        void tolera_espacios_alrededor_del_nombre(TaxRegime regime) {
            assertThat(TaxRegime.fromName("  " + regime.name() + "  ")).isEqualTo(regime);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "REGIMEN_INVENTADO", "responsable_iva"})
        @DisplayName("un valor ausente o desconocido no rompe: es un documento antiguo sin el dato")
        void un_valor_desconocido_no_rompe(String persisted) {
            assertThat(TaxRegime.fromName(persisted)).isNull();
        }
    }

    @Nested
    @DisplayName("responsabilidad fiscal del adquiriente — parseo tolerante")
    class ResponsabilidadFiscal {

        @ParameterizedTest
        @EnumSource(FiscalResponsibility.class)
        @DisplayName("recupera la responsabilidad desde el nombre con el que se persistio")
        void recupera_la_responsabilidad_desde_su_nombre(FiscalResponsibility responsibility) {
            assertThat(FiscalResponsibility.fromName(responsibility.name()))
                    .isEqualTo(responsibility);
        }

        @ParameterizedTest
        @EnumSource(FiscalResponsibility.class)
        @DisplayName("tolera espacios alrededor del nombre persistido")
        void tolera_espacios_alrededor_del_nombre(FiscalResponsibility responsibility) {
            assertThat(FiscalResponsibility.fromName(" " + responsibility.name() + " "))
                    .isEqualTo(responsibility);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "O-13", "gran_contribuyente"})
        @DisplayName("un valor ausente o desconocido no rompe: es un documento antiguo sin el dato")
        void un_valor_desconocido_no_rompe(String persisted) {
            assertThat(FiscalResponsibility.fromName(persisted)).isNull();
        }

        @Test
        @DisplayName("cubre las cinco responsabilidades del RUT que el proveedor mapea")
        void cubre_las_cinco_responsabilidades_del_rut() {
            assertThat(FiscalResponsibility.values()).containsExactlyInAnyOrder(
                    FiscalResponsibility.NO_APLICA, FiscalResponsibility.GRAN_CONTRIBUYENTE,
                    FiscalResponsibility.AUTORRETENEDOR, FiscalResponsibility.AGENTE_RETENCION_IVA,
                    FiscalResponsibility.REGIMEN_SIMPLE);
        }
    }
}
