package com.vetsoftware.app.infrastructure.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Previsualización local de un correo que no se envió porque el envío está
 * deshabilitado ({@code
 * vetsoftware.email.enabled=false}). Imprime el enlace o los códigos que el
 * correo habría llevado, para poder continuar un flujo de verificación o de
 * restablecimiento sin buzón.
 *
 * <p>
 * <b>Es uno de los dos únicos canales de log sin redacción del sistema</b> —el
 * otro es {@code AI_PAYLOAD}, la conversación con el modelo, ver
 * {@code BedrockModelInvoker}—, y por eso está acotado por construcción y no
 * por confianza: escribe al logger {@code DEV_EMAIL_PREVIEW}, que {@code
 * logback-spring.xml} declara <em>solo</em> en el perfil local, con
 * {@code additivity="false"} y un único appender de consola. No hay ninguna
 * ruta desde este logger hasta el appender de OpenTelemetry, así que su
 * contenido no puede llegar a archivos ni a Loki. Fuera de local el logger no
 * está declarado y sus eventos caen en la raíz, que sí está redactada.
 *
 * <p>
 * <b>Los dos, y solo los dos.</b> La lista es cerrada y la sostiene
 * {@code LogbackRedactionConfigTest}: un tercer logger con appender crudo, o
 * cualquiera de estos dos declarado fuera del perfil local, rompe el build. Al
 * añadir el segundo canal este javadoc decía «el único» y ya era falso; esa es
 * la razón de que la prueba compruebe ahora la lista entera y no un nombre.
 *
 * <p>
 * Usarlo únicamente para material efímero de desarrollo. Cualquier otro log de
 * un secreto debe pasar por la política normal — ver
 * {@code docs/POLITICA_REDACCION_LOGS.md}.
 *
 * @see RedactingAppender
 */
public final class DevEmailPreview {

    private static final Logger preview = LoggerFactory.getLogger("DEV_EMAIL_PREVIEW");

    private DevEmailPreview() {
    }

    public static void show(String recipient, String description, String payload) {
        preview.info("[dev] Envío de correo deshabilitado. {} para {}: {}", description, recipient,
                payload);
    }
}
