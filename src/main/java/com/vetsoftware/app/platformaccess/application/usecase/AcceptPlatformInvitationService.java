package com.vetsoftware.app.platformaccess.application.usecase;

import com.vetsoftware.app.platformaccess.application.command.AcceptPlatformInvitationCommand;
import com.vetsoftware.app.platformaccess.application.port.in.AcceptPlatformInvitationUseCase;
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
import com.vetsoftware.app.platformaccess.domain.PlatformAccessPasswords;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessRequest;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Consume la invitación y crea el superadministrador. Es el único hecho
 * irreversible del flujo, y por eso todo lo que hay aquí está escrito para que
 * no pueda ocurrir por accidente.
 *
 * <p>
 * <b>El correo sale del token, jamás del cuerpo.</b> La petición es
 * {@code {token, password}} y no lleva correo; si algún día alguien se lo
 * añade, se ignora. Un correo en el cuerpo permitiría a quien posee una
 * invitación legítima para una dirección crear la cuenta de otra, es decir,
 * elegir la identidad del superadministrador que va a nacer.
 *
 * <p>
 * <b>Si ya existe una cuenta con ese correo, sale el mismo error que un token
 * muerto.</b> No se le cambia la contraseña: eso sería un reseteo de contraseña
 * de superadministrador desde un endpoint público. Y no se responde nada
 * distinto: el error de clave duplicada tampoco es un oráculo —para provocarlo
 * hay que poseer una invitación válida, y quien la posee ya conoce el correo,
 * que es el suyo— pero un mensaje propio sí lo sería.
 *
 * <p>
 * <b>Callar hacia fuera obliga a hablar hacia dentro.</b> Los cuatro rechazos
 * posibles —token inexistente, caducado, consumido y correo ya aprovisionado—
 * salen por el mismo 404, así que el evento de auditoría es el único sitio
 * donde el hecho puede existir. Cada uno emite
 * {@code system_user_invitation_denied} con su {@code reason} y su contador;
 * sin eso, presentar una invitación válida contra una identidad ya existente no
 * dejaba rastro en ninguna parte.
 *
 * <p>
 * <b>Responde 204 sin cuerpo y no emite sesión.</b> Ni JWT, ni refresh, ni
 * cookie: el usuario entra después por el login normal.
 *
 * <p>
 * <b>Un solo evento para un solo hecho.</b> Aceptar y crear la cuenta ocurren
 * en la misma transacción, así que se emite únicamente
 * {@code system_user_provisioned}; dos eventos para el mismo instante
 * duplicarían el conteo del que cuelga la única alerta del flujo.
 */
@Observed(name = "system.user.invitation.accept")
@Service
public class AcceptPlatformInvitationService implements AcceptPlatformInvitationUseCase {

    private static final Logger log = LoggerFactory
            .getLogger(AcceptPlatformInvitationService.class);

    private final PlatformAccessInvitationRepository invitationRepository;
    private final PlatformAccessRequestRepository requestRepository;
    private final PlatformSystemUserProvisioningPort provisioning;
    private final SecretHasherPort secretHasher;
    private final PlatformAccessEmailSender emailSender;
    private final PlatformAccessAuditPort audit;
    private final PlatformAccessMetrics metrics;
    private final Clock clock;

