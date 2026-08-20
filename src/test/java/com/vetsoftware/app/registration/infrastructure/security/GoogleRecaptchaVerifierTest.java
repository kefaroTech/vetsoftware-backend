package com.vetsoftware.app.registration.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verifyNoInteractions;

import com.vetsoftware.app.registration.application.exception.CaptchaVerificationException;
import java.lang.reflect.Constructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Adaptador de reCAPTCHA: puerto de infraestructura externa. Se mockea el
 * cliente HTTP subyacente ({@code RestClient}), no la clase bajo prueba.
 *
 * <p>
 * {@code RequestBodySpec} sobrecarga {@code body(...)} tres veces —
 * {@code body(Object)}, {@code body(T, ParameterizedTypeReference<T>)} y
 * {@code body(StreamingHttpOutputMessage.Body)}—, y {@code any()} sin tipo
 * resuelve, entre las dos sobrecargas de un solo argumento, a la MÁS ESPECÍFICA
 * ({@code StreamingHttpOutputMessage.Body}), no a {@code body(Object)} que es
 * la que usa producción con el {@code MultiValueMap} del formulario. Stubear
 * con {@code any()} a secas compila pero apunta a un método distinto y Mockito
 * lo reporta como "argument mismatch" en vez de "wrong overload".
 * {@code any(Object.class)} fuerza la sobrecarga correcta.
 *
 * <p>
 * {@code SiteVerifyResponse} es un record privado de
 * {@code GoogleRecaptchaVerifier}; se instancia por reflexión para poder
 * devolverlo desde el mock del cuerpo de la respuesta.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GoogleRecaptchaVerifier")
class GoogleRecaptchaVerifierTest {

    private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    @Mock
    private RestClient.Builder restClientBuilder;
    @Mock
    private RestClient restClient;
    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock
    private RestClient.RequestBodySpec requestBodySpec;
    @Mock
    private RestClient.ResponseSpec responseSpec;

