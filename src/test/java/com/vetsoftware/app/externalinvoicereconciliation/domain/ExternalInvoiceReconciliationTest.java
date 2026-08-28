package com.vetsoftware.app.externalinvoicereconciliation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.externalinvoicereconciliation.testsupport.ExternalInvoiceReconciliationMother;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * <b>La regla que esta clase existe para vigilar son los dos pesos.</b>
 *
 * <p>
 * La tolerancia no es indulgencia: el total propio se calcula una vez sobre la
 * base agregada y el emisor externo lo calcula linea a linea, asi que los dos
 * redondeos al centavo no caen en el mismo sitio. Eso produce diferencias de
 * hasta dos pesos que <b>no son un error</b>, y confundirlas con un descuadre
 * real llenaria la bandeja de ruido hasta que nadie la mirara. La matriz de
 * {@link Tolerancia} fija los cuatro bordes con sus dos signos —{@code 0},
 * {@code 0,01}, {@code 2,00} y {@code 2,01}—, que es donde se rompen las tres
 * implementaciones equivocadas posibles: la que compara sin {@code abs}, la que
 * usa {@code <} donde va {@code <=}, y la que compara con {@code equals}.
 *
 * <p>
 * <b>Lo de {@code equals} merece su propio caso.</b> {@code new
 * BigDecimal("2.00")} y {@code new BigDecimal("2.0")} son el mismo numero y NO
 * son {@code equals} —difieren en la escala—, asi que una comparacion por
 * {@code equals} clasificaria como {@code MISMATCH} un descuadre que esta justo
 * en el limite dependiendo de cuantos ceros trajera el JSON del tercero. Ese es
 * el tipo de defecto que pasa revision humana sin que nadie parpadee.
 */
@DisplayName("ExternalInvoiceReconciliation — la tolerancia de dos pesos y las parejas del esquema")
class ExternalInvoiceReconciliationTest {

    @Nested
    @DisplayName("Apertura")
    class Apertura {

        @Test
        @DisplayName("nace en MISSING_EXTERNAL con los cuatro campos externos vacios")
        void nace_en_missing_external_con_los_campos_externos_vacios() {
            ExternalInvoiceReconciliation abierta = ExternalInvoiceReconciliationMother.abierta();

            // Esta fila incompleta ES la alarma: mientras siga aqui hay dinero
            // devengado que nadie facturo. Si naciera ya conciliada, el caso que
            // importa -la factura que no llega nunca- no dejaria ninguna fila.
            assertThat(abierta.getStatus())
                    .isEqualTo(ExternalInvoiceReconciliationStatus.MISSING_EXTERNAL);
            assertThat(abierta.isMissingExternal()).isTrue();
            assertThat(abierta.isResolved()).isFalse();
            assertThat(abierta.getExternalInvoiceId()).isNull();
            assertThat(abierta.getExternalTotal()).isNull();
            assertThat(abierta.getExternalTax()).isNull();
            assertThat(abierta.getDifference()).isNull();
            assertThat(abierta.getComputedTotal()).isEqualByComparingTo("119000.00");
            assertThat(abierta.getComputedTax()).isEqualByComparingTo("19000.00");
        }

