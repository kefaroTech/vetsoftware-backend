package com.vetsoftware.app.registration.infrastructure.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.infrastructure.email.ResendEmailClient;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResendVerificationEmailSender")
class ResendVerificationEmailSenderTest {

    @Mock
    private ResendEmailClient email;

    private ResendVerificationEmailSender sender(String verificationBaseUrl) {
        return new ResendVerificationEmailSender(email, verificationBaseUrl, "template-123",
                "https://vetsoftware.co/ayuda", "https://vetsoftware.co/privacidad",
                "https://vetsoftware.co/terminos");
    }

    @Nested
    @DisplayName("Envío deshabilitado (dev)")
    class EnvioDeshabilitado {

        @Test
        @DisplayName("no llama a Resend cuando el envío está deshabilitado")
        void no_llama_a_resend_cuando_esta_deshabilitado() {
            when(email.isEnabled()).thenReturn(false);

            sender("https://app.vetsoftware.co/verificar").send("orlando@vetrina.co",
                    "Orlando Velásquez", "Veterinaria Vetrina", "raw-token");

            verify(email, never()).sendTemplate(anyString(), any(), anyString(), anyString(),
                    any());
        }
    }

    @Nested
    @DisplayName("Envío habilitado")
    class EnvioHabilitado {

        @Test
        @DisplayName("envía la plantilla con el enlace y las variables del dueño")
        void envia_la_plantilla_con_el_enlace_y_las_variables() {
            when(email.isEnabled()).thenReturn(true);

            sender("https://app.vetsoftware.co/verificar").send("orlando@vetrina.co",
                    "Orlando Velásquez", "Veterinaria Vetrina", "raw-token");

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> variables = ArgumentCaptor.forClass(Map.class);
            verify(email).sendTemplate(eq("orlando@vetrina.co"), isNull(),
                    eq("Verifica tu cuenta de VetSoftware"), eq("template-123"),
                    variables.capture());
            assertThat(variables.getValue()).containsEntry("ADMIN_NAME", "Orlando Velásquez")
                    .containsEntry("COMPANY_NAME", "Veterinaria Vetrina")
                    .containsEntry("HELP_URL", "https://vetsoftware.co/ayuda")
                    .containsEntry("PRIVACY_URL", "https://vetsoftware.co/privacidad")
                    .containsEntry("TERMS_URL", "https://vetsoftware.co/terminos");
            assertThat((String) variables.getValue().get("VERIFY_URL"))
                    .isEqualTo("https://app.vetsoftware.co/verificar?token=raw-token");
        }

        @Test
        @DisplayName("una URL base con query param existente encadena el token con &")
        void url_base_con_query_param_encadena_con_ampersand() {
            when(email.isEnabled()).thenReturn(true);

            sender("https://app.vetsoftware.co/verificar?utm=campana").send("orlando@vetrina.co",
                    "Orlando", "Vetrina", "raw-token");

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> variables = ArgumentCaptor.forClass(Map.class);
            verify(email).sendTemplate(anyString(), any(), anyString(), anyString(),
                    variables.capture());
            assertThat((String) variables.getValue().get("VERIFY_URL"))
                    .isEqualTo("https://app.vetsoftware.co/verificar?utm=campana&token=raw-token");
        }

        @Test
        @DisplayName("nombre y empresa nulos se envían como cadena vacía, nunca como null")
        void nombre_y_empresa_nulos_se_envian_como_cadena_vacia() {
            when(email.isEnabled()).thenReturn(true);

            sender("https://app.vetsoftware.co/verificar").send("orlando@vetrina.co", null, null,
                    "raw-token");

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> variables = ArgumentCaptor.forClass(Map.class);
            verify(email).sendTemplate(anyString(), any(), anyString(), anyString(),
                    variables.capture());
            assertThat(variables.getValue()).containsEntry("ADMIN_NAME", "")
                    .containsEntry("COMPANY_NAME", "");
        }

        @Test
        @DisplayName("un token con caracteres especiales viaja URL-encoded en el enlace")
        void token_con_caracteres_especiales_viaja_url_encoded() {
            when(email.isEnabled()).thenReturn(true);

            sender("https://app.vetsoftware.co/verificar").send("orlando@vetrina.co", "Orlando",
                    "Vetrina", "a+b/c=d");

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> variables = ArgumentCaptor.forClass(Map.class);
            verify(email).sendTemplate(anyString(), any(), anyString(), anyString(),
                    variables.capture());
            assertThat((String) variables.getValue().get("VERIFY_URL"))
                    .contains("token=a%2Bb%2Fc%3Dd");
        }
    }

    /**
     * Sin el default commiteado en application.yml, una clave ausente ya no degrada
     * a "correo que no sale": tumba el arranque. Es el mismo fail-fast del alta de
     * superadministradores y por el mismo motivo, porque el silencio de
     * {@code sendTemplate} con la plantilla vacia es indistinguible del exito desde
     * fuera.
     */
    @Nested
    @DisplayName("Configuracion incompleta")
    class ConfiguracionIncompleta {

        private ResendVerificationEmailSender sinPlantilla() {
            return new ResendVerificationEmailSender(email, "https://app.vetsoftware.co/verificar",
                    "", "https://vetsoftware.co/ayuda", "https://vetsoftware.co/privacidad",
                    "https://vetsoftware.co/terminos");
        }

        private ResendVerificationEmailSender sinUrlBase() {
            return new ResendVerificationEmailSender(email, "   ", "template-123",
                    "https://vetsoftware.co/ayuda", "https://vetsoftware.co/privacidad",
                    "https://vetsoftware.co/terminos");
        }

        @Test
        @DisplayName("con el correo habilitado, la plantilla vacia TUMBA el arranque")
        void la_plantilla_vacia_tumba_el_arranque() {
            when(email.isEnabled()).thenReturn(true);

            // Antes arrancaba: POST /register respondia con exito, sendTemplate
            // escribia un warning y retornaba, y el dueno nunca recibia el enlace. La
            // cuenta quedaba creada y sin poder iniciar sesion, y nadie se enteraba.
            assertThatThrownBy(this::sinPlantilla).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("verification-template-id");
        }

        @Test
        @DisplayName("la URL base tambien es obligatoria: sin ella el enlace no va a ningun sitio")
        void la_url_base_tambien_es_obligatoria() {
            when(email.isEnabled()).thenReturn(true);

            assertThatThrownBy(this::sinUrlBase).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("verification-base-url");
        }

        @Test
        @DisplayName("con el correo deshabilitado no exige nada: es el modo de las rodajas y del contrato OpenAPI")
        void con_el_correo_deshabilitado_no_exige_nada() {
            when(email.isEnabled()).thenReturn(false);

            // application-openapi.yml declara vetsoftware.email.enabled=false, asi que
            // OpenApiContractIT y el perfil local siguen levantando sin una sola clave.
            assertThatCode(this::sinPlantilla).doesNotThrowAnyException();
        }
    }
}