    public AcceptPlatformInvitationService(PlatformAccessInvitationRepository invitationRepository,
            PlatformAccessRequestRepository requestRepository,
            PlatformSystemUserProvisioningPort provisioning, SecretHasherPort secretHasher,
            PlatformAccessEmailSender emailSender, PlatformAccessAuditPort audit,
            PlatformAccessMetrics metrics, Clock clock) {
        this.invitationRepository = invitationRepository;
        this.requestRepository = requestRepository;
        this.provisioning = provisioning;
        this.secretHasher = secretHasher;
        this.emailSender = emailSender;
        this.audit = audit;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void execute(AcceptPlatformInvitationCommand command) {
        PlatformAccessPasswords.require(command.password());

        PlatformAccessInvitation invitation = locateOrDeny(command.token());
        LocalDateTime now = LocalDateTime.now(clock);

        audit.bindRequest(invitation.getAccessRequestId());
        try {
            if (!invitation.isUsable(now)) {
                // Consumida y caducada salen por el mismo 404, pero son hechos
                // distintos: la primera es un token de un solo uso reproducido y la
                // segunda un enlace viejo. Distinguirlas cuesta una llamada y es lo
                // que permite responder despues «esto fue un ataque» o «esto fue un
                // usuario lento».
                throw denyInvitation(invitation.isConsumed() ? "token_consumed" : "token_expired",
                        invitation.getAccessRequestId(),
                        invitation.isConsumed()
                                ? InvitationResult.TOKEN_CONSUMED
                                : InvitationResult.EXPIRED,
                        "Invitation is no longer usable");
            }
            PlatformAccessRequest request = requestRepository
                    .findById(invitation.getAccessRequestId())
                    .orElseThrow(() -> denyInvitation("token_invalid",
                            invitation.getAccessRequestId(), InvitationResult.TOKEN_INVALID,
                            "Invitation has no readable access request"));

            if (provisioning.emailTaken(request.getEmail())) {
                // El unico rastro que va a quedar de este intento. La respuesta es
                // deliberadamente indistinguible de un token muerto, asi que sin este
                // evento el hecho no ocurre en ningun registro del sistema —y para
                // provocarlo hay que poseer una invitacion valida—.
                throw denyInvitation("email_already_provisioned", request.getId(),
                        InvitationResult.EMAIL_ALREADY_PROVISIONED,
                        "There is already a system user for this email");
            }

            String code = PlatformSystemUserCodes.generateAvailable(request.getFullName(),
                    provisioning::codeTaken);
            Long systemUserId = provisioning.provision(code, request.getEmail(),
                    request.getFullName(), secretHasher.hash(command.password()), now);

            // El UPDATE condicional es la barrera real contra el doble consumo; el
            // isUsable de arriba solo evita trabajo. rowcount = 0 significa que otra
            // petición consumió la invitación entre la lectura y esta línea.
            if (invitationRepository.consume(invitation.getId(), systemUserId, now) == 0) {
                throw denyInvitation("token_consumed", request.getId(),
                        InvitationResult.TOKEN_CONSUMED, "Invitation was already consumed");
            }

            audit.systemUserProvisioned(request.getId(), systemUserId);
            metrics.invitation(InvitationResult.ACCEPTED);
            metrics.provisioned();
            sendAfterCommit(request.getId(), request.getEmail(), request.getFullName(), code);
        } finally {
            audit.unbindRequest();
        }
    }

    /**
     * Localiza la invitación y deja rastro si no existe. El token vacío y el
     * inexistente siguen siendo el mismo 404 y la misma excepción; lo que cambia es
     * que ahora se cuentan.
     */
    private PlatformAccessInvitation locateOrDeny(String rawToken) {
        try {
            return PlatformInvitations.locate(invitationRepository, rawToken);
        } catch (InvalidInvitationTokenException exception) {
            // requestId null: sin invitación no hay solicitud a la que atribuirlo,
            // igual que en el lado del aprobador.
            audit.invitationDenied("token_invalid", null);
            metrics.invitation(InvitationResult.TOKEN_INVALID);
            throw exception;
        }
    }

    /**
     * Registra el rechazo y devuelve la excepción para que el llamador la lance.
     * Devolverla en vez de lanzarla desde aquí es lo que deja el {@code throw}
     * escrito en la rama que lo decide, donde una revisión lo ve, y lo que permite
     * usarla también dentro de un {@code orElseThrow}.
     */
    private InvalidInvitationTokenException denyInvitation(String reason, Long requestId,
            InvitationResult result, String message) {
        audit.invitationDenied(reason, requestId);
        metrics.invitation(result);
        return new InvalidInvitationTokenException(message);
    }

    /**
     * Difiere la bienvenida al commit. Clase anónima y no lambda: la regla que
     * vigila este patrón atribuye el lambda al método declarante y da un falso
     * positivo.
     *
     * <p>
     * Este correo es el único sitio donde el nuevo superadministrador se entera de
     * su código de usuario, que es con lo que se inicia sesión. Si se pierde, la
     * cuenta existe y su dueño no puede entrar: por eso el envío se registra y por
     * eso la solicitud tiene que poder reemitirse.
     */
    private void sendAfterCommit(Long requestId, String toEmail, String fullName,
            String systemUserCode) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            emailSender.sendWelcome(requestId, toEmail, fullName, systemUserCode);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    emailSender.sendWelcome(requestId, toEmail, fullName, systemUserCode);
                } catch (RuntimeException exception) {
                    log.warn("No se pudo encolar la bienvenida del superadministrador nuevo",
                            exception);
                }
            }
        });
    }
}
