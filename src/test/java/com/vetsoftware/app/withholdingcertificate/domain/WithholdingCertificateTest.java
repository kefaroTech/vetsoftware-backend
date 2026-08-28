package com.vetsoftware.app.withholdingcertificate.domain;

import static com.vetsoftware.app.withholdingcertificate.testsupport.WithholdingCertificateMother.ANO_GRAVABLE;
import static com.vetsoftware.app.withholdingcertificate.testsupport.WithholdingCertificateMother.COMPANY_ID;
import static com.vetsoftware.app.withholdingcertificate.testsupport.WithholdingCertificateMother.CREADO_EL;
import static com.vetsoftware.app.withholdingcertificate.testsupport.WithholdingCertificateMother.EXPEDIDO_EL;
import static com.vetsoftware.app.withholdingcertificate.testsupport.WithholdingCertificateMother.IMPORTE_CERTIFICADO;
import static com.vetsoftware.app.withholdingcertificate.testsupport.WithholdingCertificateMother.NIT_DEL_CLIENTE;
import static com.vetsoftware.app.withholdingcertificate.testsupport.WithholdingCertificateMother.RECIBIDO_EL;
import static com.vetsoftware.app.withholdingcertificate.testsupport.WithholdingCertificateMother.TARIFA_ICA_POR_MIL;
import static com.vetsoftware.app.withholdingcertificate.testsupport.WithholdingCertificateMother.TARIFA_RENTA;
import static com.vetsoftware.app.withholdingcertificate.testsupport.WithholdingCertificateMother.VENCE_EL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.withholdingcertificate.testsupport.WithholdingCertificateMother;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * El agregado, probado sin un solo doble: las invariantes viven en el
 * constructor y probarlas contra un mock seria probar el mock.
 *
 * <p>
 * Casi todo lo que hay aqui es <b>espejo de una constraint del changeset
 * 328</b>, y el motivo de duplicarlo en Java no es desconfianza del motor sino
 * la forma del error: la base rechaza con un mensaje de integridad que el front
 * no sabe pintar, y lo hace despues de haber abierto la transaccion. La unica
 * regla que la base <b>no</b> puede cuidar es la de
 * {@link Recepcion#no_se_puede_recibir_dos_veces()}, y es la unica que lanza
 * una excepcion propia.
 */
@DisplayName("WithholdingCertificate — el papel que hace descontable la retencion")
class WithholdingCertificateTest {

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("nace como expectativa abierta: sin recepcion, sin archivo y sin sustituto")
        void nace_como_expectativa_abierta() {
            WithholdingCertificate certificado = WithholdingCertificateMother.deRenta();

            assertThat(certificado.getReceivedOn()).isNull();
            assertThat(certificado.getFileRef()).isNull();
            assertThat(certificado.getSubstituteEvidenceKind()).isNull();
            assertThat(certificado.getSubstituteEvidenceRef()).isNull();
            assertThat(certificado.isMissing()).isTrue();
            assertThat(certificado.isSupported()).isFalse();
            assertThat(certificado.getId()).isNull();
        }

        @Test
        @DisplayName("conserva cada fecha y cada importe en su propio campo")
        void conserva_cada_fecha_y_cada_importe_en_su_campo() {
            WithholdingCertificate certificado = WithholdingCertificateMother.deRenta();

            // Las tres fechas son distintas entre si: si el constructor cruzara
            // issuedOn con legalDeadlineOn, esta asercion caeria.
            assertThat(certificado.getIssuedOn()).isEqualTo(EXPEDIDO_EL);
            assertThat(certificado.getLegalDeadlineOn()).isEqualTo(VENCE_EL);
            assertThat(certificado.getCreatedDate()).isEqualTo(CREADO_EL);
            assertThat(certificado.getCompanyId()).isEqualTo(COMPANY_ID);
            assertThat(certificado.getIssuedByTaxId()).isEqualTo(NIT_DEL_CLIENTE);
            assertThat(certificado.getCertificateNumber()).isEqualTo("CERT-2025-0001");
            assertThat(certificado.getFiscalYear()).isEqualTo(ANO_GRAVABLE);
            assertThat(certificado.getCertifiedAmount()).isEqualByComparingTo(IMPORTE_CERTIFICADO);
        }

        @Test
        @DisplayName("la tarifa de ICA por mil sobrevive con sus seis decimales")
        void la_tarifa_de_ica_por_mil_sobrevive_con_sus_seis_decimales() {
            // 6,9 por mil es 0,69 %. Este caso existe porque la ficha original pedia
            // cuatro decimales sobre la fraccion: alli 4,14 por mil (0,00414) se
            // cortaba y base por tarifa dejaba de dar el importe certificado.
            WithholdingCertificate certificado = WithholdingCertificateMother.deIca();

            assertThat(certificado.getRatePercent()).isEqualByComparingTo(TARIFA_ICA_POR_MIL);
            assertThat(certificado.getWithholdingType()).isEqualTo(WithholdingType.ICA);
            assertThat(certificado.getFiscalPeriodKey()).isEqualTo("2025-B03");
        }

        @Test
        @DisplayName("admite la tarifa mas fina que la columna soporta: seis decimales")
        void admite_la_tarifa_mas_fina_que_la_columna_soporta() {
            assertThatCode(() -> conTarifa(new BigDecimal("0.004140"))).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("sin empresa no hay certificado")
        void sin_empresa_no_hay_certificado() {
            assertThatThrownBy(() -> new WithholdingCertificate(null, null, NIT_DEL_CLIENTE,
                    "CERT-2025-0001", WithholdingType.INCOME_TAX, ANO_GRAVABLE, "2025-A",
                    TARIFA_RENTA, IMPORTE_CERTIFICADO, EXPEDIDO_EL, VENCE_EL, null, null, null,
                    null, CREADO_EL)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("companyId is required");
        }

        @ParameterizedTest(name = "NIT invalido: [{0}]")
        @DisplayName("el NIT de quien expide es obligatorio y no pasa de cincuenta")
        @ValueSource(strings = {"", "   ", "012345678901234567890123456789012345678901234567890"})
        void el_nit_de_quien_expide_es_obligatorio_y_acotado(String nit) {
            assertThatThrownBy(() -> new WithholdingCertificate(null, COMPANY_ID, nit,
                    "CERT-2025-0001", WithholdingType.INCOME_TAX, ANO_GRAVABLE, "2025-A",
                    TARIFA_RENTA, IMPORTE_CERTIFICADO, EXPEDIDO_EL, VENCE_EL, null, null, null,
                    null, CREADO_EL)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("issuedByTaxId");
        }

        @Test
        @DisplayName("el numero del certificado es obligatorio")
        void el_numero_del_certificado_es_obligatorio() {
            assertThatThrownBy(() -> new WithholdingCertificate(null, COMPANY_ID, NIT_DEL_CLIENTE,
                    "  ", WithholdingType.INCOME_TAX, ANO_GRAVABLE, "2025-A", TARIFA_RENTA,
                    IMPORTE_CERTIFICADO, EXPEDIDO_EL, VENCE_EL, null, null, null, null, CREADO_EL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("certificateNumber is required");
        }

        @ParameterizedTest(name = "ano gravable fuera de rango: {0}")
        @DisplayName("sin ano gravable valido la retencion no se puede imputar")
        @ValueSource(ints = {2019, 2101, 0, 1900})
        void sin_ano_gravable_valido_la_retencion_no_se_puede_imputar(int ano) {
            // Perder el ano gravable es perder el derecho a descontar la retencion y
            // acabar pagando dos veces el mismo impuesto. Espejo de
            // chk_withholding_certificates_year.
            assertThatThrownBy(() -> new WithholdingCertificate(null, COMPANY_ID, NIT_DEL_CLIENTE,
                    "CERT-2025-0001", WithholdingType.INCOME_TAX, ano, ano + "-A", TARIFA_RENTA,
                    IMPORTE_CERTIFICADO, EXPEDIDO_EL, VENCE_EL, null, null, null, null, CREADO_EL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("fiscalYear must be between 2020 and 2100");
        }

        @Test
        @DisplayName("el ano gravable nulo se rechaza antes de compararlo con nada")
        void el_ano_gravable_nulo_se_rechaza() {
            assertThatThrownBy(() -> new WithholdingCertificate(null, COMPANY_ID, NIT_DEL_CLIENTE,
                    "CERT-2025-0001", WithholdingType.INCOME_TAX, null, "2025-A", TARIFA_RENTA,
                    IMPORTE_CERTIFICADO, EXPEDIDO_EL, VENCE_EL, null, null, null, null, CREADO_EL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("fiscalYear is required");
        }

        @Test
        @DisplayName("los dos limites del rango de anos si entran")
        void los_dos_limites_del_rango_de_anos_si_entran() {
            assertThatCode(() -> conPeriodo(WithholdingType.INCOME_TAX, 2020, "2020-A"))
                    .doesNotThrowAnyException();
            assertThatCode(() -> conPeriodo(WithholdingType.INCOME_TAX, 2100, "2100-A"))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest(name = "importe certificado invalido: {0}")
        @DisplayName("el valor certificado tiene que ser mayor que cero")
        @ValueSource(strings = {"0", "0.00", "-0.01", "-1847320.55"})
        void el_valor_certificado_tiene_que_ser_mayor_que_cero(String importe) {
            assertThatThrownBy(() -> new WithholdingCertificate(null, COMPANY_ID, NIT_DEL_CLIENTE,
                    "CERT-2025-0001", WithholdingType.INCOME_TAX, ANO_GRAVABLE, "2025-A",
                    TARIFA_RENTA, new BigDecimal(importe), EXPEDIDO_EL, VENCE_EL, null, null, null,
                    null, CREADO_EL)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("certifiedAmount must be greater than zero");
        }

        @Test
        @DisplayName("sin fecha limite legal no hay forma de avisar antes de que sea tarde")
        void sin_fecha_limite_legal_no_hay_aviso() {
            // Es la unica fecha dura de todo el bloque fiscal y se guarda como dato:
            // sin ella, el listado de los que faltan por recibir no existe.
            assertThatThrownBy(() -> new WithholdingCertificate(null, COMPANY_ID, NIT_DEL_CLIENTE,
                    "CERT-2025-0001", WithholdingType.INCOME_TAX, ANO_GRAVABLE, "2025-A",
                    TARIFA_RENTA, IMPORTE_CERTIFICADO, EXPEDIDO_EL, null, null, null, null, null,
                    CREADO_EL)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("legalDeadlineOn is required");
        }
    }

    @Nested
    @DisplayName("Periodo fiscal")
    class PeriodoFiscal {

        @ParameterizedTest(name = "{0} con {2} en {1} es valido")
        @DisplayName("cada impuesto acepta la forma de clave que le corresponde")
        @CsvSource({"INCOME_TAX, 2025, 2025-A", "VAT, 2025, 2025-B01", "VAT, 2025, 2025-B06",
                "ICA, 2025, 2025-B03", "ICA, 2026, 2026-B01", "INCOME_TAX, 2020, 2020-A"})
        void cada_impuesto_acepta_su_forma_de_clave(WithholdingType tipo, int ano, String clave) {
            assertThat(conPeriodo(tipo, ano, clave).getFiscalPeriodKey()).isEqualTo(clave);
        }

        @ParameterizedTest(name = "{0} con {2} en {1} se rechaza")
        @DisplayName("replica exacta del CHECK: forma por impuesto y ano que coincide")
        @CsvSource({"INCOME_TAX, 2025, 2025-B01", "INCOME_TAX, 2025, 2024-A",
                "INCOME_TAX, 2025, 2025-a", "VAT, 2025, 2025-A", "VAT, 2025, 2025-B00",
                "VAT, 2025, 2025-B07", "VAT, 2025, 2025-B3", "ICA, 2025, 2024-B03",
                "ICA, 2025, 2026-B03", "ICA, 2025, B03"})
        void replica_exacta_del_check_de_periodo(WithholdingType tipo, int ano, String clave) {
            assertThatThrownBy(() -> conPeriodo(tipo, ano, clave))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("fiscalPeriodKey");
        }

        @Test
        @DisplayName("un bimestre correcto colgado de otro ano manda la retencion a la declaracion equivocada")
        void un_bimestre_correcto_de_otro_ano_se_rechaza() {
            // Es la mitad del CHECK que parece redundante y no lo es: 2024-B03 pasa el
            // formato, se lee perfectamente en una revision, y suma la retencion en la
            // declaracion del ano que no era.
            assertThatThrownBy(() -> conPeriodo(WithholdingType.ICA, 2025, "2024-B03"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("fiscalPeriodKey year must match fiscalYear 2025");
        }

        @Test
        @DisplayName("sin periodo fiscal no se construye")
        void sin_periodo_fiscal_no_se_construye() {
            assertThatThrownBy(() -> conPeriodo(WithholdingType.VAT, 2025, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("fiscalPeriodKey is required");
        }
    }

    @Nested
    @DisplayName("Tarifa")
    class Tarifa {

        @ParameterizedTest(name = "tarifa invalida: {0}")
        @DisplayName("la tarifa es un porcentaje entre cero excluido y cien incluido")
        @ValueSource(strings = {"0", "0.000000", "-0.690000", "100.000001", "101", "1000"})
        void la_tarifa_es_un_porcentaje_entre_cero_y_cien(String tarifa) {
            assertThatThrownBy(() -> conTarifa(new BigDecimal(tarifa)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ratePercent");
        }

        @Test
        @DisplayName("el cien exacto si entra: el tope es inclusivo")
        void el_cien_exacto_si_entra() {
            assertThat(conTarifa(new BigDecimal("100.000000")).getRatePercent())
                    .isEqualByComparingTo("100");
        }

        @Test
        @DisplayName("un septimo decimal se rechaza en vez de perderse redondeado")
        void un_septimo_decimal_se_rechaza() {
            // DECIMAL(9,6) redondea en silencio lo que le sobra: 0,6912345 entraria
            // como 0,691235 sin un aviso, y base por tarifa dejaria de cuadrar. Aqui
            // esa perdida es un error visible.
            assertThatThrownBy(() -> conTarifa(new BigDecimal("0.6912345")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ratePercent must have 6 decimals or less");
        }

        @Test
        @DisplayName("los ceros de relleno no cuentan como decimales de mas")
        void los_ceros_de_relleno_no_cuentan_como_decimales_de_mas() {
            // 0,690000000 es 0,69 escrito con ceros: rechazarlo seria un falso
            // positivo, porque no se pierde nada al guardarlo.
            assertThat(conTarifa(new BigDecimal("0.690000000")).getRatePercent())
                    .isEqualByComparingTo("0.69");
        }
    }

    @Nested
    @DisplayName("Recepcion")
    class Recepcion {

        @Test
        @DisplayName("recibirlo cierra la expectativa con su fecha y su archivo")
        void recibirlo_cierra_la_expectativa() {
            WithholdingCertificate certificado = WithholdingCertificateMother.conId(7L);

            certificado.receive(RECIBIDO_EL, "s3://certificados/2025/CERT-2025-0001.pdf");

            assertThat(certificado.getReceivedOn()).isEqualTo(RECIBIDO_EL);
            assertThat(certificado.getFileRef())
                    .isEqualTo("s3://certificados/2025/CERT-2025-0001.pdf");
            assertThat(certificado.isMissing()).isFalse();
            assertThat(certificado.isSupported()).isTrue();
        }

        @Test
        @DisplayName("no se puede recibir dos veces")
        void no_se_puede_recibir_dos_veces() {
            // La UNICA regla de este agregado que la base no cuida: un segundo UPDATE
            // que pisa received_on y file_ref es una fila valida para el motor, y se
            // lleva por delante el archivo que ya se habia guardado.
            WithholdingCertificate certificado = WithholdingCertificateMother.recibido(7L);

            assertThatThrownBy(() -> certificado.receive(LocalDate.of(2026, 3, 25),
                    "s3://certificados/otro.pdf"))
                    .isInstanceOf(WithholdingCertificateAlreadyReceivedException.class)
                    .hasMessageContaining("was already received on " + RECIBIDO_EL);
            assertThat(certificado.getFileRef())
                    .isEqualTo("s3://certificados/2025/CERT-2025-0001.pdf");
        }

        @Test
        @DisplayName("no puede llegar antes de haberse expedido")
        void no_puede_llegar_antes_de_haberse_expedido() {
            WithholdingCertificate certificado = WithholdingCertificateMother.conId(7L);

            assertThatThrownBy(() -> certificado.receive(EXPEDIDO_EL.minusDays(1), "s3://x.pdf"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("receivedOn cannot be before issuedOn");
        }

        @Test
        @DisplayName("llegar el mismo dia en que se expidio si vale")
        void llegar_el_mismo_dia_en_que_se_expidio_si_vale() {
            WithholdingCertificate certificado = WithholdingCertificateMother.conId(7L);

            certificado.receive(EXPEDIDO_EL, "s3://x.pdf");

            assertThat(certificado.getReceivedOn()).isEqualTo(EXPEDIDO_EL);
        }

        @ParameterizedTest(name = "archivo invalido: [{0}]")
        @DisplayName("un certificado recibido sin archivo es un certificado que nadie puede ensenar")
        @ValueSource(strings = {"", "   "})
        void un_certificado_recibido_sin_archivo_no_prueba_nada(String archivo) {
            WithholdingCertificate certificado = WithholdingCertificateMother.conId(7L);

            assertThatThrownBy(() -> certificado.receive(RECIBIDO_EL, archivo))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("fileRef is required once the certificate arrives");
        }

        @Test
        @DisplayName("el archivo nulo se rechaza igual que el vacio")
        void el_archivo_nulo_se_rechaza_igual_que_el_vacio() {
            WithholdingCertificate certificado = WithholdingCertificateMother.conId(7L);

            assertThatThrownBy(() -> certificado.receive(RECIBIDO_EL, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("fileRef is required once the certificate arrives");
        }

        @Test
        @DisplayName("al llegar el papel, el sustituto se retira solo")
        void al_llegar_el_papel_el_sustituto_se_retira_solo() {
            // Si no se retirara, el UPDATE chocaria contra
            // chk_withholding_certificates_substitute y el fallo llegaria como un
            // error de integridad en vez de como lo que es: un hecho que ya no aplica.
            WithholdingCertificate certificado = WithholdingCertificateMother.conSustituto(7L);

            certificado.receive(RECIBIDO_EL, "s3://certificados/llego.pdf");

            assertThat(certificado.getSubstituteEvidenceKind()).isNull();
            assertThat(certificado.getSubstituteEvidenceRef()).isNull();
            assertThat(certificado.isSupported()).isTrue();
        }
    }

    @Nested
    @DisplayName("Sustituto")
    class Sustituto {

        @Test
        @DisplayName("el comprobante de pago acredita la retencion cuando el papel no llego")
        void el_comprobante_de_pago_acredita_la_retencion() {
            WithholdingCertificate certificado = WithholdingCertificateMother.conId(7L);

            certificado.attachSubstituteEvidence(SubstituteEvidenceKind.PAYMENT_RECEIPT,
                    "s3://pagos/2025/REC-77120.pdf");

            assertThat(certificado.getSubstituteEvidenceKind())
                    .isEqualTo(SubstituteEvidenceKind.PAYMENT_RECEIPT);
            assertThat(certificado.getSubstituteEvidenceRef())
                    .isEqualTo("s3://pagos/2025/REC-77120.pdf");
            assertThat(certificado.isMissing()).isTrue();
            assertThat(certificado.isSupported()).isTrue();
        }

        @Test
        @DisplayName("no se puede adjuntar un sustituto a un certificado que ya llego")
        void no_se_puede_adjuntar_un_sustituto_a_uno_que_ya_llego() {
            WithholdingCertificate certificado = WithholdingCertificateMother.recibido(7L);

            assertThatThrownBy(() -> certificado.attachSubstituteEvidence(
                    SubstituteEvidenceKind.PAYMENT_RECEIPT, "s3://pagos/REC-1.pdf"))
                    .isInstanceOf(WithholdingCertificateAlreadyReceivedException.class)
                    .hasMessageContaining("was already received on " + RECIBIDO_EL);
        }

        @ParameterizedTest(name = "referencia invalida: [{0}]")
        @DisplayName("el sustituto sin referencia no acredita nada")
        @ValueSource(strings = {"", "   "})
        void el_sustituto_sin_referencia_no_acredita_nada(String referencia) {
            WithholdingCertificate certificado = WithholdingCertificateMother.conId(7L);

            assertThatThrownBy(() -> certificado
                    .attachSubstituteEvidence(SubstituteEvidenceKind.PAYMENT_RECEIPT, referencia))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("substitute evidence needs both");
        }

        @Test
        @DisplayName("construir con la clase del sustituto y sin su referencia se rechaza")
        void construir_con_clase_y_sin_referencia_se_rechaza() {
            assertThatThrownBy(() -> new WithholdingCertificate(7L, COMPANY_ID, NIT_DEL_CLIENTE,
                    "CERT-2025-0001", WithholdingType.INCOME_TAX, ANO_GRAVABLE, "2025-A",
                    TARIFA_RENTA, IMPORTE_CERTIFICADO, EXPEDIDO_EL, VENCE_EL, null, null,
                    SubstituteEvidenceKind.PAYMENT_RECEIPT, null, CREADO_EL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("substitute evidence needs both");
        }

        @Test
        @DisplayName("construir con sustituto y recepcion a la vez se rechaza")
        void construir_con_sustituto_y_recepcion_a_la_vez_se_rechaza() {
            // Espejo de la tercera rama de chk_withholding_certificates_substitute: el
            // sustituto y el papel son dos pruebas del mismo hecho.
            assertThatThrownBy(() -> new WithholdingCertificate(7L, COMPANY_ID, NIT_DEL_CLIENTE,
                    "CERT-2025-0001", WithholdingType.INCOME_TAX, ANO_GRAVABLE, "2025-A",
                    TARIFA_RENTA, IMPORTE_CERTIFICADO, EXPEDIDO_EL, VENCE_EL, RECIBIDO_EL,
                    "s3://certificados/llego.pdf", SubstituteEvidenceKind.PAYMENT_RECEIPT,
                    "s3://pagos/REC-1.pdf", CREADO_EL)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not allowed once the certificate arrived");
        }

        @Test
        @DisplayName("los dos campos del sustituto nulos son el estado normal")
        void los_dos_campos_nulos_son_el_estado_normal() {
            assertThat(WithholdingCertificateMother.deRenta().getSubstituteEvidenceKind()).isNull();
        }
    }

    // --- andamio ------------------------------------------------------------

    private static WithholdingCertificate conPeriodo(WithholdingType tipo, int ano, String clave) {
        return new WithholdingCertificate(null, COMPANY_ID, NIT_DEL_CLIENTE, "CERT-2025-0001", tipo,
                ano, clave, TARIFA_RENTA, IMPORTE_CERTIFICADO, EXPEDIDO_EL, VENCE_EL, null, null,
                null, null, CREADO_EL);
    }

    private static WithholdingCertificate conTarifa(BigDecimal tarifa) {
        return new WithholdingCertificate(null, COMPANY_ID, NIT_DEL_CLIENTE, "CERT-2025-0001",
                WithholdingType.INCOME_TAX, ANO_GRAVABLE, "2025-A", tarifa, IMPORTE_CERTIFICADO,
                EXPEDIDO_EL, VENCE_EL, null, null, null, null, CREADO_EL);
    }
}
