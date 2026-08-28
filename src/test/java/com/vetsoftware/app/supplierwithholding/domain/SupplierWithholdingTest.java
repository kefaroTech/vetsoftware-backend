package com.vetsoftware.app.supplierwithholding.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.supplierwithholding.testsupport.SupplierWithholdingMother;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Las ocho comprobaciones del constructor de {@link SupplierWithholding}.
 *
 * <p>
 * <b>La que mas dinero protege es {@code amount <= taxableBase}.</b> Una
 * retencion mayor que la base es un calculo invertido —la tarifa aplicada como
 * fraccion, o la base y lo retenido cruzados— y sin esa linea entra, se
 * certifica al proveedor y se declara. El proveedor se descuenta un valor que
 * no se le retuvo y la diferencia aparece meses despues, en un cruce de la
 * DIAN.
 *
 * <p>
 * <b>Las matrices van con {@link EnumSource} para que un
 * {@code SupplierWithholdingType} nuevo no entre sin forma de periodo.</b> El
 * {@code validatePeriod} no usa un {@code switch} exhaustivo sino un ternario
 * sobre {@code INCOME_TAX}: un tipo nuevo caeria callado en la rama bimestral,
 * y el caso parametrizado es lo unico que lo delata.
 */
@DisplayName("SupplierWithholding — las validaciones del constructor")
class SupplierWithholdingTest {

    @Nested
    @DisplayName("Importes (chk_sw_amounts)")
    class Importes {

        @Test
        @DisplayName("una retencion mayor que la base no se construye")
        void una_retencion_mayor_que_la_base_no_se_construye() {
            // El calculo invertido: 2.500.000 de base y 2.750.000 retenidos.
            assertThatThrownBy(() -> conBaseYRetenido("2500000.00", "2750000.00"))
                    .hasMessageContaining("must not exceed the taxable base");
        }

        @Test
        @DisplayName("una retencion igual a la base si se construye: es el limite, no el error")
        void una_retencion_igual_a_la_base_si_se_construye() {
            // El limite es <=, no <. Sin este caso, endurecer la comprobacion a
            // estrictamente menor pasaria desapercibido.
            assertThatCode(() -> conBaseYRetenido("2500000.00", "2500000.00"))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @CsvSource({"0.00, 62500.00, taxableBase", "-1.00, 62500.00, taxableBase",
                "2500000.00, 0.00, amount", "2500000.00, -1.00, amount"})
        @DisplayName("ni la base ni lo retenido admiten cero o negativo")
        void ni_la_base_ni_lo_retenido_admiten_cero_o_negativo(String base, String retenido,
                String campo) {
            assertThatThrownBy(() -> conBaseYRetenido(base, retenido))
                    .hasMessageContaining(campo + " must be greater than zero");
        }

        @ParameterizedTest
        @CsvSource({"2500000.001, 62500.00, taxableBase", "2500000.00, 62500.001, amount"})
        @DisplayName("un tercer decimal no se acepta: DECIMAL(19,2) lo redondearia en silencio")
        void un_tercer_decimal_no_se_acepta(String base, String retenido, String campo) {
            assertThatThrownBy(() -> conBaseYRetenido(base, retenido))
                    .hasMessageContaining(campo + " must have 2 decimals or fewer");
        }

        @Test
        @DisplayName("sin base no se construye")
        void sin_base_no_se_construye() {
            assertThatThrownBy(
                    () -> crudo(null, new BigDecimal("2.500000"), new BigDecimal("62500.00")))
                    .hasMessageContaining("taxableBase is required");
        }
    }

    @Nested
    @DisplayName("Tarifa (chk_sw_rate)")
    class Tarifa {

        @Test
        @DisplayName("sin tarifa no se construye")
        void sin_tarifa_no_se_construye() {
            assertThatThrownBy(
                    () -> crudo(new BigDecimal("2500000.00"), null, new BigDecimal("62500.00")))
                    .hasMessageContaining("ratePercent is required");
        }

