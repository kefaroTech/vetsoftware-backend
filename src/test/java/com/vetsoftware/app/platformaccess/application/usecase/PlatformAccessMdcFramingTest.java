package com.vetsoftware.app.platformaccess.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.infrastructure.audit.AuditLogger;
import com.vetsoftware.app.infrastructure.logging.MdcKeys;
import com.vetsoftware.app.platformaccess.application.command.AcceptPlatformInvitationCommand;
import com.vetsoftware.app.platformaccess.application.command.RequestPlatformAccessCommand;
import com.vetsoftware.app.platformaccess.application.command.ResolvePlatformAccessCommand;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessEmailSender;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessInvitationRepository;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessMetrics;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessRequestRepository;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessSwitchPort;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformSystemUserProvisioningPort;
import com.vetsoftware.app.platformaccess.application.port.out.SecretHasherPort;
import com.vetsoftware.app.platformaccess.domain.InvalidApprovalTokenException;
import com.vetsoftware.app.platformaccess.domain.InvalidInvitationTokenException;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessDecision;
import com.vetsoftware.app.platformaccess.infrastructure.audit.PlatformAccessAuditAdapter;
import com.vetsoftware.app.platformaccess.testsupport.PlatformAccessMother;
import io.micrometer.observation.ObservationRegistry;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
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
 * <b>La incidencia #533 se cerró afirmando que los seis casos de uso enmarcan
 * el MDC con {@code bindRequest}/{@code finally unbindRequest}. Esto lo
 * comprueba de verdad, y para los seis.</b>
 *
 * <p>
 * Lo que había hasta ahora era un solo caso sobre el camino feliz de aprobar, y
 * además contra un <b>doble</b> del puerto de auditoría: un mock verifica que
 * se llamó al método, no que la clave saliera del {@code ThreadLocal}. Por eso
 * aquí el puerto es el {@link PlatformAccessAuditAdapter} real —el único que
 * toca {@code MDC}— y la aserción es sobre {@code MDC.get(...)}, no sobre
 * interacciones.
 *
 * <p>
 * <b>Por qué importa el camino de excepción.</b> Estos seis casos de uso son
 * públicos y sin JWT; los sirve un hilo de un pool que se reutiliza. Un
 * {@code MDC.put} cuyo {@code remove} quede fuera de un {@code finally}
 * sobrevive al fallo y etiqueta la petición del <b>siguiente</b> usuario con el
 * {@code system.user.request.id} de una solicitud ajena. El resultado no es un
 * error visible: es una investigación de seguridad que apunta a la persona
 * equivocada, con los quince eventos del flujo correlacionados por esa misma
 * clave.
 *
 * <p>
 * Cada caso comprueba las dos mitades: que la clave <b>está puesta mientras el
 * caso de uso trabaja</b> —capturada desde un colaborador invocado dentro del
 * {@code try}— y que <b>no queda nada</b> al salir, termine bien o mal. Sin la
 * primera mitad, un servicio que nunca atara nada pasaría el test.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("El MDC del id de solicitud se ata y se desata en los seis casos de uso (#533)")
class PlatformAccessMdcFramingTest {

    private static final String CLAVE = MdcKeys.SYSTEM_USER_REQUEST_ID;
    private static final String ID_ESPERADO = String.valueOf(PlatformAccessMother.ID_SOLICITUD);

    @Mock
    private PlatformAccessRequestRepository requestRepository;
    @Mock
    private PlatformAccessInvitationRepository invitationRepository;
    @Mock
    private PlatformAccessSwitchPort accessSwitch;
    @Mock
    private PlatformSystemUserProvisioningPort provisioning;
    @Mock
    private SecretHasherPort secretHasher;
    @Mock
    private PlatformAccessEmailSender emailSender;
    @Mock
    private PlatformAccessMetrics metrics;

    /** El adaptador real: es el único sitio del proyecto que escribe este MDC. */
    private final PlatformAccessAuditAdapter audit = new PlatformAccessAuditAdapter(
            new AuditLogger(), ObservationRegistry.NOOP);

    /** Lo que el MDC valía mientras el caso de uso estaba a mitad de camino. */
    private final AtomicReference<String> enVuelo = new AtomicReference<>();

    @BeforeEach
    void limpiarElMdcAntes() {
        MDC.remove(CLAVE);
    }

