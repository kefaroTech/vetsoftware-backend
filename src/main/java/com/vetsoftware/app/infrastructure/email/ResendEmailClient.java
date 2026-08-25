package com.vetsoftware.app.infrastructure.email;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.annotation.Observed;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Único punto de salida de correo de la aplicación, sobre la API HTTP de Resend
 * (https://resend.com/docs/api-reference/emails/send-email):
 * {@code POST /emails} con {@code
 * Authorization: Bearer <api-key>}. Soporta dos modos:
 *
 * <ul>
 * <li>{@link #send} — cuerpo HTML propio (con adjuntos), p. ej. la factura.
 * <li>{@link #sendTemplate} — plantilla server-side de Resend por {@code id} +
 * {@code variables}. También devuelve su desenlace.
 * </ul>
 *
 * <p>
 * <b>Asíncrono y no bloqueante:</b> ambos métodos son {@code @Async} (pool
 * {@code
 * emailTaskExecutor}) y NUNCA lanzan: si el envío está deshabilitado, falta el
 * destinatario o la API key, o Resend/la red fallan, se registra el fallo y se
 * continúa. Aplica a todos los usos.
 *
 * <p>
 * <b>El desenlace sí vuelve al llamador (issue #242).</b> Que no lance no
 * significa que no informe: {@link #send} devuelve un
 * {@code CompletableFuture<EmailDispatchOutcome>} que se completa —siempre de
 * forma normal, nunca excepcional— con el resultado real del envío, ya en el
 * hilo del pool. Ignorar el retorno deja el comportamiento fire-and-forget
 * exacto de antes, que es lo que hacen cuatro de los cinco flujos; componerlo
 * con {@code thenAccept} es la única forma de contar entregas en vez de
 * encolados. Antes de esto, un {@code try/catch} alrededor de la llamada era
 * código muerto: se ejecutaba solo si el executor rechazaba la tarea.
 *
 * <p>
 * <b>Aquí se pierde el correo, y por eso el fallo se ramifica.</b> Al cruzar el
 * salto {@code @Async} el llamador ya recibió su respuesta: nadie está
 * escuchando, no hay reintento y no hay cola de salida, así que el mensaje que
 * no sale de {@link #dispatch} no sale nunca. Este es el punto común de los
 * cinco flujos de correo —confirmación de cita, factura, recuperación de
 * código, invitación y restablecimiento—, así que lo que aquí se registre mal
 * se registra mal cinco veces. El fallo se separa en dos familias
 * ({@link EmailErrorType}): la transitoria se registra como {@code WARN} —puede
 * salir el envío siguiente— y la determinista como {@code ERROR} —nadie la
 * arregla salvo una persona cambiando configuración—. Las dos adjuntan la
 * excepción, no su {@code getMessage()}: sin la traza el diagnóstico se queda
 * en «falló la red».
 *
 * <p>
 * Configuración: {@code vetsoftware.email.enabled},
 * {@code vetsoftware.email.from}, {@code
 * vetsoftware.email.resend.api-key}.
 */
@Component
public class ResendEmailClient {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailClient.class);

    /**
     * Tope del cuerpo de error que se copia al registro. Resend devuelve un JSON
     * corto, pero un error de un proxy intermedio puede devolver una página entera:
     * volcarla cruda mete kilobytes por línea en un pipeline de logs facturado por
     * volumen y arrastra a los recortes lo que sí importaba.
     */
    private static final int MAX_BODY_CHARS = 512;

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final boolean enabled;
    private final String from;
    private final String apiKey;
    private final RestClient restClient;
    private final ObservationRegistry observationRegistry;

    public ResendEmailClient(@Value("${vetsoftware.email.enabled:true}") boolean enabled,
            @Value("${vetsoftware.email.from}") String from,
            @Value("${vetsoftware.email.resend.api-key:}") String apiKey,
            @Value("${vetsoftware.email.resend.base-url:https://api.resend.com}") String baseUrl,
            RestClient.Builder restClientBuilder, ObservationRegistry observationRegistry) {
        this.enabled = enabled;
        this.from = from;
        this.apiKey = apiKey;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(30_000);
        this.restClient = restClientBuilder.baseUrl(baseUrl).requestFactory(factory).build();
        this.observationRegistry = observationRegistry;
    }

    /**
     * {@code true} si el envío de correo está habilitado (permite a los llamadores
     * dar fallback en dev).
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Adjunto de correo; {@code content} son los bytes crudos (se codifican a
     * base64 al enviar).
     */
    public record Attachment(String filename, byte[] content) {
    }

    /**
     * Envía un correo con HTML propio y adjuntos opcionales. Ver contrato en el
     * javadoc de la clase.
     *
     * @return futuro que se completa con el desenlace real del envío, ya en el hilo
     *         del pool. Nunca se completa excepcionalmente. Ignorarlo es válido y
     *         deja el comportamiento fire-and-forget de siempre.
     */
    @Async("emailTaskExecutor")
    @Observed(name = "email.send", contextualName = "send email")
    public CompletableFuture<EmailDispatchOutcome> send(String to, String cc, String subject,
            String html, List<Attachment> attachments) {
        Optional<EmailDispatchOutcome> early = notReady(to, subject);
        if (early.isPresent())
            return CompletableFuture.completedFuture(early.get());

        Map<String, Object> body = baseBody(to, cc, subject);
        body.put("html", html);
        if (attachments != null && !attachments.isEmpty()) {
            List<Map<String, Object>> atts = new ArrayList<>();
            for (Attachment a : attachments) {
                atts.add(Map.of("filename", a.filename(), "content",
                        Base64.getEncoder().encodeToString(a.content())));
            }
            body.put("attachments", atts);
        }
        return CompletableFuture.completedFuture(dispatch(to, body));
    }

    /**
     * Envía usando una plantilla server-side de Resend:
     * {@code template: { id, variables }}. Las variables corresponden a los
     * placeholders {@code {{{VARIABLE}}}} de la plantilla. {@code
     * subject} puede ser {@code null} para dejar que la plantilla defina el suyo.
     */
    @Async("emailTaskExecutor")
    @Observed(name = "email.send.template", contextualName = "send email template")
    public CompletableFuture<EmailDispatchOutcome> sendTemplate(String to, String cc,
            String subject, String templateId, Map<String, Object> variables) {
        // Devuelve el desenlace, igual que send(). Era void mientras ningún flujo de
        // plantilla tuvo un contador de negocio que alimentar; el alta de
        // superadministradores sí lo tiene, y ahí perder el correo de invitación en
        // silencio significa que la cuenta aprobada nunca llega a existir y nadie se
        // entera. Los cuatro adaptadores que ignoran el retorno siguen siendo
        // fire-and-forget exactos: ignorar un CompletableFuture no cambia nada.
        Optional<EmailDispatchOutcome> early = notReady(to, subject);
        if (early.isPresent())
            return CompletableFuture.completedFuture(early.get());
        if (templateId == null || templateId.isBlank()) {
            recordOutcome("invalid");
            // FAILED y no SKIPPED: para el destinatario es indistinguible de una caída
            // del proveedor —no le llegó y nadie lo va a reintentar—, y contarlo como
            // omisión escondería exactamente el despliegue mal configurado que este
            // valor existe para delatar.
            log.warn("No se envía correo a {}: templateId de Resend no configurado", to);
            return CompletableFuture.completedFuture(EmailDispatchOutcome.FAILED);
        }

        Map<String, Object> body = baseBody(to, cc, subject);
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("id", templateId);
        template.put("variables", variables == null ? Map.of() : variables);
        body.put("template", template);
        return CompletableFuture.completedFuture(dispatch(to, body));
    }

    /**
     * Comprueba las tres condiciones que abortan el envío antes de tocar la red.
     *
     * @return vacío si hay que seguir adelante; si no, el desenlace con el que se
     *         cierra el envío. El correo deshabilitado es {@code SKIPPED} —dev
     *         normal, no un fallo—; destinatario vacío y API key ausente son
     *         {@code FAILED}, porque para el destinatario son indistinguibles de
     *         una caída del proveedor: no le llegó y nadie lo va a reintentar.
     */
    private Optional<EmailDispatchOutcome> notReady(String to, String subject) {
        if (!enabled) {
            recordOutcome("skipped");
            log.info(
                    "Email deshabilitado (vetsoftware.email.enabled=false); se omite el envío a {}",
                    to);
            return Optional.of(EmailDispatchOutcome.SKIPPED);
        }
        if (to == null || to.isBlank()) {
            recordOutcome("invalid");
            log.warn("No se envía correo: destinatario vacío (asunto '{}')", subject);
            return Optional.of(EmailDispatchOutcome.FAILED);
        }
        if (apiKey == null || apiKey.isBlank()) {
            recordOutcome("misconfigured");
            log.warn("No se envía correo a {}: RESEND_API_KEY no configurada (asunto '{}')", to,
                    subject);
            return Optional.of(EmailDispatchOutcome.FAILED);
        }
        return Optional.empty();
    }

    private Map<String, Object> baseBody(String to, String cc, String subject) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("from", from);
        body.put("to", List.of(to));
        if (cc != null && !cc.isBlank()) {
            body.put("cc", List.of(cc));
        }
        if (subject != null && !subject.isBlank()) {
            body.put("subject", subject);
        }
        return body;
    }

    private EmailDispatchOutcome dispatch(String to, Map<String, Object> body) {
        try {
            restClient.post().uri("/emails").header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON).body(body).retrieve()
                    .toBodilessEntity();
            recordOutcome("success");
            return EmailDispatchOutcome.ACCEPTED;
        } catch (RestClientResponseException exception) {
            EmailErrorType errorType = EmailErrorType.of(exception);
            recordFailure(exception, errorType);
            int status = exception.getStatusCode().value();
            String detail = summarize(exception.getResponseBodyAsString());
            if (errorType.transitory()) {
                log.warn("Resend rechazó de forma pasajera el correo a {} (HTTP {},"
                        + " error.type={}); no hay reintento, así que este mensaje se pierde: {}",
                        to, status, errorType.tag(), detail, exception);
            } else {
                log.error("Resend rechazó de forma permanente el correo a {} (HTTP {},"
                        + " error.type={}); reintentar no cambiaría nada y el mensaje se pierde"
                        + " hasta que alguien corrija la configuración: {}", to, status,
                        errorType.tag(), detail, exception);
            }
        } catch (Exception exception) {
            EmailErrorType errorType = EmailErrorType.of(exception);
            recordFailure(exception, errorType);
            if (errorType.transitory()) {
                log.warn("No se pudo entregar el correo a {}: fallo de transporte contra Resend"
                        + " (error.type={}). No hay reintento, así que este mensaje se pierde.", to,
                        errorType.tag(), exception);
            } else {
                log.error("Fallo sin clasificar al enviar el correo a {} (error.type={}). El"
                        + " mensaje se pierde y la clasificación necesita una rama nueva en"
                        + " EmailErrorType.", to, errorType.tag(), exception);
            }
        }
        // Un único return para las dos familias de fallo: el llamador no distingue
        // transitorio de determinista porque ninguno de los dos se reintenta aquí.
        // Esa distinción vive en el nivel del log y en error.type de la observación.
        return EmailDispatchOutcome.FAILED;
    }

    /**
     * Deja el cuerpo de error en una sola línea y acotado. Es el único sitio donde
     * se ve el motivo real que da Resend, así que no se puede omitir, pero tampoco
     * volcarse crudo.
     */
    private static String summarize(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "(sin cuerpo de respuesta)";
        }
        String flattened = WHITESPACE.matcher(responseBody).replaceAll(" ").trim();
        if (flattened.length() <= MAX_BODY_CHARS) {
            return flattened;
        }
        return flattened.substring(0, MAX_BODY_CHARS) + "… (truncado)";
    }

    private void recordOutcome(String outcome) {
        recordOutcome(outcome, EmailErrorType.NONE);
    }

    /**
     * {@code error.type} se emite <b>siempre</b>, también en el camino feliz. Una
     * etiqueta que solo aparece al fallar parte el medidor en dos juegos de
     * etiquetas distintos y las series dejan de sumarse entre sí.
     */
    private void recordOutcome(String outcome, EmailErrorType errorType) {
        Observation current = observationRegistry.getCurrentObservation();
        if (current != null) {
            current.lowCardinalityKeyValue("email.outcome", outcome);
            current.lowCardinalityKeyValue("error.type", errorType.tag());
        }
    }

    private void recordFailure(Throwable error, EmailErrorType errorType) {
        Observation current = observationRegistry.getCurrentObservation();
        if (current != null) {
            current.error(error);
        }
        recordOutcome("failure", errorType);
    }
}
