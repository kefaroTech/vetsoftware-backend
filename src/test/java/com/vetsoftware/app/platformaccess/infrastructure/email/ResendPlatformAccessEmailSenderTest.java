package com.vetsoftware.app.platformaccess.infrastructure.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.infrastructure.email.EmailDispatchOutcome;
import com.vetsoftware.app.infrastructure.email.ResendEmailClient;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessAuditPort;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessEmailSender.AccessRequestedNotification;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessMetrics;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessMetrics.InvitationResult;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Lo que este test protege es un agujero de seguridad concreto, no un formato.
 *
 * <p>
 * {@code POST /platform/access-request} es público y anónimo, y su cuerpo lleva
 * {@code fullName} y {@code reason} —texto libre de un desconocido— que se
 * pintan en un correo cuyo destinatario es <b>la persona que puede crear
 * superadministradores de plataforma</b>. Las plantillas de Resend de este
 * repositorio usan triple llave, que es inserción sin escapar. Sin el escapado,
 * un motivo con su propio bloque HTML produce un correo del remitente legítimo,
 * con la marca correcta, y un botón «Aprobar acceso» que apunta al atacante — y
 * con estilos puede además ocultar el enlace real.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ResendPlatformAccessEmailSender — los cuatro correos del alta")
class ResendPlatformAccessEmailSenderTest {

    private static final LocalDateTime SOLICITADA = LocalDateTime.of(2026, 3, 14, 9, 30);

    @Mock
    private ResendEmailClient email;
    @Mock
    private PlatformAccessAuditPort audit;
    @Mock
    private PlatformAccessMetrics metrics;

    private ResendPlatformAccessEmailSender crearRemitente() {
        return new ResendPlatformAccessEmailSender(email, audit, metrics, "aprobador@vetrina.co",
                "https://consola.vetrina.co/aprobar-acceso",
                "https://consola.vetrina.co/aceptar-invitacion", "https://consola.vetrina.co/login",
                "tpl-solicitud", "tpl-aprobada", "tpl-rechazada", "tpl-bienvenida",
                "https://vetrina.co/ayuda", "https://vetrina.co/privacidad",
                "https://vetrina.co/terminos");
    }

