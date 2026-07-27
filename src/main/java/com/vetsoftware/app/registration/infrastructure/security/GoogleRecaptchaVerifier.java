package com.vetsoftware.app.registration.infrastructure.security;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vetsoftware.app.registration.application.exception.CaptchaVerificationException;
import com.vetsoftware.app.registration.application.port.out.CaptchaVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Adapter de reCAPTCHA (Google). Verifica el token server-side contra {@code siteverify}. Soporta v2
 * (solo {@code success}) y v3 (ademas {@code score}, comparado contra {@code min-score}).
 *
 * <p>Se controla por configuracion:
 * <ul>
 *   <li>{@code vetsoftware.recaptcha.enabled=false} (por defecto) → no-op, util en dev/local y tests.</li>
 *   <li>{@code vetsoftware.recaptcha.secret} → llave secreta del proveedor (por env, sin versionar).</li>
 * </ul>
 */
@Component
public class GoogleRecaptchaVerifier implements CaptchaVerifier {

    private static final Logger log = LoggerFactory.getLogger(GoogleRecaptchaVerifier.class);

    private final boolean enabled;
    private final String secret;
    private final String verifyUrl;
    private final double minScore;
    private final RestClient restClient;

    public GoogleRecaptchaVerifier(
            @Value("${vetsoftware.recaptcha.enabled:false}") boolean enabled,
            @Value("${vetsoftware.recaptcha.secret:}") String secret,
            @Value("${vetsoftware.recaptcha.verify-url:https://www.google.com/recaptcha/api/siteverify}") String verifyUrl,
            @Value("${vetsoftware.recaptcha.min-score:0.5}") double minScore,
            RestClient.Builder restClientBuilder) {
        this.enabled = enabled;
        this.secret = secret;
        this.verifyUrl = verifyUrl;
        this.minScore = minScore;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(10_000);
        this.restClient = restClientBuilder.requestFactory(factory).build();
    }

    @Override
    public void verify(String captchaToken, String remoteIp) {
        if (!enabled) return;

        if (secret == null || secret.isBlank()) {
            // Config invalida: captcha activo pero sin secreto. Fallar cerrado (no permitir registro).
            log.error("reCAPTCHA enabled but 'vetsoftware.recaptcha.secret' is not set");
            throw new CaptchaVerificationException("Captcha is not configured");
        }
        if (captchaToken == null || captchaToken.isBlank()) {
            throw new CaptchaVerificationException("Captcha token is required");
        }

        SiteVerifyResponse result;
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("secret", secret);
            form.add("response", captchaToken);
            if (remoteIp != null && !remoteIp.isBlank()) form.add("remoteip", remoteIp);
            result = restClient.post()
                    .uri(verifyUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(SiteVerifyResponse.class);
        } catch (Exception e) {
            log.error("reCAPTCHA verification call failed: {}", e.getMessage());
            throw new CaptchaVerificationException("Could not verify captcha");
        }

        if (result == null || !result.success()) {
            throw new CaptchaVerificationException("Captcha validation failed");
        }
        // v3: si el proveedor devuelve score, exigir el minimo configurado.
        if (result.score() != null && result.score() < minScore) {
            throw new CaptchaVerificationException("Captcha score too low");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SiteVerifyResponse(
            @JsonProperty("success") boolean success,
            @JsonProperty("score") Double score) {}
}