    @AfterEach
    void limpiarElMdcDespues() {
        MDC.remove(CLAVE);
    }

    private void anotarElMdcEnVuelo() {
        enVuelo.set(MDC.get(CLAVE));
    }

    private static ResolvePlatformAccessCommand resolver() {
        return new ResolvePlatformAccessCommand(PlatformAccessMother.TOKEN_PLANO,
                PlatformAccessMother.CODIGO);
    }

    private void dadoQueElTokenDeAprobacionResuelve(
            com.vetsoftware.app.platformaccess.domain.PlatformAccessRequest solicitud) {
        when(requestRepository.findByApprovalTokenHash(anyString()))
                .thenReturn(Optional.of(solicitud));
    }

    @Nested
    @DisplayName("1. POST /platform/access-request")
    class Solicitar {

        private RequestPlatformAccessService crearServicio() {
            return new RequestPlatformAccessService(accessSwitch, requestRepository, secretHasher,
                    emailSender, audit, metrics, PlatformAccessMother.RELOJ, 72L, 5);
        }

        private void dadoElFormularioAbierto() {
            when(accessSwitch.isOpen()).thenReturn(true);
            when(requestRepository.findLivePendingByEmail(anyString(), any()))
                    .thenReturn(Optional.empty());
            // Sin este stub el hash del codigo saldria null y el constructor del
            // dominio reventaria antes de llegar al MDC, que es lo que se mide.
            when(secretHasher.hash(anyString())).thenReturn(PlatformAccessMother.HASH_CODIGO);
            when(requestRepository.save(any()))
                    .thenReturn(PlatformAccessMother.solicitudPendiente());
        }

        @Test
        @DisplayName("ata la clave mientras trabaja y la desata al terminar bien")
        void ata_y_desata_en_el_camino_feliz() {
            dadoElFormularioAbierto();
            doAnswer(invocacion -> {
                anotarElMdcEnVuelo();
                return null;
            }).when(emailSender).sendAccessRequested(any());

            crearServicio().execute(new RequestPlatformAccessCommand(PlatformAccessMother.NOMBRE,
                    PlatformAccessMother.CORREO, PlatformAccessMother.MOTIVO));

            assertThat(enVuelo.get()).isEqualTo(ID_ESPERADO);
            assertThat(MDC.get(CLAVE)).isNull();
        }

        @Test
        @DisplayName("la desata aunque el aviso al aprobador reviente en pleno vuelo")
        void la_desata_aunque_el_aviso_reviente() {
            dadoElFormularioAbierto();
            doThrow(new IllegalStateException("Resend caido")).when(emailSender)
                    .sendAccessRequested(any());

            assertThatThrownBy(() -> crearServicio()
                    .execute(new RequestPlatformAccessCommand(PlatformAccessMother.NOMBRE,
                            PlatformAccessMother.CORREO, PlatformAccessMother.MOTIVO)))
                    .isInstanceOf(IllegalStateException.class);

            assertThat(MDC.get(CLAVE)).isNull();
        }

        @Test
        @DisplayName("la desata tambien en la rama del duplicado, que ata su propio id")
        void la_desata_en_la_rama_del_duplicado() {
            when(accessSwitch.isOpen()).thenReturn(true);
            when(requestRepository.findLivePendingByEmail(anyString(), any()))
                    .thenReturn(Optional.of(PlatformAccessMother.solicitudPendiente()));

            crearServicio().execute(new RequestPlatformAccessCommand(PlatformAccessMother.NOMBRE,
                    PlatformAccessMother.CORREO, PlatformAccessMother.MOTIVO));

            assertThat(MDC.get(CLAVE)).isNull();
        }
    }

    @Nested
    @DisplayName("2. GET /platform/access-request/validate")
    class ValidarAprobacion {

        private ValidatePlatformAccessTokenService crearServicio() {
            return new ValidatePlatformAccessTokenService(requestRepository, audit, metrics,
                    PlatformAccessMother.RELOJ);
        }

        @Test
        @DisplayName("no deja la clave puesta tras devolver la solicitud")
        void no_deja_la_clave_tras_devolver() {
            dadoQueElTokenDeAprobacionResuelve(PlatformAccessMother.solicitudPendiente());

            crearServicio().execute(PlatformAccessMother.TOKEN_PLANO);

            assertThat(MDC.get(CLAVE)).isNull();
        }

