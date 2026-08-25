package com.vetsoftware.app.platformaccess.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.platformaccess.application.command.RequestPlatformAccessCommand;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessAuditPort;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessEmailSender;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessEmailSender.AccessRequestedNotification;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessMetrics;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessMetrics.RequestResult;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessRequestRepository;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessSwitchPort;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformSystemUserProvisioningPort;
import com.vetsoftware.app.platformaccess.application.port.out.SecretHasherPort;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessClosedException;
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
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Todo lo que este servicio decide está subordinado a no crear un oráculo de
 * enumeración de cuentas de plataforma. Los tres tests que más valen aquí son
 * el del orden del interruptor, el del duplicado silencioso y el de que el hash
 * del código —no el código— es lo que se persiste.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RequestPlatformAccessService — solicitud pública de acceso")
class RequestPlatformAccessServiceTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-03-14T12:00:00Z"),
            ZoneOffset.UTC);
    private static final LocalDateTime AHORA = LocalDateTime.ofInstant(RELOJ.instant(),
            ZoneOffset.UTC);
    private static final String MOTIVO = "Necesito acceso para operar la plataforma";

    @Mock
    private PlatformAccessSwitchPort accessSwitch;
    @Mock
    private PlatformAccessRequestRepository requestRepository;
    @Mock
    private SecretHasherPort secretHasher;
    @Mock
    private PlatformAccessEmailSender emailSender;
    @Mock
    private PlatformAccessAuditPort audit;
    @Mock
    private PlatformAccessMetrics metrics;

    private RequestPlatformAccessService crearServicio() {
        return new RequestPlatformAccessService(accessSwitch, requestRepository, secretHasher,
                emailSender, audit, metrics, RELOJ, 72L, 5);
    }

    private static RequestPlatformAccessCommand comando() {
        return new RequestPlatformAccessCommand("Ana Ramirez", "ana@vetrina.co", MOTIVO);
    }

    private void dadoElFormularioAbiertoYSinDuplicado() {
        when(accessSwitch.isOpen()).thenReturn(true);
        when(requestRepository.findLivePendingByEmail(anyString(), any()))
                .thenReturn(Optional.empty());
    }

    private PlatformAccessRequest capturarLaGuardada() {
        ArgumentCaptor<PlatformAccessRequest> captor = ArgumentCaptor
                .forClass(PlatformAccessRequest.class);
        verify(requestRepository).save(captor.capture());
        return captor.getValue();
    }

    @Nested
    @DisplayName("formulario cerrado")
    class FormularioCerrado {

        @Test
        @DisplayName("no mira el correo del solicitante: la latencia seria el oraculo")
        void no_mira_el_correo_del_solicitante() {
            when(accessSwitch.isOpen()).thenReturn(false);

            assertThatThrownBy(() -> crearServicio().execute(comando()))
                    .isInstanceOf(PlatformAccessClosedException.class);

            // Si una rama consultara la base y la otra no, la diferencia de tiempo de
            // respuesta distinguiria los dos casos y devolveria el oraculo que el 202
            // se molesta en evitar.
            verifyNoInteractions(requestRepository, secretHasher, emailSender);
            verify(audit).accessRequestDenied("form_closed", null, null);
            verify(metrics).requested(RequestResult.FORM_CLOSED);
        }
    }

    @Nested
    @DisplayName("solicitud nueva")
    class SolicitudNueva {

        @Test
        @DisplayName("persiste el HASH del token y el HASH del codigo, nunca los valores planos")
        void persiste_los_hashes_y_no_los_valores_planos() {
            dadoElFormularioAbiertoYSinDuplicado();
            when(secretHasher.hash(anyString())).thenReturn("$2a$10$hash-bcrypt");
            when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            crearServicio().execute(comando());

            PlatformAccessRequest guardada = capturarLaGuardada();
            // 64 caracteres hex es SHA-256: un volcado de la tabla no entrega tokens
            // usables.
            assertThat(guardada.getApprovalTokenHash()).hasSize(64).matches("[0-9a-f]{64}");
            // El codigo va con bcrypt y no con SHA-256: seis digitos son 10^6
            // combinaciones y con SHA-256 recorrerlas es cuestion de milisegundos.
            assertThat(guardada.getVerificationCodeHash()).isEqualTo("$2a$10$hash-bcrypt");
        }

        @Test
        @DisplayName("congela la politica de intentos y la caducidad en la propia fila")
        void congela_la_politica_en_la_fila() {
            dadoElFormularioAbiertoYSinDuplicado();
            when(secretHasher.hash(anyString())).thenReturn("$2a$10$hash-bcrypt");
            when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            crearServicio().execute(comando());

            PlatformAccessRequest guardada = capturarLaGuardada();
            // El limite con el que se emitio una credencial es una propiedad de esa
            // credencial: si manana la politica baja de 5 a 3, esta sigue con 5.
            assertThat(guardada.getMaxAttempts()).isEqualTo(5);
            assertThat(guardada.getVerificationAttempts()).isZero();
            assertThat(guardada.getCreatedDate()).isEqualTo(AHORA);
            assertThat(guardada.getExpiresAt()).isEqualTo(AHORA.plusHours(72));
        }

        @Test
        @DisplayName("normaliza el correo a minusculas y recorta los espacios")
        void normaliza_el_correo() {
            dadoElFormularioAbiertoYSinDuplicado();
            when(secretHasher.hash(anyString())).thenReturn("$2a$10$hash-bcrypt");
            when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            crearServicio().execute(new RequestPlatformAccessCommand("  Ana Ramirez  ",
                    "  ANA@Vetrina.CO ", "  " + MOTIVO + "  "));

            assertThat(capturarLaGuardada().getEmail()).isEqualTo("ana@vetrina.co");
        }

        @Test
        @DisplayName("el aviso al aprobador lleva el token y el codigo planos, resueltos en la transaccion")
        void el_aviso_lleva_el_token_y_el_codigo_planos() {
            dadoElFormularioAbiertoYSinDuplicado();
            when(secretHasher.hash(anyString())).thenReturn("$2a$10$hash-bcrypt");
            when(requestRepository.save(any())).thenAnswer(inv -> {
                PlatformAccessRequest sinId = inv.getArgument(0);
                return new PlatformAccessRequest(4271L, sinId.getFullName(), sinId.getEmail(),
                        sinId.getReason(), sinId.getApprovalTokenHash(),
                        sinId.getVerificationCodeHash(), 0, sinId.getMaxAttempts(),
                        sinId.getExpiresAt(), null, null, sinId.getCreatedDate(), 0L);
            });

            crearServicio().execute(comando());

            ArgumentCaptor<AccessRequestedNotification> captor = ArgumentCaptor
                    .forClass(AccessRequestedNotification.class);
            verify(emailSender).sendAccessRequested(captor.capture());
            AccessRequestedNotification aviso = captor.getValue();
            assertThat(aviso.requestId()).isEqualTo(4271L);
            assertThat(aviso.rawApprovalToken()).isNotBlank();
            assertThat(aviso.verificationCode()).matches("\\d{6}");
            // El payload viaja entero: el callback no vuelve a leer nada despues del
            // commit, cuando la conexion ya volvio al pool.
            assertThat(aviso.requesterEmail()).isEqualTo("ana@vetrina.co");
            assertThat(aviso.requestedAt()).isEqualTo(AHORA);
        }

        @Test
        @DisplayName("emite el evento y el contador con el dominio del correo, nunca con la direccion")
        void emite_el_evento_con_el_dominio() {
            dadoElFormularioAbiertoYSinDuplicado();
            when(secretHasher.hash(anyString())).thenReturn("$2a$10$hash-bcrypt");
            when(requestRepository.save(any())).thenAnswer(inv -> {
                PlatformAccessRequest sinId = inv.getArgument(0);
                return new PlatformAccessRequest(4271L, sinId.getFullName(), sinId.getEmail(),
                        sinId.getReason(), sinId.getApprovalTokenHash(),
                        sinId.getVerificationCodeHash(), 0, sinId.getMaxAttempts(),
                        sinId.getExpiresAt(), null, null, sinId.getCreatedDate(), 0L);
            });

            crearServicio().execute(comando());

            verify(audit).accessRequested(4271L, "vetrina.co");
            verify(metrics).requested(RequestResult.SUCCESS);
            verify(audit).bindRequest(4271L);
            verify(audit).unbindRequest();
        }
    }

    @Nested
    @DisplayName("solicitud duplicada")
    class SolicitudDuplicada {

        @Test
        @DisplayName("no crea otra fila, no manda nada y termina sin error: el cliente ve el mismo 202")
        void no_crea_otra_fila_y_termina_sin_error() {
            when(accessSwitch.isOpen()).thenReturn(true);
            PlatformAccessRequest viva = new PlatformAccessRequest(4271L, "Ana Ramirez",
                    "ana@vetrina.co", MOTIVO, "a".repeat(64), "$2a$10$hash-bcrypt", 0, 5,
                    AHORA.plusHours(1), null, null, AHORA.minusHours(1), 0L);
            when(requestRepository.findLivePendingByEmail(eq("ana@vetrina.co"), any()))
                    .thenReturn(Optional.of(viva));

            crearServicio().execute(comando());

            // No se reenvia nada: el token plano de la solicitud viva ya no existe
            // —solo su hash— y rotarlo dejaria muerto el enlace que el aprobador
            // tiene en su buzon, que es exactamente lo que un tercero querria.
            verify(requestRepository, never()).save(any());
            verifyNoInteractions(emailSender);
            verify(audit).accessRequestDenied("duplicate_request", 4271L, "vetrina.co");
            verify(metrics).requested(RequestResult.DUPLICATE_IGNORED);
        }

        @Test
        @DisplayName("ejecuta el bcrypt igual y tira el resultado: sin eso, el cronometro delata la solicitud viva")
        void ejecuta_el_bcrypt_igual_y_tira_el_resultado() {
            when(accessSwitch.isOpen()).thenReturn(true);
            when(requestRepository.findLivePendingByEmail(anyString(), any()))
                    .thenReturn(Optional.of(new PlatformAccessRequest(4271L, "Ana Ramirez",
                            "ana@vetrina.co", MOTIVO, "a".repeat(64), "$2a$10$hash-bcrypt", 0, 5,
                            AHORA.plusHours(1), null, null, AHORA.minusHours(1), 0L)));

            crearServicio().execute(comando());

            // bcrypt cuesta ~100 ms y la consulta ~1 ms. Con el hash despues del
            // if, la rama duplicada respondia en ~15 ms y la normal en ~120: un
            // anonimo con un cronometro y un correo concreto averiguaba si hay un
            // alta de superadministrador EN CURSO para esa organizacion, que es la
            // ventana en la que atacar el buzon del aprobador tiene sentido. El
            // limite de 3/h no lo impide, porque tres medidas bastan.
            verify(secretHasher).hash(anyString());
            verify(requestRepository, never()).save(any());
        }

        @Test
        @DisplayName("el trabajo caro va ANTES de consultar si hay una solicitud viva, en ese orden")
        void el_trabajo_caro_va_antes_de_consultar_el_duplicado() {
            when(accessSwitch.isOpen()).thenReturn(true);
            when(requestRepository.findLivePendingByEmail(anyString(), any()))
                    .thenReturn(Optional.of(new PlatformAccessRequest(4271L, "Ana Ramirez",
                            "ana@vetrina.co", MOTIVO, "a".repeat(64), "$2a$10$hash-bcrypt", 0, 5,
                            AHORA.plusHours(1), null, null, AHORA.minusHours(1), 0L)));

            crearServicio().execute(comando());

            // El orden ES la invariante. Verificar solo «se llamo al hasher» dejaria
            // pasar un refactor que lo moviera detras del if dentro de la rama
            // normal: las dos ramas volverian a costar cosas distintas.
            InOrder orden = inOrder(accessSwitch, secretHasher, requestRepository);
            orden.verify(accessSwitch).isOpen();
            orden.verify(secretHasher).hash(anyString());
            orden.verify(requestRepository).findLivePendingByEmail(anyString(), any());
        }
    }

    @Nested
    @DisplayName("sin oráculo de enumeración de cuentas")
    class SinOraculoDeEnumeracion {

        @Test
        @DisplayName("el interruptor se consulta ANTES que nada, en ese orden y no en otro")
        void el_interruptor_se_consulta_antes_que_nada() {
            dadoElFormularioAbiertoYSinDuplicado();
            when(secretHasher.hash(anyString())).thenReturn("$2a$10$hash-bcrypt");
            when(requestRepository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

            crearServicio().execute(comando());

            // verifyNoInteractions prueba que la rama cerrada no consulta; esto
            // prueba lo complementario, que es lo que hace que las dos ramas cuesten
            // lo mismo: en la abierta, el interruptor se mira PRIMERO. Si el orden se
            // invirtiera, la rama cerrada seria mas lenta que la abierta y la
            // diferencia de latencia volveria a distinguirlas.
            InOrder orden = inOrder(accessSwitch, requestRepository);
            orden.verify(accessSwitch).isOpen();
            orden.verify(requestRepository).findLivePendingByEmail(anyString(), any());
            orden.verify(requestRepository).save(any());
        }

        @Test
        @DisplayName("el servicio no puede saber si ya hay una cuenta con ese correo: no recibe el puerto")
        void el_servicio_no_puede_consultar_las_cuentas_existentes() {
            // Garantía estructural, no de comportamiento. Comprobar «no llamó al
            // puerto» exigiría que el puerto estuviera ahí; lo que se afirma es más
            // fuerte: no hay ninguna vía. Esa comprobación existe y vive donde tiene
            // sentido —AcceptPlatformInvitationService, ya con un token válido en la
            // mano—, no en un endpoint anónimo.
            assertThat(RequestPlatformAccessService.class.getDeclaredConstructors()[0]
                    .getParameterTypes()).doesNotContain(PlatformSystemUserProvisioningPort.class);
        }

        @Test
        @DisplayName("una solicitud nueva y una duplicada emiten el mismo desenlace hacia fuera: ninguno")
        void la_nueva_y_la_duplicada_no_devuelven_nada_distinto() {
            when(accessSwitch.isOpen()).thenReturn(true);
            when(requestRepository.findLivePendingByEmail(anyString(), any()))
                    .thenReturn(Optional.of(new PlatformAccessRequest(4271L, "Ana Ramirez",
                            "ana@vetrina.co", MOTIVO, "a".repeat(64), "$2a$10$hash-bcrypt", 0, 5,
                            AHORA.plusHours(1), null, null, AHORA.minusHours(1), 0L)));

            // El caso de uso es void y no lanza: el controller responde 202 en los
            // dos casos sin poder diferenciarlos. La unica huella de la diferencia
            // esta en el evento de auditoria, que no sale al cliente.
            assertThatCode(() -> crearServicio().execute(comando())).doesNotThrowAnyException();
        }
    }
}
