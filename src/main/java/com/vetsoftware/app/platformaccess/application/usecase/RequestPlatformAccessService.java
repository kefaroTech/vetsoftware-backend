package com.vetsoftware.app.platformaccess.application.usecase;

import com.vetsoftware.app.platformaccess.application.command.RequestPlatformAccessCommand;
import com.vetsoftware.app.platformaccess.application.port.in.RequestPlatformAccessUseCase;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessAuditPort;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessEmailSender;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessEmailSender.AccessRequestedNotification;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessMetrics;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessMetrics.RequestResult;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessRequestRepository;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessSwitchPort;
import com.vetsoftware.app.platformaccess.application.port.out.SecretHasherPort;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessClosedException;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessRequest;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Recibe la solicitud pública de acceso, emite el token del aprobador y su
 * código de 6 dígitos, y avisa al aprobador por correo.
 *
 * <p>
 * <b>Todo lo que decide este servicio está subordinado a no crear un
 * oráculo.</b> El endpoint es anónimo y su respuesta —202 sin cuerpo— tiene que
 * ser idéntica exista o no ya una cuenta con ese correo, y la haya pedido ya
 * esa persona o no. De ahí tres decisiones que parecen arbitrarias y no lo son:
 *
 * <ol>
 * <li><b>El interruptor se consulta primero, antes de tocar el correo.</b> Si
 * la rama cerrada saliera antes o después que la abierta, la diferencia de
 * latencia sería el oráculo que el 202 se molesta en evitar.</li>
 * <li><b>Una solicitud viva del mismo correo se ignora y se responde 202
 * igual.</b> No se reenvía nada: el token plano ya no existe —solo su hash— y
 * rotarlo permitiría a un tercero invalidar el enlace que el aprobador tiene en
 * su buzón. Queda el evento {@code duplicate_request} para poder verlo.</li>
 * <li><b>No se comprueba si ya hay un superadministrador con ese correo.</b>
 * Esa comprobación es interna y solo se ejecuta al aceptar, con un token válido
 * en la mano. Hacerla aquí abriría exactamente la enumeración que el 202
 * cierra.</li>
 * <li><b>Las dos ramas hacen el mismo trabajo caro.</b> El token y el bcrypt
 * del código se calculan antes de consultar si hay una solicitud viva, y en la
 * rama duplicada se descartan. Responder lo mismo no sirve de nada si se tarda
 * ocho veces menos en decirlo: el cronómetro es un oráculo igual que el cuerpo
 * de la respuesta.</li>
 * </ol>
 */
@Observed(name = "system.user.request.create")
@Service
public class RequestPlatformAccessService implements RequestPlatformAccessUseCase {

    private static final Logger log = LoggerFactory.getLogger(RequestPlatformAccessService.class);

    private final PlatformAccessSwitchPort accessSwitch;
    private final PlatformAccessRequestRepository requestRepository;
    private final SecretHasherPort secretHasher;
    private final PlatformAccessEmailSender emailSender;
    private final PlatformAccessAuditPort audit;
    private final PlatformAccessMetrics metrics;
    private final Clock clock;
    private final long approvalTokenTtlHours;
    private final int maxAttempts;

    public RequestPlatformAccessService(PlatformAccessSwitchPort accessSwitch,
            PlatformAccessRequestRepository requestRepository, SecretHasherPort secretHasher,
            PlatformAccessEmailSender emailSender, PlatformAccessAuditPort audit,
            PlatformAccessMetrics metrics, Clock clock,
            @Value("${vetsoftware.platform-access.approval-token-ttl-hours:72}") long approvalTokenTtlHours,
            @Value("${vetsoftware.platform-access.max-verification-attempts:5}") int maxAttempts) {
        this.accessSwitch = accessSwitch;
        this.requestRepository = requestRepository;
        this.secretHasher = secretHasher;
        this.emailSender = emailSender;
        this.audit = audit;
        this.metrics = metrics;
        this.clock = clock;
        this.approvalTokenTtlHours = approvalTokenTtlHours;
        this.maxAttempts = maxAttempts;
    }

