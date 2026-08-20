package com.vetsoftware.app.registration.infrastructure.security;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vetsoftware.app.registration.application.exception.CaptchaVerificationException;
import com.vetsoftware.app.registration.application.port.out.CaptchaVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adapter de reCAPTCHA (Google). Verifica el token server-side contra
 * {@code siteverify}. Soporta v2 (solo {@code success}) y v3 (ademas
 * {@code score}, comparado contra {@code min-score}).
 *
 * <p>
 * Se controla por configuracion:
 *
 * <ul>
 * <li>{@code vetsoftware.recaptcha.enabled=false} (por defecto) → no-op, util
 * en dev/local y tests.
 * <li>{@code vetsoftware.recaptcha.secret} → llave secreta del proveedor (por
 * env, sin versionar).
 * </ul>
 */
@Component
public class GoogleRecaptchaVerifier implements CaptchaVerifier {

    private final boolean enabled;
    private final String secret;
    private final String verifyUrl;
    private final double minScore;
    private final RestClient restClient;

    public GoogleRecaptchaVerifier(@Value("${vetsoftware.recaptcha.enabled:false}") boolean enabled,
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
        if (!enabled)
            return;

        if (secret == null || secret.isBlank()) {
            // Config invalida: captcha activo pero sin secreto. Fallar cerrado (no permitir
            // registro). No se registra aqui: el hecho lo registra el handler, que es el
            // punto unico de registro (#99).
            throw new CaptchaConfigurationException(
                    "reCAPTCHA is enabled but 'vetsoftware.recaptcha.secret' is not set");
        }
        if (captchaToken == null || captchaToken.isBlank()) {
            throw new CaptchaVerificationException("Captcha token is required");
        }

        SiteVerifyResponse result;
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("secret", secret);
            form.add("response", captchaToken);
            if (remoteIp != null && !remoteIp.isBlank())
                form.add("remoteip", remoteIp);
            result = restClient.post().uri(verifyUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve()
                    .body(SiteVerifyResponse.class);
        } catch (HttpClientErrorException e) {
            // Un 4xx de siteverify NO habla del usuario. El cuerpo que manda este adapter
            // es siempre el mismo par (secret, response), asi que lo unico que Google puede
            // estar rechazando es la credencial: secreto vacio, de otro proyecto, o de un
            // tipo de reCAPTCHA distinto al del sitio. Eso no falla para un registro: falla
            // para todos, y no se arregla reintentando.
            throw new CaptchaConfigurationException(
                    "reCAPTCHA siteverify rejected the request with " + e.getStatusCode()
                            + "; check 'vetsoftware.recaptcha.secret'",
                    e);
        } catch (RestClientException e) {
            // 5xx, timeout de conexion/lectura o corte de red: el proveedor no contesta. Es
            // transitorio y ajeno a este despliegue, y por eso no comparte ni clase ni
            // severidad con el caso de arriba (#99). La causa viaja entera: es lo unico que
            // distingue un read timeout de un 503.
            throw new CaptchaProviderUnavailableException(
                    "reCAPTCHA siteverify call failed: " + e.getClass().getSimpleName(), e);
        } catch (RuntimeException e) {
            // Cola residual. Separar las dos poblaciones de arriba NO puede estrechar lo
            // que este metodo atrapa: el catch (Exception) que habia antes garantizaba que
            // un fallo de la llamada terminara siempre en un 400 que falla cerrado, y
            // dejarlo escapar ahora lo convertiria en un 500. Todo lo que RestClient lanza
            // de verdad cae en los catch anteriores; esto cubre lo que no previmos, y lo
            // trata como transitorio porque es lo conservador: no afirma que la
            // configuracion este mal cuando no lo sabe.
            throw new CaptchaProviderUnavailableException(
                    "reCAPTCHA siteverify call failed: " + e.getClass().getSimpleName(), e);
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
    private record SiteVerifyResponse(@JsonProperty("success") boolean success,
            @JsonProperty("score") Double score) {
    }
}
