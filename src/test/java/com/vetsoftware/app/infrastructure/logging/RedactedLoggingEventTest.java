package com.vetsoftware.app.infrastructure.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.LoggerContextVO;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Marker;
import org.slf4j.event.KeyValuePair;

/**
 * {@link RedactedLoggingEvent} es package-private: se prueba directamente el
 * decorador, mockeando el {@link ILoggingEvent} delegado — no es el redactor
 * bajo prueba, es el evento externo de Logback que envuelve.
 */
class RedactedLoggingEventTest {

    @Nested
    @DisplayName("of(...)")
    class Of {

        @Test
        @DisplayName("un evento sin nada que redactar se devuelve tal cual, sin copiarse")
        void un_evento_limpio_se_devuelve_sin_copiarse() {
            ILoggingEvent clean = mock(ILoggingEvent.class);
            when(clean.getFormattedMessage()).thenReturn("login success");
            when(clean.getMDCPropertyMap()).thenReturn(Map.of());
            when(clean.getKeyValuePairs()).thenReturn(null);
            when(clean.getThrowableProxy()).thenReturn(null);

            assertThat(RedactedLoggingEvent.of(clean)).isSameAs(clean);
        }

        @Test
        @DisplayName("un mensaje sensible produce un evento nuevo con el mensaje ya redactado")
        void un_mensaje_sensible_produce_un_evento_redactado() {
            ILoggingEvent dirty = mock(ILoggingEvent.class);
            when(dirty.getFormattedMessage()).thenReturn("password=Sup3rSecreto");
            when(dirty.getMDCPropertyMap()).thenReturn(Map.of());
            when(dirty.getKeyValuePairs()).thenReturn(null);
            when(dirty.getThrowableProxy()).thenReturn(null);

            ILoggingEvent redacted = RedactedLoggingEvent.of(dirty);

            assertThat(redacted).isNotSameAs(dirty);
            assertThat(redacted.getMessage()).isEqualTo("password=***");
            assertThat(redacted.getFormattedMessage()).isEqualTo("password=***");
            assertThat(redacted.getArgumentArray()).isNull();
        }
    }

    @Nested
    @DisplayName("delegación pura")
    class Delegacion {

        private final ILoggingEvent delegate = mock(ILoggingEvent.class);
        private final RedactedLoggingEvent event;

        Delegacion() {
            when(delegate.getFormattedMessage()).thenReturn("password=Sup3rSecreto");
            when(delegate.getMDCPropertyMap()).thenReturn(Map.of());
            when(delegate.getKeyValuePairs()).thenReturn(null);
            when(delegate.getThrowableProxy()).thenReturn(null);
            event = (RedactedLoggingEvent) RedactedLoggingEvent.of(delegate);
        }

        @Test
        @DisplayName("cada superficie no redactada delega íntegramente en el evento original")
        void cada_superficie_no_redactada_delega_en_el_original() {
            Marker marker = mock(Marker.class);
            LoggerContextVO contextVo = mock(LoggerContextVO.class);
            Instant instant = Instant.parse("2026-07-28T15:00:00Z");
            StackTraceElement[] callerData = new StackTraceElement[0];
            when(delegate.getThreadName()).thenReturn("main");
            when(delegate.getLevel()).thenReturn(Level.WARN);
            when(delegate.getLoggerName()).thenReturn("logger-de-prueba");
            when(delegate.getLoggerContextVO()).thenReturn(contextVo);
            when(delegate.getCallerData()).thenReturn(callerData);
            when(delegate.hasCallerData()).thenReturn(true);
            when(delegate.getMarkerList()).thenReturn(List.of(marker));
            when(delegate.getTimeStamp()).thenReturn(123L);
            when(delegate.getNanoseconds()).thenReturn(456);
            when(delegate.getInstant()).thenReturn(instant);
            when(delegate.getSequenceNumber()).thenReturn(789L);

            assertThat(event.getThreadName()).isEqualTo("main");
            assertThat(event.getLevel()).isEqualTo(Level.WARN);
            assertThat(event.getLoggerName()).isEqualTo("logger-de-prueba");
            assertThat(event.getLoggerContextVO()).isSameAs(contextVo);
            assertThat(event.getCallerData()).isSameAs(callerData);
            assertThat(event.hasCallerData()).isTrue();
            assertThat(event.getMarkerList()).containsExactly(marker);
            assertThat(event.getTimeStamp()).isEqualTo(123L);
            assertThat(event.getNanoseconds()).isEqualTo(456);
            assertThat(event.getInstant()).isEqualTo(instant);
            assertThat(event.getSequenceNumber()).isEqualTo(789L);
        }

