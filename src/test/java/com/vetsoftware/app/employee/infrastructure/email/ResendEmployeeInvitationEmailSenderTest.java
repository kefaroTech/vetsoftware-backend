package com.vetsoftware.app.employee.infrastructure.email;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
 * Correo de invitación de empleado: plantilla server-side de Resend con las
 * variables que el HTML espera. Async/best-effort lo garantiza
 * {@link ResendEmailClient}, no esta clase.
 */
@ExtendWith(MockitoExtension.class)
class ResendEmployeeInvitationEmailSenderTest {

    private static final String TEMPLATE_ID = "tmpl_employee_invitation";
    private static final String LOGIN_URL = "https://app.vetrina.co/login";

    @Mock
    private ResendEmailClient email;

    private ResendEmployeeInvitationEmailSender sender;

    @Nested
    @DisplayName("envio")
    class Envio {

        @Test
        @DisplayName("envia la plantilla con todas las variables completas")
        void envia_la_plantilla_con_todas_las_variables() {
            sender = new ResendEmployeeInvitationEmailSender(email, TEMPLATE_ID, LOGIN_URL);

            sender.send("mariana@vetrina.co", "Mariana Rojas", "Veterinaria Vetrina", "VV-MARIANA",
                    "Temporal123*", "Veterinario");

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> variables = ArgumentCaptor.forClass(Map.class);
            verify(email).sendTemplate(eq("mariana@vetrina.co"), isNull(),
                    eq("Tu cuenta de Vetrina está lista"), eq(TEMPLATE_ID), variables.capture());

            Map<String, Object> vars = variables.getValue();
            org.assertj.core.api.Assertions.assertThat(vars)
                    .containsEntry("EMPLOYEE_NAME", "Mariana Rojas")
                    .containsEntry("COMPANY_NAME", "Veterinaria Vetrina")
                    .containsEntry("EMPLOYEE_CODE", "VV-MARIANA")
                    .containsEntry("TEMP_PASSWORD", "Temporal123*")
                    .containsEntry("ROLE_NAME", "Veterinario").containsEntry("LOGIN_URL", LOGIN_URL)
                    .containsEntry("EMPLOYEE_EMAIL", "mariana@vetrina.co");
        }

        @Test
        @DisplayName("las variables nulas se envian como cadena vacia, nunca como null")
        void las_variables_nulas_se_envian_como_cadena_vacia() {
            sender = new ResendEmployeeInvitationEmailSender(email, TEMPLATE_ID, LOGIN_URL);

            sender.send("mariana@vetrina.co", null, null, null, null, null);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> variables = ArgumentCaptor.forClass(Map.class);
            verify(email).sendTemplate(eq("mariana@vetrina.co"), isNull(),
                    eq("Tu cuenta de Vetrina está lista"), eq(TEMPLATE_ID), variables.capture());

            Map<String, Object> vars = variables.getValue();
            org.assertj.core.api.Assertions.assertThat(vars).containsEntry("EMPLOYEE_NAME", "")
                    .containsEntry("COMPANY_NAME", "").containsEntry("ROLE_NAME", "");
        }
    }

    /**
     * Sin el default commiteado en application.yml, una clave ausente ya no degrada
     * a "correo que no sale": tumba el arranque. El codigo de usuario y la
     * contrasena temporal solo viajan por este correo, asi que perderlo deja al
     * empleado creado y sin forma de entrar.
     */
    @Nested
    @DisplayName("configuracion incompleta")
    class ConfiguracionIncompleta {

        private ResendEmployeeInvitationEmailSender sinPlantilla() {
            return new ResendEmployeeInvitationEmailSender(email, "", LOGIN_URL);
        }

        private ResendEmployeeInvitationEmailSender sinLoginUrl() {
            return new ResendEmployeeInvitationEmailSender(email, TEMPLATE_ID, "   ");
        }

        @Test
        @DisplayName("con el correo habilitado, la plantilla vacia TUMBA el arranque")
        void la_plantilla_vacia_tumba_el_arranque() {
            when(email.isEnabled()).thenReturn(true);

            assertThatThrownBy(this::sinPlantilla).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("invitation-template-id");
        }

        @Test
        @DisplayName("login-url tambien es obligatoria: el correo saldria sin destino al que ir")
        void login_url_tambien_es_obligatoria() {
            when(email.isEnabled()).thenReturn(true);

            assertThatThrownBy(this::sinLoginUrl).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("employee.login-url");
        }

        @Test
        @DisplayName("con el correo deshabilitado no exige nada: es el modo de las rodajas y del contrato OpenAPI")
        void con_el_correo_deshabilitado_no_exige_nada() {
            when(email.isEnabled()).thenReturn(false);

            assertThatCode(this::sinPlantilla).doesNotThrowAnyException();
        }
    }
}
