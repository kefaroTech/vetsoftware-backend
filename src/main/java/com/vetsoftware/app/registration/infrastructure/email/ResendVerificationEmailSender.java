package com.vetsoftware.app.registration.infrastructure.email;

import com.vetsoftware.app.infrastructure.email.ResendEmailClient;
import com.vetsoftware.app.infrastructure.logging.DevEmailPreview;
import com.vetsoftware.app.registration.application.port.out.VerificationEmailSender;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Correo de verificación de cuenta (auto-registro Opción B) enviado con una
 * <b>plantilla de Resend</b> ({@code template: { id, variables }}). El enlace
 * apunta a la página del front ({@code
 * verification-base-url}) con el token plano; el front llama a
 * {@code POST /register/verify}. Si el envío está deshabilitado (dev), deja el
 * enlace + código en el log. El envío es asíncrono y no bloquea el registro (si
 * Resend falla, se registra un warning y el registro continúa).
 *
 * <p>
 * Variables de la plantilla (deben coincidir con los {@code {{{VARIABLE}}}} del
 * HTML en Resend): ADMIN_NAME, COMPANY_NAME, VERIFY_URL, HELP_URL, PRIVACY_URL,
 * TERMS_URL.
 */
@Component
public class ResendVerificationEmailSender implements VerificationEmailSender {

    private static final Logger log = LoggerFactory.getLogger(ResendVerificationEmailSender.class);

    private static final String SUBJECT = "Verifica tu cuenta de VetSoftware";

    private final ResendEmailClient email;
    private final String verificationBaseUrl;
    private final String templateId;
    private final String helpUrl;
    private final String privacyUrl;
    private final String termsUrl;

    // El default vacio de la configuracion NO es la politica: es lo que permite que
    // el contrato OpenAPI, las rodajas de test y el perfil local arranquen sin
    // declarar nada. Quien decide si un valor ausente es tolerable es
    // requireConfiguredWhenEmailIsEnabled(), abajo.
    public ResendVerificationEmailSender(ResendEmailClient email,
            @Value("${vetsoftware.registration.verification-base-url}") String verificationBaseUrl,
            @Value("${vetsoftware.registration.verification-template-id:}") String templateId,
            @Value("${vetsoftware.email.help-url:}") String helpUrl,
            @Value("${vetsoftware.email.privacy-url:}") String privacyUrl,
            @Value("${vetsoftware.email.terms-url:}") String termsUrl) {
        this.email = email;
        this.verificationBaseUrl = verificationBaseUrl;
        this.templateId = templateId;
        this.helpUrl = helpUrl;
        this.privacyUrl = privacyUrl;
        this.termsUrl = termsUrl;
        requireConfiguredWhenEmailIsEnabled();
    }

    @Override
    public void send(String toEmail, String employeeName, String companyName, String rawToken) {
        String link = buildLink(rawToken);
        if (!email.isEnabled()) {
            // El enlace lleva el token de verificación en claro: va por el canal de
            // previsualización
            // local, que no alcanza el pipeline exportado (ver DevEmailPreview).
            DevEmailPreview.show(toEmail, "Enlace de verificación", link);
            return;
        }
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("ADMIN_NAME", nz(employeeName));
        variables.put("COMPANY_NAME", nz(companyName));
        variables.put("VERIFY_URL", link);
        variables.put("HELP_URL", helpUrl);
        variables.put("PRIVACY_URL", privacyUrl);
        variables.put("TERMS_URL", termsUrl);

        email.sendTemplate(toEmail, null, SUBJECT, templateId, variables);
    }

    private String buildLink(String rawToken) {
        String separator = verificationBaseUrl.contains("?") ? "&" : "?";
        return verificationBaseUrl + separator + "token="
                + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
    }

    /**
     * Fallo al arrancar, y solo cuando el correo esta habilitado.
     *
     * <p>
     * Con {@code verification-template-id} vacio, {@code sendTemplate} escribe un
     * warning y retorna: la app levanta, {@code POST /register} responde con exito,
     * y el correo de verificacion nunca sale. La cuenta recien creada queda sin
     * poder iniciar sesion (auto-registro Opcion B) y nadie se entera: ni el dueno,
     * que no recibio nada, ni el operador, que solo tiene un warning en el log.
     * Mientras el identificador viajo commiteado como default, esa red existia por
     * accidente; con el valor fuera de la imagen, la unica red es esta.
     *
     * <p>
     * El default vacio existe para que el contrato OpenAPI, las rodajas de test y
     * el perfil local, que declaran {@code vetsoftware.email.enabled=false},
     * arranquen sin declarar nada; ninguno de ellos pasa por aqui. Los unicos que
     * si lo hacen son dev y prod, que declaran {@code enabled: true}, que es
     * exactamente donde el silencio cuesta.
     */
    private void requireConfiguredWhenEmailIsEnabled() {
        if (!email.isEnabled()) {
            return;
        }
        requireConfigured(verificationBaseUrl, "vetsoftware.registration.verification-base-url");
        requireConfigured(templateId, "vetsoftware.registration.verification-template-id");
    }

    private static void requireConfigured(String value, String key) {
        if (value == null || value.isBlank()) {
            log.error("{} sin valor con el correo habilitado; la aplicacion no arrancara: el correo"
                    + " de verificacion del auto-registro no saldria y la cuenta naceria sin poder"
                    + " iniciar sesion, sin que nadie se entere", key);
            throw new IllegalStateException(
                    "Configuracion del correo de verificacion incompleta: " + key);
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
