package com.vetsoftware.app.quote.application.usecase;

import static com.vetsoftware.app.quote.testsupport.QuoteMother.AHORA;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.CLIENT_REQUEST_ID;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.HOY;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.empresa;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.modulo;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.usuarioExtra;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.infrastructure.config.ClockConfig;
import com.vetsoftware.app.quote.application.command.CreateQuoteCommand;
import com.vetsoftware.app.quote.application.command.QuoteLineCommand;
import com.vetsoftware.app.quote.application.command.SelfServeQuoteCommand;
import com.vetsoftware.app.quote.application.command.SelfServeQuoteLineCommand;
import com.vetsoftware.app.quote.application.dto.QuoteDto;
import com.vetsoftware.app.quote.application.port.out.PlatformQuoteIssuerPort;
import com.vetsoftware.app.quote.application.port.out.PriceListQueryPort;
import com.vetsoftware.app.quote.domain.PriceListRef;
import com.vetsoftware.app.quote.domain.QuoteStatus;
import com.vetsoftware.app.quote.testsupport.QuoteMother;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * La autocontratacion, mirada por donde puede doler: <b>el dinero</b>.
 *
 * <p>
 * <b>La garantia de fondo no es una validacion, es el tipo.</b>
 * {@link SelfServeQuoteCommand} no declara ni un campo economico —ni tarifa, ni
 * vigencia, ni descuento, ni dias de prueba— y
 * {@link SelfServeQuoteLineCommand} tampoco. No estan validados a cero: <b>no
 * se pueden escribir</b>. Eso lo fija {@link LaGarantiaEsElTipo}, que ademas
 * contrasta contra {@link CreateQuoteCommand}, donde esos campos SI viajan: sin
 * el contraste, la ausencia podria ser una casualidad de como quedo el record.
 *
 * <p>
 * Lo que queda por probar es lo que el servidor escribe <em>en su lugar</em>, y
 * cada punto cierra un abuso concreto:
 *
 * <ul>
 * <li><b>La tarifa</b> es la {@code PUBLISHED} vigente HOY segun el reloj
 * inyectado. Si viniera del cuerpo bastaria apuntar a una lista vieja que nadie
 * archivo para contratarse al precio del ano pasado — D-73 visto desde el otro
 * lado.</li>
 * <li><b>El descuento</b> se escribe en cero en el unico sitio del camino donde
 * ese campo llega a existir.</li>
 * <li><b>La vigencia</b> son 15 dias desde hoy; del cuerpo saldria una oferta
 * perpetua.</li>
 * <li><b>{@code trialDays = 0} en la cabecera es deliberado</b>: la prueba
 * vence por linea, no por contrato, y rellenar el entero con «el maximo»
 * regalaria de mas y con «el minimo» de menos.</li>
 * </ul>
 *
 * <p>
 * Que la oferta quede {@code SENT} y no {@code DRAFT} —sin lo cual
 * {@code Quote.accept} rechazaria el ultimo clic, ver
 * {@code QuoteTest.un_borrador_no_se_acepta}— lo decide
 * {@code PlatformQuoteIssuerAdapter} y se prueba en su propia clase.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SelfServeQuoteService — nadie se cotiza a si mismo el precio")
class SelfServeQuoteServiceTest {

    /** Las 10:30 del 2026-08-22 en la zona del negocio. */
    private static final Clock RELOJ = Clock
            .fixed(AHORA.atZone(ClockConfig.BUSINESS_ZONE).toInstant(), ClockConfig.BUSINESS_ZONE);

    /** Lo que dura una oferta de autoservicio: 15 dias desde hoy. */
    private static final LocalDate VIGENTE_HASTA_ESPERADO = HOY.plusDays(15);

    private static final Long TARIFA_VIGENTE = 70L;
    private static final Long TARIFA_CADUCADA = 69L;

