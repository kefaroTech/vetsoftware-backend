package com.vetsoftware.app.electronicdocument.application.dto;

import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.bd;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.customer;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.efectivo;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.exentoLine;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.excluidoLine;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.incLine;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.issuer;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.ivaLine;
import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.electronicdocument.application.dto.ElectronicDocumentDto.LineDto;
import com.vetsoftware.app.electronicdocument.application.dto.ElectronicDocumentDto.PaymentDto;
import com.vetsoftware.app.electronicdocument.application.dto.ElectronicDocumentDto.TaxTotalDto;
import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.DocumentReference;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentLine;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentPayment;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import com.vetsoftware.app.electronicdocument.domain.PaymentForm;
import com.vetsoftware.app.electronicdocument.domain.PaymentMeans;
import com.vetsoftware.app.electronicdocument.domain.TaxCategory;
import com.vetsoftware.app.electronicdocument.domain.TaxScheme;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Proyeccion de salida del documento. Se comprueba campo por campo porque los
 * cruces (emisor con datos del adquiriente, base con impuesto, prefijo con
 * resolucion) son invisibles en un test de "no es null" y muy visibles en la
 * factura que recibe el cliente.
 */
@DisplayName("ElectronicDocumentDto — proyeccion del documento")
class ElectronicDocumentDtoTest {

    private static final LocalDate EMISION = LocalDate.of(2026, 3, 10);
    private static final LocalDateTime CREACION = LocalDateTime.of(2026, 3, 10, 10, 15);
    private static final LocalDateTime VALIDACION = LocalDateTime.of(2026, 3, 10, 10, 30);

    private static ElectronicDocument documento(ElectronicDocumentType type,
            List<ElectronicDocumentLine> lines, List<ElectronicDocumentPayment> payments,
            DocumentReference reference, String reasonCode, String reasonText, boolean reversed) {
        return new ElectronicDocument(55L, 9L, 100L, type, "SETP", 990L, "18760000001", EMISION,
                "10:15:00-05:00", "CUFE-1", "CUDE-1", "uuid-1", "qr-data", "https://qr/1", "<xml/>",
                "invoices/9/55/SETP990.pdf", DianStatus.VALIDADO, VALIDACION, issuer(), customer(),
                bd("1000.00"), bd("1000.00"), bd("1190.00"), bd("1190.00"), PaymentForm.CONTADO,
                lines, payments, CREACION, true, reference, reasonCode, reasonText, reversed,
                bd("25.00"), bd("28.50"), bd("9.66"), "req-1", 4L, 7L);
    }

    private static ElectronicDocument factura() {
        return documento(ElectronicDocumentType.FE_VENTA,
                List.of(ivaLine(1, "1000.00", "19", "190.00")), efectivo("1190.00"), null, null,
                null, false);
    }

    @Nested
    @DisplayName("cabecera")
    class Cabecera {

        @Test
        @DisplayName("copia identificadores, numeracion y fechas del documento")
        void copia_identificadores_numeracion_y_fechas() {
            ElectronicDocumentDto dto = ElectronicDocumentDto.from(factura());

            assertThat(dto.id()).isEqualTo(55L);
            assertThat(dto.companyId()).isEqualTo(9L);
            assertThat(dto.openAccountId()).isEqualTo(100L);
            assertThat(dto.branchId()).isEqualTo(7L);
            assertThat(dto.documentType()).isEqualTo(ElectronicDocumentType.FE_VENTA);
            assertThat(dto.prefix()).isEqualTo("SETP");
            assertThat(dto.consecutive()).isEqualTo(990L);
            assertThat(dto.issueDate()).isEqualTo(EMISION);
            assertThat(dto.issueTime()).isEqualTo("10:15:00-05:00");
            assertThat(dto.createdDate()).isEqualTo(CREACION);
            assertThat(dto.enabled()).isTrue();
        }

        @Test
        @DisplayName("copia los sellos DIAN y el estado de validacion")
        void copia_los_sellos_dian() {
            ElectronicDocumentDto dto = ElectronicDocumentDto.from(factura());

            assertThat(dto.cufe()).isEqualTo("CUFE-1");
            assertThat(dto.cude()).isEqualTo("CUDE-1");
            assertThat(dto.uuid()).isEqualTo("uuid-1");
            assertThat(dto.qrUrl()).isEqualTo("https://qr/1");
            assertThat(dto.dianStatus()).isEqualTo(DianStatus.VALIDADO);
            assertThat(dto.dianValidationDate()).isEqualTo(VALIDACION);
        }

        @Test
        @DisplayName("no expone el XML firmado ni la clave del PDF: no son datos de pantalla")
        void no_expone_el_xml_firmado_ni_la_clave_del_pdf() {
            assertThat(ElectronicDocumentDto.class.getRecordComponents()).extracting("name")
                    .doesNotContain("xmlSigned", "pdfRepresentation", "qrData")
                    .contains("qrUrl", "cufe", "cude");
        }
    }