        @Test
        @DisplayName("ata la clave antes de denunciar el estado y la desata al lanzar")
        void ata_antes_de_denunciar_y_desata_al_lanzar() {
            dadoQueElTokenDeAprobacionResuelve(PlatformAccessMother.solicitudCaducada());
            doAnswer(invocacion -> {
                anotarElMdcEnVuelo();
                return null;
            }).when(metrics).resolved(any());

            assertThatThrownBy(() -> crearServicio().execute(PlatformAccessMother.TOKEN_PLANO))
                    .isInstanceOf(InvalidApprovalTokenException.class);

            // El evento de denegacion sale CON el id atado: sin eso la investigacion
            // no puede unir el token muerto con la solicitud que lo emitio.
            assertThat(enVuelo.get()).isEqualTo(ID_ESPERADO);
            assertThat(MDC.get(CLAVE)).isNull();
        }
    }

    @Nested
    @DisplayName("3. POST /platform/access-request/approve")
    class Aprobar {

        private ApprovePlatformAccessRequestService crearServicio() {
            return new ApprovePlatformAccessRequestService(requestRepository, invitationRepository,
                    secretHasher, emailSender, audit, metrics, PlatformAccessMother.RELOJ, 7L);
        }

        @Test
        @DisplayName("ata la clave mientras emite la invitacion y la desata al terminar")
        void ata_mientras_emite_y_desata() {
            dadoQueElTokenDeAprobacionResuelve(PlatformAccessMother.solicitudPendiente());
            when(secretHasher.matches(anyString(), anyString())).thenReturn(true);
            when(requestRepository.applyDecision(anyLong(), any(), any())).thenReturn(1);
            when(invitationRepository.save(any())).thenAnswer(invocacion -> {
                anotarElMdcEnVuelo();
                return invocacion.getArgument(0);
            });

            crearServicio().execute(resolver());

            assertThat(enVuelo.get()).isEqualTo(ID_ESPERADO);
            assertThat(MDC.get(CLAVE)).isNull();
        }

        @Test
        @DisplayName("la desata cuando pierde la carrera contra la otra pestana")
        void la_desata_cuando_pierde_la_carrera() {
            dadoQueElTokenDeAprobacionResuelve(PlatformAccessMother.solicitudPendiente());
            when(secretHasher.matches(anyString(), anyString())).thenReturn(true);
            when(requestRepository.applyDecision(anyLong(), any(), any())).thenReturn(0);

            assertThatThrownBy(() -> crearServicio().execute(resolver()))
                    .isInstanceOf(InvalidApprovalTokenException.class);

            assertThat(MDC.get(CLAVE)).isNull();
        }
    }

    @Nested
    @DisplayName("4. POST /platform/access-request/reject")
    class Rechazar {

        private RejectPlatformAccessRequestService crearServicio() {
            return new RejectPlatformAccessRequestService(requestRepository, secretHasher,
                    emailSender, audit, metrics, PlatformAccessMother.RELOJ);
        }

        @Test
        @DisplayName("ata la clave mientras avisa del rechazo y la desata al terminar")
        void ata_mientras_avisa_y_desata() {
            dadoQueElTokenDeAprobacionResuelve(PlatformAccessMother.solicitudPendiente());
            when(secretHasher.matches(anyString(), anyString())).thenReturn(true);
            when(requestRepository.applyDecision(anyLong(), any(), any())).thenReturn(1);
            doAnswer(invocacion -> {
                anotarElMdcEnVuelo();
                return null;
            }).when(emailSender).sendRejection(anyLong(), anyString(), anyString());

            crearServicio().execute(resolver());

            assertThat(enVuelo.get()).isEqualTo(ID_ESPERADO);
            assertThat(MDC.get(CLAVE)).isNull();
        }

        @Test
        @DisplayName("la desata cuando el codigo no casa y el caso de uso lanza")
        void la_desata_cuando_el_codigo_no_casa() {
            dadoQueElTokenDeAprobacionResuelve(
                    PlatformAccessMother.solicitudDecidida(PlatformAccessDecision.APPROVED));

            assertThatThrownBy(() -> crearServicio().execute(resolver()))
                    .isInstanceOf(InvalidApprovalTokenException.class);

            assertThat(MDC.get(CLAVE)).isNull();
        }
    }