    private static AccessRequestedNotification solicitud(String fullName, String reason) {
        return new AccessRequestedNotification(4271L, fullName, "ana@vetrina.co", reason,
                SOLICITADA, "token-plano-de-prueba", "123456");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> variablesEnviadas() {
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(email).sendTemplate(anyString(), isNull(), anyString(), anyString(),
                captor.capture());
        return captor.getValue();
    }

    @Nested
    @DisplayName("aviso al aprobador")
    class AvisoAlAprobador {

        @Test
        @DisplayName("va al correo del aprobador de la configuracion, nunca al del solicitante")
        void va_al_aprobador_de_la_configuracion() {
            when(email.isEnabled()).thenReturn(true);
            when(email.sendTemplate(anyString(), isNull(), anyString(), anyString(), anyMap()))
                    .thenReturn(CompletableFuture.completedFuture(EmailDispatchOutcome.ACCEPTED));

            crearRemitente().sendAccessRequested(solicitud("Ana Ramirez", "Un motivo suficiente."));

            // Quien pueda nombrar al destinatario puede aprobarse a si mismo un
            // superadministrador: por eso sale de la configuracion y no del cuerpo.
            verify(email).sendTemplate(eq("aprobador@vetrina.co"), isNull(),
                    eq("Nueva solicitud de acceso de plataforma"), eq("tpl-solicitud"), anyMap());
        }

        @Test
        @DisplayName("ESCAPA el HTML de FULL_NAME y de REASON antes de meterlos en la plantilla")
        void escapa_el_html_de_full_name_y_reason() {
            when(email.isEnabled()).thenReturn(true);
            when(email.sendTemplate(anyString(), isNull(), anyString(), anyString(), anyMap()))
                    .thenReturn(CompletableFuture.completedFuture(EmailDispatchOutcome.ACCEPTED));
            String motivoMalicioso = "<a href='https://atacante.invalid'>Aprobar acceso</a>";

            crearRemitente()
                    .sendAccessRequested(solicitud("<script>alert(1)</script>", motivoMalicioso));

            Map<String, Object> variables = variablesEnviadas();
            // Ni un solo signo de menor puede sobrevivir: con triple llave, lo que
            // entre aqui se inserta tal cual en el HTML que lee el aprobador.
            assertThat((String) variables.get("FULL_NAME")).doesNotContain("<").doesNotContain(">")
                    .contains("&lt;script&gt;");
            assertThat((String) variables.get("REASON")).doesNotContain("<a href")
                    .contains("&lt;a href=&#39;https://atacante.invalid&#39;&gt;");
        }

        @Test
        @DisplayName("el enlace lleva el token codificado para URL y el codigo va en su propia variable")
        void el_enlace_lleva_el_token_codificado() {
            when(email.isEnabled()).thenReturn(true);
            when(email.sendTemplate(anyString(), isNull(), anyString(), anyString(), anyMap()))
                    .thenReturn(CompletableFuture.completedFuture(EmailDispatchOutcome.ACCEPTED));

            crearRemitente().sendAccessRequested(
                    new AccessRequestedNotification(4271L, "Ana Ramirez", "ana@vetrina.co",
                            "Un motivo suficiente.", SOLICITADA, "token+con/simbolos=", "123456"));

            Map<String, Object> variables = variablesEnviadas();
            assertThat((String) variables.get("REVIEW_URL")).isEqualTo(
                    "https://consola.vetrina.co/aprobar-acceso?token=token%2Bcon%2Fsimbolos%3D");
            // El codigo viaja en el MISMO correo que el enlace. Es una decision humana
            // tomada con el riesgo delante: no es un segundo factor, confirma la
            // intencion.
            assertThat(variables.get("VERIFICATION_CODE")).isEqualTo("123456");
        }

        @Test
        @DisplayName("con el correo deshabilitado no llama a Resend: el token sale por el canal de dev")
        void con_el_correo_deshabilitado_no_llama_a_resend() {
            when(email.isEnabled()).thenReturn(false);

            crearRemitente().sendAccessRequested(solicitud("Ana Ramirez", "Un motivo suficiente."));

            verify(email, never()).sendTemplate(any(), any(), any(), any(), anyMap());
        }
    }

    @Nested
    @DisplayName("invitacion — el unico correo cuyo desenlace se vigila")
    class Invitacion {

        @Test
        @DisplayName("un envio aceptado registra el evento de invitacion y cuenta sent")
        void un_envio_aceptado_registra_el_evento() {
            when(email.isEnabled()).thenReturn(true);
            when(email.sendTemplate(anyString(), isNull(), anyString(), anyString(), anyMap()))
                    .thenReturn(CompletableFuture.completedFuture(EmailDispatchOutcome.ACCEPTED));

            crearRemitente().sendInvitation(4271L, "ana@vetrina.co", "Ana Ramirez", "inv-token");

            verify(audit).invited(4271L, "vetrina.co");
            verify(metrics).invitation(InvitationResult.SENT);
        }

        @Test
        @DisplayName("un envio perdido emite el evento en ERROR con el id de la solicitud")
        void un_envio_perdido_emite_el_evento_de_correo_perdido() {
            when(email.isEnabled()).thenReturn(true);
            when(email.sendTemplate(anyString(), isNull(), anyString(), anyString(), anyMap()))
                    .thenReturn(CompletableFuture.completedFuture(EmailDispatchOutcome.FAILED));

            crearRemitente().sendInvitation(4271L, "ana@vetrina.co", "Ana Ramirez", "inv-token");

            // Sin reintento ni cola de salida, un correo que no sale no sale nunca:
            // la cuenta aprobada no llega a existir y nadie se entera. El id viaja
            // como argumento y no confiado al MDC porque esto corre al otro lado de
            // un salto de hilo.
            verify(audit).invitationUndelivered(4271L, "vetrina.co");
            verify(metrics).invitation(InvitationResult.FAILED);
        }

        @Test
        @DisplayName("solo sale el dominio del correo, nunca la direccion completa")
        void solo_sale_el_dominio_del_correo() {
            when(email.isEnabled()).thenReturn(true);
            when(email.sendTemplate(anyString(), isNull(), anyString(), anyString(), anyMap()))
                    .thenReturn(CompletableFuture.completedFuture(EmailDispatchOutcome.ACCEPTED));

            crearRemitente().sendInvitation(4271L, "ana.ramirez@vetrina.co", "Ana", "inv-token");

            // El dominio responde «cuarenta desechables o tres de una empresa» sin
            // identificar a nadie; la direccion completa son datos personales de
            // alguien que quiza nunca fue aprobado.
            verify(audit).invited(4271L, "vetrina.co");
        }

        @Test
        @DisplayName("el correo deshabilitado cuenta skipped, NO failed")
        void el_correo_deshabilitado_cuenta_skipped() {
            when(email.isEnabled()).thenReturn(false);

            crearRemitente().sendInvitation(4271L, "ana@vetrina.co", "Ana Ramirez", "inv-token");

            // Es el modo normal de dev. Contarlo como perdida llenaria de falsos
            // positivos cualquier alerta de tasa.
            verify(metrics).invitation(InvitationResult.SKIPPED);
            verifyNoInteractions(audit);
        }
    }

    @Nested
    @DisplayName("bienvenida — el cuarto correo")
    class Bienvenida {

        @Test
        @DisplayName("lleva el codigo de usuario, sin el cual la cuenta creada no se puede usar")
        void lleva_el_codigo_de_usuario() {
            when(email.isEnabled()).thenReturn(true);
            when(email.sendTemplate(anyString(), isNull(), anyString(), anyString(), anyMap()))
                    .thenReturn(CompletableFuture.completedFuture(EmailDispatchOutcome.ACCEPTED));

            crearRemitente().sendWelcome(4271L, "ana@vetrina.co", "Ana Ramirez", "SYS-ANARAMIREZ");

            Map<String, Object> variables = variablesEnviadas();
            // El login de las cuentas de sistema es por codigo, no por correo: sin
            // este dato la cuenta existe y su dueno no sabe con que usuario entrar.
            assertThat(variables.get("SYSTEM_USER_CODE")).isEqualTo("SYS-ANARAMIREZ");
            assertThat(variables.get("FULL_NAME")).isEqualTo("Ana Ramirez");
        }
    }

    @Nested
    @DisplayName("rechazo")
    class Rechazo {

        @Test
        @DisplayName("no lleva motivo: el rechazo no se justifica")
        void no_lleva_motivo() {
            when(email.isEnabled()).thenReturn(true);
            when(email.sendTemplate(anyString(), isNull(), anyString(), anyString(), anyMap()))
                    .thenReturn(CompletableFuture.completedFuture(EmailDispatchOutcome.ACCEPTED));

            crearRemitente().sendRejection(4271L, "ana@vetrina.co", "Ana Ramirez");

            Map<String, Object> variables = variablesEnviadas();
            assertThat(variables).containsOnlyKeys("HELP_URL", "PRIVACY_URL", "TERMS_URL",
                    "FULL_NAME");
        }
    }

    @Nested
    @DisplayName("escapado — el hallazgo crítico de la auditoría, hasta el fondo")
    class EscapadoProfundo {

        private void dadoElCorreoHabilitado() {
            when(email.isEnabled()).thenReturn(true);
            when(email.sendTemplate(anyString(), isNull(), anyString(), anyString(), anyMap()))
                    .thenReturn(CompletableFuture.completedFuture(EmailDispatchOutcome.ACCEPTED));
        }

        @Test
        @DisplayName("un bloque <style> del motivo llega neutralizado: con estilos se oculta el enlace real")
        void un_bloque_style_llega_neutralizado() {
            dadoElCorreoHabilitado();
            String motivoConEstilos = "<style>a[href*='vetrina']{display:none}</style>"
                    + "Necesito acceso para operar";

            crearRemitente().sendAccessRequested(solicitud("Ana Ramirez", motivoConEstilos));

            String reason = (String) variablesEnviadas().get("REASON");
            // Un <style> no necesita enlace propio para hacer dano: basta con ocultar
            // el legitimo y dejar visible el suyo. Por eso no vale filtrar solo <a>.
            assertThat(reason).doesNotContain("<").doesNotContain(">").contains("&lt;style&gt;")
                    .contains("&lt;/style&gt;");
        }

        @Test
        @DisplayName("comillas, apostrofos y ampersands salen escapados y sin doble escapado")
        void comillas_apostrofos_y_ampersands_salen_escapados() {
            dadoElCorreoHabilitado();

            crearRemitente().sendAccessRequested(solicitud("Ana \"La Jefa\" O'Neill & Cia",
                    "Motivo con \" y ' y & suficientes caracteres"));

            Map<String, Object> variables = variablesEnviadas();
            // Las comillas son lo que permite salir de un atributo HTML sin usar «<».
            assertThat((String) variables.get("FULL_NAME")).contains("&quot;").contains("&#39;")
                    .contains("&amp;").doesNotContain("&amp;quot;").doesNotContain("&amp;#39;");
            assertThat((String) variables.get("REASON")).contains("&quot;").contains("&#39;")
                    .contains("&amp;");
        }

        @Test
        @DisplayName("el motivo con saltos de linea de control no puede abrir ninguna etiqueta")
        void el_motivo_con_saltos_de_linea_no_abre_etiquetas() {
            dadoElCorreoHabilitado();
            String motivoConControles = "Primera linea\r\n<a href=\"https://atacante.invalid\">"
                    + "Aprobar acceso</a> Segunda linea";

            crearRemitente().sendAccessRequested(solicitud("Ana Ramirez", motivoConControles));

            String reason = (String) variablesEnviadas().get("REASON");
            assertThat(reason).doesNotContain("<a href").doesNotContain("</a>").doesNotContain("<")
                    .doesNotContain(">")
                    .contains("&lt;a href=&quot;https://atacante.invalid&quot;&gt;");
            // Los caracteres de control SI sobreviven, y hoy son inertes: en HTML son
            // espacio en blanco y no pueden abrir una etiqueta porque «<» ya viaja
            // escapado. El dominio los rechaza en fullName y NO en reason; la
            // asimetria esta registrada como incidencia y este caso la fija: si
            // alguien la cierra, cae aqui y hay que actualizarlo a proposito.
            assertThat(reason).contains("\r\n").contains(" ");
        }

        @Test
        @DisplayName("el correo de invitacion tambien escapa el nombre, no solo el del aprobador")
        void la_invitacion_tambien_escapa_el_nombre() {
            dadoElCorreoHabilitado();

            crearRemitente().sendInvitation(4271L, "ana@vetrina.co", "<img src=x onerror=alert(1)>",
                    "token-plano");

            assertThat((String) variablesEnviadas().get("FULL_NAME")).doesNotContain("<")
                    .doesNotContain(">").contains("&lt;img");
        }

        @Test
        @DisplayName("el correo de bienvenida tambien escapa el nombre")
        void la_bienvenida_tambien_escapa_el_nombre() {
            dadoElCorreoHabilitado();

            crearRemitente().sendWelcome(4271L, "ana@vetrina.co", "<b>Ana</b>", "SYS-ANA");

            assertThat((String) variablesEnviadas().get("FULL_NAME")).doesNotContain("<")
                    .contains("&lt;b&gt;");
        }

        @Test
        @DisplayName("el aviso de rechazo tambien escapa el nombre")
        void el_rechazo_tambien_escapa_el_nombre() {
            dadoElCorreoHabilitado();

            crearRemitente().sendRejection(4271L, "ana@vetrina.co", "<b>Ana</b>");

            assertThat((String) variablesEnviadas().get("FULL_NAME")).doesNotContain("<")
                    .contains("&lt;b&gt;");
        }

        @Test
        @DisplayName("un nombre nulo se convierte en cadena vacia, nunca en el texto \"null\"")
        void un_nombre_nulo_se_convierte_en_cadena_vacia() {
            dadoElCorreoHabilitado();

            crearRemitente().sendRejection(4271L, "ana@vetrina.co", null);

            assertThat(variablesEnviadas().get("FULL_NAME")).isEqualTo("");
        }
    }

    @Nested
    @DisplayName("configuracion incompleta — el arranque, no el silencio")
    class ConfiguracionIncompleta {

        private ResendPlatformAccessEmailSender conPlantillaDeBienvenidaVacia() {
            return new ResendPlatformAccessEmailSender(email, audit, metrics,
                    "aprobador@vetrina.co", "https://consola.vetrina.co/aprobar-acceso",
                    "https://consola.vetrina.co/aceptar-invitacion",
                    "https://consola.vetrina.co/login", "tpl-solicitud", "tpl-aprobada",
                    "tpl-rechazada", "", "https://vetrina.co/ayuda",
                    "https://vetrina.co/privacidad", "https://vetrina.co/terminos");
        }

        private ResendPlatformAccessEmailSender sinLoginUrl() {
            return new ResendPlatformAccessEmailSender(email, audit, metrics,
                    "aprobador@vetrina.co", "https://consola.vetrina.co/aprobar-acceso",
                    "https://consola.vetrina.co/aceptar-invitacion", "  ", "tpl-solicitud",
                    "tpl-aprobada", "tpl-rechazada", "tpl-bienvenida", "https://vetrina.co/ayuda",
                    "https://vetrina.co/privacidad", "https://vetrina.co/terminos");
        }

        @Test
        @DisplayName("con el correo habilitado, una plantilla vacia TUMBA el arranque")
        void una_plantilla_vacia_tumba_el_arranque() {
            when(email.isEnabled()).thenReturn(true);

            // Antes arrancaba: el endpoint respondia 202, sendTemplate escribia un
            // warning y retornaba, y el aprobador no recibia nada. La solicitud moria
            // sin rastro y el solicitante no podia saberlo, porque el 202 es identico
            // por diseno anti-enumeracion.
            assertThatThrownBy(this::conPlantillaDeBienvenidaVacia)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("welcome-template-id");
        }

        @Test
        @DisplayName("login-url tambien es obligatoria: sin ella la cuenta nace inaccesible")
        void login_url_tambien_es_obligatoria() {
            when(email.isEnabled()).thenReturn(true);

            // Es el unico canal por el que el superadministrador recien creado conoce
            // su `code`, que es su usuario de login.
            assertThatThrownBy(this::sinLoginUrl).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("login-url");
        }

        @Test
        @DisplayName("con el correo deshabilitado no exige nada: es el modo de las rodajas y del contrato OpenAPI")
        void con_el_correo_deshabilitado_no_exige_nada() {
            when(email.isEnabled()).thenReturn(false);

            // application-openapi.yml declara vetsoftware.email.enabled=false, asi que
            // OpenApiContractIT y el perfil local siguen levantando sin una sola clave.
            assertThatCode(this::conPlantillaDeBienvenidaVacia).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("bienvenida — su desenlace tambien se vigila")
    class BienvenidaVigilada {

        @Test
        @DisplayName("un envio fallido deja el evento de auditoria: la cuenta existe y nadie puede entrar")
        void un_envio_fallido_deja_evento_de_auditoria() {
            when(email.isEnabled()).thenReturn(true);
            when(email.sendTemplate(anyString(), isNull(), anyString(), anyString(), anyMap()))
                    .thenReturn(CompletableFuture.completedFuture(EmailDispatchOutcome.FAILED));

            crearRemitente().sendWelcome(4271L, "ana@vetrina.co", "Ana Ramirez", "SYS-ANARAMIREZ");

            // Ignorar el futuro era el defecto: el login de las cuentas de sistema es
            // por `code`, no por correo, asi que perder este mensaje deja una cuenta
            // con control total en la que su dueno no puede entrar.
            verify(audit).welcomeUndelivered(4271L, "vetrina.co");
        }

        @Test
        @DisplayName("un envio aceptado no emite nada: el alta ya tiene su propio evento")
        void un_envio_aceptado_no_emite_nada() {
            when(email.isEnabled()).thenReturn(true);
            when(email.sendTemplate(anyString(), isNull(), anyString(), anyString(), anyMap()))
                    .thenReturn(CompletableFuture.completedFuture(EmailDispatchOutcome.ACCEPTED));

            crearRemitente().sendWelcome(4271L, "ana@vetrina.co", "Ana Ramirez", "SYS-ANARAMIREZ");

            verifyNoInteractions(audit);
        }

        @Test
        @DisplayName("no toca el contador de invitaciones: contaria un correo distinto en su serie")
        void no_toca_el_contador_de_invitaciones() {
            when(email.isEnabled()).thenReturn(true);
            when(email.sendTemplate(anyString(), isNull(), anyString(), anyString(), anyMap()))
                    .thenReturn(CompletableFuture.completedFuture(EmailDispatchOutcome.FAILED));

            crearRemitente().sendWelcome(4271L, "ana@vetrina.co", "Ana Ramirez", "SYS-ANARAMIREZ");

            // "invitaciones enviadas" dejaria de significar lo que dice sin que nada
            // lo delatara.
            verifyNoInteractions(metrics);
        }
    }
}
