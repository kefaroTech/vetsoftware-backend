package com.vetsoftware.app.quote.domain;

import static com.vetsoftware.app.quote.testsupport.QuoteMother.HOY;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.tarifa;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.tarifaCaducada;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.tarifaFutura;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.tarifaSinCierre;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.shared.pricing.PriceListNotEffectiveException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * D-73 en el dominio: <em>el precio sale de la tarifa vigente POR FECHA</em>.
 *
 * <p>
 * Estar publicada no basta. El defecto que cierra esta clase es que una lista
 * del ano pasado que nadie archivo seguia en {@code PUBLISHED} y ponia precio a
 * la cotizacion de hoy: se cotizaba con la tarifa de 2025 sin error, sin alarma
 * y sin nada en el documento firmado que lo delatara.
 */
@DisplayName("PriceListRef: vigente es publicada Y dentro de la ventana (D-73)")
class PriceListRefTest {

    @Nested
    @DisplayName("La ventana de vigencia")
    class Ventana {

        @Test
        @DisplayName("COT-020: entre varias tarifas publicadas solo rige la de la fecha")
        void entre_varias_publicadas_solo_rige_la_de_la_fecha() {
            // Las tres estan PUBLISHED: el estado no las distingue, la fecha si.
            List<PriceListRef> publicadas = List.of(tarifaCaducada(), tarifa(), tarifaFutura());

            List<PriceListRef> vigentes = publicadas.stream()
                    .filter(lista -> lista.isEffectiveOn(HOY)).toList();

            assertThat(vigentes).containsExactly(tarifa());
        }

        @Test
        @DisplayName("COT-021: una tarifa publicada pero caducada no rige hoy")
        void una_tarifa_caducada_no_rige_hoy() {
            assertThat(tarifaCaducada().isEffectiveOn(HOY)).isFalse();
        }

        @Test
        @DisplayName("COT-021: una tarifa publicada que aun no empieza tampoco rige hoy")
        void una_tarifa_futura_no_rige_hoy() {
            assertThat(tarifaFutura().isEffectiveOn(HOY)).isFalse();
        }

        @Test
        @DisplayName("el primer dia de vigencia ya cuenta: el extremo es inclusivo")
        void el_primer_dia_ya_cuenta() {
            PriceListRef lista = tarifa();

            assertThat(lista.isEffectiveOn(lista.validFrom())).isTrue();
            assertThat(lista.isEffectiveOn(lista.validFrom().minusDays(1))).isFalse();
        }

        @Test
        @DisplayName("el ultimo dia de vigencia todavia cuenta: el extremo tambien es inclusivo")
        void el_ultimo_dia_todavia_cuenta() {
            PriceListRef lista = tarifa();

            assertThat(lista.isEffectiveOn(lista.validTo())).isTrue();
            assertThat(lista.isEffectiveOn(lista.validTo().plusDays(1))).isFalse();
        }
    }

    @Nested
    @DisplayName("Sin fecha de fin (valid_to nulo)")
    class SinFechaDeFin {

        /**
         * El nulo no es un hueco del fixture: la lista viva del catalogo se publica sin
         * cierre. Un {@code hoy <= validTo} escrito sin comprobar el nulo descartaria
         * la fila y dejaria la plataforma sin ninguna tarifa —el alta de empresas
         * entera se caeria—, que es un fallo peor que el que se venia a corregir.
         */
        @Test
        @DisplayName("COT-020: una tarifa con valid_to nulo esta vigente, no descartada")
        void una_tarifa_sin_cierre_esta_vigente() {
            PriceListRef abierta = tarifaSinCierre();

            assertThat(abierta.validTo()).isNull();
            assertThat(abierta.isEffectiveOn(HOY)).isTrue();
            assertThat(abierta.isEffectiveOn(abierta.validFrom())).isTrue();
            assertThat(abierta.isEffectiveOn(LocalDate.of(2099, 12, 31))).isTrue();
        }

        @Test
        @DisplayName("pero sin cierre sigue sin regir antes de empezar")
        void sin_cierre_sigue_sin_regir_antes_de_empezar() {
            PriceListRef abierta = tarifaSinCierre();

            assertThat(abierta.isEffectiveOn(abierta.validFrom().minusDays(1))).isFalse();
        }
    }

    @Nested
    @DisplayName("Exigir la vigencia")
    class Exigencia {

        @Test
        @DisplayName("COT-020: con la tarifa vigente no protesta y deja seguir")
        void con_la_vigente_deja_seguir() {
            tarifa().requireEffectiveOn(HOY);
        }

        @Test
        @DisplayName("COT-021: con una caducada falla con tipo propio y lleva dentro la ventana y"
                + " el dia con el que se comparo")
        void con_una_caducada_falla_con_la_ventana_dentro() {
            PriceListRef caducada = tarifaCaducada();

            assertThatThrownBy(() -> caducada.requireEffectiveOn(HOY))
                    .isInstanceOfSatisfying(PriceListNotEffectiveException.class, fallo -> {
                        assertThat(fallo.getPriceListId()).isEqualTo(caducada.id());
                        assertThat(fallo.getCode()).isEqualTo(caducada.code());
                        assertThat(fallo.getValidFrom()).isEqualTo(caducada.validFrom());
                        assertThat(fallo.getValidTo()).isEqualTo(caducada.validTo());
                        assertThat(fallo.getQuotedOn()).isEqualTo(HOY);
                    });
        }

        @Test
        @DisplayName("COT-021: con una que aun no empieza tambien falla, y el validTo viaja aunque"
                + " sea nulo")
        void con_una_futura_tambien_falla() {
            PriceListRef futura = tarifaFutura();

            assertThatThrownBy(() -> futura.requireEffectiveOn(HOY))
                    .isInstanceOf(PriceListNotEffectiveException.class);
        }
    }

    @Nested
    @DisplayName("Construccion")
    class Construccion {

        @Test
        @DisplayName("sin validFrom no hay ventana que comprobar: la fila esta corrupta")
        void sin_validFrom_no_hay_ventana() {
            assertThatThrownBy(() -> new PriceListRef(7L, "LISTA", "COP", null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("validFrom is required");
        }

        @Test
        @DisplayName("un cierre anterior al inicio no es una ventana")
        void un_cierre_anterior_al_inicio_no_es_una_ventana() {
            assertThatThrownBy(() -> new PriceListRef(7L, "LISTA", "COP", LocalDate.of(2026, 6, 1),
                    LocalDate.of(2026, 5, 31))).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("validTo must not be before validFrom");
        }

        @Test
        @DisplayName("comparar contra una fecha nula seria decidir a ciegas")
        void comparar_contra_una_fecha_nula_no_se_permite() {
            assertThatThrownBy(() -> tarifa().isEffectiveOn(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("date is required");
        }
    }
}