    @Nested
    @DisplayName("emisor y adquiriente")
    class EmisorYAdquiriente {

        @Test
        @DisplayName("el emisor sale con SUS datos, no con los del adquiriente")
        void el_emisor_sale_con_sus_datos() {
            ElectronicDocumentDto.IssuerDto emisor = ElectronicDocumentDto.from(factura()).issuer();

            assertThat(emisor.documentType()).isEqualTo("NIT");
            assertThat(emisor.documentId()).isEqualTo("900123456");
            assertThat(emisor.verificationDigit()).isEqualTo("7");
            assertThat(emisor.legalName()).isEqualTo("Veterinaria Vet SAS");
            assertThat(emisor.taxRegime()).isEqualTo("RESPONSABLE");
            assertThat(emisor.email()).isEqualTo("facturacion@vet.co");
        }

        @Test
        @DisplayName("el adquiriente sale con SUS datos, no con los del emisor")
        void el_adquiriente_sale_con_sus_datos() {
            ElectronicDocumentDto.CustomerDto adquiriente = ElectronicDocumentDto.from(factura())
                    .customer();

            assertThat(adquiriente.documentType()).isEqualTo("CEDULA_CIUDADANIA");
            assertThat(adquiriente.documentId()).isEqualTo("1020304050");
            assertThat(adquiriente.verificationDigit()).isEqualTo("3");
            assertThat(adquiriente.personType()).isEqualTo("NATURAL");
            assertThat(adquiriente.legalName()).isEqualTo("Ana Maria Perez");
            assertThat(adquiriente.name()).isEqualTo("Ana Perez");
            assertThat(adquiriente.email()).isEqualTo("ana@correo.co");
        }
    }

    @Nested
    @DisplayName("totales y retenciones")
    class TotalesYRetenciones {

        @Test
        @DisplayName("copia los cuatro totales legales sin recalcularlos")
        void copia_los_cuatro_totales_legales() {
            ElectronicDocumentDto dto = ElectronicDocumentDto.from(factura());

            assertThat(dto.lineExtensionAmount()).isEqualByComparingTo("1000.00");
            assertThat(dto.taxExclusiveAmount()).isEqualByComparingTo("1000.00");
            assertThat(dto.taxInclusiveAmount()).isEqualByComparingTo("1190.00");
            assertThat(dto.payableAmount()).isEqualByComparingTo("1190.00");
        }

        @Test
        @DisplayName("copia las tres retenciones sin mezclarlas entre si")
        void copia_las_tres_retenciones_sin_mezclarlas() {
            ElectronicDocumentDto dto = ElectronicDocumentDto.from(factura());

            assertThat(dto.reteFuenteAmount()).isEqualByComparingTo("25.00");
            assertThat(dto.reteIvaAmount()).isEqualByComparingTo("28.50");
            assertThat(dto.reteIcaAmount()).isEqualByComparingTo("9.66");
        }

        @Test
        @DisplayName("el neto a pagar es el total menos las tres retenciones")
        void el_neto_a_pagar_descuenta_las_retenciones() {
            assertThat(ElectronicDocumentDto.from(factura()).netPayableAmount())
                    .isEqualByComparingTo("1126.84");
        }

        @Test
        @DisplayName("copia la forma de pago")
        void copia_la_forma_de_pago() {
            assertThat(ElectronicDocumentDto.from(factura()).paymentForm())
                    .isEqualTo(PaymentForm.CONTADO);
        }
    }

    @Nested
    @DisplayName("lineas")
    class Lineas {

        @Test
        @DisplayName("proyecta cada linea campo por campo")
        void proyecta_cada_linea_campo_por_campo() {
            ElectronicDocumentLine linea = new ElectronicDocumentLine(88L, 3, "Vacuna",
                    new BigDecimal("2"), "94", new BigDecimal("500.00"), bd("1000.00"),
                    TaxCategory.GRAVADO, TaxScheme.IVA, new BigDecimal("19"), bd("190.00"),
                    bd("1190.00"));

            LineDto dto = ElectronicDocumentDto.from(documento(ElectronicDocumentType.FE_VENTA,
                    List.of(linea), efectivo("1190.00"), null, null, null, false)).lines()
                    .getFirst();

            assertThat(dto.id()).isEqualTo(88L);
            assertThat(dto.lineNumber()).isEqualTo(3);
            assertThat(dto.description()).isEqualTo("Vacuna");
            assertThat(dto.quantity()).isEqualByComparingTo("2");
            assertThat(dto.unitMeasureCode()).isEqualTo("94");
            assertThat(dto.unitPrice()).isEqualByComparingTo("500.00");
            assertThat(dto.lineExtensionAmount()).isEqualByComparingTo("1000.00");
            assertThat(dto.taxCategory()).isEqualTo(TaxCategory.GRAVADO);
            assertThat(dto.taxScheme()).isEqualTo(TaxScheme.IVA);
            assertThat(dto.taxRate()).isEqualByComparingTo("19");
            assertThat(dto.taxAmount()).isEqualByComparingTo("190.00");
            assertThat(dto.totalAmount()).isEqualByComparingTo("1190.00");
        }

