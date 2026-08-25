package com.vetsoftware.app.platformaccess.infrastructure.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.infrastructure.audit.AuditLogger;
import com.vetsoftware.app.infrastructure.logging.MdcKeys;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

/**
 * El adaptador que traduce el puerto de auditoría de la feature al canal
 * {@code AUDIT} común, y el <b>único sitio del proyecto que escribe
 * {@code system.user.request.id} en el MDC</b>.
 *
 * <p>
 * Dos cosas se fijan aquí y en ningún otro sitio. La primera: el identificador
 * de correlación se declara en <b>dos</b> planos —MDC para los logs y
 * {@code highCardinalityKeyValue} para la traza—, y olvidar uno no rompe nada
 * visible; deja huecos silenciosos el día que alguien investigue un alta. La
 * segunda: un {@code requestId} nulo no puede escribir la clave, porque un
 * {@code "null"} en el MDC agrupa en Grafana como si fuera un valor real.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PlatformAccessAuditAdapter — correlación y traducción al canal AUDIT")
class PlatformAccessAuditAdapterTest {

    private static final String CLAVE = MdcKeys.SYSTEM_USER_REQUEST_ID;

    @Mock
    private AuditLogger auditLogger;

    @BeforeEach
    void limpiarAntes() {
        MDC.remove(CLAVE);
    }

    @AfterEach
    void limpiarDespues() {
        MDC.remove(CLAVE);
    }

    private PlatformAccessAuditAdapter conRegistroVacio() {
        return new PlatformAccessAuditAdapter(auditLogger, ObservationRegistry.NOOP);
    }

    @Nested
    @DisplayName("correlación")
    class Correlacion {

        @Test
        @DisplayName("bindRequest escribe el id en el MDC como texto")
        void bind_request_escribe_el_id_en_el_mdc() {
            conRegistroVacio().bindRequest(4271L);

            assertThat(MDC.get(CLAVE)).isEqualTo("4271");
        }

        @Test
        @DisplayName("unbindRequest borra la clave: sin esto el hilo del pool la arrastra")
        void unbind_request_borra_la_clave() {
            PlatformAccessAuditAdapter adaptador = conRegistroVacio();
            adaptador.bindRequest(4271L);

            adaptador.unbindRequest();

            assertThat(MDC.get(CLAVE)).isNull();
        }

        @Test
        @DisplayName("un id nulo NO escribe la clave: un \"null\" agruparia en Grafana como un valor")
        void un_id_nulo_no_escribe_la_clave() {
            conRegistroVacio().bindRequest(null);

            assertThat(MDC.get(CLAVE)).isNull();
        }

        @Test
        @DisplayName("unbindRequest sin bind previo no revienta: los casos de uso lo llaman en finally")
        void unbind_sin_bind_no_revienta() {
            assertThatCode(() -> conRegistroVacio().unbindRequest()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("con una observacion en curso, el id tambien viaja a la traza")
        void con_observacion_en_curso_el_id_viaja_a_la_traza(@Mock ObservationRegistry registro,
                @Mock Observation observacion) {
            when(registro.getCurrentObservation()).thenReturn(observacion);

            new PlatformAccessAuditAdapter(auditLogger, registro).bindRequest(4271L);

            // Alta cardinalidad y no baja: un id de solicitud por peticion reventaria
            // el numero de series de la metrica, pero en la traza es justo lo que
            // permite saltar del log al span concreto.
            verify(observacion).highCardinalityKeyValue(CLAVE, "4271");
            assertThat(MDC.get(CLAVE)).isEqualTo("4271");
        }

        @Test
        @DisplayName("sin observacion en curso no falla: los seis endpoints son publicos y pueden llegar sin span")
        void sin_observacion_en_curso_no_falla(@Mock ObservationRegistry registro) {
            when(registro.getCurrentObservation()).thenReturn(null);

            assertThatCode(
                    () -> new PlatformAccessAuditAdapter(auditLogger, registro).bindRequest(4271L))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("bindRequest con id nulo ni siquiera pregunta por la observacion")
        void bind_nulo_no_pregunta_por_la_observacion(@Mock ObservationRegistry registro) {
            new PlatformAccessAuditAdapter(auditLogger, registro).bindRequest(null);

            verifyNoInteractions(registro, auditLogger);
        }
    }

    @Nested
    @DisplayName("traducción de los quince hechos al canal AUDIT")
    class Traduccion {

        @Test
        @DisplayName("la solicitud recibida sale con id y dominio del correo")
        void la_solicitud_recibida_sale_con_id_y_dominio() {
            conRegistroVacio().accessRequested(4271L, "vetrina.co");

            verify(auditLogger).systemUserRequested(4271L, "vetrina.co");
        }

        @Test
        @DisplayName("la solicitud denegada traslada el motivo tal cual: es vocabulario cerrado")
        void la_solicitud_denegada_traslada_el_motivo() {
            conRegistroVacio().accessRequestDenied("form_closed", null, "vetrina.co");

            verify(auditLogger).systemUserRequestDenied("form_closed", null, "vetrina.co");
        }

        @Test
        @DisplayName("la aprobacion denegada traslada motivo e id")
        void la_aprobacion_denegada_traslada_motivo_e_id() {
            conRegistroVacio().approvalDenied("token_expired", 4271L);

            verify(auditLogger).systemUserApprovalDenied("token_expired", 4271L);
        }

        @Test
        @DisplayName("la reproduccion lleva los segundos desde el consumo, que separan el doble clic del ataque")
        void la_reproduccion_lleva_los_segundos() {
            conRegistroVacio().approvalDeniedByReplay(4271L, 86_400L);

            verify(auditLogger).systemUserApprovalReplayed(4271L, 86_400L);
        }

        @Test
        @DisplayName("el codigo incorrecto lleva el margen restante, nunca el codigo")
        void el_codigo_incorrecto_lleva_el_margen() {
            conRegistroVacio().approvalDeniedByCodeMismatch(4271L, 3);

            verify(auditLogger).systemUserApprovalCodeMismatch(4271L, 3);
        }

        @Test
        @DisplayName("el bloqueo por intentos agotados llega a su propio metodo")
        void el_bloqueo_llega_a_su_metodo() {
            conRegistroVacio().approvalLocked(4271L);

            verify(auditLogger).systemUserApprovalLocked(4271L);
        }

        @Test
        @DisplayName("aprobar y rechazar son dos hechos distintos, no uno con bandera")
        void aprobar_y_rechazar_son_dos_hechos() {
            PlatformAccessAuditAdapter adaptador = conRegistroVacio();

            adaptador.requestApproved(4271L);
            adaptador.requestRejected(4272L);

            verify(auditLogger).systemUserRequestApproved(4271L);
            verify(auditLogger).systemUserRequestRejected(4272L);
        }

        @Test
        @DisplayName("la invitacion enviada y la no entregada son hechos separados")
        void la_invitacion_enviada_y_la_perdida_son_hechos_separados() {
            PlatformAccessAuditAdapter adaptador = conRegistroVacio();

            adaptador.invited(4271L, "vetrina.co");
            adaptador.invitationUndelivered(4272L, "vetrina.co");

            verify(auditLogger).systemUserInvited(4271L, "vetrina.co");
            verify(auditLogger).systemUserInvitationUndelivered(4272L, "vetrina.co");
        }

        @Test
        @DisplayName("el alta del superadministrador lleva solicitud y usuario creado")
        void el_alta_lleva_solicitud_y_usuario() {
            conRegistroVacio().systemUserProvisioned(4271L, 9001L);

            // Es el hecho del que cuelga la unica alerta del flujo: sin el
            // systemUserId no se puede demostrar QUE cuenta nacio de QUE solicitud.
            verify(auditLogger).systemUserProvisioned(4271L, 9001L);
        }

        @Test
        @DisplayName("ningun metodo de traduccion toca el MDC: solo bind y unbind lo hacen")
        void ningun_metodo_de_traduccion_toca_el_mdc() {
            PlatformAccessAuditAdapter adaptador = conRegistroVacio();

            adaptador.accessRequested(4271L, "vetrina.co");
            adaptador.approvalLocked(4271L);
            adaptador.systemUserProvisioned(4271L, 9001L);

            assertThat(MDC.get(CLAVE)).isNull();
            verify(auditLogger).systemUserRequested(anyLong(), anyString());
        }
    }

    @Nested
    @DisplayName("los dos eventos que faltaban")
    class LosDosEventosQueFaltaban {

        @Test
        @DisplayName("invitationDenied traduce el motivo tal cual, incluido el requestId nulo")
        void invitation_denied_traduce_el_motivo() {
            PlatformAccessAuditAdapter adaptador = conRegistroVacio();

            adaptador.invitationDenied("email_already_provisioned", 4271L);
            adaptador.invitationDenied("token_invalid", null);

            verify(auditLogger).systemUserInvitationDenied("email_already_provisioned", 4271L);
            verify(auditLogger).systemUserInvitationDenied("token_invalid", null);
        }

        @Test
        @DisplayName("la bienvenida perdida es un hecho distinto de la invitacion perdida")
        void la_bienvenida_perdida_es_un_hecho_distinto() {
            PlatformAccessAuditAdapter adaptador = conRegistroVacio();

            adaptador.welcomeUndelivered(4271L, "vetrina.co");

            // Mezclarlos en un solo evento haria imposible distinguir "la invitacion
            // nunca llego" de "la cuenta existe y su dueno no sabe con que entrar",
            // que se arreglan de formas distintas.
            verify(auditLogger).systemUserWelcomeUndelivered(4271L, "vetrina.co");
            verify(auditLogger, never()).systemUserInvitationUndelivered(anyLong(), anyString());
        }
    }
}
