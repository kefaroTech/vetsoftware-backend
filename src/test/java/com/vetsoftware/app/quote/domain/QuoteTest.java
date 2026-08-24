package com.vetsoftware.app.quote.domain;

import static com.vetsoftware.app.quote.testsupport.QuoteMother.AHORA;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.CLIENT_REQUEST_ID;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.HOY;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.NUMERO;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.PRICE_LIST_ID;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.VIGENTE_HASTA;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.borrador;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.borradorDeProspecto;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.empresa;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.enviada;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.lineaConDescuento;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.lineaModulo;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.modulo;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.precioGravado;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.usuarioExtra;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("Quote: el documento y el cuadre de sus totales")
class QuoteTest {

    private static final BigDecimal SIN_DESCUENTO = BigDecimal.ZERO;

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("nace en DRAFT y sin prueba de aceptacion")
        void nace_en_borrador() {
            Quote quote = borrador();

            assertThat(quote.getStatus()).isEqualTo(QuoteStatus.DRAFT);
            assertThat(quote.getAcceptedAt()).isNull();
            assertThat(quote.getAcceptedByEmail()).isNull();
            assertThat(quote.getAcceptedIp()).isNull();
        }

        @Test
        @DisplayName("guarda la llave de idempotencia que le dio el cliente")
        void guarda_la_llave_de_idempotencia() {
            assertThat(borrador().getClientRequestId()).isEqualTo(CLIENT_REQUEST_ID);
        }