        @Test
        @DisplayName("conserva el orden de las lineas del documento")
        void conserva_el_orden_de_las_lineas() {
            ElectronicDocumentDto dto = ElectronicDocumentDto.from(documento(
                    ElectronicDocumentType.FE_VENTA, List.of(ivaLine(1, "100.00", "19", "19.00"),
                            incLine(2, "200.00", "8", "16.00"), exentoLine(3, "300.00")),
                    efectivo("635.00"), null, null, null, false));

            assertThat(dto.lines()).extracting(LineDto::lineNumber).containsExactly(1, 2, 3);
        }
    }

    @Nested
    @DisplayName("pagos")
    class Pagos {

        @Test
        @DisplayName("cada pago viaja con su enum Y con el codigo DIAN resuelto")
        void cada_pago_viaja_con_su_codigo_dian() {
            List<ElectronicDocumentPayment> pagos = List.of(
                    new ElectronicDocumentPayment(11L, PaymentMeans.EFECTIVO, bd("190.00")),
                    new ElectronicDocumentPayment(12L, PaymentMeans.TARJETA_CREDITO,
                            bd("1000.00")));

            List<PaymentDto> dto = ElectronicDocumentDto
                    .from(documento(ElectronicDocumentType.FE_VENTA,
                            List.of(ivaLine(1, "1000.00", "19", "190.00")), pagos, null, null, null,
                            false))
                    .payments();

            assertThat(dto)
                    .extracting(PaymentDto::id, PaymentDto::paymentMeans, PaymentDto::dianCode,
                            PaymentDto::amount)
                    .containsExactly(
                            org.assertj.core.groups.Tuple.tuple(11L, PaymentMeans.EFECTIVO, "10",
                                    bd("190.00")),
                            org.assertj.core.groups.Tuple.tuple(12L, PaymentMeans.TARJETA_CREDITO,
                                    "49", bd("1000.00")));
        }

        @Test
        @DisplayName("un documento sin pagos registrados proyecta una lista vacia, no null")
        void un_documento_sin_pagos_proyecta_lista_vacia() {
            ElectronicDocumentDto dto = ElectronicDocumentDto.from(documento(
                    ElectronicDocumentType.FE_VENTA, List.of(ivaLine(1, "1000.00", "19", "190.00")),
                    List.of(), null, null, null, false));

            assertThat(dto.payments()).isEmpty();
        }
    }

    @Nested
    @DisplayName("desglose de impuesto por tarifa (insumo del formulario 300)")
    class DesgloseDeImpuesto {

        @Test
        @DisplayName("agrupa las lineas de la misma tarifa y suma base e impuesto")
        void agrupa_las_lineas_de_la_misma_tarifa() {
            ElectronicDocumentDto dto = ElectronicDocumentDto
                    .from(documento(ElectronicDocumentType.FE_VENTA,
                            List.of(ivaLine(1, "1000.00", "19", "190.00"),
                                    ivaLine(2, "500.00", "19", "95.00")),
                            efectivo("1785.00"), null, null, null, false));

            assertThat(dto.taxTotalsByRate()).singleElement().satisfies(total -> {
                assertThat(total.taxScheme()).isEqualTo(TaxScheme.IVA);
                assertThat(total.taxRate()).isEqualByComparingTo("19");
                assertThat(total.taxableAmount()).isEqualByComparingTo("1500.00");
                assertThat(total.taxAmount()).isEqualByComparingTo("285.00");
            });
        }

        @Test
        @DisplayName("separa por esquema: IVA al 19 % y INC al 8 % no se suman juntos")
        void separa_iva_de_inc() {
            ElectronicDocumentDto dto = ElectronicDocumentDto
                    .from(documento(ElectronicDocumentType.FE_VENTA,
                            List.of(ivaLine(1, "1000.00", "19", "190.00"),
                                    incLine(2, "200.00", "8", "16.00")),
                            efectivo("1406.00"), null, null, null, false));

            assertThat(dto.taxTotalsByRate())
                    .extracting(TaxTotalDto::taxScheme, TaxTotalDto::taxAmount).containsExactly(
                            org.assertj.core.groups.Tuple.tuple(TaxScheme.IVA, bd("190.00")),
                            org.assertj.core.groups.Tuple.tuple(TaxScheme.INC, bd("16.00")));
        }

