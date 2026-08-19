package com.vetsoftware.app.electronicdocument.domain;

import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.bd;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.customer;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.efectivo;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.issuer;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.unaLineaGravada;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * El constructor publico es la puerta por la que entra TODO documento
 * rehidratado de la base: el mapper JPA lo usa fila a fila. Si un campo fiscal
 * obligatorio pudiera llegar null por aqui, el documento roto no se detectaria
 * al leerlo sino al transmitirlo, ya delante del cliente.
 */
@DisplayName("ElectronicDocument — invariantes del constructor")
class ElectronicDocumentConstructorInvariantsTest {

    private static final LocalDate EMISION = LocalDate.of(2026, 3, 10);
    private static final String HORA = "10:15:00-05:00";

    /** Documento minimo valido; cada caso cambia UN campo para verlo rechazado. */
    private static ElectronicDocument documento(Long companyId, ElectronicDocumentType type,
            LocalDate issueDate, String issueTime, DianStatus status, IssuerSnapshot issuer,
            CustomerSnapshot customer, BigDecimal lineExtension, BigDecimal taxExclusive,
            BigDecimal taxInclusive, BigDecimal payable, PaymentForm paymentForm,
            List<ElectronicDocumentLine> lines, Long branchId) {
        return new ElectronicDocument(null, companyId, null, type, null, null, null, issueDate,
                issueTime, null, null, null, null, null, null, null, status, null, issuer, customer,
                lineExtension, taxExclusive, taxInclusive, payable, paymentForm, lines,
                efectivo("1190.00"), LocalDateTime.of(2026, 3, 10, 10, 15), null, true, null, null,
                null, false, null, null, null, null, null, branchId);
    }

    private static ElectronicDocument valido() {
        return documento(9L, ElectronicDocumentType.FE_VENTA, EMISION, HORA, DianStatus.PENDIENTE,
                issuer(), customer(), bd("1000.00"), bd("1000.00"), bd("1190.00"), bd("1190.00"),
                PaymentForm.CONTADO, unaLineaGravada(), 7L);
    }

    @Nested
    @DisplayName("campos fiscales obligatorios")
    class CamposObligatorios {

