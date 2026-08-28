package com.vetsoftware.app.withholdingraterule.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.withholdingraterule.testsupport.WithholdingRateRuleMother;
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
 * Las invariantes de la tarifa de retencion, que viven en el constructor y no
 * en el service ni en el controller.
 *
 * <p>
 * Dos de los grupos de aqui existen contra fallos que <b>no dan error</b>:
 * {@link EnumsCompartidos} congela los literales que se comparten con
 * {@code catalog_items} —divergir en uno deja la retencion esperada en cero sin
 * una sola excepcion— y {@link LaTarifaEsPorcentaje} congela la unidad y la
 * escala, que es donde se pierde casi un uno por ciento por factura.
 */
@DisplayName("WithholdingRateRule — la tarifa de retencion y sus invariantes")
class WithholdingRateRuleTest {

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("una tarifa nacional nace habilitada, abierta y sin version")
        void una_tarifa_nacional_nace_habilitada_abierta_y_sin_version() {
            WithholdingRateRule regla = WithholdingRateRuleMother.nueva();

            assertThat(regla.getId()).isNull();
            assertThat(regla.getVersion()).isNull();
            assertThat(regla.isEnabled()).isTrue();
            assertThat(regla.isOpen()).isTrue();
            assertThat(regla.getValidTo()).isNull();
            assertThat(regla.getMunicipalityCode()).isNull();
        }

        @Test
        @DisplayName("conserva cada campo en su sitio y no cruza las dos bases minimas")
        void conserva_cada_campo_y_no_cruza_las_dos_bases() {
            WithholdingRateRule regla = WithholdingRateRuleMother.ica();

            assertThat(regla.getWithholdingType()).isEqualTo(WithholdingType.ICA);
            assertThat(regla.getServiceNature()).isEqualTo(ServiceNature.CONSULTING);
            assertThat(regla.getMunicipalityCode()).isEqualTo(WithholdingRateRuleMother.BOGOTA);
            // Las dos son BigDecimal y cruzarlas compila: con valores iguales
            // ningun test lo veria, con estos si.
            assertThat(regla.getMinimumBaseAmount()).isEqualByComparingTo("213010.00");
            assertThat(regla.getMinimumBaseUvt()).isEqualByComparingTo("4.00");
            assertThat(regla.getLegalReference()).isEqualTo("Acuerdo 65 de 2002");
            assertThat(regla.getCreatedDate()).isEqualTo(WithholdingRateRuleMother.CREADA_EL);
        }

        @Test
        @DisplayName("una tarifa cargada ya cerrada es valida: asi entra el historico")
        void una_tarifa_cargada_ya_cerrada_es_valida() {
            WithholdingRateRule regla = WithholdingRateRuleMother.cerrada();

            assertThat(regla.isOpen()).isFalse();
            assertThat(regla.getValidTo()).isEqualTo(WithholdingRateRuleMother.HASTA);
        }

