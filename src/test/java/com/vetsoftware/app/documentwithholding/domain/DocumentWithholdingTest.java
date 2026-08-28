package com.vetsoftware.app.documentwithholding.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.documentwithholding.testsupport.DocumentWithholdingMother;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Las invariantes de la retencion, que viven en el constructor y no en el
 * servicio.
 *
 * <p>
 * <b>Cada bloque de aqui replica un CHECK del changeset 329</b>, y esa
 * duplicidad es deliberada: la base es el cinturon y el dominio el tirante. Sin
 * el dominio, el error llega como una violacion de integridad que el operador
 * no sabe leer; sin la base, cualquier escritura que no pase por el agregado
 * —una migracion de datos, un script de correccion— mete filas que ninguna
 * declaracion puede usar.
 */
@DisplayName("DocumentWithholding — las invariantes de la retencion")
class DocumentWithholdingTest {

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("una retencion recien practicada nace sin respaldo documental")
        void nace_sin_respaldo_documental() {
            DocumentWithholding retencion = DocumentWithholdingMother.renta();

            // El nulo no es un dato que falte: es el estado del negocio. Mientras
            // dure, esta retencion es cartera que hay que reclamarle al cliente.
            assertThat(retencion.getCertificateId()).isNull();
            assertThat(retencion.isUncertified()).isTrue();
            assertThat(retencion.getId()).isNull();
            assertThat(retencion.getVersion()).isNull();
        }

        @Test
        @DisplayName("conserva cada importe y cada fecha en su sitio, sin cruzarlos")
        void conserva_cada_importe_y_cada_fecha_en_su_sitio() {
            DocumentWithholding retencion = DocumentWithholdingMother.ica();

            assertThat(retencion.getCompanyId()).isEqualTo(DocumentWithholdingMother.EMPRESA);
            assertThat(retencion.getBillingDocumentId())
                    .isEqualTo(DocumentWithholdingMother.FACTURA);
            assertThat(retencion.getType()).isEqualTo(WithholdingType.ICA);
            assertThat(retencion.getTaxableBase()).isEqualByComparingTo("1234567.89");
            assertThat(retencion.getAmount()).isEqualByComparingTo("8518.52");
            assertThat(retencion.getMunicipalityCode()).isEqualTo("05001");
            assertThat(retencion.getFiscalYear()).isEqualTo(2026);
            assertThat(retencion.getFiscalPeriodKey()).isEqualTo("2026-B02");
            assertThat(retencion.getPracticedOn()).isEqualTo(LocalDate.of(2026, 3, 5));
            assertThat(retencion.getCreatedDate())
                    .isEqualTo(LocalDateTime.of(2026, 3, 7, 8, 45, 0));
        }