        static Stream<Arguments> camposAusentes() {
            return Stream.of(Arguments.of("companyId",
                    (ThrowingCallable) () -> documento(null, ElectronicDocumentType.FE_VENTA,
                            EMISION, HORA, DianStatus.PENDIENTE, issuer(), customer(),
                            bd("1000.00"), bd("1000.00"), bd("1190.00"), bd("1190.00"),
                            PaymentForm.CONTADO, unaLineaGravada(), 7L),
                    "companyId is required"),
                    Arguments.of("documentType",
                            (ThrowingCallable) () -> documento(9L, null, EMISION, HORA,
                                    DianStatus.PENDIENTE, issuer(), customer(), bd("1000.00"),
                                    bd("1000.00"), bd("1190.00"), bd("1190.00"),
                                    PaymentForm.CONTADO, unaLineaGravada(), 7L),
                            "documentType is required"),
                    Arguments.of("issueDate",
                            (ThrowingCallable) () -> documento(9L, ElectronicDocumentType.FE_VENTA,
                                    null, HORA, DianStatus.PENDIENTE, issuer(), customer(),
                                    bd("1000.00"), bd("1000.00"), bd("1190.00"), bd("1190.00"),
                                    PaymentForm.CONTADO, unaLineaGravada(), 7L),
                            "issueDate is required"),
                    Arguments.of("issueTime",
                            (ThrowingCallable) () -> documento(9L, ElectronicDocumentType.FE_VENTA,
                                    EMISION, null, DianStatus.PENDIENTE, issuer(), customer(),
                                    bd("1000.00"), bd("1000.00"), bd("1190.00"), bd("1190.00"),
                                    PaymentForm.CONTADO, unaLineaGravada(), 7L),
                            "issueTime is required"),
                    Arguments.of("dianStatus",
                            (ThrowingCallable) () -> documento(9L, ElectronicDocumentType.FE_VENTA,
                                    EMISION, HORA, null, issuer(), customer(), bd("1000.00"),
                                    bd("1000.00"), bd("1190.00"), bd("1190.00"),
                                    PaymentForm.CONTADO, unaLineaGravada(), 7L),
                            "dianStatus is required"),
                    Arguments.of("issuer",
                            (ThrowingCallable) () -> documento(9L, ElectronicDocumentType.FE_VENTA,
                                    EMISION, HORA, DianStatus.PENDIENTE, null, customer(),
                                    bd("1000.00"), bd("1000.00"), bd("1190.00"), bd("1190.00"),
                                    PaymentForm.CONTADO, unaLineaGravada(), 7L),
                            "issuer snapshot is required"),
                    Arguments.of("customer",
                            (ThrowingCallable) () -> documento(9L, ElectronicDocumentType.FE_VENTA,
                                    EMISION, HORA, DianStatus.PENDIENTE, issuer(), null,
                                    bd("1000.00"), bd("1000.00"), bd("1190.00"), bd("1190.00"),
                                    PaymentForm.CONTADO, unaLineaGravada(), 7L),
                            "customer snapshot is required"),
                    Arguments.of("lineExtensionAmount",
                            (ThrowingCallable) () -> documento(9L, ElectronicDocumentType.FE_VENTA,
                                    EMISION, HORA, DianStatus.PENDIENTE, issuer(), customer(), null,
                                    bd("1000.00"), bd("1190.00"), bd("1190.00"),
                                    PaymentForm.CONTADO, unaLineaGravada(), 7L),
                            "lineExtensionAmount is required"),
                    Arguments.of("taxExclusiveAmount",
                            (ThrowingCallable) () -> documento(9L, ElectronicDocumentType.FE_VENTA,
                                    EMISION, HORA, DianStatus.PENDIENTE, issuer(), customer(),
                                    bd("1000.00"), null, bd("1190.00"), bd("1190.00"),
                                    PaymentForm.CONTADO, unaLineaGravada(), 7L),
                            "taxExclusiveAmount is required"),
                    Arguments.of("taxInclusiveAmount",
                            (ThrowingCallable) () -> documento(9L, ElectronicDocumentType.FE_VENTA,
                                    EMISION, HORA, DianStatus.PENDIENTE, issuer(), customer(),
                                    bd("1000.00"), bd("1000.00"), null, bd("1190.00"),
                                    PaymentForm.CONTADO, unaLineaGravada(), 7L),
                            "taxInclusiveAmount is required"),
                    Arguments.of("payableAmount",
                            (ThrowingCallable) () -> documento(9L, ElectronicDocumentType.FE_VENTA,
                                    EMISION, HORA, DianStatus.PENDIENTE, issuer(), customer(),
                                    bd("1000.00"), bd("1000.00"), bd("1190.00"), null,
                                    PaymentForm.CONTADO, unaLineaGravada(), 7L),
                            "payableAmount is required"),
                    Arguments.of("paymentForm",
                            (ThrowingCallable) () -> documento(9L, ElectronicDocumentType.FE_VENTA,
                                    EMISION, HORA, DianStatus.PENDIENTE, issuer(), customer(),
                                    bd("1000.00"), bd("1000.00"), bd("1190.00"), bd("1190.00"),
                                    null, unaLineaGravada(), 7L),
                            "paymentForm is required"),
                    Arguments.of("lines null",
                            (ThrowingCallable) () -> documento(9L, ElectronicDocumentType.FE_VENTA,
                                    EMISION, HORA, DianStatus.PENDIENTE, issuer(), customer(),
                                    bd("1000.00"), bd("1000.00"), bd("1190.00"), bd("1190.00"),
                                    PaymentForm.CONTADO, null, 7L),
                            "a document requires at least one line"),
                    Arguments.of("lines vacias",
                            (ThrowingCallable) () -> documento(9L, ElectronicDocumentType.FE_VENTA,
                                    EMISION, HORA, DianStatus.PENDIENTE, issuer(), customer(),
                                    bd("1000.00"), bd("1000.00"), bd("1190.00"), bd("1190.00"),
                                    PaymentForm.CONTADO, List.of(), 7L),
                            "a document requires at least one line"),
                    Arguments.of("branchId",
                            (ThrowingCallable) () -> documento(9L, ElectronicDocumentType.FE_VENTA,
                                    EMISION, HORA, DianStatus.PENDIENTE, issuer(), customer(),
                                    bd("1000.00"), bd("1000.00"), bd("1190.00"), bd("1190.00"),
                                    PaymentForm.CONTADO, unaLineaGravada(), null),
                            "branchId is required"));
        }