        @Test
        @DisplayName("separa por tarifa dentro del mismo esquema: 19 % y 5 % van en filas "
                + "distintas")
        void separa_por_tarifa_dentro_del_mismo_esquema() {
            ElectronicDocumentDto dto = ElectronicDocumentDto
                    .from(documento(ElectronicDocumentType.FE_VENTA,
                            List.of(ivaLine(1, "1000.00", "19", "190.00"),
                                    ivaLine(2, "1000.00", "5", "50.00")),
                            efectivo("2240.00"), null, null, null, false));

            assertThat(dto.taxTotalsByRate()).hasSize(2).extracting(TaxTotalDto::taxRate)
                    .containsExactly(bd("19"), bd("5"));
        }

        @Test
        @DisplayName("la linea EXENTA aparece como IVA al 0 %, separada de la gravada")
        void la_linea_exenta_aparece_como_iva_cero() {
            ElectronicDocumentDto dto = ElectronicDocumentDto
                    .from(documento(ElectronicDocumentType.FE_VENTA,
                            List.of(ivaLine(1, "1000.00", "19", "190.00"), exentoLine(2, "300.00")),
                            efectivo("1490.00"), null, null, null, false));

            assertThat(dto.taxTotalsByRate()).hasSize(2);
            assertThat(dto.taxTotalsByRate().getLast().taxRate())
                    .isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(dto.taxTotalsByRate().getLast().taxableAmount())
                    .isEqualByComparingTo("300.00");
        }

        @Test
        @DisplayName("la linea EXCLUIDA no entra en el desglose: no tiene esquema tributario")
        void la_linea_excluida_no_entra_en_el_desglose() {
            ElectronicDocumentDto dto = ElectronicDocumentDto
                    .from(documento(ElectronicDocumentType.FE_VENTA,
                            List.of(ivaLine(1, "1000.00", "19", "190.00"),
                                    excluidoLine(2, "300.00")),
                            efectivo("1490.00"), null, null, null, false));

            assertThat(dto.taxTotalsByRate()).singleElement().satisfies(
                    total -> assertThat(total.taxableAmount()).isEqualByComparingTo("1000.00"));
        }

        @Test
        @DisplayName("un documento solo de excluidos no genera desglose alguno")
        void un_documento_solo_de_excluidos_no_genera_desglose() {
            ElectronicDocumentDto dto = ElectronicDocumentDto.from(
                    documento(ElectronicDocumentType.FE_VENTA, List.of(excluidoLine(1, "300.00")),
                            efectivo("300.00"), null, null, null, false));

            assertThat(dto.taxTotalsByRate()).isEmpty();
        }
    }

    @Nested
    @DisplayName("referencia de la nota")
    class ReferenciaDeLaNota {

        @Test
        @DisplayName("una factura no lleva referencia ni concepto de correccion")
        void una_factura_no_lleva_referencia() {
            ElectronicDocumentDto dto = ElectronicDocumentDto.from(factura());

            assertThat(dto.reference()).isNull();
            assertThat(dto.noteReasonCode()).isNull();
            assertThat(dto.noteReasonText()).isNull();
        }

        @Test
        @DisplayName("una nota proyecta la referencia al documento corregido y su concepto")
        void una_nota_proyecta_la_referencia() {
            DocumentReference referencia = new DocumentReference("CUFE-ORIGINAL", "FE", 12L,
                    LocalDate.of(2026, 1, 5));

            ElectronicDocumentDto dto = ElectronicDocumentDto
                    .from(documento(ElectronicDocumentType.NOTA_CREDITO,
                            List.of(ivaLine(1, "1000.00", "19", "190.00")), efectivo("1190.00"),
                            referencia, "2", "Anulacion de factura electronica", false));

            assertThat(dto.reference().cufe()).isEqualTo("CUFE-ORIGINAL");
            assertThat(dto.reference().prefix()).isEqualTo("FE");
            assertThat(dto.reference().number()).isEqualTo(12L);
            assertThat(dto.reference().issueDate()).isEqualTo(LocalDate.of(2026, 1, 5));
            assertThat(dto.noteReasonCode()).isEqualTo("2");
            assertThat(dto.noteReasonText()).isEqualTo("Anulacion de factura electronica");
        }

        @Test
        @DisplayName("proyecta la marca de reversada de la factura anulada")
        void proyecta_la_marca_de_reversada() {
            ElectronicDocumentDto dto = ElectronicDocumentDto.from(documento(
                    ElectronicDocumentType.FE_VENTA, List.of(ivaLine(1, "1000.00", "19", "190.00")),
                    efectivo("1190.00"), null, null, null, true));

            assertThat(dto.reversed()).isTrue();
        }
    }
}
