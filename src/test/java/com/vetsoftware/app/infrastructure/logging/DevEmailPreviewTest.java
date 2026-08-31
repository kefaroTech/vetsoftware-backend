package com.vetsoftware.app.infrastructure.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * {@link DevEmailPreview} es uno de los dos canales de log <b>sin</b> redacción
 * del sistema —el otro es {@code AI_PAYLOAD}; la lista cerrada la sostiene
 * {@link LogbackRedactionConfigTest}—, así que esta prueba afirma sobre el
 * mensaje tal cual sale, sin pasar por {@link RedactingAppender}.
 *
 * <p>
 * El sumidero va enganchado al <b>propio logger</b> y no a la raíz, y con el
 * nivel fijado a mano: {@code logback-spring.xml} declara este canal con
 * {@code additivity="false"}, así que en cuanto una rodaja de Spring del mismo
 * fork carga esa configuración, sus eventos dejan de propagar hacia arriba.
 * Leerlo desde la raíz sería una medición que se cae sola según con quién
 * comparta JVM.
 */
class DevEmailPreviewTest {

    private Logger previewLogger;
    private ListAppender<ILoggingEvent> sink;
    private Level previousLevel;

    @BeforeEach
    void wireSink() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        previewLogger = context.getLogger("DEV_EMAIL_PREVIEW");
        previousLevel = previewLogger.getLevel();
        previewLogger.setLevel(Level.INFO);

        sink = new ListAppender<>();
        sink.setContext(context);
        sink.start();
        previewLogger.addAppender(sink);
    }

    @AfterEach
    void tearDown() {
        previewLogger.detachAppender(sink);
        previewLogger.setLevel(previousLevel);
    }

    @Test
    @DisplayName("muestra el destinatario, la descripción y el payload sin enmascarar nada")
    void muestra_destinatario_descripcion_y_payload_sin_enmascarar() {
        DevEmailPreview.show("due@correo.co", "Enlace de verificación",
                "https://app.vetrina.co/verify?token=abc123");

        assertThat(sink.list).hasSize(1);
        assertThat(sink.list.get(0).getFormattedMessage()).contains("due@correo.co",
                "Enlace de verificación", "https://app.vetrina.co/verify?token=abc123");
    }
}