    @Override
    @Transactional
    public void execute(RequestPlatformAccessCommand command) {
        if (!accessSwitch.isOpen()) {
            audit.accessRequestDenied("form_closed", null, null);
            metrics.requested(RequestResult.FORM_CLOSED);
            throw new PlatformAccessClosedException("Platform access request form is closed");
        }

        String fullName = trim(command.fullName());
        String email = normalizeEmail(command.email());
        String reason = trim(command.reason());
        LocalDateTime now = LocalDateTime.now(clock);

        // El token, su hash y el bcrypt del código se calculan ANTES de mirar si hay
        // una solicitud viva, y en la rama duplicada se tiran. Es trabajo desechado a
        // propósito: bcrypt cuesta ~100 ms y la consulta ~1 ms, así que hacerlo
        // después dejaba la rama duplicada en ~15 ms frente a ~120 ms de la normal.
        // Esa diferencia es medible desde fuera con un cronómetro y un correo
        // concreto, y responde justo la pregunta que el 202 se molesta en no
        // responder: si hay un alta de superadministrador en curso para esa
        // organización, que es la ventana en la que atacar el buzón del aprobador
        // tiene sentido. El límite de 3/h no lo impide: tres medidas bastan.
        String rawToken = PlatformAccessTokens.generateRawToken();
        String rawCode = PlatformAccessTokens.generateVerificationCode();
        String approvalTokenHash = PlatformAccessTokens.hash(rawToken);
        String verificationCodeHash = secretHasher.hash(rawCode);

        Optional<PlatformAccessRequest> live = requestRepository.findLivePendingByEmail(email, now);
        if (live.isPresent()) {
            ignoreDuplicate(live.get());
            return;
        }

        PlatformAccessRequest saved = requestRepository.save(PlatformAccessRequest.issue(fullName,
                email, reason, approvalTokenHash, verificationCodeHash, maxAttempts, now,
                now.plusHours(approvalTokenTtlHours)));

        audit.bindRequest(saved.getId());
        try {
            audit.accessRequested(saved.getId(), saved.emailDomain());
            metrics.requested(RequestResult.SUCCESS);
            // El payload se resuelve DENTRO de la transacción: el callback no vuelve a
            // leer nada después del commit, y el token y el código planos solo existen
            // en memoria (en la base están sus hashes).
            sendAfterCommit(new AccessRequestedNotification(saved.getId(), saved.getFullName(),
                    saved.getEmail(), saved.getReason(), saved.getCreatedDate(), rawToken,
                    rawCode));
        } finally {
            // Sin este remove, la clave sobrevive en el hilo del pool y etiqueta la
            // petición del siguiente usuario con el id de una solicitud ajena.
            audit.unbindRequest();
        }
    }

    private void ignoreDuplicate(PlatformAccessRequest existing) {
        audit.bindRequest(existing.getId());
        try {
            audit.accessRequestDenied("duplicate_request", existing.getId(),
                    existing.emailDomain());
        } finally {
            audit.unbindRequest();
        }
        metrics.requested(RequestResult.DUPLICATE_IGNORED);
    }

    /**
     * Difiere el aviso al commit. El adaptador de correo es {@code @Async}, así que
     * encolarlo dentro de la transacción entregaría el enlace de aprobación de una
     * solicitud que un rollback posterior podría no haber creado nunca.
     *
     * <p>
     * Clase anónima y no lambda: la regla que vigila este patrón atribuye el lambda
     * al método declarante y da un falso positivo.
     *
     * <p>
     * Sin transacción activa se envía en el acto —es lo correcto ahí, y registrar
     * la sincronización lanzaría—. El {@code catch} protege al llamador: una
     * excepción en {@code afterCommit} se propaga aunque la transacción ya haya
     * confirmado, y convertiría una solicitud aceptada en un 500.
     */
    private void sendAfterCommit(AccessRequestedNotification notification) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            emailSender.sendAccessRequested(notification);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    emailSender.sendAccessRequested(notification);
                } catch (RuntimeException exception) {
                    log.warn("No se pudo encolar el aviso de la solicitud de acceso de plataforma",
                            exception);
                }
            }
        });
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    /**
     * La collation del esquema es acento e mayúsculas insensitiva, así que
     * normalizar no cambia la unicidad; sirve para que la misma persona no genere
     * dos filas escribiendo su correo con otra caja.
     */
    private static String normalizeEmail(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}