    @Mock
    private PlatformQuoteIssuerPort issuer;
    @Mock
    private PriceListQueryPort priceListQueryPort;
    @Captor
    private ArgumentCaptor<CreateQuoteCommand> emitido;

    private SelfServeQuoteService servicio(Clock reloj) {
        return new SelfServeQuoteService(issuer, priceListQueryPort, reloj);
    }

    private static PriceListRef vigente() {
        return new PriceListRef(TARIFA_VIGENTE, "LISTA-2026-08", "COP", LocalDate.of(2026, 8, 1),
                null);
    }

    private static PriceListRef caducada() {
        return new PriceListRef(TARIFA_CADUCADA, "LISTA-2025-01", "COP", LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31));
    }

    private static SelfServeQuoteCommand comando(List<SelfServeQuoteLineCommand> lineas) {
        return new SelfServeQuoteCommand(CLIENT_REQUEST_ID, empresa().id(), "MONTHLY", lineas);
    }

    private static SelfServeQuoteCommand comando() {
        return comando(List.of(new SelfServeQuoteLineCommand(modulo().id(), 1),
                new SelfServeQuoteLineCommand(usuarioExtra().id(), 15)));
    }

    /** El embudo devuelve la oferta ya emitida; aqui solo hace de espejo. */
    private void elIssuerEmite() {
        when(issuer.issue(any()))
                .thenReturn(QuoteDto.from(QuoteMother.persistida(1L, QuoteStatus.SENT)));
    }

    @Nested
    @DisplayName("La tarifa la resuelve el servidor")
    class TarifaResueltaEnServidor {

        @Test
        @DisplayName("de las publicadas cotiza con la vigente HOY, no con la del ano pasado")
        void cotiza_con_la_vigente_hoy() {
            when(priceListQueryPort.findAllPublished()).thenReturn(List.of(caducada(), vigente()));
            elIssuerEmite();

            servicio(RELOJ).execute(comando());

            verify(issuer).issue(emitido.capture());
            assertThat(emitido.getValue().priceListId()).isEqualTo(TARIFA_VIGENTE);
        }

        /**
         * Dos ventanas solapadas son legales en el esquema. Que gane «la primera que
         * devuelva la consulta» significaria que el precio de una contratacion depende
         * del plan de ejecucion de MySQL.
         */
        @Test
        @DisplayName("con dos vigentes solapadas gana la de validFrom mas reciente")
        void con_dos_vigentes_gana_la_mas_reciente() {
            when(priceListQueryPort.findAllPublished()).thenReturn(List.of(vigente(),
                    new PriceListRef(99L, "LISTA-2026-01", "COP", LocalDate.of(2026, 1, 1), null)));
            elIssuerEmite();

            servicio(RELOJ).execute(comando());

            verify(issuer).issue(emitido.capture());
            assertThat(emitido.getValue().priceListId()).isEqualTo(TARIFA_VIGENTE);
        }

        @Test
        @DisplayName("a igualdad de validFrom gana el id mayor: la ultima publicada")
        void a_igualdad_de_valid_from_gana_el_id_mayor() {
            when(priceListQueryPort.findAllPublished()).thenReturn(List.of(
                    new PriceListRef(99L, "LISTA-GEMELA", "COP", LocalDate.of(2026, 8, 1), null),
                    vigente()));
            elIssuerEmite();

            servicio(RELOJ).execute(comando());

            verify(issuer).issue(emitido.capture());
            assertThat(emitido.getValue().priceListId()).isEqualTo(99L);
        }

        @Test
        @DisplayName("sin tarifa vigente no se emite nada: la contratacion se para aqui")
        void sin_tarifa_vigente_no_se_emite_nada() {
            when(priceListQueryPort.findAllPublished()).thenReturn(List.of(caducada()));

            assertThatThrownBy(() -> servicio(RELOJ).execute(comando()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No published price list is effective on");

            verifyNoInteractions(issuer);
        }

        @Test
        @DisplayName("sin ninguna tarifa publicada tampoco se emite nada")
        void sin_ninguna_tarifa_publicada_tampoco() {
            when(priceListQueryPort.findAllPublished()).thenReturn(List.of());

            assertThatThrownBy(() -> servicio(RELOJ).execute(comando()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No published price list is effective on");

            verifyNoInteractions(issuer);
        }

        /**
         * D-81. A las 19:30 en Bogota del ultimo dia de la tarifa, «hoy» en horario
         * universal ya es manana: un {@code LocalDate.now()} pelado —o un
         * {@code CURRENT_DATE} del motor— rechazaria una contratacion legitima.
         */
        @Test
        @DisplayName("a las 19:30 del ultimo dia de la tarifa la contratacion sigue siendo posible")
        void a_las_19_30_del_ultimo_dia_todavia_se_contrata() {
            Clock alFilo = Clock.fixed(ZonedDateTime
                    .of(HOY, LocalTime.of(19, 30), ClockConfig.BUSINESS_ZONE).toInstant(),
                    ClockConfig.BUSINESS_ZONE);
            when(priceListQueryPort.findAllPublished())
                    .thenReturn(List.of(new PriceListRef(TARIFA_VIGENTE, "LISTA-CIERRE", "COP",
                            LocalDate.of(2026, 8, 1), HOY)));
            elIssuerEmite();

            servicio(alFilo).execute(comando());

            verify(issuer).issue(emitido.capture());
            assertThat(emitido.getValue().priceListId()).isEqualTo(TARIFA_VIGENTE);
        }
    }

    @Nested
    @DisplayName("Los terminos los escribe el servidor")
    class TerminosQueEscribeElServidor {

        @Test
        @DisplayName("cada linea sale con descuento CERO, en el unico sitio donde ese campo existe")
        void cada_linea_sale_con_descuento_cero() {
            when(priceListQueryPort.findAllPublished()).thenReturn(List.of(vigente()));
            elIssuerEmite();

            servicio(RELOJ).execute(comando());

            verify(issuer).issue(emitido.capture());
            assertThat(emitido.getValue().lines()).containsExactly(
                    new QuoteLineCommand(modulo().id(), 1, BigDecimal.ZERO, false),
                    new QuoteLineCommand(usuarioExtra().id(), 15, BigDecimal.ZERO, false));
        }

        @Test
        @DisplayName("la oferta vale 15 dias desde hoy, no lo que diga el cuerpo")
        void la_oferta_vale_quince_dias() {
            when(priceListQueryPort.findAllPublished()).thenReturn(List.of(vigente()));
            elIssuerEmite();

            servicio(RELOJ).execute(comando());

            verify(issuer).issue(emitido.capture());
            assertThat(emitido.getValue().validUntil()).isEqualTo(VIGENTE_HASTA_ESPERADO);
        }

        /**
         * Cero significa «esta cabecera no promete prueba». La prueba real se concede
         * articulo a articulo en el camino de contrato, y {@code GET /plans} la publica
         * por linea; un numero aqui seria una promesa plana que el modelo no tiene.
         */
        @Test
        @DisplayName("la cabecera no promete prueba: trialDays cero, a proposito")
        void la_cabecera_no_promete_prueba() {
            when(priceListQueryPort.findAllPublished()).thenReturn(List.of(vigente()));
            elIssuerEmite();

            servicio(RELOJ).execute(comando());

            verify(issuer).issue(emitido.capture());
            assertThat(emitido.getValue().trialDays()).isZero();
        }

        @Test
        @DisplayName("la empresa, la llave de idempotencia y el ciclo viajan tal cual")
        void la_empresa_y_la_llave_viajan_tal_cual() {
            when(priceListQueryPort.findAllPublished()).thenReturn(List.of(vigente()));
            elIssuerEmite();

            servicio(RELOJ).execute(comando());

            verify(issuer).issue(emitido.capture());
            assertThat(emitido.getValue().companyId()).isEqualTo(empresa().id());
            assertThat(emitido.getValue().clientRequestId()).isEqualTo(CLIENT_REQUEST_ID);
            assertThat(emitido.getValue().billingCycle()).isEqualTo("MONTHLY");
        }

        /**
         * Quien se autocontrata es una empresa, no un prospecto: los cuatro campos del
         * prospecto van nulos. Rellenarlos desde el cuerpo permitiria emitir una oferta
         * a nombre de cualquiera.
         */
        @Test
        @DisplayName("no hay prospecto que rellenar: los cuatro campos van nulos")
        void no_hay_prospecto_que_rellenar() {
            when(priceListQueryPort.findAllPublished()).thenReturn(List.of(vigente()));
            elIssuerEmite();

            servicio(RELOJ).execute(comando());

            verify(issuer).issue(emitido.capture());
            assertThat(emitido.getValue().prospectName()).isNull();
            assertThat(emitido.getValue().prospectEmail()).isNull();
            assertThat(emitido.getValue().prospectDocument()).isNull();
            assertThat(emitido.getValue().prospectPhone()).isNull();
        }

        @Test
        @DisplayName("no se copia ninguna respuesta del configurador")
        void no_se_copia_ninguna_respuesta_del_configurador() {
            when(priceListQueryPort.findAllPublished()).thenReturn(List.of(vigente()));
            elIssuerEmite();

            servicio(RELOJ).execute(comando());

            verify(issuer).issue(emitido.capture());
            assertThat(emitido.getValue().answers()).isEmpty();
        }

        @Test
        @DisplayName("un cuerpo sin lineas produce una oferta sin lineas, no un NullPointer")
        void un_cuerpo_sin_lineas_no_revienta() {
            when(priceListQueryPort.findAllPublished()).thenReturn(List.of(vigente()));
            elIssuerEmite();

            servicio(RELOJ).execute(comando(null));

            verify(issuer).issue(emitido.capture());
            assertThat(emitido.getValue().lines()).isEmpty();
        }

        @Test
        @DisplayName("devuelve tal cual la oferta que emitio el embudo")
        void devuelve_tal_cual_la_oferta_emitida() {
            QuoteDto emitida = QuoteDto.from(QuoteMother.persistida(1L, QuoteStatus.SENT));
            when(priceListQueryPort.findAllPublished()).thenReturn(List.of(vigente()));
            when(issuer.issue(any())).thenReturn(emitida);

            assertThat(servicio(RELOJ).execute(comando())).isSameAs(emitida);
        }
    }

    @Nested
    @DisplayName("La garantia es el tipo, no una validacion")
    class LaGarantiaEsElTipo {

        /**
         * El contraste es la mitad del argumento: si {@link CreateQuoteCommand} tampoco
         * los declarase, la ausencia de arriba no probaria nada.
         */
        @Test
        @DisplayName("el comando de autoservicio no declara ni tarifa, ni vigencia, ni dias de"
                + " prueba; el de plataforma si")
        void el_comando_de_autoservicio_no_declara_terminos() {
            assertThat(SelfServeQuoteCommand.class.getRecordComponents())
                    .extracting(RecordComponent::getName)
                    .containsExactly("clientRequestId", "companyId", "billingCycle", "lines");

            assertThat(CreateQuoteCommand.class.getRecordComponents())
                    .extracting(RecordComponent::getName)
                    .contains("priceListId", "validUntil", "trialDays");
        }

        @Test
        @DisplayName("la linea de autoservicio no declara descuento; la de plataforma si")
        void la_linea_de_autoservicio_no_declara_descuento() {
            assertThat(SelfServeQuoteLineCommand.class.getRecordComponents())
                    .extracting(RecordComponent::getName)
                    .containsExactly("catalogItemId", "quantity");

            assertThat(QuoteLineCommand.class.getRecordComponents())
                    .extracting(RecordComponent::getName)
                    .contains("discountPercent", "discountIsConditional");
        }
    }
}