        @Test
        @DisplayName("una cotizacion a prospecto no tiene empresa, y eso es legitimo")
        void un_prospecto_no_tiene_empresa() {
            Quote quote = borradorDeProspecto();

            assertThat(quote.getCompany()).isNull();
            assertThat(quote.getCompanyId()).isNull();
            assertThat(quote.getProspectName()).isEqualTo("Veterinaria del Sur");
        }
    }

    @Nested
    @DisplayName("Cuadre de totales (R5)")
    class CuadreDeTotales {

        @Test
        @DisplayName("los cuatro totales salen de sumar las lineas, no de fuera")
        void los_totales_salen_de_las_lineas() {
            Quote quote = borrador(List.of(lineaModulo()));

            assertThat(quote.getSubtotalAmount()).isEqualByComparingTo("100000.00");
            assertThat(quote.getDiscountAmount()).isEqualByComparingTo("0.00");
            assertThat(quote.getTaxAmount()).isEqualByComparingTo("19000.00");
            assertThat(quote.getTotalAmount()).isEqualByComparingTo("119000.00");
        }

        @Test
        @DisplayName("con varias lineas suma cada concepto por separado")
        void suma_cada_concepto_por_separado() {
            QuoteLine segunda = QuoteLine.freeze(2, usuarioExtra(), precioGravado("12000.00"), 3,
                    SIN_DESCUENTO, AHORA);
            Quote quote = borrador(List.of(lineaConDescuento("10.00"), segunda));

            assertThat(quote.getSubtotalAmount()).isEqualByComparingTo("136000.00");
            assertThat(quote.getDiscountAmount()).isEqualByComparingTo("10000.00");
            assertThat(quote.getTaxAmount()).isEqualByComparingTo("23940.00");
            assertThat(quote.getTotalAmount()).isEqualByComparingTo("149940.00");
        }

        @Test
        @DisplayName("el total tambien cuadra como subtotal menos descuento mas IVA")
        void el_total_cuadra_por_la_cabecera() {
            Quote quote = borrador(List.of(lineaConDescuento("15.00")));

            BigDecimal porCabecera = quote.getSubtotalAmount().subtract(quote.getDiscountAmount())
                    .add(quote.getTaxAmount());
            assertThat(quote.getTotalAmount()).isEqualByComparingTo(porCabecera);
        }

        @Test
        @DisplayName("un total guardado que no cuadra con las lineas se rechaza al reconstruir")
        void un_total_que_no_cuadra_se_rechaza() {
            List<QuoteLine> lineas = List.of(lineaModulo());

            assertThatThrownBy(() -> new Quote(1L, NUMERO, empresa(), null, null, null, null,
                    PRICE_LIST_ID, BillingCycle.MONTHLY, new BigDecimal("100000.00"),
                    new BigDecimal("0.00"), new BigDecimal("19000.00"), new BigDecimal("999999.00"),
                    QuoteStatus.SENT, VIGENTE_HASTA, 0, null, null, null, CLIENT_REQUEST_ID, AHORA,
                    1L, true, lineas, List.of())).isInstanceOf(QuoteTotalsMismatchException.class)
                    .hasMessageContaining("totalAmount");
        }

        @Test
        @DisplayName("un IVA de cabecera que no cuadra con las lineas se rechaza al reconstruir")
        void un_iva_de_cabecera_que_no_cuadra_se_rechaza() {
            List<QuoteLine> lineas = List.of(lineaModulo());

            assertThatThrownBy(() -> new Quote(1L, NUMERO, empresa(), null, null, null, null,
                    PRICE_LIST_ID, BillingCycle.MONTHLY, new BigDecimal("100000.00"),
                    new BigDecimal("0.00"), new BigDecimal("1.00"), new BigDecimal("119000.00"),
                    QuoteStatus.SENT, VIGENTE_HASTA, 0, null, null, null, CLIENT_REQUEST_ID, AHORA,
                    1L, true, lineas, List.of())).isInstanceOf(QuoteTotalsMismatchException.class)
                    .hasMessageContaining("taxAmount");
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("sin empresa y sin nombre de prospecto no hay destinatario")
        void exige_empresa_o_prospecto() {
            assertThatThrownBy(() -> Quote.create(NUMERO, null, null, null, null, null,
                    PRICE_LIST_ID, BillingCycle.MONTHLY, VIGENTE_HASTA, 0, CLIENT_REQUEST_ID,
                    List.of(lineaModulo()), List.of(), AHORA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company or a prospect name");
        }

        @Test
        @DisplayName("una cotizacion sin lineas no es una oferta")
        void exige_al_menos_una_linea() {
            assertThatThrownBy(() -> Quote.create(NUMERO, empresa(), null, null, null, null,
                    PRICE_LIST_ID, BillingCycle.MONTHLY, VIGENTE_HASTA, 0, CLIENT_REQUEST_ID,
                    List.of(), List.of(), AHORA)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least one line");
        }

        @Test
        @DisplayName("el mismo articulo dos veces es la via rapida a un total que no cuadra")
        void rechaza_el_mismo_articulo_dos_veces() {
            QuoteLine primera = QuoteLine.freeze(1, modulo(), precioGravado("100.00"), 1,
                    SIN_DESCUENTO, AHORA);
            QuoteLine repetida = QuoteLine.freeze(2, modulo(), precioGravado("100.00"), 1,
                    SIN_DESCUENTO, AHORA);

            assertThatThrownBy(() -> borrador(List.of(primera, repetida)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("duplicate catalog item");
        }

        @Test
        @DisplayName("dos lineas con el mismo numero rompen el orden de impresion")
        void rechaza_dos_lineas_con_el_mismo_numero() {
            QuoteLine primera = QuoteLine.freeze(1, modulo(), precioGravado("100.00"), 1,
                    SIN_DESCUENTO, AHORA);
            QuoteLine mismaPosicion = QuoteLine.freeze(1, usuarioExtra(), precioGravado("100.00"),
                    1, SIN_DESCUENTO, AHORA);

            assertThatThrownBy(() -> borrador(List.of(primera, mismaPosicion)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("duplicate line number");
        }

        @Test
        @DisplayName("sin valid_until alguien aparece en 2029 con una cotizacion de 2026")
        void exige_valid_until() {
            assertThatThrownBy(() -> Quote.create(NUMERO, empresa(), null, null, null, null,
                    PRICE_LIST_ID, BillingCycle.MONTHLY, null, 0, CLIENT_REQUEST_ID,
                    List.of(lineaModulo()), List.of(), AHORA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("validUntil is required");
        }

        @Test
        @DisplayName("sin llave de idempotencia no hay proteccion contra el doble clic")
        void exige_client_request_id() {
            assertThatThrownBy(() -> Quote.create(NUMERO, empresa(), null, null, null, null,
                    PRICE_LIST_ID, BillingCycle.MONTHLY, VIGENTE_HASTA, 0, "  ",
                    List.of(lineaModulo()), List.of(), AHORA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("clientRequestId is required");
        }

        @Test
        @DisplayName("los dias de prueba no pueden ser negativos")
        void rechaza_dias_de_prueba_negativos() {
            assertThatThrownBy(() -> Quote.create(NUMERO, empresa(), null, null, null, null,
                    PRICE_LIST_ID, BillingCycle.MONTHLY, VIGENTE_HASTA, -1, CLIENT_REQUEST_ID,
                    List.of(lineaModulo()), List.of(), AHORA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("trialDays cannot be negative");
        }

        @Test
        @DisplayName("ACCEPTED sin fecha de aceptacion es un estado imposible")
        void aceptada_exige_fecha_de_aceptacion() {
            List<QuoteLine> lineas = List.of(lineaModulo());

            assertThatThrownBy(() -> new Quote(1L, NUMERO, empresa(), null, null, null, null,
                    PRICE_LIST_ID, BillingCycle.MONTHLY, new BigDecimal("100000.00"),
                    new BigDecimal("0.00"), new BigDecimal("19000.00"), new BigDecimal("119000.00"),
                    QuoteStatus.ACCEPTED, VIGENTE_HASTA, 0, null, null, null, CLIENT_REQUEST_ID,
                    AHORA, 1L, true, lineas, List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("acceptedAt is required");
        }
    }

    @Nested
    @DisplayName("Transiciones")
    class Transiciones {

        @Test
        @DisplayName("DRAFT pasa a SENT")
        void borrador_pasa_a_enviada() {
            Quote quote = borrador();

            quote.send(HOY);

            assertThat(quote.getStatus()).isEqualTo(QuoteStatus.SENT);
        }

        @Test
        @DisplayName("enviar dos veces no es una transicion valida")
        void no_se_envia_dos_veces() {
            Quote quote = enviada();

            assertThatThrownBy(() -> quote.send(HOY))
                    .isInstanceOf(InvalidQuoteStatusTransitionException.class)
                    .hasMessageContaining("SENT -> SENT");
        }

        @Test
        @DisplayName("SENT pasa a ACCEPTED dejando cuando, quien y desde donde")
        void enviada_pasa_a_aceptada_con_su_prueba() {
            Quote quote = enviada();

            quote.accept("ana@ejemplo.com", "190.85.1.7", AHORA, HOY);

            assertThat(quote.getStatus()).isEqualTo(QuoteStatus.ACCEPTED);
            assertThat(quote.getAcceptedAt()).isEqualTo(AHORA);
            assertThat(quote.getAcceptedByEmail()).isEqualTo("ana@ejemplo.com");
            assertThat(quote.getAcceptedIp()).isEqualTo("190.85.1.7");
        }

        @Test
        @DisplayName("un borrador que nunca se envio no se puede aceptar")
        void un_borrador_no_se_acepta() {
            Quote quote = borrador();

            assertThatThrownBy(() -> quote.accept("ana@ejemplo.com", "1.1.1.1", AHORA, HOY))
                    .isInstanceOf(InvalidQuoteStatusTransitionException.class)
                    .hasMessageContaining("DRAFT -> ACCEPTED");
        }

        @Test
        @DisplayName("aceptar sin correo deja la aceptacion sin firmar")
        void aceptar_exige_correo() {
            Quote quote = enviada();

            assertThatThrownBy(() -> quote.accept("  ", "1.1.1.1", AHORA, HOY))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("acceptedByEmail is required");
        }

        @Test
        @DisplayName("SENT pasa a REJECTED")
        void enviada_pasa_a_rechazada() {
            Quote quote = enviada();

            quote.reject();

            assertThat(quote.getStatus()).isEqualTo(QuoteStatus.REJECTED);
        }

        @ParameterizedTest(name = "desde {0} no se puede rechazar")
        @EnumSource(value = QuoteStatus.class, names = {"DRAFT", "ACCEPTED", "REJECTED", "EXPIRED"})
        @DisplayName("solo una cotizacion enviada se puede rechazar")
        void solo_lo_enviado_se_rechaza(QuoteStatus desde) {
            Quote quote = new Quote(1L, NUMERO, empresa(), null, null, null, null, PRICE_LIST_ID,
                    BillingCycle.MONTHLY, new BigDecimal("100000.00"), new BigDecimal("0.00"),
                    new BigDecimal("19000.00"), new BigDecimal("119000.00"), desde, VIGENTE_HASTA,
                    0, desde == QuoteStatus.ACCEPTED ? AHORA : null, null, null, CLIENT_REQUEST_ID,
                    AHORA, 1L, true, List.of(lineaModulo()), List.of());

            assertThatThrownBy(quote::reject)
                    .isInstanceOf(InvalidQuoteStatusTransitionException.class);
        }
    }

    @Nested
    @DisplayName("Vencimiento")
    class Vencimiento {

        private static final LocalDate DESPUES_DE_VENCER = LocalDate.of(2026, 10, 1);

        @Test
        @DisplayName("una oferta vencida no se puede enviar")
        void una_vencida_no_se_envia() {
            Quote quote = borrador();

            assertThatThrownBy(() -> quote.send(DESPUES_DE_VENCER))
                    .isInstanceOf(QuoteExpiredException.class)
                    .hasMessageContaining("expired on 2026-09-30");
        }

        @Test
        @DisplayName("una oferta vencida no se puede aceptar: el precio ya no se respeta")
        void una_vencida_no_se_acepta() {
            Quote quote = enviada();

            assertThatThrownBy(
                    () -> quote.accept("ana@ejemplo.com", "1.1.1.1", AHORA, DESPUES_DE_VENCER))
                    .isInstanceOf(QuoteExpiredException.class);
        }

        @Test
        @DisplayName("el ultimo dia de vigencia todavia vale")
        void el_ultimo_dia_todavia_vale() {
            Quote quote = borrador();

            quote.send(VIGENTE_HASTA);

            assertThat(quote.getStatus()).isEqualTo(QuoteStatus.SENT);
            assertThat(quote.isExpiredOn(VIGENTE_HASTA)).isFalse();
        }

        @Test
        @DisplayName("vencer una que todavia esta vigente seria falsear el embudo")
        void no_vence_lo_que_sigue_vigente() {
            Quote quote = enviada();

            assertThatThrownBy(() -> quote.expire(HOY)).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("still valid until");
        }

        @Test
        @DisplayName("una enviada que vencio pasa a EXPIRED")
        void una_enviada_vencida_pasa_a_expired() {
            Quote quote = enviada();

            quote.expire(DESPUES_DE_VENCER);

            assertThat(quote.getStatus()).isEqualTo(QuoteStatus.EXPIRED);
        }

        @Test
        @DisplayName("una ya aceptada no vuelve a vencer")
        void una_aceptada_no_vence() {
            Quote quote = enviada();
            quote.accept("ana@ejemplo.com", "1.1.1.1", AHORA, HOY);

            assertThatThrownBy(() -> quote.expire(DESPUES_DE_VENCER))
                    .isInstanceOf(InvalidQuoteStatusTransitionException.class);
        }
    }

    @Nested
    @DisplayName("Baja logica")
    class BajaLogica {

        @Test
        @DisplayName("un borrador se puede dar de baja")
        void un_borrador_se_da_de_baja() {
            borrador().requireDeletable();
        }

        @Test
        @DisplayName("una enviada no: desactivarla borraria la prueba de lo que se ofrecio")
        void una_enviada_no_se_da_de_baja() {
            Quote quote = enviada();

            assertThatThrownBy(quote::requireDeletable)
                    .isInstanceOf(InvalidQuoteStatusTransitionException.class)
                    .hasMessageContaining("SENT -> DRAFT");
        }
    }
}