        @ParameterizedTest(name = "{1}")
        @CsvSource({"withholdingType, withholdingType is required",
                "serviceNature, serviceNature is required", "ratePercent, ratePercent is required",
                "validFrom, validFrom is required", "createdDate, createdDate is required"})
        @DisplayName("rechaza cada campo obligatorio que falte")
        void rechaza_cada_campo_obligatorio_que_falte(String campo, String mensaje) {
            assertThatThrownBy(() -> reglaSin(campo)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(mensaje);
        }

        @Test
        @DisplayName("una referencia legal de mas de 255 caracteres no cabe en la columna")
        void una_referencia_legal_demasiado_larga_no_cabe() {
            assertThatThrownBy(
                    () -> nacionalCon(WithholdingRateRuleMother.RENTA_SERVICIOS, "x".repeat(256)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("legalReference must be 255 chars or less");
        }
    }

    @Nested
    @DisplayName("Enums compartidos con catalog_items")
    class EnumsCompartidos {

        @Test
        @DisplayName("ServiceNature declara EXACTAMENTE los tres literales del CHECK")
        void service_nature_declara_exactamente_los_tres_literales() {
            // Este caso es la unica red que hay contra el fallo mas caro del
            // modelo. La lista se comparte con catalog_items (changeset 229) y con
            // chk_withholding_rate_rules_service_nature (317): si alguien renombra
            // aqui un valor, la busqueda de la tarifa devuelve VACIO, la retencion
            // esperada sale cero y NO HAY ERROR. Nadie se entera hasta que el
            // cliente gira de menos y la cartera no cuadra.
            assertThat(ServiceNature.values()).extracting(Enum::name)
                    .containsExactly("SOFTWARE_LICENSING", "TECHNICAL_SERVICE", "CONSULTING");
        }

        @Test
        @DisplayName("WithholdingType declara EXACTAMENTE los tres literales del CHECK")
        void withholding_type_declara_exactamente_los_tres_literales() {
            assertThat(WithholdingType.values()).extracting(Enum::name)
                    .containsExactly("INCOME_TAX", "VAT", "ICA");
        }
    }

    @Nested
    @DisplayName("El municipio: si y solo si es ICA")
    class ElMunicipio {

        @Test
        @DisplayName("ICA sin municipio no se puede escribir")
        void ica_sin_municipio_no_se_puede_escribir() {
            assertThatThrownBy(() -> icaCon(null)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("municipalityCode is required for ICA");
        }

        @ParameterizedTest(name = "{0} con municipio")
        @EnumSource(value = WithholdingType.class, names = {"INCOME_TAX", "VAT"})
        @DisplayName("una retencion nacional con municipio tampoco: seria un supuesto duplicado")
        void una_retencion_nacional_con_municipio_tampoco(WithholdingType tipo) {
            // Sin esta mitad habria dos filas para el mismo supuesto nacional —una
            // con municipio y otra sin el— que la unicidad no veria como iguales.
            assertThatThrownBy(() -> conTipoYMunicipio(tipo, WithholdingRateRuleMother.BOGOTA))
                    .isInstanceOf(IllegalArgumentException.class).hasMessage(
                            "municipalityCode must be absent unless the withholding type is ICA");
        }

        @ParameterizedTest
        @ValueSource(strings = {"110", "110011", "1"})
        @DisplayName("el codigo del municipio tiene cinco caracteres, ni uno mas ni uno menos")
        void el_codigo_del_municipio_tiene_cinco_caracteres(String codigo) {
            assertThatThrownBy(() -> icaCon(codigo)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("municipalityCode must be 5 characters");
        }

        @Test
        @DisplayName("la clave del municipio usa el centinela cuando la retencion es nacional")
        void la_clave_del_municipio_usa_el_centinela_cuando_es_nacional() {
            // Es el mismo valor que la base calcula en municipality_key. Existe
            // porque en SQL dos NULL no son iguales: sin el, dos tarifas
            // nacionales del mismo supuesto no chocarian en el indice unico y la
            // consulta devolveria dos filas para la misma vigencia.
            assertThat(WithholdingRateRuleMother.nacional().municipalityKey()).isEqualTo("-");
            assertThat(WithholdingRateRuleMother.ica().municipalityKey()).isEqualTo("11001");
        }
    }

    @Nested
    @DisplayName("La tarifa es un PORCENTAJE, no una fraccion")
    class LaTarifaEsPorcentaje {

        @Test
        @DisplayName("el ICA de Bogota, 6,9 por mil, se guarda como 0.690000 con sus 6 decimales")
        void el_ica_de_bogota_se_guarda_como_069_con_seis_decimales() {
            BigDecimal tarifa = WithholdingRateRuleMother.ica().getRatePercent();

            // La unidad va en el nombre de la columna, no en un comentario.
            assertThat(tarifa).isEqualByComparingTo("0.69");
            // Y NO es la fraccion: 0.0069 seria el mismo numero en la otra
            // lectura, y retendria cien veces menos.
            assertThat(tarifa).isNotEqualByComparingTo("0.0069");
            // Ni el por mil crudo, que retendria diez veces mas.
            assertThat(tarifa).isNotEqualByComparingTo("6.9");
            // La escala sobrevive entera. Con dos decimales, un 4,14 por mil
            // —0.414000— se cortaria a 0.41 y se retendria casi un uno por ciento
            // de menos en cada factura, calculado en silencio.
            assertThat(tarifa.scale()).isEqualTo(6);
        }

        @Test
        @DisplayName("acepta el 4,14 por mil entero, que es el que se pierde al redondear")
        void acepta_el_414_por_mil_entero() {
            assertThatCode(() -> nacionalCon(new BigDecimal("0.414000"), "Acuerdo local"))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({"0.000000, ratePercent must be greater than zero",
                "-0.500000, ratePercent must be greater than zero",
                "100.000001, ratePercent must not exceed 100",
                "0.1234567, ratePercent must have 6 decimals or fewer"})
        @DisplayName("rechaza la tarifa fuera de rango y la que no cabe en la escala")
        void rechaza_la_tarifa_fuera_de_rango_y_la_que_no_cabe(String tarifa, String mensaje) {
            assertThatThrownBy(() -> nacionalCon(new BigDecimal(tarifa), "Referencia"))
                    .isInstanceOf(IllegalArgumentException.class).hasMessage(mensaje);
        }

        @Test
        @DisplayName("el 100 % justo si entra: el tope es inclusivo")
        void el_cien_por_ciento_justo_si_entra() {
            assertThatCode(() -> nacionalCon(new BigDecimal("100.000000"), "Referencia"))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("La base minima, en pesos y en unidades")
    class LaBaseMinima {

        @Test
        @DisplayName("sin ninguna de las dos bases la regla no se puede escribir")
        void sin_ninguna_de_las_dos_bases_no_se_puede_escribir() {
            assertThatThrownBy(() -> conBases(null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("at least one of minimumBaseAmount or minimumBaseUvt is required");
        }

        @Test
        @DisplayName("basta con la de pesos: el numero que envejece cada ano")
        void basta_con_la_de_pesos() {
            assertThatCode(() -> conBases(new BigDecimal("213010.00"), null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("basta con la de UVT: el numero que no envejece")
        void basta_con_la_de_uvt() {
            assertThatCode(() -> conBases(null, new BigDecimal("4.00"))).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("cero es una base valida: hay supuestos que retienen desde el primer peso")
        void cero_es_una_base_valida() {
            assertThatCode(() -> conBases(BigDecimal.ZERO, null)).doesNotThrowAnyException();
        }

        @ParameterizedTest(name = "pesos={0} uvt={1}")
        @CsvSource({"-1.00, , minimumBaseAmount must not be negative",
                ", -1.00, minimumBaseUvt must not be negative"})
        @DisplayName("una base negativa no tiene sentido en ninguna de las dos unidades")
        void una_base_negativa_no_tiene_sentido(BigDecimal pesos, BigDecimal uvt, String mensaje) {
            assertThatThrownBy(() -> conBases(pesos, uvt))
                    .isInstanceOf(IllegalArgumentException.class).hasMessage(mensaje);
        }
    }

    @Nested
    @DisplayName("Vigencia y cierre")
    class VigenciaYCierre {

        @ParameterizedTest
        @ValueSource(strings = {"2026-01-01", "2025-12-31"})
        @DisplayName("la fecha de fin tiene que ser estrictamente posterior a la de inicio")
        void la_fecha_de_fin_tiene_que_ser_estrictamente_posterior(String hasta) {
            assertThatThrownBy(
                    () -> conVigencia(WithholdingRateRuleMother.DESDE, LocalDate.parse(hasta)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("validTo must be after validFrom");
        }

        @Test
        @DisplayName("cerrar deja la fecha de fin y conserva la version para el bloqueo optimista")
        void cerrar_deja_la_fecha_y_conserva_la_version() {
            WithholdingRateRule cerrada = WithholdingRateRuleMother.nacional()
                    .close(WithholdingRateRuleMother.HASTA);

            assertThat(cerrada.getValidTo()).isEqualTo(WithholdingRateRuleMother.HASTA);
            assertThat(cerrada.isOpen()).isFalse();
            // Sin la version, el save posterior seria un insert y no una edicion:
            // dos vigencias para el mismo supuesto, que es lo que las columnas
            // generadas existen para impedir.
            assertThat(cerrada.getVersion()).isEqualTo(0L);
            assertThat(cerrada.getId()).isEqualTo(WithholdingRateRuleMother.nacional().getId());
        }

        @Test
        @DisplayName("cerrar no muta la regla original")
        void cerrar_no_muta_la_regla_original() {
            WithholdingRateRule original = WithholdingRateRuleMother.nacional();

            original.close(WithholdingRateRuleMother.HASTA);

            assertThat(original.getValidTo()).isNull();
            assertThat(original.isOpen()).isTrue();
        }

        @Test
        @DisplayName("cerrar dos veces se rechaza: la base no lo impide y machacaria la fecha")
        void cerrar_dos_veces_se_rechaza() {
            // current_rule_marker vale NULL en una regla cerrada, y una unicidad
            // sobre columna nula no restringe nada: el segundo cierre pasaria en
            // silencio y se llevaria por delante la fecha del primero.
            WithholdingRateRule cerrada = WithholdingRateRuleMother.cerrada();

            assertThatThrownBy(() -> cerrada.close(LocalDate.of(2028, 1, 1)))
                    .isInstanceOf(WithholdingRateRuleAlreadyClosedException.class)
                    .hasMessageContaining("8303").hasMessageContaining("2027-01-01");
        }

        @Test
        @DisplayName("una regla abierta aplica desde su fecha de inicio y no antes")
        void una_regla_abierta_aplica_desde_su_fecha_y_no_antes() {
            WithholdingRateRule regla = WithholdingRateRuleMother.nacional();

            assertThat(regla.isEffectiveOn(LocalDate.of(2025, 12, 31))).isFalse();
            assertThat(regla.isEffectiveOn(WithholdingRateRuleMother.DESDE)).isTrue();
            assertThat(regla.isEffectiveOn(LocalDate.of(2030, 6, 1))).isTrue();
        }

        @Test
        @DisplayName("el dia de la fecha de fin la regla YA no aplica: el limite es estricto")
        void el_dia_de_la_fecha_de_fin_ya_no_aplica() {
            // Es lo que permite que la regla que se cierra el 1 de enero y la que
            // empieza ese mismo dia se releven sin pisarse. Un >= aqui haria que
            // las dos aplicaran a la vez durante un dia entero.
            WithholdingRateRule cerrada = WithholdingRateRuleMother.cerrada();

            assertThat(cerrada.isEffectiveOn(LocalDate.of(2026, 12, 31))).isTrue();
            assertThat(cerrada.isEffectiveOn(WithholdingRateRuleMother.HASTA)).isFalse();
        }
    }

    // --- andamio ------------------------------------------------------------

    private static WithholdingRateRule reglaSin(String campo) {
        return new WithholdingRateRule(null,
                "withholdingType".equals(campo) ? null : WithholdingType.INCOME_TAX,
                "serviceNature".equals(campo) ? null : ServiceNature.TECHNICAL_SERVICE, null,
                "ratePercent".equals(campo) ? null : WithholdingRateRuleMother.RENTA_SERVICIOS,
                WithholdingRateRuleMother.BASE_EN_PESOS, null, null,
                "validFrom".equals(campo) ? null : WithholdingRateRuleMother.DESDE, null,
                "createdDate".equals(campo) ? null : WithholdingRateRuleMother.CREADA_EL, true,
                null);
    }

    private static WithholdingRateRule nacionalCon(BigDecimal tarifa, String referenciaLegal) {
        return new WithholdingRateRule(null, WithholdingType.INCOME_TAX,
                ServiceNature.TECHNICAL_SERVICE, null, tarifa,
                WithholdingRateRuleMother.BASE_EN_PESOS, null, referenciaLegal,
                WithholdingRateRuleMother.DESDE, null, WithholdingRateRuleMother.CREADA_EL, true,
                null);
    }

    private static WithholdingRateRule icaCon(String municipio) {
        return conTipoYMunicipio(WithholdingType.ICA, municipio);
    }

    private static WithholdingRateRule conTipoYMunicipio(WithholdingType tipo, String municipio) {
        return new WithholdingRateRule(null, tipo, ServiceNature.CONSULTING, municipio,
                WithholdingRateRuleMother.ICA_BOGOTA, WithholdingRateRuleMother.BASE_EN_PESOS, null,
                null, WithholdingRateRuleMother.DESDE, null, WithholdingRateRuleMother.CREADA_EL,
                true, null);
    }

    private static WithholdingRateRule conBases(BigDecimal pesos, BigDecimal uvt) {
        return new WithholdingRateRule(null, WithholdingType.INCOME_TAX,
                ServiceNature.TECHNICAL_SERVICE, null, WithholdingRateRuleMother.RENTA_SERVICIOS,
                pesos, uvt, null, WithholdingRateRuleMother.DESDE, null,
                WithholdingRateRuleMother.CREADA_EL, true, null);
    }

    private static WithholdingRateRule conVigencia(LocalDate desde, LocalDate hasta) {
        return new WithholdingRateRule(null, WithholdingType.INCOME_TAX,
                ServiceNature.TECHNICAL_SERVICE, null, WithholdingRateRuleMother.RENTA_SERVICIOS,
                WithholdingRateRuleMother.BASE_EN_PESOS, null, null, desde, hasta,
                LocalDateTime.of(2026, 1, 3, 8, 45), true, null);
    }
}
