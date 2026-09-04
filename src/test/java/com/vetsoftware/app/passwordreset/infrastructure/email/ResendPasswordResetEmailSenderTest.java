package com.vetsoftware.app.passwordreset.infrastructure.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
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

/**
 * No mockea {@code DevEmailPreview.show} (llamada estatica de solo logging): el
 * canal de previsualizacion local es deliberadamente el unico lugar donde el
 * token en claro se ve fuera del correo, y no tiene efecto observable desde
 * este test.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ResendPasswordResetEmailSender")
class ResendPasswordResetEmailSenderTest {

    private static final String SUBJECT = "Restablece tu contraseña de Lumbre";

    @Mock
    private ResendEmailClient email;

    @Nested
    @DisplayName("email deshabilitado (dev): no llama a Resend")
    class EmailDeshabilitado {

        @Test
        @DisplayName("no envia la plantilla cuando el email esta deshabilitado")
        void no_envia_la_plantilla_cuando_esta_deshabilitado() {
            when(email.isEnabled()).thenReturn(false);
            var sender = new ResendPasswordResetEmailSender(email, "https://app.vetrina.co/reset",
                    "tpl-1");

            sender.send("ana@vetrina.co", "Ana Ruiz", "EMP001", "Clinica Norte", "raw-token");

            verify(email, never()).sendTemplate(any(), any(), any(), any(), anyMap());
        }
    }

    @Nested
    @DisplayName("email habilitado: arma la plantilla de Resend")
    class EmailHabilitado {

        @Test
        @DisplayName("envia con el asunto fijo, la plantilla configurada y las cinco variables")
        void envia_con_las_cinco_variables() {
            when(email.isEnabled()).thenReturn(true);
            var sender = new ResendPasswordResetEmailSender(email, "https://app.vetrina.co/reset",
                    "tpl-1");

            sender.send("ana@vetrina.co", "Ana Ruiz", "EMP001", "Clinica Norte", "raw-token");

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor
                    .forClass(Map.class);
            verify(email).sendTemplate(eq("ana@vetrina.co"), isNull(), eq(SUBJECT), eq("tpl-1"),
                    variablesCaptor.capture());
            Map<String, Object> variables = variablesCaptor.getValue();
            assertThat(variables).containsEntry("EMPLOYEE_NAME", "Ana Ruiz")
                    .containsEntry("COMPANY_NAME", "Clinica Norte")
                    .containsEntry("EMPLOYEE_CODE", "EMP001")
                    .containsEntry("EMPLOYEE_EMAIL", "ana@vetrina.co");
            assertThat(variables.get("RESET_URL"))
                    .isEqualTo("https://app.vetrina.co/reset?token=raw-token");
        }

        @Test
        @DisplayName("cuando la URL base ya trae query params, el token se agrega con &")
        void con_query_params_previos_el_token_se_agrega_con_ampersand() {
            when(email.isEnabled()).thenReturn(true);
            var sender = new ResendPasswordResetEmailSender(email,
                    "https://app.vetrina.co/reset?ref=email", "tpl-1");

            sender.send("ana@vetrina.co", "Ana Ruiz", "EMP001", "Clinica Norte", "raw-token");

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor
                    .forClass(Map.class);
            verify(email).sendTemplate(any(), isNull(), eq(SUBJECT), eq("tpl-1"),
                    variablesCaptor.capture());
            assertThat(variablesCaptor.getValue().get("RESET_URL"))
                    .isEqualTo("https://app.vetrina.co/reset?ref=email&token=raw-token");
        }

        @Test
        @DisplayName("el token viaja URL-encoded (espacios y simbolos reservados)")
        void el_token_se_codifica_para_url() {
            when(email.isEnabled()).thenReturn(true);
            var sender = new ResendPasswordResetEmailSender(email, "https://app.vetrina.co/reset",
                    "tpl-1");

            sender.send("ana@vetrina.co", "Ana Ruiz", "EMP001", "Clinica Norte", "a b+c");

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor
                    .forClass(Map.class);
            verify(email).sendTemplate(any(), isNull(), eq(SUBJECT), eq("tpl-1"),
                    variablesCaptor.capture());
            assertThat(variablesCaptor.getValue().get("RESET_URL"))
                    .isEqualTo("https://app.vetrina.co/reset?token=a+b%2Bc");
        }

        @Test
        @DisplayName("nombre, empresa y codigo nulos se envian como cadena vacia, nunca como null literal")
        void campos_nulos_se_envian_como_cadena_vacia() {
            when(email.isEnabled()).thenReturn(true);
            var sender = new ResendPasswordResetEmailSender(email, "https://app.vetrina.co/reset",
                    "tpl-1");

            sender.send("ana@vetrina.co", null, null, null, "raw-token");

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor
                    .forClass(Map.class);
            verify(email).sendTemplate(any(), isNull(), eq(SUBJECT), eq("tpl-1"),
                    variablesCaptor.capture());
            Map<String, Object> variables = variablesCaptor.getValue();
            assertThat(variables).containsEntry("EMPLOYEE_NAME", "")
                    .containsEntry("COMPANY_NAME", "").containsEntry("EMPLOYEE_CODE", "");
        }
    }

    /**
     * Sin el default commiteado en application.yml, una clave ausente ya no degrada
     * a "correo que no sale": tumba el arranque. Aqui el silencio es especialmente
     * caro porque la respuesta del endpoint es identica por diseno
     * anti-enumeracion: nadie, ni el usuario ni el operador, puede distinguir el
     * correo perdido del enviado.
     */
    @Nested
    @DisplayName("configuracion incompleta: el arranque falla antes que el correo")
    class ConfiguracionIncompleta {

        private ResendPasswordResetEmailSender sinPlantilla() {
            return new ResendPasswordResetEmailSender(email, "https://app.vetrina.co/reset", "");
        }

        private ResendPasswordResetEmailSender sinUrlBase() {
            return new ResendPasswordResetEmailSender(email, "   ", "tpl-1");
        }

        @Test
        @DisplayName("con el correo habilitado, la plantilla vacia TUMBA el arranque")
        void la_plantilla_vacia_tumba_el_arranque() {
            when(email.isEnabled()).thenReturn(true);

            assertThatThrownBy(this::sinPlantilla).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("password-reset.template-id");
        }

        @Test
        @DisplayName("la URL base tambien es obligatoria: es el destino del enlace del correo")
        void la_url_base_tambien_es_obligatoria() {
            when(email.isEnabled()).thenReturn(true);

            assertThatThrownBy(this::sinUrlBase).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("reset-base-url");
        }

        @Test
        @DisplayName("con el correo deshabilitado no exige nada: es el modo de las rodajas y del contrato OpenAPI")
        void con_el_correo_deshabilitado_no_exige_nada() {
            when(email.isEnabled()).thenReturn(false);

            assertThatCode(this::sinPlantilla).doesNotThrowAnyException();
        }
    }
}
