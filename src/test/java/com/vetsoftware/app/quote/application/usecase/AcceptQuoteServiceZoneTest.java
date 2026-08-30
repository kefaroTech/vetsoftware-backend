package com.vetsoftware.app.quote.application.usecase;

import static com.vetsoftware.app.quote.testsupport.QuoteMother.VIGENTE_HASTA;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.persistida;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.infrastructure.config.ClockConfig;
import com.vetsoftware.app.quote.application.command.AcceptQuoteCommand;
import com.vetsoftware.app.quote.application.dto.QuoteDto;
import com.vetsoftware.app.quote.application.port.out.QuoteRepository;
import com.vetsoftware.app.quote.application.port.out.SubscriptionProvisioningPort;
import com.vetsoftware.app.quote.domain.Quote;
import com.vetsoftware.app.quote.domain.QuoteExpiredException;
import com.vetsoftware.app.quote.domain.QuoteStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * D-81 en la feature quote: <em>la zona del reloj decide que dia es hoy</em>.
 *
 * <p>
 * {@code AcceptQuoteService} deriva la fecha de calendario del reloj
 * ({@code LocalDateTime.now(clock).toLocalDate()}) y se la pasa a
 * {@code Quote.accept}, que rechaza la aceptacion si esa fecha es posterior a
 * {@code validUntil}. Mientras el bean del reloj fue
 * {@code Clock.systemDefaultZone()} y la imagen no declaro zona, produccion
 * decidia en UTC: <b>entre las 19:00 y la medianoche de Bogota, «hoy» ya era
 * manana</b>.
 *
 * <p>
 * El escenario que fija esta clase es el del documento de diseno: <i>una
 * aceptacion a las 19:30 hora local del ultimo dia valido tiene que
 * aceptarse</i>. Los dos casos parten del <b>mismo y unico instante</b> —las
 * 19:30 del {@code 2026-09-30} en Bogota, que es el {@code 2026-10-01T00:30Z}—
 * y solo cambia la zona del {@code Clock.fixed(...)}. Si los dos dieran el
 * mismo resultado, la prueba no probaria nada; lo que demuestra el par es que
 * <b>la unica variable es la zona</b>.
 *
 * <p>
 * El caso de {@link ZonaUtc} es el chivato de la regresion: vuelve a verde el
 * dia que alguien devuelva {@link ClockConfig#systemClock()} a
 * {@code systemDefaultZone()}, y por eso afirma en negativo —que la aceptacion
 * <em>muere</em>— en vez de limitarse a comprobar que el camino feliz pasa.
 *
 * @see ClockConfig
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AcceptQuoteService: la zona del reloj decide cual es el ultimo dia valido (D-81)")
class AcceptQuoteServiceZoneTest {

    private static final Long ID = 55L;
    private static final Long EMPRESA = 42L;
    private static final String EMAIL = "ana@ejemplo.com";
    private static final String IP = "190.85.1.7";

    /**
     * Las 19:30 del ultimo dia de validez, hora de Bogota. Se construye desde
     * {@link ClockConfig#BUSINESS_ZONE} y no escribiendo {@code -05:00} a mano:
     * escribir el desplazamiento duplicaria la decision del bean en vez de
     * comprobarla.
     */
    private static final Instant LAS_19_30_DEL_ULTIMO_DIA_VALIDO = ZonedDateTime
            .of(VIGENTE_HASTA, LocalTime.of(19, 30), ClockConfig.BUSINESS_ZONE).toInstant();

    private static final LocalDateTime SELLO_ESPERADO = LocalDateTime.of(VIGENTE_HASTA,
            LocalTime.of(19, 30));

    @Mock
    private QuoteRepository repository;

    /**
     * DC-2: aceptar firma ademas el contrato, por este puerto. Aqui es un doble
     * mudo a proposito —lo que estas clases prueban son las transiciones y la zona
     * del reloj—; que el contrato nazca lo prueba
     * {@code AcceptQuoteProvisioningTest}.
     */
    @Mock
    private SubscriptionProvisioningPort provisioning;

    private static AcceptQuoteCommand comando() {
        return new AcceptQuoteCommand(ID, EMPRESA, EMAIL, IP);
    }

    @Nested
    @DisplayName("Con el reloj del negocio en America/Bogota (el bean corregido)")
    class ZonaDeNegocio {

        private final Clock reloj = Clock.fixed(LAS_19_30_DEL_ULTIMO_DIA_VALIDO,
                ClockConfig.BUSINESS_ZONE);

        @Test
        @DisplayName("aceptar a las 19:30 del ultimo dia valido se acepta")
        void a_las_19_30_del_ultimo_dia_valido_se_acepta() {
            when(repository.findByIdAndCompanyId(ID, EMPRESA))
                    .thenReturn(Optional.of(persistida(ID, QuoteStatus.SENT)));
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            QuoteDto dto = new AcceptQuoteService(repository, provisioning, reloj)
                    .execute(comando());

            assertThat(dto.status()).isEqualTo(QuoteStatus.ACCEPTED.name());
            assertThat(dto.acceptedAt()).isEqualTo(SELLO_ESPERADO);
            assertThat(dto.acceptedByEmail()).isEqualTo(EMAIL);
        }

        @Test
        @DisplayName("la fecha sellada es el ultimo dia valido, no el dia siguiente")
        void sella_el_ultimo_dia_valido_y_no_el_siguiente() {
            when(repository.findByIdAndCompanyId(ID, EMPRESA))
                    .thenReturn(Optional.of(persistida(ID, QuoteStatus.SENT)));
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            new AcceptQuoteService(repository, provisioning, reloj).execute(comando());

            ArgumentCaptor<Quote> guardada = ArgumentCaptor.forClass(Quote.class);
            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue().getAcceptedAt().toLocalDate()).isEqualTo(VIGENTE_HASTA)
                    .isNotEqualTo(VIGENTE_HASTA.plusDays(1));
            assertThat(guardada.getValue().getStatus()).isEqualTo(QuoteStatus.ACCEPTED);
        }
    }

    @Nested
    @DisplayName("Con el mismo instante y el reloj en UTC (la regresion de D-81)")
    class ZonaUtc {

        private final Clock relojSinZonaDeNegocio = Clock.fixed(LAS_19_30_DEL_ULTIMO_DIA_VALIDO,
                ZoneOffset.UTC);

        /**
         * El dominio no sella una fecha equivocada: la corta antes.
         * {@code Quote.accept} llama a {@code requireNotExpired(today)} y
         * {@code isExpiredOn} compara {@code validUntil.isBefore(today)}, asi que con
         * {@code today = 2026-10-01} la cotizacion valida hasta el {@code 2026-09-30}
         * ya vencio. La decision que cambia es el rechazo, no el sello.
         */
        @Test
        @DisplayName("el mismo instante deriva el dia siguiente y la aceptacion se rechaza como"
                + " vencida")
        void el_mismo_instante_en_utc_rechaza_la_aceptacion_como_vencida() {
            when(repository.findByIdAndCompanyId(ID, EMPRESA))
                    .thenReturn(Optional.of(persistida(ID, QuoteStatus.SENT)));

            assertThatThrownBy(
                    () -> new AcceptQuoteService(repository, provisioning, relojSinZonaDeNegocio)
                            .execute(comando()))
                    .isInstanceOf(QuoteExpiredException.class)
                    .hasMessageContaining("expired on " + VIGENTE_HASTA);
        }

        @Test
        @DisplayName("y al rechazarla no escribe: no queda prueba de aceptacion sellada")
        void el_rechazo_por_zona_equivocada_no_escribe() {
            when(repository.findByIdAndCompanyId(ID, EMPRESA))
                    .thenReturn(Optional.of(persistida(ID, QuoteStatus.SENT)));

            assertThatThrownBy(
                    () -> new AcceptQuoteService(repository, provisioning, relojSinZonaDeNegocio)
                            .execute(comando()))
                    .isInstanceOf(QuoteExpiredException.class);

            verify(repository, never()).save(any());
        }
    }
}