    private static Object siteVerifyResponse(boolean success, Double score) {
        try {
            Class<?> type = Class.forName(
                    "com.vetsoftware.app.registration.infrastructure.security.GoogleRecaptchaVerifier$SiteVerifyResponse");
            Constructor<?> constructor = type.getDeclaredConstructor(boolean.class, Double.class);
            constructor.setAccessible(true);
            return constructor.newInstance(success, score);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * La factory HTTP se construye en el constructor SIEMPRE, esté o no habilitado
     * el captcha.
     */
    private void stubConstructorChain() {
        doReturn(restClientBuilder).when(restClientBuilder).requestFactory(any());
        doReturn(restClient).when(restClientBuilder).build();
    }

    private GoogleRecaptchaVerifier verifierHabilitado(double minScore) {
        stubConstructorChain();
        doReturn(requestBodyUriSpec).when(restClient).post();
        doReturn(requestBodySpec).when(requestBodyUriSpec).uri(VERIFY_URL);
        doReturn(requestBodySpec).when(requestBodySpec).contentType(any());
        doReturn(requestBodySpec).when(requestBodySpec).body(any(Object.class));
        doReturn(responseSpec).when(requestBodySpec).retrieve();
        return new GoogleRecaptchaVerifier(true, "secret-key", VERIFY_URL, minScore,
                restClientBuilder);
    }

    @Nested
    @DisplayName("Captcha deshabilitado por configuración")
    class Deshabilitado {

        @Test
        @DisplayName("no llama al proveedor y no lanza nada")
        void no_llama_al_proveedor() {
            stubConstructorChain();
            GoogleRecaptchaVerifier verifier = new GoogleRecaptchaVerifier(false, "", VERIFY_URL,
                    0.5, restClientBuilder);

            verifier.verify("token", "10.0.0.1");

            verifyNoInteractions(restClient);
        }
    }

    @Nested
    @DisplayName("Configuración o entrada inválida")
    class ConfiguracionInvalida {

        @Test
        @DisplayName("habilitado sin secreto configurado falla cerrado sin llamar al proveedor")
        void habilitado_sin_secreto_falla_cerrado() {
            stubConstructorChain();
            GoogleRecaptchaVerifier verifier = new GoogleRecaptchaVerifier(true, "  ", VERIFY_URL,
                    0.5, restClientBuilder);

            // El tipo es lo que se afirma, no el texto (#99): esta poblacion rompe el
            // 100 % de los registros y solo se arregla tocando el despliegue, asi que
            // tiene clase propia para que el handler la registre en ERROR. Si alguien
            // la degradara a CaptchaVerificationException a secas, la caida total
            // volveria a esconderse entre los rechazos legitimos y este test no lo veria
            // afirmando solo la superclase.
            assertThatThrownBy(() -> verifier.verify("token", "10.0.0.1"))
                    .isInstanceOf(CaptchaConfigurationException.class)
                    .hasMessageContaining("vetsoftware.recaptcha.secret");

            verifyNoInteractions(restClient);
        }

        @Test
        @DisplayName("sin token de captcha se rechaza antes de llamar al proveedor")
        void sin_token_se_rechaza() {
            stubConstructorChain();
            GoogleRecaptchaVerifier verifier = new GoogleRecaptchaVerifier(true, "secret-key",
                    VERIFY_URL, 0.5, restClientBuilder);

            assertThatThrownBy(() -> verifier.verify(" ", "10.0.0.1"))
                    .isInstanceOf(CaptchaVerificationException.class)
                    .hasMessageContaining("required");

            verifyNoInteractions(restClient);
        }
    }

    @Nested
    @DisplayName("Respuesta del proveedor")
    class RespuestaDelProveedor {

        @Test
        @DisplayName("success=true y sin score aprueba el captcha (v2)")
        void success_sin_score_aprueba() {
            GoogleRecaptchaVerifier verifier = verifierHabilitado(0.5);
            doReturn(siteVerifyResponse(true, null)).when(responseSpec).body(any(Class.class));

            verifier.verify("token-valido", "10.0.0.1");
        }

        @Test
        @DisplayName("success=true y score por encima del mínimo aprueba (v3)")
        void score_por_encima_del_minimo_aprueba() {
            GoogleRecaptchaVerifier verifier = verifierHabilitado(0.5);
            doReturn(siteVerifyResponse(true, 0.9)).when(responseSpec).body(any(Class.class));

            verifier.verify("token-valido", "10.0.0.1");
        }

        @Test
        @DisplayName("score por debajo del mínimo configurado se rechaza (v3)")
        void score_bajo_se_rechaza() {
            GoogleRecaptchaVerifier verifier = verifierHabilitado(0.5);
            doReturn(siteVerifyResponse(true, 0.1)).when(responseSpec).body(any(Class.class));

            assertThatThrownBy(() -> verifier.verify("token-sospechoso", "10.0.0.1"))
                    .isInstanceOf(CaptchaVerificationException.class)
                    .hasMessageContaining("score too low");
        }

        @Test
        @DisplayName("success=false se rechaza")
        void success_false_se_rechaza() {
            GoogleRecaptchaVerifier verifier = verifierHabilitado(0.5);
            doReturn(siteVerifyResponse(false, null)).when(responseSpec).body(any(Class.class));

            assertThatThrownBy(() -> verifier.verify("token-invalido", "10.0.0.1"))
                    .isInstanceOf(CaptchaVerificationException.class)
                    .hasMessageContaining("validation failed");
        }

        @Test
        @DisplayName("un cuerpo de respuesta malformado (nulo) se rechaza")
        void respuesta_nula_se_rechaza() {
            GoogleRecaptchaVerifier verifier = verifierHabilitado(0.5);
            doReturn(null).when(responseSpec).body(any(Class.class));

            assertThatThrownBy(() -> verifier.verify("token", "10.0.0.1"))
                    .isInstanceOf(CaptchaVerificationException.class)
                    .hasMessageContaining("validation failed");
        }

        @Test
        @DisplayName("un 4xx del proveedor es problema de configuracion, no del usuario")
        void un_4xx_del_proveedor_es_configuracion() {
            GoogleRecaptchaVerifier verifier = verifierHabilitado(0.5);
            // El cuerpo que manda este adapter es siempre el mismo par (secret,
            // response): lo unico que Google puede rechazar con un 4xx es la credencial.
            HttpClientErrorException rechazo = HttpClientErrorException.create(HttpStatus.FORBIDDEN,
                    "Forbidden", HttpHeaders.EMPTY, new byte[0], null);
            org.mockito.Mockito.doThrow(rechazo).when(responseSpec).body(any(Class.class));

            assertThatThrownBy(() -> verifier.verify("token", "10.0.0.1"))
                    .isInstanceOf(CaptchaConfigurationException.class)
                    .hasMessageContaining("siteverify rejected the request")
                    .hasMessageContaining("403").cause().isSameAs(rechazo);
        }

        @Test
        @DisplayName("un corte de red del proveedor es indisponibilidad transitoria, con la causa")
        void un_corte_de_red_es_indisponibilidad() {
            GoogleRecaptchaVerifier verifier = verifierHabilitado(0.5);
            ResourceAccessException corte = new ResourceAccessException(
                    "I/O error: read timed out");
            org.mockito.Mockito.doThrow(corte).when(responseSpec).body(any(Class.class));

            // isNotInstanceOf es la mitad del valor: las dos clases extienden
            // CaptchaVerificationException, asi que sin esta linea el test pasaria
            // igual si las dos poblaciones volvieran a colapsar en una sola.
            assertThatThrownBy(() -> verifier.verify("token", "10.0.0.1"))
                    .isInstanceOf(CaptchaProviderUnavailableException.class)
                    .isNotInstanceOf(CaptchaConfigurationException.class)
                    .hasMessageContaining("siteverify call failed")
                    .hasMessageContaining("ResourceAccessException").cause().isSameAs(corte);
        }

        @Test
        @DisplayName("un fallo inesperado del cliente cae en la cola residual y sigue fallando cerrado")
        void un_fallo_inesperado_cae_en_la_cola_residual() {
            GoogleRecaptchaVerifier verifier = verifierHabilitado(0.5);
            RuntimeException imprevisto = new RuntimeException("connect timed out");
            org.mockito.Mockito.doThrow(imprevisto).when(responseSpec).body(any(Class.class));

            // Lo que no se previo se trata como transitorio: es lo conservador, porque
            // no afirma que la configuracion este mal cuando no se sabe. Lo que NO puede
            // pasar es que escape y se convierta en un 500.
            assertThatThrownBy(() -> verifier.verify("token", "10.0.0.1"))
                    .isInstanceOf(CaptchaProviderUnavailableException.class)
                    .hasMessageContaining("siteverify call failed").cause().isSameAs(imprevisto);
        }

        @Test
        @DisplayName("un remoteIp en blanco no impide la verificación")
        void remote_ip_en_blanco_no_impide_la_verificacion() {
            GoogleRecaptchaVerifier verifier = verifierHabilitado(0.5);
            doReturn(siteVerifyResponse(true, null)).when(responseSpec).body(any(Class.class));

            verifier.verify("token-valido", " ");
        }

        @Test
        @DisplayName("un remoteIp nulo tampoco: la guarda comprueba el null antes que el blanco")
        void remote_ip_nulo_no_impide_la_verificacion() {
            GoogleRecaptchaVerifier verifier = verifierHabilitado(0.5);
            doReturn(siteVerifyResponse(true, null)).when(responseSpec).body(any(Class.class));

            // Si alguien simplificara la guarda a remoteIp.isBlank(), un registro sin IP
            // de cliente reventaria con NPE y saldria como 500 en vez de completarse.
            verifier.verify("token-valido", null);
        }
    }
}