        @ParameterizedTest
        @ValueSource(strings = {"0.000000", "-0.100000"})
        @DisplayName("una tarifa que no es positiva no se construye")
        void una_tarifa_que_no_es_positiva_no_se_construye(String tarifa) {
            assertThatThrownBy(() -> crudo(new BigDecimal("2500000.00"), new BigDecimal(tarifa),
                    new BigDecimal("62500.00")))
                    .hasMessageContaining("ratePercent must be greater than zero");
        }

        @Test
        @DisplayName("una tarifa por encima de cien no se construye")
        void una_tarifa_por_encima_de_cien_no_se_construye() {
            assertThatThrownBy(() -> crudo(new BigDecimal("2500000.00"),
                    new BigDecimal("100.000001"), new BigDecimal("62500.00")))
                    .hasMessageContaining("ratePercent must not exceed 100");
        }

        @Test
        @DisplayName("la tarifa del cien por cien si se construye: es el limite")
        void la_tarifa_del_cien_por_cien_si_se_construye() {
            assertThatCode(() -> crudo(new BigDecimal("2500000.00"), new BigDecimal("100.000000"),
                    new BigDecimal("62500.00"))).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("un septimo decimal en la tarifa no se acepta")
        void un_septimo_decimal_en_la_tarifa_no_se_acepta() {
            // DECIMAL(9,6) no rechaza el septimo decimal: lo REDONDEA. La tarifa
            // guardada dejaria de ser la aplicada, y el certificado del proveedor
            // llevaria un porcentaje que no cuadra con el importe.
            assertThatThrownBy(() -> crudo(new BigDecimal("2500000.00"),
                    new BigDecimal("2.5000001"), new BigDecimal("62500.00")))
                    .hasMessageContaining("ratePercent must have 6 decimals or fewer");
        }
    }

    @Nested
    @DisplayName("Municipio (chk_sw_municipality)")
    class Municipio {

        @Test
        @DisplayName("ICA sin municipio no se construye")
        void ica_sin_municipio_no_se_construye() {
            assertThatThrownBy(() -> conTipoMunicipioYPeriodo(SupplierWithholdingType.ICA, null,
                    SupplierWithholdingMother.BIMESTRE))
                    .hasMessageContaining("municipalityCode is required for ICA");
        }

        @Test
        @DisplayName("ICA con un municipio que no tiene cinco caracteres no se construye")
        void ica_con_municipio_de_longitud_equivocada_no_se_construye() {
            assertThatThrownBy(() -> conTipoMunicipioYPeriodo(SupplierWithholdingType.ICA, "5001",
                    SupplierWithholdingMother.BIMESTRE))
                    .hasMessageContaining("municipalityCode must be 5 characters");
        }

        @ParameterizedTest
        @EnumSource(value = SupplierWithholdingType.class, names = "ICA", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("las retenciones nacionales rechazan el municipio")
        void las_nacionales_rechazan_el_municipio(SupplierWithholdingType tipo) {
            assertThatThrownBy(
                    () -> conTipoMunicipioYPeriodo(tipo, SupplierWithholdingMother.MUNICIPIO,
                            SupplierWithholdingMother.periodoValido(tipo)))
                    .hasMessageContaining("municipalityCode must be absent");
        }

        @Test
        @DisplayName("una retencion nacional expone el centinela que calcula la base")
        void una_retencion_nacional_expone_el_centinela() {
            assertThat(SupplierWithholdingMother.renta().municipalityKey())
                    .isEqualTo(SupplierWithholding.NATIONAL_MUNICIPALITY_KEY);
            assertThat(SupplierWithholdingMother.ica().municipalityKey())
                    .isEqualTo(SupplierWithholdingMother.MUNICIPIO);
        }
    }

    @Nested
    @DisplayName("Periodo (chk_sw_period)")
    class Periodo {

