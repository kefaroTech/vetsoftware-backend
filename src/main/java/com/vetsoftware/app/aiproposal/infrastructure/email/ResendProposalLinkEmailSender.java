package com.vetsoftware.app.aiproposal.infrastructure.email;

import com.vetsoftware.app.aiproposal.application.dto.ProposalLinkEmail;
import com.vetsoftware.app.aiproposal.application.port.out.ProposalEmailThrottlePort;
import com.vetsoftware.app.aiproposal.application.port.out.ProposalLinkEmailSender;
import com.vetsoftware.app.infrastructure.email.HtmlEscaper;
import com.vetsoftware.app.infrastructure.email.ResendEmailClient;
import com.vetsoftware.app.infrastructure.logging.DevEmailPreview;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * El correo con el enlace de la propuesta, por Resend.
 *
 * <p>
 * &#9940; <strong>El cuerpo no lleva ni una palabra escrita por el modelo ni
 * por el cliente.</strong> Ni motivos, ni la descripcion libre, ni el nombre de
 * la clinica, ni un resumen, ni siquiera los nombres de los modulos propuestos.
 * Lleva una frase fija, el enlace, la fecha de caducidad y el pie. Y la razon
 * no es de estilo: <strong>la direccion no esta verificada</strong>. El
 * prospecto la escribio en un formulario anonimo y nadie comprobo que sea suya,
 * asi que cualquiera puede hacer que este dominio -con SPF y DKIM en regla, es
 * decir uno que pasa los filtros- entregue un mensaje a un tercero. Con prosa
 * del modelo dentro, y siendo el texto libre del atacante lo que la produce,
 * eso es un rele de phishing firmado por nosotros. La propuesta se ve al abrir
 * el enlace, que es donde el control de acceso existe.
 *
 * <p>
 * <strong>Lo unico variable que se interpola son dos valores que genera el
 * sistema</strong> -una URL construida aqui a partir de un token de
 * {@code SecureRandom} y una fecha-, y aun asi los dos pasan por
 * {@link HtmlEscaper}: no porque puedan traer HTML, sino porque el dia que
 * alguien añada un tercer marcador copie el patron correcto.
 *
 * <p>
 * <strong>Verificar la direccion antes de enviar esta fuera del
 * alcance</strong> y hay que decirlo: implicaria un doble opt-in que rompe la
 * promesa de "ve tu propuesta ya", y es una decision del dueño del producto.
 * Mientras no se tome, el cupo por hora y el cuerpo sin contenido son la
 * mitigacion — y son mitigacion, no solucion.
 *
 * <p>
 * <strong>Nunca lanza.</strong> Lo llama un {@code afterCommit} con la
 * transaccion ya confirmada, donde una excepcion convertiria una propuesta bien
 * guardada en un 500 para el prospecto.
 */
@Component
public class ResendProposalLinkEmailSender implements ProposalLinkEmailSender {

    private static final Logger log = LoggerFactory.getLogger(ResendProposalLinkEmailSender.class);

    private static final String SUBJECT = "Tu propuesta de Lumbre";

    private static final String TEMPLATE_PATH = "email-templates/ai-proposal-link.html";

    private static final DateTimeFormatter CADUCIDAD = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ResendEmailClient email;

    private final ProposalEmailThrottlePort throttle;

    private final String baseUrl;

    private final String template;

    public ResendProposalLinkEmailSender(ResendEmailClient email,
            ProposalEmailThrottlePort throttle,
            @Value("${vetsoftware.ai.proposal.link-base-url:}") String baseUrl) {
        this.email = email;
        this.throttle = throttle;
        this.baseUrl = baseUrl;
        this.template = loadTemplate();
    }

    @Override
    public void send(ProposalLinkEmail enlace) {
        if (enlace == null) {
            return;
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            // Sin URL base el enlace no lleva a ninguna parte. Se calla en vez de
            // mandar un correo roto: el prospecto ya tiene su propuesta en pantalla.
            log.warn("vetsoftware.ai.proposal.link-base-url sin configurar; no se envia el enlace");
            return;
        }
        if (!throttle.tryAcquire(enlace.contactEmail())) {
            log.info("Cupo de correo de propuesta agotado para este destinatario; no se envia");
            return;
        }

        String url = baseUrl + (baseUrl.endsWith("/") ? "" : "/") + "?token="
                + enlace.publicToken();
        if (!email.isEnabled()) {
            // ⛔ Ni aqui se escribe el token: DevEmailPreview va al log, y el log es
            // exactamente el sitio del que el token se saco al moverlo a ?token=.
            DevEmailPreview.show(enlace.contactEmail(), "Enlace de la propuesta",
                    "caduca el " + enlace.expiresAt().format(CADUCIDAD));
            return;
        }

        String html = template.replace("{{{PROPOSAL_URL}}}", HtmlEscaper.escape(url)).replace(
                "{{{EXPIRES_AT}}}", HtmlEscaper.escape(enlace.expiresAt().format(CADUCIDAD)));
        try {
            email.send(enlace.contactEmail(), null, SUBJECT, html, null);
        } catch (RuntimeException fallo) {
            log.warn("No se pudo enviar el enlace de la propuesta: {}", fallo.getMessage());
        }
    }

    /**
     * La plantilla se carga al construir el bean: si el recurso no esta en el
     * classpath, el contexto no arranca. Un despliegue que no levanta es preferible
     * a uno que acepta propuestas y descarta el 100 % de los enlaces en silencio.
     */
    private static String loadTemplate() {
        try {
            return new ClassPathResource(TEMPLATE_PATH).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException fallo) {
            log.error("No se pudo cargar la plantilla de correo {}; la aplicacion no arrancara",
                    TEMPLATE_PATH, fallo);
            throw new IllegalStateException(
                    "Plantilla de correo ausente o ilegible en el classpath: " + TEMPLATE_PATH,
                    fallo);
        }
    }
}