    @Nested
    @DisplayName("5. GET /platform/invitation/validate")
    class ValidarInvitacion {

        private ValidatePlatformInvitationTokenService crearServicio() {
            return new ValidatePlatformInvitationTokenService(invitationRepository,
                    requestRepository, audit, PlatformAccessMother.RELOJ);
        }

        @Test
        @DisplayName("ata la clave antes de leer la solicitud y la desata al devolver")
        void ata_antes_de_leer_y_desata() {
            when(invitationRepository.findByTokenHash(anyString()))
                    .thenReturn(Optional.of(PlatformAccessMother.invitacionViva()));
            when(requestRepository.findById(anyLong())).thenAnswer(invocacion -> {
                anotarElMdcEnVuelo();
                return Optional.of(PlatformAccessMother.solicitudPendiente());
            });

            crearServicio().execute("token-invitacion");

            assertThat(enVuelo.get()).isEqualTo(ID_ESPERADO);
            assertThat(MDC.get(CLAVE)).isNull();
        }

        @Test
        @DisplayName("la desata cuando la invitacion ya no sirve")
        void la_desata_cuando_la_invitacion_ya_no_sirve() {
            when(invitationRepository.findByTokenHash(anyString()))
                    .thenReturn(Optional.of(PlatformAccessMother.invitacionConsumida()));

            assertThatThrownBy(() -> crearServicio().execute("token-invitacion"))
                    .isInstanceOf(InvalidInvitationTokenException.class);

            assertThat(MDC.get(CLAVE)).isNull();
        }
    }

    @Nested
    @DisplayName("6. POST /platform/invitation/accept")
    class Aceptar {

        private AcceptPlatformInvitationService crearServicio() {
            return new AcceptPlatformInvitationService(invitationRepository, requestRepository,
                    provisioning, secretHasher, emailSender, audit, metrics,
                    PlatformAccessMother.RELOJ);
        }

        @Test
        @DisplayName("ata la clave mientras crea el superadministrador y la desata al terminar")
        void ata_mientras_crea_la_cuenta_y_desata() {
            when(invitationRepository.findByTokenHash(anyString()))
                    .thenReturn(Optional.of(PlatformAccessMother.invitacionViva()));
            when(requestRepository.findById(anyLong()))
                    .thenReturn(Optional.of(PlatformAccessMother.solicitudPendiente()));
            when(provisioning.emailTaken(anyString())).thenReturn(false);
            when(provisioning.codeTaken(anyString())).thenReturn(false);
            when(secretHasher.hash(anyString())).thenReturn("$2a$10$hash");
            when(provisioning.provision(anyString(), anyString(), anyString(), anyString(), any()))
                    .thenAnswer(invocacion -> {
                        anotarElMdcEnVuelo();
                        return 9001L;
                    });
            when(invitationRepository.consume(anyLong(), anyLong(), any())).thenReturn(1);

            crearServicio().execute(
                    new AcceptPlatformInvitationCommand("token-invitacion", "contrasena-larga-1"));

            // El hecho irreversible del flujo tiene que quedar correlacionado con la
            // solicitud que lo autorizo, o la evidencia del alta queda coja.
            assertThat(enVuelo.get()).isEqualTo(ID_ESPERADO);
            assertThat(MDC.get(CLAVE)).isNull();
        }

        @Test
        @DisplayName("la desata cuando el correo ya tiene cuenta y el caso de uso lanza")
        void la_desata_cuando_el_correo_ya_tiene_cuenta() {
            when(invitationRepository.findByTokenHash(anyString()))
                    .thenReturn(Optional.of(PlatformAccessMother.invitacionViva()));
            when(requestRepository.findById(anyLong()))
                    .thenReturn(Optional.of(PlatformAccessMother.solicitudPendiente()));
            when(provisioning.emailTaken(anyString())).thenReturn(true);

            assertThatThrownBy(() -> crearServicio().execute(
                    new AcceptPlatformInvitationCommand("token-invitacion", "contrasena-larga-1")))
                    .isInstanceOf(InvalidInvitationTokenException.class);

            assertThat(MDC.get(CLAVE)).isNull();
        }
    }
}