        @Test
        @DisplayName("los tres literales del tipo son los que el CHECK admite y no otros")
        void los_tres_literales_del_tipo_son_los_que_el_check_admite() {
            // Renombrar cualquiera de los tres a RENTA o RETEIVA compilaria sin una
            // queja y dejaria la feature escribiendo filas que la base rechaza una a
            // una, con un error de integridad que no dice por que.
            assertThat(WithholdingType.values()).extracting(Enum::name)
                    .containsExactly("INCOME_TAX", "VAT", "ICA");
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("sin ano gravable no hay retencion: un ano fuera de rango se rechaza")
        void un_ano_fuera_de_rango_se_rechaza() {
            // Perder el ano gravable es perder el derecho a descontar la retencion, y
            // eso es pagar dos veces el mismo impuesto: una via retencion y otra al
            // declarar. El rango caza ademas el error de escribirlo con dos digitos.
            assertThatThrownBy(() -> retencionDeRentaDelAno(26))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("fiscalYear must be between 2020 and 2100");
            assertThatThrownBy(() -> retencionDeRentaDelAno(2019))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("fiscalYear");
            assertThatThrownBy(() -> retencionDeRentaDelAno(2101))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("fiscalYear");
        }

        @ParameterizedTest(name = "{0} con periodo {1} es {2}")
        @CsvSource({
                // La renta es anual: su unica forma valida es AAAA-A.
                "INCOME_TAX, 2026-A, true", "INCOME_TAX, 2026-B01, false",
                "INCOME_TAX, 2026-B03, false",
                // El IVA y el ICA se imputan por bimestre: seis periodos y ni uno mas.
                "VAT, 2026-B01, true", "VAT, 2026-B06, true", "VAT, 2026-B07, false",
                "VAT, 2026-B00, false", "VAT, 2026-A, false", "ICA, 2026-B02, true",
                "ICA, 2026-B06, true", "ICA, 2026-A, false", "ICA, 2026-B7, false",
                // El ano de la clave tiene que ser el mismo que fiscalYear: sin esta
                // mitad, una retencion de 2026 se declararia en un periodo de 2025.
                "INCOME_TAX, 2025-A, false", "VAT, 2025-B02, false", "ICA, 2025-B02, false",
                // Formas sueltas que un teclado produce con facilidad.
                "VAT, 2026B02, false", "VAT, 26-B02, false", "INCOME_TAX, 2026-a, false"})
        @DisplayName("la forma del periodo fiscal la impone el tipo, replicando el CHECK")
        void la_forma_del_periodo_fiscal_la_impone_el_tipo(WithholdingType tipo, String periodo,
                boolean valido) {
            // Replica exacta de chk_document_withholdings_period. Con la granularidad
            // anual que tenia la ficha no se podia armar NINGUNA de las dos
            // declaraciones bimestrales: habria que reconstruir el bimestre a mano
            // desde practicedOn, y esa reconstruccion no cuadra con lo presentado.
            assertThat(esPeriodoAceptado(tipo, periodo)).isEqualTo(valido);
        }

        @ParameterizedTest
        @EnumSource(value = WithholdingType.class, names = {"INCOME_TAX", "VAT"})
        @DisplayName("el municipio esta prohibido cuando la retencion no es de ICA")
        void el_municipio_esta_prohibido_fuera_de_ica(WithholdingType tipo) {
            // Un municipio en una retencion nacional afirma un hecho falso: la
            // retefuente y el reteiva no son municipales y no hay tarifa local que
            // verificar.
            assertThatThrownBy(() -> retencionCon(tipo, periodoDe(tipo), "05001"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("municipalityCode is only allowed for ICA");
        }

        @Test
        @DisplayName("el municipio es obligatorio en ICA porque la tarifa cambia con el")
        void el_municipio_es_obligatorio_en_ica() {
            assertThatThrownBy(() -> retencionCon(WithholdingType.ICA, "2026-B02", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("municipalityCode is required for ICA");
            assertThatThrownBy(() -> retencionCon(WithholdingType.ICA, "2026-B02", "   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("municipalityCode is required for ICA");
        }

        @ParameterizedTest
        @ValueSource(strings = {"5001", "050011", "0500A", "ABCDE"})
        @DisplayName("el municipio son cinco digitos DIVIPOLA y no cualquier cadena")
        void el_municipio_son_cinco_digitos_divipola(String codigo) {
            // '5001' y '05001' son el mismo municipio escrito de dos formas, y el cero
            // perdido es justo lo que el LPAD del changeset 315 tuvo que arreglar. Aqui
            // se rechaza antes de que llegue a la base.
            assertThatThrownBy(() -> retencionCon(WithholdingType.ICA, "2026-B02", codigo))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("municipalityCode must be five digits");
        }

        @Test
        @DisplayName("la tarifa es un PORCENTAJE: una fraccion cabe pero un 101 no")
        void la_tarifa_es_un_porcentaje() {
            // El tope es 100 y no 1 porque la columna guarda porcentaje. Una tarifa
            // escrita como fraccion pasaria este control sin una queja —por eso hace
            // falta ademas el caso de la tarifa por mil, mas abajo—, pero un valor
            // mayor que 100 no es una tarifa de nada.
            assertThat(retencionConTarifa(new BigDecimal("100.000000")).getRatePercent())
                    .isEqualByComparingTo("100");
            assertThatThrownBy(() -> retencionConTarifa(new BigDecimal("100.000001")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ratePercent must not exceed 100");
            assertThatThrownBy(() -> retencionConTarifa(BigDecimal.ZERO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ratePercent must be greater than zero");
        }

        @Test
        @DisplayName("una tarifa por mil real conserva sus seis decimales sin redondearse")
        void una_tarifa_por_mil_conserva_sus_seis_decimales() {
            // 4,14 por mil es 0,414 %. Con dos decimales se guardaria 0,41 y base por
            // tarifa dejaria de dar el importe certificado: se retiene de menos, en
            // silencio, en cada factura y siempre en la misma direccion.
            DocumentWithholding retencion = retencionConTarifa(new BigDecimal("0.414000"));

            assertThat(retencion.getRatePercent()).isEqualByComparingTo("0.414000");
            assertThat(retencion.getRatePercent().scale()).isEqualTo(6);
            // Y no es lo mismo que 0,41: si alguien recortara la escala, esta
            // comparacion exacta se pone roja.
            assertThat(retencion.getRatePercent()).isNotEqualByComparingTo("0.410000");
        }

        @Test
        @DisplayName("retener mas que la base gravable se rechaza, y el peso justo pasa")
        void retener_mas_que_la_base_se_rechaza() {
            // Replica de chk_document_withholdings_amounts. Retener por encima de la
            // base es plata que sale sin nada que la sostenga ante la DIAN.
            assertThatThrownBy(
                    () -> retencionCon(new BigDecimal("1000.00"), new BigDecimal("1000.01")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("amount must not exceed taxableBase");
            assertThat(
                    retencionCon(new BigDecimal("1000.00"), new BigDecimal("1000.00")).getAmount())
                    .isEqualByComparingTo("1000.00");
        }

        @Test
        @DisplayName("ni la base ni el valor retenido pueden ser cero o negativos")
        void ni_la_base_ni_el_retenido_pueden_ser_cero() {
            assertThatThrownBy(() -> retencionCon(BigDecimal.ZERO, new BigDecimal("1.00")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("taxableBase must be greater than zero");
            assertThatThrownBy(
                    () -> retencionCon(new BigDecimal("1000.00"), new BigDecimal("-1.00")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("amount must be greater than zero");
        }

        @Test
        @DisplayName("la empresa y la factura de cobro son obligatorias")
        void la_empresa_y_la_factura_son_obligatorias() {
            assertThatThrownBy(() -> DocumentWithholding.register(null,
                    DocumentWithholdingMother.FACTURA, WithholdingType.INCOME_TAX,
                    DocumentWithholdingMother.BASE_GRAVABLE, DocumentWithholdingMother.TARIFA_RENTA,
                    DocumentWithholdingMother.RETENIDO, null, 2026, "2026-A",
                    DocumentWithholdingMother.PRACTICADA_EL, DocumentWithholdingMother.CREADA_EL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("companyId is required");
            assertThatThrownBy(() -> DocumentWithholding.register(DocumentWithholdingMother.EMPRESA,
                    null, WithholdingType.INCOME_TAX, DocumentWithholdingMother.BASE_GRAVABLE,
                    DocumentWithholdingMother.TARIFA_RENTA, DocumentWithholdingMother.RETENIDO,
                    null, 2026, "2026-A", DocumentWithholdingMother.PRACTICADA_EL,
                    DocumentWithholdingMother.CREADA_EL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("billingDocumentId is required");
        }
    }

    @Nested
    @DisplayName("Certificacion")
    class Certificacion {

        @Test
        @DisplayName("apuntarla a su certificado devuelve otra instancia y conserva la version")
        void apuntarla_a_su_certificado_conserva_la_version() {
            DocumentWithholding sinRespaldo = DocumentWithholdingMother.yaRegistrada(41L);

            DocumentWithholding respaldada = sinRespaldo.linkTo(8410L);

            assertThat(respaldada).isNotSameAs(sinRespaldo);
            assertThat(respaldada.getCertificateId()).isEqualTo(8410L);
            assertThat(respaldada.isUncertified()).isFalse();
            // La version viaja intacta: es lo unico que permite al UPDATE detectar que
            // otro operario certifico esta misma fila mientras tanto.
            assertThat(respaldada.getVersion()).isEqualTo(sinRespaldo.getVersion());
            assertThat(respaldada.getId()).isEqualTo(41L);
            // Y la original no se toco: la tabla solo se agrega.
            assertThat(sinRespaldo.getCertificateId()).isNull();
        }

        @Test
        @DisplayName("repetir el mismo certificado es idempotente y no falla")
        void repetir_el_mismo_certificado_es_idempotente() {
            DocumentWithholding respaldada = DocumentWithholdingMother.yaCertificada(41L, 8410L);

            // Cubre el reintento del operador sin castigarlo: el resultado que pedia
            // ya es el que hay.
            assertThat(respaldada.linkTo(8410L)).isSameAs(respaldada);
        }

        @Test
        @DisplayName("repuntarla a OTRO certificado es un conflicto, no una correccion")
        void repuntarla_a_otro_certificado_es_un_conflicto() {
            DocumentWithholding respaldada = DocumentWithholdingMother.yaCertificada(41L, 8410L);

            // Repuntar en silencio dejaria una declaracion ya presentada respaldada por
            // un papel distinto del que se uso, y sin rastro del cambio.
            assertThatThrownBy(() -> respaldada.linkTo(8411L))
                    .isInstanceOf(WithholdingAlreadyCertifiedException.class)
                    .hasMessageContaining("already backed by certificate 8410")
                    .hasMessageContaining("cannot relink it to 8411");
        }

        @Test
        @DisplayName("apuntarla a un certificado nulo se rechaza")
        void apuntarla_a_un_certificado_nulo_se_rechaza() {
            assertThatThrownBy(() -> DocumentWithholdingMother.yaRegistrada(41L).linkTo(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("certificateId is required");
        }
    }

    // --- andamio ------------------------------------------------------------

    private static boolean esPeriodoAceptado(WithholdingType tipo, String periodo) {
        return construyeSinError(
                () -> retencionCon(tipo, periodo, tipo == WithholdingType.ICA ? "05001" : null));
    }

    /**
     * Envuelve la construccion para que el caso parametrizado pueda afirmar sobre
     * un booleano en vez de bifurcar en su cuerpo: la convencion de la casa prohibe
     * {@code if} y {@code try} dentro de un test, no en su andamio.
     */
    private static boolean construyeSinError(Runnable construccion) {
        try {
            construccion.run();
            return true;
        } catch (IllegalArgumentException rechazada) {
            return false;
        }
    }

    private static String periodoDe(WithholdingType tipo) {
        return tipo == WithholdingType.INCOME_TAX ? "2026-A" : "2026-B02";
    }

    private static DocumentWithholding retencionDeRentaDelAno(int ano) {
        return DocumentWithholding.register(DocumentWithholdingMother.EMPRESA,
                DocumentWithholdingMother.FACTURA, WithholdingType.INCOME_TAX,
                DocumentWithholdingMother.BASE_GRAVABLE, DocumentWithholdingMother.TARIFA_RENTA,
                DocumentWithholdingMother.RETENIDO, null, ano, ano + "-A",
                DocumentWithholdingMother.PRACTICADA_EL, DocumentWithholdingMother.CREADA_EL);
    }

    private static DocumentWithholding retencionCon(WithholdingType tipo, String periodo,
            String municipio) {
        return DocumentWithholding.register(DocumentWithholdingMother.EMPRESA,
                DocumentWithholdingMother.FACTURA, tipo, DocumentWithholdingMother.BASE_GRAVABLE,
                DocumentWithholdingMother.TARIFA_RENTA, DocumentWithholdingMother.RETENIDO,
                municipio, 2026, periodo, DocumentWithholdingMother.PRACTICADA_EL,
                DocumentWithholdingMother.CREADA_EL);
    }

    private static DocumentWithholding retencionCon(BigDecimal base, BigDecimal retenido) {
        return DocumentWithholding.register(DocumentWithholdingMother.EMPRESA,
                DocumentWithholdingMother.FACTURA, WithholdingType.INCOME_TAX, base,
                DocumentWithholdingMother.TARIFA_RENTA, retenido, null, 2026, "2026-A",
                DocumentWithholdingMother.PRACTICADA_EL, DocumentWithholdingMother.CREADA_EL);
    }

    private static DocumentWithholding retencionConTarifa(BigDecimal tarifa) {
        return DocumentWithholding.register(DocumentWithholdingMother.EMPRESA,
                DocumentWithholdingMother.FACTURA, WithholdingType.INCOME_TAX,
                DocumentWithholdingMother.BASE_GRAVABLE, tarifa, DocumentWithholdingMother.RETENIDO,
                null, 2026, "2026-A", DocumentWithholdingMother.PRACTICADA_EL,
                DocumentWithholdingMother.CREADA_EL);
    }
}