        @Test
        @DisplayName("prepareForDeferredProcessing() se reenvía al evento original")
        void prepare_for_deferred_processing_se_reenvia() {
            event.prepareForDeferredProcessing();

            org.mockito.Mockito.verify(delegate).prepareForDeferredProcessing();
        }

        @Test
        @DisplayName("el getMdc() heredado (deprecado) expone el mismo MDC ya redactado")
        void get_mdc_deprecado_expone_el_mismo_mdc_redactado() {
            assertThat(event.getMdc()).isEqualTo(event.getMDCPropertyMap());
        }
    }

    @Nested
    @DisplayName("superficies redactadas")
    class Redaccion {

        @Test
        @DisplayName("el MDC redactado sustituye al original en getMDCPropertyMap")
        void el_mdc_redactado_sustituye_al_original() {
            ILoggingEvent dirty = mock(ILoggingEvent.class);
            when(dirty.getFormattedMessage()).thenReturn("actualizando propietario");
            when(dirty.getMDCPropertyMap()).thenReturn(Map.of("owner.email", "cliente@correo.co"));
            when(dirty.getKeyValuePairs()).thenReturn(null);
            when(dirty.getThrowableProxy()).thenReturn(null);

            ILoggingEvent redacted = RedactedLoggingEvent.of(dirty);

            assertThat(redacted.getMDCPropertyMap()).containsEntry("owner.email", LogRedactor.MASK);
        }

        @Test
        @DisplayName("los pares clave-valor redactados sustituyen a los originales")
        void los_pares_clave_valor_redactados_sustituyen_a_los_originales() {
            ILoggingEvent dirty = mock(ILoggingEvent.class);
            when(dirty.getFormattedMessage()).thenReturn("evento de auditoría");
            when(dirty.getMDCPropertyMap()).thenReturn(Map.of());
            when(dirty.getKeyValuePairs())
                    .thenReturn(List.of(new KeyValuePair("owner.document", "1032456789")));
            when(dirty.getThrowableProxy()).thenReturn(null);

            ILoggingEvent redacted = RedactedLoggingEvent.of(dirty);

            assertThat(redacted.getKeyValuePairs()).extracting(pair -> pair.value)
                    .containsExactly(LogRedactor.MASK);
        }

        @Test
        @DisplayName("la excepción redactada sustituye a la original en getThrowableProxy")
        void la_excepcion_redactada_sustituye_a_la_original() {
            ILoggingEvent dirty = mock(ILoggingEvent.class);
            when(dirty.getFormattedMessage()).thenReturn("Unexpected error");
            when(dirty.getMDCPropertyMap()).thenReturn(Map.of());
            when(dirty.getKeyValuePairs()).thenReturn(null);
            IThrowableProxy original = new ch.qos.logback.classic.spi.ThrowableProxy(
                    new IllegalStateException("password=Sup3rSecreto"));
            when(dirty.getThrowableProxy()).thenReturn(original);

            ILoggingEvent redacted = RedactedLoggingEvent.of(dirty);

            assertThat(redacted.getThrowableProxy()).isNotSameAs(original);
            assertThat(redacted.getThrowableProxy().getMessage()).isEqualTo("password=***");
        }
    }
}
