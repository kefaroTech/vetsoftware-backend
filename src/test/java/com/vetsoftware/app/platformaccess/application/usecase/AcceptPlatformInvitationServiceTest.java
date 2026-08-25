package com.vetsoftware.app.platformaccess.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.platformaccess.application.command.AcceptPlatformInvitationCommand;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessAuditPort;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessEmailSender;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessInvitationRepository;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessMetrics;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessMetrics.InvitationResult;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessRequestRepository;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformSystemUserProvisioningPort;
import com.vetsoftware.app.platformaccess.application.port.out.SecretHasherPort;
import com.vetsoftware.app.platformaccess.domain.InvalidInvitationTokenException;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessInvitation;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Aceptar es el hecho irreversible del flujo: al terminar existe una cuenta con
 * control total sobre todos los tenants. Los dos tests que más valen aquí son
 * el de que el correo sale del token —y no del cuerpo— y el de que un correo ya
 * tomado produce exactamente el mismo error que un token muerto.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AcceptPlatformInvitationService — consumir la invitación y crear la cuenta")
class AcceptPlatformInvitationServiceTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-03-14T12:00:00Z"),
            ZoneOffset.UTC);
    private static final LocalDateTime AHORA = LocalDateTime.ofInstant(RELOJ.instant(),
            ZoneOffset.UTC);
    private static final String MOTIVO = "Necesito acceso para operar la plataforma";
    private static final String CONTRASENA = "contrasena-larga-y-valida";

    @Mock
    private PlatformAccessInvitationRepository invitationRepository;
    @Mock
    private PlatformAccessRequestRepository requestRepository;
    @Mock
    private PlatformSystemUserProvisioningPort provisioning;
    @Mock
    private SecretHasherPort secretHasher;
    @Mock
    private PlatformAccessEmailSender emailSender;
    @Mock
    private PlatformAccessAuditPort audit;
    @Mock
    private PlatformAccessMetrics metrics;

    private AcceptPlatformInvitationService crearServicio() {
        return new AcceptPlatformInvitationService(invitationRepository, requestRepository,
                provisioning, secretHasher, emailSender, audit, metrics, RELOJ);
    }

    private static PlatformAccessInvitation invitacion(LocalDateTime consumida, Long systemUserId) {
        return new PlatformAccessInvitation(88L, 4271L, "b".repeat(64), AHORA.plusDays(1),
                consumida, systemUserId, AHORA.minusDays(1));
    }

    private static PlatformAccessRequest solicitud() {
        return new PlatformAccessRequest(4271L, "Ana Ramirez", "ana@vetrina.co", MOTIVO,
                "a".repeat(64), "$2a$10$hash-bcrypt", 0, 5, AHORA.plusHours(1), null, null,
                AHORA.minusHours(2), 0L);
    }

    private void dadoUnaInvitacionViva() {
        when(invitationRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(invitacion(null, null)));
        when(requestRepository.findById(4271L)).thenReturn(Optional.of(solicitud()));
    }

    private static AcceptPlatformInvitationCommand comando() {
        return new AcceptPlatformInvitationCommand("token-plano", CONTRASENA);
    }

    @Nested
    @DisplayName("camino feliz")
    class Alta {

        @Test
        @DisplayName("crea la cuenta con el correo y el nombre de la SOLICITUD, no del cuerpo")
        void crea_la_cuenta_con_los_datos_de_la_solicitud() {
            dadoUnaInvitacionViva();
            when(provisioning.emailTaken("ana@vetrina.co")).thenReturn(false);
            when(provisioning.codeTaken(anyString())).thenReturn(false);
            when(secretHasher.hash(CONTRASENA)).thenReturn("$2a$10$hash-de-la-contrasena");
            when(provisioning.provision(anyString(), anyString(), anyString(), anyString(), any()))
                    .thenReturn(777L);
            when(invitationRepository.consume(88L, 777L, AHORA)).thenReturn(1);

            crearServicio().execute(comando());

            // La cadena token_hash -> access_request_id -> email va toda por clave
            // unica: no hay eslabon que el cliente pueda torcer para elegir la
            // identidad del superadministrador que va a nacer.
            verify(provisioning).provision(anyString(), eq("ana@vetrina.co"), eq("Ana Ramirez"),
                    eq("$2a$10$hash-de-la-contrasena"), eq(AHORA));
        }

        @Test
        @DisplayName("genera un codigo de login y lo manda en el correo de bienvenida")
        void genera_un_codigo_y_lo_manda_en_la_bienvenida() {
            dadoUnaInvitacionViva();
            when(provisioning.emailTaken(anyString())).thenReturn(false);
            when(provisioning.codeTaken(anyString())).thenReturn(false);
            when(secretHasher.hash(anyString())).thenReturn("$2a$10$hash");
            when(provisioning.provision(anyString(), anyString(), anyString(), anyString(), any()))
                    .thenReturn(777L);
            when(invitationRepository.consume(anyLong(), anyLong(), any())).thenReturn(1);

            crearServicio().execute(comando());

            ArgumentCaptor<String> codigo = ArgumentCaptor.forClass(String.class);
            verify(emailSender).sendWelcome(eq(4271L), eq("ana@vetrina.co"), eq("Ana Ramirez"),
                    codigo.capture());
            // Sin este correo la cuenta queda creada y su dueno sin saber con que
            // usuario entrar: el login de las cuentas de sistema es por codigo.
            assertThat(codigo.getValue()).startsWith("SYS-").hasSizeLessThanOrEqualTo(50);
        }

        @Test
        @DisplayName("emite un solo evento para el alta y publica los dos contadores")
        void emite_un_solo_evento_para_el_alta() {
            dadoUnaInvitacionViva();
            when(provisioning.emailTaken(anyString())).thenReturn(false);
            when(provisioning.codeTaken(anyString())).thenReturn(false);
            when(secretHasher.hash(anyString())).thenReturn("$2a$10$hash");
            when(provisioning.provision(anyString(), anyString(), anyString(), anyString(), any()))
                    .thenReturn(777L);
            when(invitationRepository.consume(anyLong(), anyLong(), any())).thenReturn(1);

            crearServicio().execute(comando());

            // Aceptar y crear ocurren en la misma transaccion, asi que es un solo
            // hecho: dos eventos duplicarian el conteo del que cuelga la unica alerta.
            verify(audit).systemUserProvisioned(4271L, 777L);
            verify(metrics).invitation(InvitationResult.ACCEPTED);
            verify(metrics).provisioned();
            verify(audit).unbindRequest();
        }
    }

    @Nested
    @DisplayName("caminos que no pueden crear cuenta")
    class SinAlta {

        @Test
        @DisplayName("un correo que ya tiene cuenta sale como token muerto, y NO le cambia la contrasena")
        void un_correo_ya_tomado_sale_como_token_muerto() {
            dadoUnaInvitacionViva();
            when(provisioning.emailTaken("ana@vetrina.co")).thenReturn(true);

            assertThatThrownBy(() -> crearServicio().execute(comando()))
                    .isInstanceOf(InvalidInvitationTokenException.class);

            // Actualizar la contrasena aqui seria un reseteo de contrasena de
            // superadministrador desde un endpoint publico. Y responder algo distinto
            // seria decir que ese correo tiene cuenta de plataforma.
            verify(provisioning, never()).provision(anyString(), anyString(), anyString(),
                    anyString(), any());
            verifyNoInteractions(emailSender);
        }

        @Test
        @DisplayName("una invitacion ya consumida no crea una segunda cuenta")
        void una_invitacion_ya_consumida_no_crea_otra_cuenta() {
            when(invitationRepository.findByTokenHash(anyString()))
                    .thenReturn(Optional.of(invitacion(AHORA.minusHours(1), 777L)));

            assertThatThrownBy(() -> crearServicio().execute(comando()))
                    .isInstanceOf(InvalidInvitationTokenException.class);

            verifyNoInteractions(provisioning, emailSender);
        }

        @Test
        @DisplayName("una invitacion caducada no crea cuenta")
        void una_invitacion_caducada_no_crea_cuenta() {
            when(invitationRepository.findByTokenHash(anyString()))
                    .thenReturn(Optional.of(new PlatformAccessInvitation(88L, 4271L, "b".repeat(64),
                            AHORA.minusMinutes(1), null, null, AHORA.minusDays(8))));

            assertThatThrownBy(() -> crearServicio().execute(comando()))
                    .isInstanceOf(InvalidInvitationTokenException.class);

            verifyNoInteractions(provisioning, emailSender);
        }

        @Test
        @DisplayName("si el consumo pierde la carrera, la transaccion cae y no se manda bienvenida")
        void si_el_consumo_pierde_la_carrera_no_manda_bienvenida() {
            dadoUnaInvitacionViva();
            when(provisioning.emailTaken(anyString())).thenReturn(false);
            when(provisioning.codeTaken(anyString())).thenReturn(false);
            when(secretHasher.hash(anyString())).thenReturn("$2a$10$hash");
            when(provisioning.provision(anyString(), anyString(), anyString(), anyString(), any()))
                    .thenReturn(777L);
            when(invitationRepository.consume(anyLong(), anyLong(), any())).thenReturn(0);

            // El UPDATE condicional es la barrera real; el isUsable de arriba solo
            // evita trabajo. Al lanzar, la transaccion revierte el usuario creado.
            assertThatThrownBy(() -> crearServicio().execute(comando()))
                    .isInstanceOf(InvalidInvitationTokenException.class);

            verifyNoInteractions(emailSender);
            verify(metrics, never()).provisioned();
        }

        @Test
        @DisplayName("una contrasena demasiado corta se rechaza antes de tocar nada")
        void una_contrasena_corta_se_rechaza_antes_de_tocar_nada() {
            assertThatThrownBy(() -> crearServicio()
                    .execute(new AcceptPlatformInvitationCommand("token-plano", "corta")))
                    .isInstanceOf(IllegalArgumentException.class);

            verifyNoInteractions(invitationRepository, provisioning, emailSender);
        }
    }

    @Nested
    @DisplayName("rastro de los rechazos — lo unico que queda de un 404 mudo")
    class RastroDeLosRechazos {

        @Test
        @DisplayName("un token que no existe deja el evento con requestId nulo: no hay solicitud a la que atribuirlo")
        void un_token_inexistente_deja_evento_sin_solicitud() {
            when(invitationRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> crearServicio().execute(comando()))
                    .isInstanceOf(InvalidInvitationTokenException.class);

            verify(audit).invitationDenied("token_invalid", null);
            verify(metrics).invitation(InvitationResult.TOKEN_INVALID);
        }

        @Test
        @DisplayName("una invitacion ya consumida se distingue de una caducada en el log, aunque no en la respuesta")
        void una_invitacion_consumida_se_distingue_de_una_caducada() {
            when(invitationRepository.findByTokenHash(anyString()))
                    .thenReturn(Optional.of(invitacion(AHORA.minusHours(1), 999L)));

            assertThatThrownBy(() -> crearServicio().execute(comando()))
                    .isInstanceOf(InvalidInvitationTokenException.class);

            // Un token de un solo uso reproducido y un enlace viejo salen por el
            // mismo 404 a proposito. Distinguirlos aqui es lo que permite decir
            // despues cual de los dos fue.
            verify(audit).invitationDenied("token_consumed", 4271L);
            verify(metrics).invitation(InvitationResult.TOKEN_CONSUMED);
            verifyNoInteractions(provisioning);
        }

        @Test
        @DisplayName("una invitacion caducada emite token_expired y estrena el contador EXPIRED")
        void una_invitacion_caducada_emite_token_expired() {
            when(invitationRepository.findByTokenHash(anyString()))
                    .thenReturn(Optional.of(new PlatformAccessInvitation(88L, 4271L, "b".repeat(64),
                            AHORA.minusHours(1), null, null, AHORA.minusDays(2))));

            assertThatThrownBy(() -> crearServicio().execute(comando()))
                    .isInstanceOf(InvalidInvitationTokenException.class);

            verify(audit).invitationDenied("token_expired", 4271L);
            verify(metrics).invitation(InvitationResult.EXPIRED);
        }

        @Test
        @DisplayName("una invitacion que apunta a una solicitud ilegible emite token_invalid")
        void una_solicitud_ilegible_emite_token_invalid() {
            when(invitationRepository.findByTokenHash(anyString()))
                    .thenReturn(Optional.of(invitacion(null, null)));
            when(requestRepository.findById(4271L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> crearServicio().execute(comando()))
                    .isInstanceOf(InvalidInvitationTokenException.class);

            verify(audit).invitationDenied("token_invalid", 4271L);
            verify(metrics).invitation(InvitationResult.TOKEN_INVALID);
        }

        @Test
        @DisplayName("un correo que ya tiene superadministrador DEJA RASTRO, que antes era lo unico que faltaba")
        void un_correo_ya_aprovisionado_deja_rastro() {
            dadoUnaInvitacionViva();
            when(provisioning.emailTaken("ana@vetrina.co")).thenReturn(true);

            assertThatThrownBy(() -> crearServicio().execute(comando()))
                    .isInstanceOf(InvalidInvitationTokenException.class);

            // Para provocar esto hay que poseer una invitacion valida. O es un
            // reenvio inocente, o alguien esta intentando hacerse con una identidad
            // que ya existe; sin el evento las dos cosas son indistinguibles y
            // ninguna ocurre en ningun registro del sistema.
            verify(audit).invitationDenied("email_already_provisioned", 4271L);
            verify(metrics).invitation(InvitationResult.EMAIL_ALREADY_PROVISIONED);
            verify(provisioning, never()).provision(anyString(), anyString(), anyString(),
                    anyString(), any());
        }

        @Test
        @DisplayName("el correo ya aprovisionado sigue respondiendo lo mismo que un token muerto")
        void el_correo_ya_aprovisionado_no_cambia_la_respuesta() {
            dadoUnaInvitacionViva();
            when(provisioning.emailTaken("ana@vetrina.co")).thenReturn(true);

            // El rastro va al log, jamas a la respuesta: un mensaje propio aqui si
            // seria un oraculo. El tipo y el trato HTTP son los mismos.
            assertThatThrownBy(() -> crearServicio().execute(comando()))
                    .isInstanceOf(InvalidInvitationTokenException.class);
            verifyNoInteractions(emailSender);
        }

        @Test
        @DisplayName("perder la carrera del consumo concurrente emite token_consumed")
        void perder_la_carrera_del_consumo_emite_token_consumed() {
            dadoUnaInvitacionViva();
            when(provisioning.emailTaken("ana@vetrina.co")).thenReturn(false);
            when(provisioning.codeTaken(anyString())).thenReturn(false);
            when(secretHasher.hash(CONTRASENA)).thenReturn("$2a$10$hash-de-la-contrasena");
            when(provisioning.provision(anyString(), anyString(), anyString(), anyString(), any()))
                    .thenReturn(777L);
            when(invitationRepository.consume(88L, 777L, AHORA)).thenReturn(0);

            assertThatThrownBy(() -> crearServicio().execute(comando()))
                    .isInstanceOf(InvalidInvitationTokenException.class);

            verify(audit).invitationDenied("token_consumed", 4271L);
            verify(metrics).invitation(InvitationResult.TOKEN_CONSUMED);
            verify(audit, never()).systemUserProvisioned(anyLong(), anyLong());
        }
    }
}