        @ParameterizedTest
        @EnumSource(SupplierWithholdingType.class)
        @DisplayName("cada clase de retencion acepta la forma que le toca")
        void cada_clase_acepta_la_forma_que_le_toca(SupplierWithholdingType tipo) {
            assertThatCode(() -> conTipoMunicipioYPeriodo(tipo,
                    SupplierWithholdingMother.municipioValido(tipo),
                    SupplierWithholdingMother.periodoValido(tipo))).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("la retencion de renta es MENSUAL aqui, y un bimestre no cuela")
        void la_retencion_de_renta_es_mensual() {
            // Y esto no es un descuido del modelo: la retencion que TE practican se
            // imputa al ano gravable de tu renta; la que TU practicas se declara en
            // la retencion en la fuente, que es mensual. Va escrito para que el
            // primer lector no lo «corrija».
            assertThatThrownBy(() -> conTipoMunicipioYPeriodo(SupplierWithholdingType.INCOME_TAX,
                    null, SupplierWithholdingMother.BIMESTRE))
                    .hasMessageContaining("(monthly, yyyy-Mnn)");
        }

        @ParameterizedTest
        @EnumSource(value = SupplierWithholdingType.class, names = "INCOME_TAX", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("las demas son BIMESTRALES, y un mes no cuela")
        void las_demas_son_bimestrales(SupplierWithholdingType tipo) {
            assertThatThrownBy(() -> conTipoMunicipioYPeriodo(tipo,
                    SupplierWithholdingMother.municipioValido(tipo), SupplierWithholdingMother.MES))
                    .hasMessageContaining("(bimonthly, yyyy-B0n)");
        }

        @Test
        @DisplayName("una clave de otro ano fiscal no se construye")
        void una_clave_de_otro_ano_fiscal_no_se_construye() {
            assertThatThrownBy(() -> conTipoMunicipioYPeriodo(SupplierWithholdingType.INCOME_TAX,
                    null, "2025-M12"))
                    .hasMessageContaining("does not start with the fiscal year 2026");
        }

        @ParameterizedTest
        @ValueSource(ints = {2019, 2101})
        @DisplayName("un ano fiscal fuera del rango 2020-2100 no se construye")
        void un_ano_fiscal_fuera_del_rango_no_se_construye(int ano) {
            assertThatThrownBy(() -> SupplierWithholdingMother.crudo("900123456", "Proveedora SAS",
                    SupplierDocumentKind.NIT, "FV-1", SupplierWithholdingType.INCOME_TAX,
                    "Concepto", new BigDecimal("2500000.00"), new BigDecimal("2.500000"),
                    new BigDecimal("62500.00"), null, ano, ano + "-M03",
                    SupplierWithholdingMother.PRACTICADA_EL, null, null, null,
                    SupplierWithholdingMother.CREADA_EL))
                    .hasMessageContaining("fiscalYear must be between 2020 and 2100");
        }

        @Test
        @DisplayName("sin clave de periodo no se construye")
        void sin_clave_de_periodo_no_se_construye() {
            assertThatThrownBy(
                    () -> conTipoMunicipioYPeriodo(SupplierWithholdingType.INCOME_TAX, null, "   "))
                    .hasMessageContaining("fiscalPeriodKey is required");
        }
    }

    @Nested
    @DisplayName("Certificado (chk_sw_certificate)")
    class Certificado {

        @Test
        @DisplayName("una fecha de certificado sin referencia no se construye")
        void una_fecha_sin_referencia_no_se_construye() {
            assertThatThrownBy(() -> SupplierWithholdingMother
                    .conCertificado(LocalDateTime.of(2026, 4, 2, 9, 0), null))
                    .hasMessageContaining("must both be present or both absent");
        }

        @Test
        @DisplayName("una referencia de certificado sin fecha no se construye")
        void una_referencia_sin_fecha_no_se_construye() {
            assertThatThrownBy(() -> SupplierWithholdingMother.conCertificado(null, "CERT-1"))
                    .hasMessageContaining("must both be present or both absent");
        }

        @Test
        @DisplayName("emitir el certificado deja la fecha y la referencia juntas")
        void emitir_el_certificado_deja_fecha_y_referencia() {
            SupplierWithholding certificada = SupplierWithholdingMother.renta()
                    .issueCertificate(LocalDateTime.of(2026, 4, 2, 9, 0), "CERT-2026-0042");

            assertThat(certificada.isCertified()).isTrue();
            assertThat(certificada.getCertificateIssuedAt())
                    .isEqualTo(LocalDateTime.of(2026, 4, 2, 9, 0));
            assertThat(certificada.getCertificateRef()).isEqualTo("CERT-2026-0042");
        }

        @Test
        @DisplayName("un certificado ya emitido no se reescribe")
        void un_certificado_ya_emitido_no_se_reescribe() {
            // La negativa es toda la barandilla que hay: chk_sw_certificate solo
            // exige que fecha y referencia vayan juntas, no que no se reescriban. El
            // numero es el que el proveedor usa para descontarse la retencion;
            // cambiarlo deja dos documentos incompatibles en circulacion.
            SupplierWithholding certificada = SupplierWithholdingMother
                    .conId(31L, SupplierWithholdingMother.renta())
                    .issueCertificate(LocalDateTime.of(2026, 4, 2, 9, 0), "CERT-2026-0042");

            assertThatThrownBy(() -> certificada
                    .issueCertificate(LocalDateTime.of(2026, 5, 2, 9, 0), "CERT-2026-0099"))
                    .isInstanceOf(SupplierWithholdingCertificateAlreadyIssuedException.class);
        }

        @Test
        @DisplayName("emitir sin fecha o sin referencia se rechaza antes de construir nada")
        void emitir_sin_fecha_o_sin_referencia_se_rechaza() {
            SupplierWithholding sinCertificar = SupplierWithholdingMother.renta();

            assertThatThrownBy(() -> sinCertificar.issueCertificate(null, "CERT-1"))
                    .hasMessageContaining("certificateIssuedAt is required");
            assertThatThrownBy(
                    () -> sinCertificar.issueCertificate(LocalDateTime.of(2026, 4, 2, 9, 0), "  "))
                    .hasMessageContaining("certificateRef is required");
        }
    }

    @Nested
    @DisplayName("Acuse de pago (art. 632 ET)")
    class AcuseDePago {

        @Test
        @DisplayName("anotar el acuse conserva todo lo demas")
        void anotar_el_acuse_conserva_todo_lo_demas() {
            SupplierWithholding conAcuse = SupplierWithholdingMother.renta()
                    .registerPaymentReceipt("CONSIG-2026-771");

            assertThat(conAcuse.getPaymentReceiptRef()).isEqualTo("CONSIG-2026-771");
            assertThat(conAcuse.getAmount()).isEqualByComparingTo("62500.00");
            assertThat(conAcuse.getFiscalPeriodKey()).isEqualTo(SupplierWithholdingMother.MES);
        }

        @Test
        @DisplayName("un acuse en blanco no se anota")
        void un_acuse_en_blanco_no_se_anota() {
            SupplierWithholding renta = SupplierWithholdingMother.renta();

            assertThatThrownBy(() -> renta.registerPaymentReceipt("  "))
                    .hasMessageContaining("paymentReceiptRef is required");
        }
    }

    @Nested
    @DisplayName("Obligatorios de texto")
    class ObligatoriosDeTexto {

        @ParameterizedTest
        @CsvSource({"supplierTaxId, 50", "supplierName, 200", "supplierInvoiceRef, 100",
                "concept, 60"})
        @DisplayName("cada texto obligatorio tiene su tope de longitud")
        void cada_texto_obligatorio_tiene_su_tope(String campo, int tope) {
            String demasiadoLargo = "x".repeat(tope + 1);

            assertThatThrownBy(() -> conTextos(campo, demasiadoLargo))
                    .hasMessageContaining(campo + " must be " + tope + " chars or less");
        }

        @ParameterizedTest
        @CsvSource({"supplierTaxId", "supplierName", "supplierInvoiceRef", "concept"})
        @DisplayName("cada texto obligatorio rechaza el blanco")
        void cada_texto_obligatorio_rechaza_el_blanco(String campo) {
            assertThatThrownBy(() -> conTextos(campo, "   "))
                    .hasMessageContaining(campo + " is required");
        }

        @Test
        @DisplayName("sin clase de documento del proveedor no se construye")
        void sin_clase_de_documento_no_se_construye() {
            assertThatThrownBy(() -> SupplierWithholdingMother.crudo("900123456", "Proveedora SAS",
                    null, "FV-1", SupplierWithholdingType.INCOME_TAX, "Concepto",
                    new BigDecimal("2500000.00"), new BigDecimal("2.500000"),
                    new BigDecimal("62500.00"), null, SupplierWithholdingMother.ANIO,
                    SupplierWithholdingMother.MES, SupplierWithholdingMother.PRACTICADA_EL, null,
                    null, null, SupplierWithholdingMother.CREADA_EL))
                    .hasMessageContaining("supplierDocType is required");
        }

        @Test
        @DisplayName("sin fecha de practica no se construye")
        void sin_fecha_de_practica_no_se_construye() {
            assertThatThrownBy(() -> SupplierWithholdingMother.crudo("900123456", "Proveedora SAS",
                    SupplierDocumentKind.NIT, "FV-1", SupplierWithholdingType.INCOME_TAX,
                    "Concepto", new BigDecimal("2500000.00"), new BigDecimal("2.500000"),
                    new BigDecimal("62500.00"), null, SupplierWithholdingMother.ANIO,
                    SupplierWithholdingMother.MES, null, null, null, null,
                    SupplierWithholdingMother.CREADA_EL))
                    .hasMessageContaining("practicedOn is required");
        }
    }

    private static SupplierWithholding conBaseYRetenido(String base, String retenido) {
        return crudo(new BigDecimal(base), new BigDecimal("2.500000"), new BigDecimal(retenido));
    }

    private static SupplierWithholding crudo(BigDecimal base, BigDecimal tarifa,
            BigDecimal retenido) {
        return SupplierWithholdingMother.crudo("900123456", "Proveedora SAS",
                SupplierDocumentKind.NIT, "FV-1", SupplierWithholdingType.INCOME_TAX, "Concepto",
                base, tarifa, retenido, null, SupplierWithholdingMother.ANIO,
                SupplierWithholdingMother.MES, SupplierWithholdingMother.PRACTICADA_EL, null, null,
                null, SupplierWithholdingMother.CREADA_EL);
    }

    private static SupplierWithholding conTipoMunicipioYPeriodo(SupplierWithholdingType tipo,
            String municipio, String periodo) {
        return SupplierWithholdingMother.crudo("900123456", "Proveedora SAS",
                SupplierDocumentKind.NIT, "FV-1", tipo, "Concepto", new BigDecimal("2500000.00"),
                new BigDecimal("2.500000"), new BigDecimal("62500.00"), municipio,
                SupplierWithholdingMother.ANIO, periodo, SupplierWithholdingMother.PRACTICADA_EL,
                null, null, null, SupplierWithholdingMother.CREADA_EL);
    }

    private static SupplierWithholding conTextos(String campo, String valor) {
        return SupplierWithholdingMother.crudo("supplierTaxId".equals(campo) ? valor : "900123456",
                "supplierName".equals(campo) ? valor : "Proveedora SAS", SupplierDocumentKind.NIT,
                "supplierInvoiceRef".equals(campo) ? valor : "FV-1",
                SupplierWithholdingType.INCOME_TAX, "concept".equals(campo) ? valor : "Concepto",
                new BigDecimal("2500000.00"), new BigDecimal("2.500000"),
                new BigDecimal("62500.00"), null, SupplierWithholdingMother.ANIO,
                SupplierWithholdingMother.MES, SupplierWithholdingMother.PRACTICADA_EL, null, null,
                null, SupplierWithholdingMother.CREADA_EL);
    }
}