        @ParameterizedTest(name = "sin {0}")
        @MethodSource("camposAusentes")
        @DisplayName("rechaza el documento cuando falta un campo fiscal obligatorio")
        void rechaza_el_documento_cuando_falta_un_campo(String campo, ThrowingCallable construccion,
                String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "\t"})
        @DisplayName("rechaza una hora de emision en blanco")
        void rechaza_una_hora_de_emision_en_blanco(String issueTime) {
            assertThatThrownBy(() -> documento(9L, ElectronicDocumentType.FE_VENTA, EMISION,
                    issueTime, DianStatus.PENDIENTE, issuer(), customer(), bd("1000.00"),
                    bd("1000.00"), bd("1190.00"), bd("1190.00"), PaymentForm.CONTADO,
                    unaLineaGravada(), 7L)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("issueTime is required");
        }

        @ParameterizedTest
        @EnumSource(DianStatus.class)
        @DisplayName("acepta cualquier estado DIAN al rehidratar desde la base")
        void acepta_cualquier_estado_dian(DianStatus status) {
            assertThatCode(() -> documento(9L, ElectronicDocumentType.FE_VENTA, EMISION, HORA,
                    status, issuer(), customer(), bd("1000.00"), bd("1000.00"), bd("1190.00"),
                    bd("1190.00"), PaymentForm.CONTADO, unaLineaGravada(), 7L))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("coherencia nota / factura")
    class CoherenciaNotaFactura {

        private static ElectronicDocument conReferenciaYConcepto(ElectronicDocumentType type,
                DocumentReference reference, String reasonCode) {
            return new ElectronicDocument(null, 9L, null, type, null, null, null, EMISION, HORA,
                    null, null, null, null, null, null, null, DianStatus.PENDIENTE, null, issuer(),
                    customer(), bd("1000.00"), bd("1000.00"), bd("1190.00"), bd("1190.00"),
                    PaymentForm.CONTADO, unaLineaGravada(), efectivo("1190.00"),
                    LocalDateTime.of(2026, 3, 10, 10, 15), null, true, reference, reasonCode,
                    "texto", false, null, null, null, null, null, 7L);
        }

        @ParameterizedTest
        @EnumSource(value = ElectronicDocumentType.class, names = {"NOTA_CREDITO", "NOTA_DEBITO"})
        @DisplayName("una nota sin referencia al documento corregido se rechaza")
        void una_nota_sin_referencia_se_rechaza(ElectronicDocumentType type) {
            assertThatThrownBy(() -> conReferenciaYConcepto(type, null, "2"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("a credit/debit note requires a document reference");
        }

        @ParameterizedTest
        @EnumSource(value = ElectronicDocumentType.class, names = {"NOTA_CREDITO", "NOTA_DEBITO"})
        @DisplayName("una nota sin concepto DIAN se rechaza")
        void una_nota_sin_concepto_se_rechaza(ElectronicDocumentType type) {
            DocumentReference referencia = new DocumentReference("CUFE-1", "SETP", 990L, EMISION);

            assertThatThrownBy(() -> conReferenciaYConcepto(type, referencia, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("a credit/debit note requires a reason code");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "\t"})
        @DisplayName("una nota con concepto en blanco se rechaza igual que sin concepto")
        void una_nota_con_concepto_en_blanco_se_rechaza(String reasonCode) {
            DocumentReference referencia = new DocumentReference("CUFE-1", "SETP", 990L, EMISION);

            assertThatThrownBy(() -> conReferenciaYConcepto(ElectronicDocumentType.NOTA_CREDITO,
                    referencia, reasonCode)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("a credit/debit note requires a reason code");
        }

        @ParameterizedTest
        @EnumSource(value = ElectronicDocumentType.class, names = {"FE_VENTA", "DOC_EQUIV_POS"})
        @DisplayName("una factura o un tiquete POS no pueden referenciar otro documento")
        void una_factura_no_puede_referenciar_otro_documento(ElectronicDocumentType type) {
            DocumentReference referencia = new DocumentReference("CUFE-1", "SETP", 990L, EMISION);

            assertThatThrownBy(() -> conReferenciaYConcepto(type, referencia, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("an invoice cannot reference another document");
        }

        @ParameterizedTest
        @EnumSource(value = ElectronicDocumentType.class, names = {"NOTA_CREDITO", "NOTA_DEBITO"})
        @DisplayName("isNote distingue las notas de las facturas")
        void isNote_distingue_las_notas(ElectronicDocumentType type) {
            DocumentReference referencia = new DocumentReference("CUFE-1", "SETP", 990L, EMISION);

            assertThat(conReferenciaYConcepto(type, referencia, "2").isNote()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(value = ElectronicDocumentType.class, names = {"FE_VENTA", "DOC_EQUIV_POS"})
        @DisplayName("una factura y un tiquete POS no son notas")
        void una_factura_no_es_nota(ElectronicDocumentType type) {
            assertThat(conReferenciaYConcepto(type, null, null).isNote()).isFalse();
        }
    }

    @Nested
    @DisplayName("normalizacion de campos opcionales")
    class Normalizacion {

        @Test
        @DisplayName("las retenciones null se normalizan a cero, nunca quedan null")
        void las_retenciones_null_se_normalizan_a_cero() {
            ElectronicDocument documento = valido();

            assertThat(documento.getReteFuenteAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(documento.getReteIvaAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(documento.getReteIcaAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("sin retenciones el neto a pagar es el total")
        void sin_retenciones_el_neto_es_el_total() {
            assertThat(valido().getNetPayableAmount()).isEqualByComparingTo("1190.00");
        }

        @Test
        @DisplayName("una lista de pagos null se normaliza a lista vacia")
        void una_lista_de_pagos_null_se_normaliza_a_vacia() {
            ElectronicDocument documento = new ElectronicDocument(null, 9L, null,
                    ElectronicDocumentType.FE_VENTA, null, null, null, EMISION, HORA, null, null,
                    null, null, null, null, null, DianStatus.PENDIENTE, null, issuer(), customer(),
                    bd("1000.00"), bd("1000.00"), bd("1190.00"), bd("1190.00"), PaymentForm.CONTADO,
                    unaLineaGravada(), null, LocalDateTime.of(2026, 3, 10, 10, 15), null, true,
                    null, null, null, false, null, null, null, null, null, 7L);

            assertThat(documento.getPayments()).isEmpty();
        }

        @Test
        @DisplayName("sin pagos registrados el medio predominante es EFECTIVO")
        void sin_pagos_el_medio_predominante_es_efectivo() {
            ElectronicDocument documento = new ElectronicDocument(null, 9L, null,
                    ElectronicDocumentType.FE_VENTA, null, null, null, EMISION, HORA, null, null,
                    null, null, null, null, null, DianStatus.PENDIENTE, null, issuer(), customer(),
                    bd("1000.00"), bd("1000.00"), bd("1190.00"), bd("1190.00"), PaymentForm.CONTADO,
                    unaLineaGravada(), null, LocalDateTime.of(2026, 3, 10, 10, 15), null, true,
                    null, null, null, false, null, null, null, null, null, 7L);

            assertThat(documento.primaryPaymentMeansCode())
                    .isEqualTo(PaymentMeans.EFECTIVO.dianCode());
        }

        @Test
        @DisplayName("mutar la lista de lineas original no altera el documento ya construido")
        void mutar_la_lista_original_no_altera_el_documento() {
            List<ElectronicDocumentLine> lineas = new ArrayList<>(unaLineaGravada());
            ElectronicDocument documento = documento(9L, ElectronicDocumentType.FE_VENTA, EMISION,
                    HORA, DianStatus.PENDIENTE, issuer(), customer(), bd("1000.00"), bd("1000.00"),
                    bd("1190.00"), bd("1190.00"), PaymentForm.CONTADO, lineas, 7L);

            lineas.clear();

            assertThat(documento.getLines()).hasSize(1);
        }

        @Test
        @DisplayName("el documento rehidratado conserva id, sede y actor fiscal")
        void el_documento_rehidratado_conserva_id_sede_y_actor() {
            ElectronicDocument documento = new ElectronicDocument(55L, 9L, 100L,
                    ElectronicDocumentType.FE_VENTA, "SETP", 990L, "18760000001", EMISION, HORA,
                    "CUFE-1", "CUDE-1", "uuid-1", "qr-data", "https://qr", "<xml/>", "s3/key.pdf",
                    DianStatus.VALIDADO, LocalDateTime.of(2026, 3, 10, 10, 16), issuer(),
                    customer(), bd("1000.00"), bd("1000.00"), bd("1190.00"), bd("1190.00"),
                    PaymentForm.CONTADO, unaLineaGravada(), efectivo("1190.00"),
                    LocalDateTime.of(2026, 3, 10, 10, 15), null, true, null, null, null, false,
                    bd("25.00"), bd("28.50"), bd("9.66"), "req-1", 4L, 7L);

            assertThat(documento.getId()).isEqualTo(55L);
            assertThat(documento.getCompanyId()).isEqualTo(9L);
            assertThat(documento.getOpenAccountId()).isEqualTo(100L);
            assertThat(documento.getBranchId()).isEqualTo(7L);
            assertThat(documento.getIssuedByEmployeeId()).isEqualTo(4L);
            assertThat(documento.getClientRequestId()).isEqualTo("req-1");
            assertThat(documento.getResolutionNumber()).isEqualTo("18760000001");
            assertThat(documento.getPdfRepresentation()).isEqualTo("s3/key.pdf");
            assertThat(documento.getXmlSigned()).isEqualTo("<xml/>");
            assertThat(documento.getQrData()).isEqualTo("qr-data");
            assertThat(documento.getQrUrl()).isEqualTo("https://qr");
            assertThat(documento.getUuid()).isEqualTo("uuid-1");
            assertThat(documento.getCude()).isEqualTo("CUDE-1");
            assertThat(documento.getIssueTime()).isEqualTo(HORA);
            assertThat(documento.isEnabled()).isTrue();
            assertThat(documento.getCreatedDate()).isEqualTo(LocalDateTime.of(2026, 3, 10, 10, 15));
            assertThat(documento.getNetPayableAmount()).isEqualByComparingTo("1126.84");
        }
    }
}