        @Test
        @DisplayName("un total propio negativo no se puede abrir")
        void un_total_propio_negativo_no_se_puede_abrir() {
            assertThatThrownBy(
                    () -> ExternalInvoiceReconciliation.open(900L, 8600L, new BigDecimal("-1.00"),
                            BigDecimal.ZERO, ExternalInvoiceReconciliationMother.CREADO_EL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("computedTotal cannot be negative");
        }

        @Test
        @DisplayName("un importe con tres decimales no entra: la base lo redondearia y el CHECK dejaria de cuadrar")
        void un_importe_con_tres_decimales_no_entra() {
            // La columna es DECIMAL(19,2). Con 100.005 la base guardaria 100.01 y
            // difference dejaria de ser la resta de los dos numeros guardados:
            // chk_eir_difference rechazaria la fila con un error de integridad que no
            // senala a la causa.
            assertThatThrownBy(
                    () -> ExternalInvoiceReconciliation.open(900L, 8600L, new BigDecimal("100.005"),
                            BigDecimal.ZERO, ExternalInvoiceReconciliationMother.CREADO_EL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("computedTotal must have at most 2 decimals");
        }

        @Test
        @DisplayName("un cero con ceros de sobra si entra: 100.000 es exactamente cien")
        void un_cero_con_ceros_de_sobra_si_entra() {
            // El limite mira la escala UTIL, no la escrita. Sin stripTrailingZeros,
            // un JSON que mandara 100.000 se rechazaria por un motivo falso.
            assertThat(
                    ExternalInvoiceReconciliation
                            .open(900L, 8600L, new BigDecimal("100.000"), new BigDecimal("0.000"),
                                    ExternalInvoiceReconciliationMother.CREADO_EL)
                            .getComputedTotal())
                    .isEqualByComparingTo("100.00");
        }
    }

    @Nested
    @DisplayName("Tolerancia")
    class Tolerancia {

        /**
         * Los cuatro bordes en sus dos signos. Un total externo MENOR que el propio da
         * diferencia positiva; uno mayor, negativa.
         */
        @ParameterizedTest(name = "externo {0} → diferencia {1} → {2}")
        @CsvSource({"119000.00, 0.00, MATCHED", "118999.99, 0.01, WITHIN_TOLERANCE",
                "119000.01, -0.01, WITHIN_TOLERANCE", "118998.00, 2.00, WITHIN_TOLERANCE",
                "119002.00, -2.00, WITHIN_TOLERANCE", "118997.99, 2.01, MISMATCH",
                "119002.01, -2.01, MISMATCH"})
        @DisplayName("clasifica cada borde de los dos pesos por su lado correcto")
        void clasifica_cada_borde_por_su_lado_correcto(BigDecimal totalExterno,
                BigDecimal diferenciaEsperada, ExternalInvoiceReconciliationStatus esperado) {
            ExternalInvoiceReconciliation reconciliation = ExternalInvoiceReconciliationMother
                    .abierta();

            reconciliation.match("FE-1043", null, totalExterno, new BigDecimal("19000.00"), null,
                    null, null, null);

            assertThat(reconciliation.getDifference()).isEqualByComparingTo(diferenciaEsperada);
            assertThat(reconciliation.getStatus()).isEqualTo(esperado);
        }

        @Test
        @DisplayName("el limite se compara con compareTo y no con equals: 2.0 y 2.00 son el mismo numero")
        void el_limite_se_compara_con_compare_to_y_no_con_equals() {
            // 2.0 no es equals a 2.00 -distinta escala- pero SI es el mismo numero.
            // Con equals, este caso saldria MISMATCH y el operador veria un descuadre
            // inventado por la forma en que el tercero escribio sus ceros.
            assertThat(ExternalInvoiceReconciliation.classify(new BigDecimal("2.0")))
                    .isEqualTo(ExternalInvoiceReconciliationStatus.WITHIN_TOLERANCE);
            assertThat(ExternalInvoiceReconciliation.classify(new BigDecimal("-2.0")))
                    .isEqualTo(ExternalInvoiceReconciliationStatus.WITHIN_TOLERANCE);
            assertThat(ExternalInvoiceReconciliation.classify(new BigDecimal("0.0")))
                    .isEqualTo(ExternalInvoiceReconciliationStatus.MATCHED);
            assertThat(ExternalInvoiceReconciliation.TOLERANCIA).isEqualByComparingTo("2.00");
        }

        @Test
        @DisplayName("la diferencia es propio menos externo, en ese orden")
        void la_diferencia_es_propio_menos_externo() {
            // Invertir la resta pasa el CHECK de la base solo por casualidad cuando la
            // diferencia es cero, y a partir de ahi rechaza cada escritura.
            ExternalInvoiceReconciliation reconciliation = ExternalInvoiceReconciliationMother
                    .abierta();

            reconciliation.match("FE-1043", null, new BigDecimal("118000.00"),
                    new BigDecimal("18840.34"), null, null, null, null);

            assertThat(reconciliation.getDifference()).isEqualByComparingTo("1000.00");
            assertThat(reconciliation.getStatus())
                    .isEqualTo(ExternalInvoiceReconciliationStatus.MISMATCH);
            // Y el impuesto se guarda aparte: con los dos numeros se puede decir si el
            // descuadre es de base o de calculo. Con solo el total, no.
            assertThat(reconciliation.getExternalTax()).isEqualByComparingTo("18840.34");
            assertThat(reconciliation.getComputedTax()).isEqualByComparingTo("19000.00");
        }
    }

    @Nested
    @DisplayName("Pareja externa")
    class ParejaExterna {

        @Test
        @DisplayName("registrar la factura dos veces sobre la misma conciliacion no se permite")
        void registrar_la_factura_dos_veces_no_se_permite() {
            ExternalInvoiceReconciliation reconciliation = ExternalInvoiceReconciliationMother
                    .conFacturaExterna(41L, new BigDecimal("119000.00"));

            assertThatThrownBy(() -> reconciliation.match("FE-9999", null, new BigDecimal("500.00"),
                    BigDecimal.ZERO, null, null, null, null))
                    .isInstanceOf(ExternalInvoiceAlreadyMatchedException.class)
                    .hasMessageContaining("External invoice already matched for reconciliation 41")
                    .hasMessageContaining("MATCHED");
        }

        @Test
        @DisplayName("una conciliacion conciliada no puede tener el total externo vacio")
        void una_conciliada_no_puede_tener_el_total_externo_vacio() {
            // Espejo de chk_eir_external_pair por el lado que el motor tambien vigila:
            // fuera de MISSING_EXTERNAL los cuatro campos van completos.
            assertThatThrownBy(() -> new ExternalInvoiceReconciliation(41L, 900L, 8600L, null, null,
                    null, null, "FE-1043", null, new BigDecimal("119000.00"),
                    new BigDecimal("19000.00"), null, new BigDecimal("19000.00"), BigDecimal.ZERO,
                    ExternalInvoiceReconciliationStatus.MATCHED, null, null, null, null,
                    ExternalInvoiceReconciliationMother.CREADO_EL, 0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("externalTotal is required");
        }

        @Test
        @DisplayName("una MISSING_EXTERNAL con factura externa no se puede construir")
        void una_missing_external_con_factura_externa_no_se_construye() {
            assertThatThrownBy(() -> new ExternalInvoiceReconciliation(41L, 900L, 8600L, null, null,
                    null, null, "FE-1043", null, new BigDecimal("119000.00"),
                    new BigDecimal("19000.00"), null, null, null,
                    ExternalInvoiceReconciliationStatus.MISSING_EXTERNAL, null, null, null, null,
                    ExternalInvoiceReconciliationMother.CREADO_EL, 0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("externalInvoiceId must be absent when missing external");
        }

        @Test
        @DisplayName("un estado que no corresponde a su diferencia no se puede construir")
        void un_estado_que_no_corresponde_a_su_diferencia_no_se_construye() {
            // chk_eir_difference comprueba la resta, NO la clasificacion: una fila con
            // difference = 1000.00 y status = MATCHED entraria en la base sin protestar.
            // La unica barandilla de esa combinacion es esta.
            assertThatThrownBy(() -> new ExternalInvoiceReconciliation(41L, 900L, 8600L, null, null,
                    null, null, "FE-1043", null, new BigDecimal("119000.00"),
                    new BigDecimal("19000.00"), new BigDecimal("118000.00"),
                    new BigDecimal("19000.00"), new BigDecimal("1000.00"),
                    ExternalInvoiceReconciliationStatus.MATCHED, null, null, null, null,
                    ExternalInvoiceReconciliationMother.CREADO_EL, 0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("status must match the classified difference");
        }

        @Test
        @DisplayName("una diferencia que no es la resta de sus propios sumandos no se puede construir")
        void una_diferencia_que_no_es_la_resta_no_se_construye() {
            assertThatThrownBy(() -> new ExternalInvoiceReconciliation(41L, 900L, 8600L, null, null,
                    null, null, "FE-1043", null, new BigDecimal("119000.00"),
                    new BigDecimal("19000.00"), new BigDecimal("118000.00"),
                    new BigDecimal("19000.00"), new BigDecimal("7.00"),
                    ExternalInvoiceReconciliationStatus.MISMATCH, null, null, null, null,
                    ExternalInvoiceReconciliationMother.CREADO_EL, 0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("difference must be computedTotal - externalTotal");
        }
    }

    @Nested
    @DisplayName("Rango de resolucion de numeracion")
    class RangoDeResolucion {

        @Test
        @DisplayName("los cuatro campos del rango entran juntos")
        void los_cuatro_campos_del_rango_entran_juntos() {
            ExternalInvoiceReconciliation reconciliation = ExternalInvoiceReconciliationMother
                    .abierta();

            reconciliation.match("FE-1043", "CUFE-0011", new BigDecimal("119000.00"),
                    new BigDecimal("19000.00"), "18764000000123", 1000, 5000,
                    ExternalInvoiceReconciliationMother.VIGENTE_HASTA);

            // Con esto se puede avisar ANTES de que se agote el rango o venza la
            // resolucion, en vez de descubrirlo el dia que una factura no sale.
            assertThat(reconciliation.getExternalResolutionNumber()).isEqualTo("18764000000123");
            assertThat(reconciliation.getExternalRangeFrom()).isEqualTo(1000);
            assertThat(reconciliation.getExternalRangeTo()).isEqualTo(5000);
            assertThat(reconciliation.getResolutionValidUntil())
                    .isEqualTo(LocalDate.of(2027, 1, 31));
        }

        @Test
        @DisplayName("un rango a medias no entra: falta la vigencia")
        void un_rango_a_medias_no_entra() {
            ExternalInvoiceReconciliation reconciliation = ExternalInvoiceReconciliationMother
                    .abierta();

            assertThatThrownBy(
                    () -> reconciliation.match("FE-1043", null, new BigDecimal("119000.00"),
                            new BigDecimal("19000.00"), "18764000000123", 1000, 5000, null))
                    .isInstanceOf(IllegalArgumentException.class).hasMessage(
                            "externalRangeFrom, externalRangeTo and resolutionValidUntil go together");
        }

        @Test
        @DisplayName("un rango que termina antes de empezar no entra")
        void un_rango_que_termina_antes_de_empezar_no_entra() {
            ExternalInvoiceReconciliation reconciliation = ExternalInvoiceReconciliationMother
                    .abierta();

            assertThatThrownBy(() -> reconciliation.match("FE-1043", null,
                    new BigDecimal("119000.00"), new BigDecimal("19000.00"), "18764000000123", 5000,
                    1000, ExternalInvoiceReconciliationMother.VIGENTE_HASTA))
                    .isInstanceOf(IllegalArgumentException.class).hasMessage(
                            "externalRangeTo must be greater than or equal to externalRangeFrom");
        }

        @Test
        @DisplayName("un rango de un solo numero si entra: desde == hasta")
        void un_rango_de_un_solo_numero_si_entra() {
            // El CHECK del esquema dice >=, no >. Un caso que probara solo 5000 > 1000
            // dejaria pasar una implementacion con > que rechaza el rango unitario.
            ExternalInvoiceReconciliation reconciliation = ExternalInvoiceReconciliationMother
                    .abierta();

            reconciliation.match("FE-1043", null, new BigDecimal("119000.00"),
                    new BigDecimal("19000.00"), "18764000000123", 7000, 7000,
                    ExternalInvoiceReconciliationMother.VIGENTE_HASTA);

            assertThat(reconciliation.getExternalRangeTo()).isEqualTo(7000);
        }
    }

    @Nested
    @DisplayName("Resolucion")
    class Resolucion {

        @Test
        @DisplayName("cierra el expediente con los cuatro campos a la vez")
        void cierra_el_expediente_con_los_cuatro_campos() {
            ExternalInvoiceReconciliation reconciliation = ExternalInvoiceReconciliationMother
                    .resuelta(41L, new BigDecimal("118998.00"));

            assertThat(reconciliation.isResolved()).isTrue();
            assertThat(reconciliation.getResolvedBySystemUserId()).isEqualTo(990L);
            assertThat(reconciliation.getResolutionNote())
                    .isEqualTo("Ajuste por redondeo del impuesto");
            assertThat(reconciliation.getPostingPeriod()).isEqualTo("2026-03");
            assertThat(reconciliation.getResolvedAt())
                    .isEqualTo(LocalDateTime.of(2026, 4, 11, 9, 20, 45));
            // El estado NO cambia al resolver: resolver explica el descuadre, no lo
            // hace desaparecer.
            assertThat(reconciliation.getStatus())
                    .isEqualTo(ExternalInvoiceReconciliationStatus.WITHIN_TOLERANCE);
        }

        @Test
        @DisplayName("una MISSING_EXTERNAL tambien se puede resolver")
        void una_missing_external_tambien_se_puede_resolver() {
            // «Este documento no lleva factura externa porque se anulo» es una
            // explicacion legitima. Sin poder escribirla, la bandeja de lo que falta se
            // llenaria de ruido permanente hasta que nadie la mirara.
            ExternalInvoiceReconciliation reconciliation = ExternalInvoiceReconciliationMother
                    .abiertaConId(41L);

            reconciliation.resolve(990L, "El documento se anulo antes de facturarse", "2026-03",
                    ExternalInvoiceReconciliationMother.RESUELTO_EL);

            assertThat(reconciliation.isMissingExternal()).isTrue();
            assertThat(reconciliation.isResolved()).isTrue();
        }

        @Test
        @DisplayName("resolver dos veces no se permite: seria reescribir el periodo de imputacion")
        void resolver_dos_veces_no_se_permite() {
            ExternalInvoiceReconciliation reconciliation = ExternalInvoiceReconciliationMother
                    .resuelta(41L, new BigDecimal("119000.00"));

            assertThatThrownBy(() -> reconciliation.resolve(991L, "Otra explicacion", "2026-04",
                    LocalDateTime.of(2026, 5, 1, 10, 0)))
                    .isInstanceOf(ExternalInvoiceReconciliationAlreadyResolvedException.class)
                    .hasMessageContaining("External invoice reconciliation 41 was already resolved")
                    .hasMessageContaining("2026-04-11T09:20:45");
        }

        @ParameterizedTest(name = "periodo invalido: {0}")
        @CsvSource({"2026-00", "2026-13", "2026-1", "26-03", "2026/03", "202603"})
        @DisplayName("un periodo contable con formato malo no cierra nada")
        void un_periodo_contable_con_formato_malo_no_cierra_nada(String periodo) {
            // Mes acotado a 01..12: sin esa mitad, 2026-13 pasaria por bueno y el ajuste
            // se imputaria a un cierre que no existe. Y no hay FK que lo salve:
            // accounting_periods no existe en el arbol de changesets.
            ExternalInvoiceReconciliation reconciliation = ExternalInvoiceReconciliationMother
                    .abiertaConId(41L);

            assertThatThrownBy(() -> reconciliation.resolve(990L, "Nota", periodo,
                    ExternalInvoiceReconciliationMother.RESUELTO_EL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("postingPeriod must be YYYY-MM with month 01..12");
        }

        @Test
        @DisplayName("resolver sin nota no se permite: sin explicacion no hay expediente")
        void resolver_sin_nota_no_se_permite() {
            ExternalInvoiceReconciliation reconciliation = ExternalInvoiceReconciliationMother
                    .abiertaConId(41L);

            assertThatThrownBy(() -> reconciliation.resolve(990L, "   ", "2026-03",
                    ExternalInvoiceReconciliationMother.RESUELTO_EL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("resolutionNote is required to resolve");
        }

        @Test
        @DisplayName("una fila con la resolucion a medias no se puede construir")
        void una_fila_con_la_resolucion_a_medias_no_se_construye() {
            // Espejo de chk_eir_resolved: los cuatro o ninguno. Aqui falta el periodo.
            assertThatThrownBy(() -> new ExternalInvoiceReconciliation(41L, 900L, 8600L, null, null,
                    null, null, null, null, new BigDecimal("119000.00"), new BigDecimal("19000.00"),
                    null, null, null, ExternalInvoiceReconciliationStatus.MISSING_EXTERNAL, 990L,
                    ExternalInvoiceReconciliationMother.RESUELTO_EL, "Nota", null,
                    ExternalInvoiceReconciliationMother.CREADO_EL, 0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("postingPeriod must be YYYY-MM with month 01..12");
        }
    }
}
