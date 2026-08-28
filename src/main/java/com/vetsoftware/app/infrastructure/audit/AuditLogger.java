package com.vetsoftware.app.infrastructure.audit;

import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Emisor central de eventos de auditoría (OWASP ASVS V7.1.2 / 7.1.4).
 *
 * <p>
 * Escribe al logger {@code "AUDIT"}, que sale por el pipeline estructurado
 * (stdout JSON → CloudWatch → Loki) para consulta en Grafana. Los campos del
 * actor ({@code
 * actor.type} / {@code actor.companyId} / {@code actor.employeeId} /
 * {@code actor.systemUserId}, poblados en {@code AuthFilter}) y el contexto
 * HTTP de la request ({@code client.ip} / {@code
 * user_agent.original} / {@code http.method} / {@code http.path}, poblados en
 * {@code
 * RequestLoggingContextFilter} para toda request — ver
 * {@link com.vetsoftware.app.infrastructure.logging.MdcKeys}) viajan por el
 * MDC, y el encoder estructurado de Spring Boot los emite como campos JSON;
 * aquí solo se añaden los campos propios del evento (no se duplican
 * {@code http.method}/{@code http.path} ya presentes en el MDC).
 *
 * <p>
 * <b>El log es hoy el único destino.</b> El outbox que persistía el mismo hecho
 * y lo archivaba en S3 Object Lock vía Firehose se retiró deliberadamente
 * (junto con su cadena de integridad). La consecuencia hay que tenerla presente
 * y no descubrirla en una auditoría: este rastro es <b>mutable y de retención
 * acotada</b> —la que dé Loki—, así que no constituye evidencia inalterable. Si
 * aparece una obligación de conservación o de no repudio (ASVS V7, NIST SP
 * 800-53 AU-9, ISO/IEC 27001 A.8.15), hay que reponer un destino durable; el
 * historial de git conserva la implementación anterior.
 *
 * <p>
 * Convención de campos: notación con punto ({@code http.*}, {@code actor.*})
 * alineada con OpenTelemetry. Nota: {@code actor.identifier} (en login) es el
 * <em>código</em> de empleado/usuario tal como llega en la request — distinto
 * de {@code actor.employeeId} del MDC, que es el id numérico del principal ya
 * autenticado.
 *
 * <p>
 * Nunca registra credenciales ni tokens — solo identificadores no sensibles.
 */
@Component
public class AuditLogger {

    private static final Logger audit = LoggerFactory.getLogger("AUDIT");

    /**
     * Mutación de recurso en el borde HTTP (POST/PUT/PATCH/DELETE).
     * http.method/http.path vía MDC.
     */
    public void mutation(String method, String path, int status, String outcome, long durationMs) {
        audit.atInfo().addKeyValue("event", "http_mutation").addKeyValue("http.status", status)
                .addKeyValue("outcome", outcome).addKeyValue("http.durationMs", durationMs)
                .log("mutation {} {} -> {} ({})", method, path, status, outcome);
    }

    /**
     * Alta de una nueva empresa (veterinaria) por auto-registro público. Sin datos
     * sensibles; {@code
     * ownerCode} es el código de acceso del dueño, no un secreto.
     * client.ip/http.path vía MDC.
     */
    public void companyRegistered(Long companyId, String companyName, String companyIdentifier,
            Long ownerEmployeeId, String ownerCode) {
        audit.atInfo().addKeyValue("event", "company_registered")
                .addKeyValue("company.id", companyId).addKeyValue("company.name", companyName)
                .addKeyValue("company.identifier", companyIdentifier)
                .addKeyValue("actor.employeeId", ownerEmployeeId)
                .addKeyValue("actor.identifier", ownerCode).addKeyValue("outcome", "SUCCESS")
                .log("company registered id={} name={}", companyId, companyName);
    }

    /**
     * Una empresa archivada vuelve al registro. Es un cambio de alcance de accesos
     * —todos los empleados de esa clínica vuelven a poder entrar de golpe—, no una
     * edición de ficha, así que deja su propio evento en vez de conformarse con el
     * {@code http_mutation} genérico del borde. El actor viaja en el MDC.
     */
    public void companyReactivated(Long companyId, String companyName, String companyIdentifier) {
        audit.atInfo().addKeyValue("event", "company_reactivated")
                .addKeyValue("company.id", companyId).addKeyValue("company.name", companyName)
                .addKeyValue("company.identifier", companyIdentifier)
                .addKeyValue("outcome", "SUCCESS")
                .log("company reactivated id={} name={}", companyId, companyName);
    }

    /**
     * Un admin (o quien tenga el permiso) envió una invitación de acceso a un nuevo
     * empleado. El actor que invita viaja en el MDC (actor.employeeId /
     * actor.companyId). {@code invitedCode} es el código de acceso del invitado, no
     * un secreto.
     */
    public void employeeInvited(Long invitedEmployeeId, String invitedCode, Long companyId) {
        audit.atInfo().addKeyValue("event", "employee_invited")
                .addKeyValue("employee.id", invitedEmployeeId)
                .addKeyValue("employee.identifier", invitedCode)
                .addKeyValue("company.id", companyId).addKeyValue("outcome", "SUCCESS")
                .log("employee invited id={} code={}", invitedEmployeeId, invitedCode);
    }

    /**
     * Reenvío de la invitación (nueva contraseña provisional) por un admin/con
     * permiso. Actor vía MDC.
     */
    public void employeeInvitationResent(Long invitedEmployeeId, String invitedCode,
            Long companyId) {
        audit.atInfo().addKeyValue("event", "employee_invitation_resent")
                .addKeyValue("employee.id", invitedEmployeeId)
                .addKeyValue("employee.identifier", invitedCode)
                .addKeyValue("company.id", companyId).addKeyValue("outcome", "SUCCESS")
                .log("employee invitation resent id={} code={}", invitedEmployeeId, invitedCode);
    }

    /**
     * El empleado invitado aceptó la invitación: completó su primer ingreso
     * cambiando la contraseña temporal (INVITED → activo con contraseña propia). El
     * propio empleado es el actor (MDC).
     */
    public void invitationAccepted(Long employeeId, Long companyId) {
        audit.atInfo().addKeyValue("event", "invitation_accepted")
                .addKeyValue("employee.id", employeeId).addKeyValue("company.id", companyId)
                .addKeyValue("outcome", "SUCCESS")
                .log("invitation accepted employeeId={}", employeeId);
    }

    /**
     * Login exitoso; {@code identifier} es el código de empleado/usuario, no un
     * secreto.
     */
    public void loginSuccess(String userType, String identifier) {
        audit.atInfo().addKeyValue("event", "login_success").addKeyValue("actor.type", userType)
                .addKeyValue("actor.identifier", identifier).addKeyValue("outcome", "SUCCESS")
                .log("login success type={} id={}", userType, identifier);
    }

    /**
     * Intento de login fallido; sin identificador (no disponible en el handler) ni
     * credenciales. http.path vía MDC.
     */
    public void loginFailure(String path, String reason) {
        audit.atWarn().addKeyValue("event", "login_failure").addKeyValue("outcome", "FAILURE")
                .addKeyValue("reason", reason).log("login failure {} reason={}", path, reason);
    }

    /**
     * Login bloqueado porque el correo del empleado aún no está verificado
     * (auto-registro Opción B). {@code identifier} es el código de empleado
     * intentado (no un secreto). http.path/client.ip vía MDC.
     */
    public void loginBlockedEmailNotVerified(String identifier) {
        audit.atWarn().addKeyValue("event", "login_blocked_email_not_verified")
                .addKeyValue("actor.identifier", identifier).addKeyValue("outcome", "DENIED")
                .addKeyValue("reason", "email_not_verified")
                .log("login blocked, email not verified id={}", identifier);
    }

    /**
     * Se presentó un refresh token ya revocado y se revocó la familia entera del
     * sujeto. Es la señal canónica de robo del OAuth 2.0 Security BCP (§4.14.2), y
     * el único evento de este archivo que describe un ataque en curso y no una
     * decisión de autorización: merece revisión humana, no solo quedar registrado.
     *
     * <p>
     * {@code seconds_since_revocation} es lo que permite separar el robo de una
     * carrera benigna entre pestañas al leer el log. Valores de pocos segundos por
     * encima de la ventana de gracia apuntan a lo segundo; horas o días, a lo
     * primero.
     */
    public void refreshTokenReuseDetected(Long subjectId, String subjectType,
            long secondsSinceRevocation) {
        audit.atWarn().addKeyValue("event", "refresh_token_reuse_detected")
                .addKeyValue("actor.id", subjectId).addKeyValue("actor.type", subjectType)
                .addKeyValue("outcome", "DENIED")
                .addKeyValue("seconds_since_revocation", secondsSinceRevocation)
                .log("refresh token reuse detected type={} id={}; revoked all sessions",
                        subjectType, subjectId);
    }

    /**
     * Denegación de autorización (@PreAuthorize → AccessDeniedException). Actor,
     * http.* vía MDC.
     */
    public void accessDenied(String method, String path) {
        audit.atWarn().addKeyValue("event", "access_denied").addKeyValue("outcome", "DENIED")
                .log("access denied {} {}", method, path);
    }

    /**
     * Acceso a un recurso protegido sin autenticación válida (token
     * ausente/inválido → 401). http.method/http.path vía MDC.
     *
     * <p>
     * <b>INFO y no WARN, deliberadamente.</b> El criterio del repo para la
     * severidad es quién debe actuar, no cuán grave suena el hecho: un 401 por
     * token caducado es el desenlace más rutinario que tiene una API con sesiones
     * —el front lo trata refrescando el token— y cualquier anónimo puede provocarlo
     * a voluntad. El sistema funcionó exactamente como debía, así que no hay nada
     * que un operador tenga que hacer. A nivel WARN este evento es la población
     * dominante del canal AUDIT y entierra los WARN que sí piden revisión humana
     * ({@code refresh_token_reuse_detected},
     * {@code login_blocked_email_not_verified}, {@code rate_limited}). El hecho
     * sigue registrado y consultable; lo que cambia es que deja de pedir atención.
     *
     * <p>
     * Un pico de 401 no se vigila leyendo este log: se vigila con la tasa de 401 y
     * con el contador {@code vetsoftware.security.tokens.rejected} de
     * {@code AuthFilter}, que separa el fallo aislado del fallo sistémico. La
     * severidad no es el mecanismo de alerta.
     *
     * <p>
     * {@code reason} es <b>vocabulario cerrado en snake_case</b>, alineado con el
     * {@code code} del ProblemDetail que ve el front: {@code token_missing},
     * {@code token_expired}, {@code token_invalid}, {@code session_replaced}. Nunca
     * un mensaje de excepción ni texto libre: {@code reason} se agrupa y se filtra
     * en Grafana, y un valor no acotado lo vuelve inagrupable.
     */
    public void unauthenticated(String method, String path, String reason) {
        audit.atInfo().addKeyValue("event", "unauthenticated").addKeyValue("outcome", "DENIED")
                .addKeyValue("reason", reason)
                .log("unauthenticated {} {} reason={}", method, path, reason);
    }

    /**
     * Login bloqueado por rate limiting (429) — señal de fuerza bruta.
     * client.ip/http.path vía MDC.
     */
    public void loginRateLimited() {
        rateLimited("LOGIN_RATE_LIMITED");
    }

    /**
     * Ruta publica bloqueada por rate limiting (429). client.ip/http.path via MDC.
     */
    public void rateLimited(String code) {
        audit.atWarn().addKeyValue("event", "rate_limited").addKeyValue("code", code)
                .addKeyValue("outcome", "DENIED").log("rate limited code={}", code);
    }

    // ── Alta de superadministradores de plataforma por invitación (#360) ────────
    //
    // Quince hechos de un flujo público de tres saltos cuyo desenlace es una
    // cuenta con control total sobre todos los tenants. Tres cosas que hay que
    // entender antes de tocar cualquiera de estos métodos:
    //
    // 1. NUNCA entra aquí el token, el enlace, el código, la contraseña, ni el
    // HASH de ninguno de ellos. Para el token la regla es obvia; para el
    // código lo es menos y es peor: seis dígitos tienen 10^6 preimágenes, así
    // que publicar su verificador en Loki es publicar el código. Y el redactor
    // central NO es red de seguridad aquí: suprime corridas de diez dígitos o
    // más, y un código de seis no casa con ninguna regla suya —saldría entero—.
    // Bajar ese umbral mutilaría todos los ids e importes del sistema, así que
    // la única respuesta correcta es no registrarlo.
    //
    // 2. El correo y el nombre del solicitante tampoco salen, ni enmascarados.
    // Son datos personales de alguien que quizá nunca fue aprobado y que no
    // consintió nada. Sale `email.domain`, que responde una pregunta operativa
    // real —¿cuarenta dominios desechables o tres personas de una empresa?— sin
    // identificar a nadie. Toda investigación empieza por
    // `system.user.request.id` y sigue en la base, que es donde esos datos
    // tienen dueño, control de acceso y política de retención.
    //
    // 3. La severidad sigue el criterio del archivo: quién debe actuar. Los
    // endpoints son públicos y cualquier anónimo puede provocar un token
    // inválido o un código incorrecto a voluntad, así que esos van en INFO —lo
    // que importa es la tasa, y la tasa es una métrica—. Solo tres suben: dos
    // WARN que describen un ataque en curso o a un aprobador que se quedó
    // fuera, y un único ERROR, el del correo que se perdió sin reintento.

    /** Solicitud recibida. El nombre, el correo y el motivo NO salen. */
    public void systemUserRequested(Long requestId, String emailDomain) {
        audit.atInfo().addKeyValue("event", "system_user_requested")
                .addKeyValue("system.user.request.id", requestId)
                .addKeyValue("email.domain", emailDomain).addKeyValue("outcome", "SUCCESS")
                .log("system user access requested");
    }

    /**
     * Solicitud no admitida. {@code reason} es vocabulario cerrado:
     * {@code form_closed} (el interruptor está cerrado) o {@code duplicate_request}
     * (ya hay una solicitud viva para ese correo).
     *
     * <p>
     * INFO en los dos casos: el sistema funcionó como debía y el cliente recibe el
     * mismo 202 o el mismo 404 que recibiría en cualquier otra circunstancia. El
     * motivo del cierre no viaja en la respuesta HTTP, solo aquí.
     */
    public void systemUserRequestDenied(String reason, Long requestId, String emailDomain) {
        audit.atInfo().addKeyValue("event", "system_user_request_denied")
                .addKeyValue("reason", reason).addKeyValue("system.user.request.id", requestId)
                .addKeyValue("email.domain", emailDomain).addKeyValue("outcome", "DENIED")
                .log("system user access request denied reason={}", reason);
    }

    /**
     * Token de aprobación rechazado. {@code reason} reutiliza el vocabulario ya
     * vivo del sistema —{@code token_invalid}, {@code token_expired}— en vez de
     * abrir uno paralelo tipo {@code approval_token_expired}: {@code event} ya
     * desambigua el canal, y un vocabulario propio impide preguntar «cuántos
     * rechazos por token caducado hubo hoy» a través de todo el sistema.
     *
     * <p>
     * {@code requestId} puede ser {@code null}: con un token que no existe no hay
     * solicitud a la que atribuir el hecho.
     */
    public void systemUserApprovalDenied(String reason, Long requestId) {
        audit.atInfo().addKeyValue("event", "system_user_approval_denied")
                .addKeyValue("reason", reason).addKeyValue("system.user.request.id", requestId)
                .addKeyValue("outcome", "DENIED")
                .log("system user approval denied reason={}", reason);
    }

    /**
     * Se presentó un token de aprobación ya usado. <b>WARN</b>, y es la misma
     * semántica que {@code refresh_token_reuse_detected}: un token de un solo uso
     * que vuelve a aparecer no es un error de tecleo, o el enlace se filtró o
     * alguien está reproduciendo el correo. Merece revisión humana, no solo quedar
     * registrado.
     *
     * <p>
     * {@code seconds_since_consumption} es lo que separa el doble clic del
     * aprobador (segundos) de la reproducción (horas o días) al leer el log.
     */
    public void systemUserApprovalReplayed(Long requestId, long secondsSinceConsumption) {
        audit.atWarn().addKeyValue("event", "system_user_approval_denied")
                .addKeyValue("reason", "token_consumed")
                .addKeyValue("system.user.request.id", requestId)
                .addKeyValue("seconds_since_consumption", secondsSinceConsumption)
                .addKeyValue("outcome", "DENIED")
                .log("system user approval token replayed; already consumed");
    }

    /**
     * Código de verificación incorrecto, con margen restante. INFO: cualquiera con
     * el enlace puede provocarlo, el bloqueo por intentos y el rate limit lo
     * acotan, y su único desenlace exitoso posible es la creación de la cuenta, que
     * ya tiene alerta propia. <b>El código no se registra.</b>
     */
    public void systemUserApprovalCodeMismatch(Long requestId, int remainingAttempts) {
        audit.atInfo().addKeyValue("event", "system_user_approval_denied")
                .addKeyValue("reason", "code_mismatch")
                .addKeyValue("system.user.request.id", requestId)
                .addKeyValue("attempts.remaining", remainingAttempts)
                .addKeyValue("outcome", "DENIED")
                .log("system user approval denied reason=code_mismatch");
    }

    /**
     * Intentos agotados: la solicitud queda bloqueada de forma terminal.
     * <b>WARN</b> por dos poblaciones a la vez, y las dos piden mirar: alguien está
     * probando códigos, y además un aprobador legítimo acaba de quedarse fuera y va
     * a pedir ayuda. Misma familia que {@code rate_limited}.
     */
    public void systemUserApprovalLocked(Long requestId) {
        audit.atWarn().addKeyValue("event", "system_user_approval_locked")
                .addKeyValue("reason", "attempts_exhausted")
                .addKeyValue("system.user.request.id", requestId).addKeyValue("outcome", "DENIED")
                .log("system user approval permanently locked; attempts exhausted");
    }

    public void systemUserRequestApproved(Long requestId) {
        audit.atInfo().addKeyValue("event", "system_user_request_approved")
                .addKeyValue("system.user.request.id", requestId).addKeyValue("outcome", "SUCCESS")
                .log("system user access request approved");
    }

    public void systemUserRequestRejected(Long requestId) {
        audit.atInfo().addKeyValue("event", "system_user_request_rejected")
                .addKeyValue("system.user.request.id", requestId).addKeyValue("outcome", "SUCCESS")
                .log("system user access request rejected");
    }

    /** Invitación entregada a Resend. El token y el enlace NO salen. */
    public void systemUserInvited(Long requestId, String emailDomain) {
        audit.atInfo().addKeyValue("event", "system_user_invited")
                .addKeyValue("system.user.request.id", requestId)
                .addKeyValue("email.domain", emailDomain).addKeyValue("outcome", "SUCCESS")
                .log("system user invitation sent");
    }

    /**
     * El correo de invitación no salió. <b>ERROR, y el único del flujo.</b> El
     * envío es {@code @Async} fire-and-forget y no hay reintento ni cola de salida:
     * el mensaje se perdió definitivamente aunque el HTTP haya respondido 202.
     * Nadie ni nada lo recupera sin que una persona reemita la invitación, y eso es
     * un fallo terminal.
     *
     * <p>
     * El arreglo de fondo no es el nivel de log: es que no hay reintento. Mientras
     * no lo haya, este ERROR necesita runbook —«reemitir la invitación desde…»— o
     * no es un ERROR.
     *
     * <p>
     * El correo deshabilitado (modo normal de dev) <b>no</b> emite este evento:
     * contarlo llenaría de falsos positivos cualquier alerta de tasa.
     */
    public void systemUserInvitationUndelivered(Long requestId, String emailDomain) {
        audit.atError().addKeyValue("event", "system_user_invitation_undelivered")
                .addKeyValue("reason", "email_failed")
                .addKeyValue("system.user.request.id", requestId)
                .addKeyValue("email.domain", emailDomain).addKeyValue("outcome", "FAILURE")
                .log("system user invitation email was lost; no retry exists, reissue it manually");
    }

    /**
     * El correo de bienvenida no salió. <b>ERROR, por el mismo motivo exacto que
     * {@link #systemUserInvitationUndelivered}</b>: el login de las cuentas de
     * sistema es por {@code code} y no por correo, este mensaje es el único canal
     * por el que su dueño lo conoce, y el envío es {@code @Async} fire-and-forget
     * sin reintento. Perdido el correo queda una cuenta con control total de la
     * plataforma en la que nadie puede entrar, y el HTTP ya respondió 204.
     *
     * <p>
     * Recuperarlo no es reenviar el mensaje —el código sigue en la fila de
     * {@code system_users}, así que se puede leer y comunicar— pero requiere a una
     * persona. Como el de la invitación, este ERROR necesita runbook o no es un
     * ERROR.
     */
    public void systemUserWelcomeUndelivered(Long requestId, String emailDomain) {
        audit.atError().addKeyValue("event", "system_user_welcome_undelivered")
                .addKeyValue("reason", "email_failed")
                .addKeyValue("system.user.request.id", requestId)
                .addKeyValue("email.domain", emailDomain).addKeyValue("outcome", "FAILURE")
                .log("system user welcome email was lost; the account exists and its owner cannot"
                        + " sign in, communicate the login code manually");
    }

    /**
     * Aceptación de invitación rechazada. {@code reason} reutiliza el vocabulario
     * ya vivo —{@code token_invalid}, {@code token_expired},
     * {@code token_consumed}— más {@code email_already_provisioned}, que no existía
     * porque no existía este caso: una invitación válida para un correo que ya
     * tiene superadministrador.
     *
     * <p>
     * <b>Es el único rastro que deja el hecho.</b> Los cuatro casos responden el
     * mismo 404 indistinguible, a propósito, así que sin este evento el intento no
     * ocurre en ningún registro del sistema. El de
     * {@code email_already_provisioned} importa además por sí mismo: para
     * provocarlo hay que poseer una invitación válida, y que alguien la presente
     * contra una identidad ya existente merece poder verse después.
     *
     * <p>
     * INFO como su gemelo del aprobador: los endpoints son públicos y lo que
     * importa es la tasa, que es una métrica.
     */
    public void systemUserInvitationDenied(String reason, Long requestId) {
        audit.atInfo().addKeyValue("event", "system_user_invitation_denied")
                .addKeyValue("reason", reason).addKeyValue("system.user.request.id", requestId)
                .addKeyValue("outcome", "DENIED")
                .log("system user invitation denied reason={}", reason);
    }

    /**
     * Se creó una cuenta con control total de la plataforma.
     *
     * <p>
     * <b>INFO deliberadamente.</b> Es un hecho normal de un flujo que funcionó.
     * Subirlo a WARN «para que destaque» sería usar la severidad como resaltador,
     * que es justo lo que satura el canal. Su visibilidad viene del contador
     * {@code vetsoftware.business.system.user.provisioned} y de la única alerta del
     * flujo, no del nivel.
     *
     * <p>
     * Aceptar la invitación y crear la cuenta ocurren en la misma transacción, así
     * que este es el único evento que se emite para ese instante: dos eventos para
     * un mismo hecho duplicarían el conteo del que cuelga la alerta.
     */
    public void systemUserProvisioned(Long requestId, Long systemUserId) {
        audit.atInfo().addKeyValue("event", "system_user_provisioned")
                .addKeyValue("system.user.request.id", requestId)
                .addKeyValue("actor.systemUserId", systemUserId).addKeyValue("outcome", "SUCCESS")
                .log("platform system user provisioned id={}", systemUserId);
    }

    // ── Dinero de suscripciones (#607) ──────────────────────────────────────────
    //
    // Catorce hechos que cambian lo que un cliente paga. Antes de tocarlos, cinco
    // cosas:
    //
    // 1. POR QUE EXISTEN, si ya estaba `http_mutation`. Ese evento solo sabe decir
    // metodo, ruta, estado y duracion: «POST /subscriptions/7/items -> 200», sin
    // importe, sin articulo y sin delta mensual. AU-3 de NIST SP 800-53 y el
    // req. 10.2 de PCI DSS v4.0 piden quien / que / cuando / desde donde / con
    // que resultado, y el «que» faltaba entero. Y hay un agujero peor que la
    // granularidad: las degradaciones a READ_ONLY y la emision de cargos del
    // cierre de mes NACEN DE UN BARRIDO y no cruzan el borde HTTP, asi que no
    // producian ni ese evento generico. Es el escenario de las tres de la
    // manana: una clinica amanece en solo lectura y la pregunta «quien la
    // degrado y cuando» solo se responde abriendo la base de produccion.
    //
    // 2. EL ACTOR NO VIAJA AQUI. Ni el empleado, ni la empresa, ni la IP: los pone
    // el MDC (AuthFilter y RequestLoggingContextFilter) y, cuando el origen es
    // un barrido, ScheduledJobTelemetry con `actor.type=SYSTEM` y `job.name`.
    // Esa es la propiedad que pedia el issue: un cambio de estado hecho por
    // proceso es INDISTINGUIBLE EN FORMATO de uno hecho por una persona y
    // PERFECTAMENTE DISTINGUIBLE EN ACTOR. Repetir el actor en cada llamada lo
    // unico que consigue es que un dia diga algo distinto del MDC.
    //
    // 3. NINGUN TEXTO LIBRE. `reason` es vocabulario cerrado en snake_case, como en
    // el resto del archivo. La descripcion que teclea un operador de plataforma
    // al anular un cargo NO entra: es texto controlado por un humano, y en un
    // canal de texto plano un CRLF dentro fabrica una linea de auditoria falsa
    // (ASVS V7.3.1). Quien necesite esa descripcion la tiene en la fila.
    //
    // 4. LOS IMPORTES SI ENTRAN, y son el punto. `monthly.delta.amount` en el alta
    // y el cambio de cantidad es literalmente «que importe se le mostro antes de
    // confirmar», que es el unico dato que prueba que el cliente supo lo que
    // aceptaba cuando niegue la ampliacion que le duplico la factura.
    //
    // 5. TODOS INFO. Son hechos normales de flujos que funcionaron; ninguno pide
    // que nadie actue. Subirlos «para que destaquen» seria usar la severidad de
    // resaltador, que es lo que satura el canal. Su vigilancia son los contadores
    // vetsoftware.business.subscription.* y sus alertas, no el nivel de log.
    //
    // 6. NINGUN IMPORTE NI MOTIVO EN EL TEXTO DEL MENSAJE, solo en los campos
    // estructurados. No es estilo: el redactor central escanea el mensaje
    // formateado y su regla LONG_DIGIT_RUN suprime toda corrida de diez digitos
    // o mas por parecerse a una cedula. Una factura anual de 500 clinicas pasa
    // de diez digitos con facilidad, asi que interpolar el importe lo dejaria
    // como '***' EN EL MENSAJE mientras el campo `amount` sale entero: dos
    // verdades distintas en el mismo evento, y la que primero ve un humano es la
    // mutilada. El mensaje se queda con ids cortos; lo variable va en campos, que
    // ademas es lo que permite agrupar por plantilla en Loki.
    //
    // LIMITE CONOCIDO, y no lo tapa este bloque: este canal sigue siendo traza
    // operativa, no registro de auditoria con garantias. Sin proteccion de
    // integridad (NIST AU-9), sin retencion declarada por cumplimiento (ISO/IEC
    // 27001 A.8.15) y sin no repudio desde que se retiro el outbox con su cadena de
    // hash y su archivo en S3 Object Lock (docs/OBSERVABILIDAD_PROD_GRAFANA_S3.md).
    // Que el rastro EXISTA es condicion necesaria y no suficiente; reponer el
    // destino durable es una decision de producto que este bloque no toma.

    /**
     * Alta de linea de contrato: el cliente contrato algo mas.
     * {@code monthlyDeltaAmount} es lo que sube la cuota recurrente.
     */
    public void subscriptionItemAdded(Long subscriptionId, Long itemId, Long catalogItemId,
            Integer quantity, BigDecimal monthlyDeltaAmount, Long amendmentId) {
        audit.atInfo().addKeyValue("event", "subscription_item_added")
                .addKeyValue("subscription.id", subscriptionId)
                .addKeyValue("subscription.item.id", itemId)
                .addKeyValue("catalog.item.id", catalogItemId).addKeyValue("quantity", quantity)
                .addKeyValue("monthly.delta.amount", monthlyDeltaAmount)
                .addKeyValue("amendment.id", amendmentId).addKeyValue("outcome", "SUCCESS")
                .log("subscription item added subscription={} item={}", subscriptionId, itemId);
    }

    /**
     * Baja de linea. {@code monthlyDeltaAmount} es negativo: lo que deja de
     * cobrarse.
     */
    public void subscriptionItemRemoved(Long subscriptionId, Long itemId,
            BigDecimal monthlyDeltaAmount, Long amendmentId) {
        audit.atInfo().addKeyValue("event", "subscription_item_removed")
                .addKeyValue("subscription.id", subscriptionId)
                .addKeyValue("subscription.item.id", itemId)
                .addKeyValue("monthly.delta.amount", monthlyDeltaAmount)
                .addKeyValue("amendment.id", amendmentId).addKeyValue("outcome", "SUCCESS")
                .log("subscription item removed subscription={} item={}", subscriptionId, itemId);
    }

    /**
     * Cambio de cantidad de una linea. Los dos valores viajan juntos a proposito:
     * «paso a 12» no es auditable sin saber de cuanto venia.
     */
    public void subscriptionItemQuantityChanged(Long subscriptionId, Long itemId,
            Integer previousQuantity, Integer quantity, BigDecimal monthlyDeltaAmount,
            Long amendmentId) {
        audit.atInfo().addKeyValue("event", "subscription_item_quantity_changed")
                .addKeyValue("subscription.id", subscriptionId)
                .addKeyValue("subscription.item.id", itemId)
                .addKeyValue("previous.quantity", previousQuantity)
                .addKeyValue("quantity", quantity)
                .addKeyValue("monthly.delta.amount", monthlyDeltaAmount)
                .addKeyValue("amendment.id", amendmentId).addKeyValue("outcome", "SUCCESS")
                .log("subscription item quantity changed subscription={} item={} {} -> {}",
                        subscriptionId, itemId, previousQuantity, quantity);
    }

    /**
     * Cambio de estado del contrato. <b>Es el evento del escenario que motivo el
     * issue</b>: la clinica que amanece en solo lectura. Con
     * {@code actor.type=SYSTEM} y {@code job.name=subscription.dunning} en el MDC,
     * la respuesta a «quien la degrado» sale de una consulta a Loki en vez de una
     * lectura de {@code subscription_status_history} en produccion.
     *
     * <p>
     * <b>{@code change.reason} y no {@code reason}, y la distincion importa.</b>
     * {@code reason} es VERBATIM en {@code LogFieldPolicy} porque en todo el resto
     * del archivo es vocabulario cerrado en snake_case. Aqui el motivo lo teclea
     * quien hace el cambio, asi que puede traer un correo, una cedula o un CRLF que
     * fabrique una linea de auditoria falsa. {@code change.reason} esta declarado
     * SCANNED: pasa por el redactor, un motivo normal sale entero y un dato
     * personal sale enmascarado. Meterlo en {@code reason} habria publicado texto
     * de usuario sin tocar y habria vuelto inagrupable un tag que hoy se filtra en
     * Grafana.
     */
    public void subscriptionStatusChanged(Long subscriptionId, String fromStatus, String toStatus,
            String reason) {
        audit.atInfo().addKeyValue("event", "subscription_status_changed")
                .addKeyValue("subscription.id", subscriptionId)
                .addKeyValue("from.status", fromStatus).addKeyValue("to.status", toStatus)
                .addKeyValue("change.reason", reason).addKeyValue("outcome", "SUCCESS")
                .log("subscription status changed subscription={} {} -> {}", subscriptionId,
                        fromStatus, toStatus);
    }

    /**
     * Solicitud de cancelacion. Se registra aparte del cambio de estado porque
     * ocurre antes: el contrato sigue vigente hasta {@code effectiveOn}, y la
     * distancia entre las dos fechas es justo lo que se discute cuando el cliente
     * reclama el ultimo mes cobrado.
     */
    public void subscriptionCancellationRequested(Long subscriptionId, String effectiveOn) {
        audit.atInfo().addKeyValue("event", "subscription_cancellation_requested")
                .addKeyValue("subscription.id", subscriptionId)
                .addKeyValue("effective.on", effectiveOn).addKeyValue("outcome", "SUCCESS")
                .log("subscription cancellation requested subscription={} effectiveOn={}",
                        subscriptionId, effectiveOn);
    }

    /**
     * Se devengo un cargo: el servicio se presto, con o sin factura todavia. Es el
     * hecho que hoy no deja rastro cuando lo emite el cierre de mes, y el que hace
     * falta para responder «cuantos cargos se emitieron esta noche y por cuanto»
     * sin abrir la base.
     */
    public void subscriptionChargeAccrued(Long chargeId, Long subscriptionId, String chargeType,
            BigDecimal amount, Long amendmentId) {
        audit.atInfo().addKeyValue("event", "subscription_charge_accrued")
                .addKeyValue("charge.id", chargeId).addKeyValue("subscription.id", subscriptionId)
                .addKeyValue("charge.type", chargeType).addKeyValue("amount", amount)
                .addKeyValue("amendment.id", amendmentId).addKeyValue("outcome", "SUCCESS")
                .log("subscription charge accrued charge={} subscription={} type={}", chargeId,
                        subscriptionId, chargeType);
    }

    /**
     * Anulacion de un cargo. El dinero de suscripciones solo agrega: no se corrige
     * encima, se emite un cargo de compensacion. Los dos ids salen porque el rastro
     * sin el par no permite reconstruir el saldo.
     */
    public void subscriptionChargeVoided(Long chargeId, Long compensationChargeId,
            Long subscriptionId, BigDecimal amount) {
        audit.atInfo().addKeyValue("event", "subscription_charge_voided")
                .addKeyValue("charge.id", chargeId)
                .addKeyValue("charge.compensation.id", compensationChargeId)
                .addKeyValue("subscription.id", subscriptionId).addKeyValue("amount", amount)
                .addKeyValue("outcome", "SUCCESS")
                .log("subscription charge voided charge={} compensation={}", chargeId,
                        compensationChargeId);
    }

    /**
     * Emision de una cuenta de cobro. {@code chargeCount} es la barandilla del
     * descuadre: un documento cuyo numero de cargos no cuadra con la conciliacion
     * mensual se detecta aqui y no un mes despues.
     *
     * <p>
     * <b>Por que el mensaje dice {@code billingDocument=} y no
     * {@code document=}</b> (aqui y en los tres eventos de facturacion que siguen).
     * {@code document} es una clave de {@code LogRedactor.PII_KEYS} —es el
     * documento de identidad, la cedula— y la regla de clave-valor sobre texto
     * libre enmascara su valor sin mirar el contexto. Escrito {@code document={}},
     * el id de la cuenta de cobro salia a Loki como {@code document=***} y el
     * evento perdia exactamente el dato que lo hace auditable. El prefijo en
     * camelCase quita la frontera de palabra que necesita el lookbehind del patron,
     * asi que el id sobrevive sin tocar la lista de PII. Lo vigila
     * {@code AuditFieldsSurviveRedactionTest}.
     */
    public void subscriptionDocumentIssued(Long documentId, String documentNumber,
            Long subscriptionId, String issueStatus, BigDecimal amount, Integer chargeCount) {
        audit.atInfo().addKeyValue("event", "subscription_document_issued")
                .addKeyValue("billing.document.id", documentId)
                .addKeyValue("billing.document.number", documentNumber)
                .addKeyValue("subscription.id", subscriptionId)
                .addKeyValue("issue.status", issueStatus).addKeyValue("amount", amount)
                .addKeyValue("billing.document.charges", chargeCount)
                .addKeyValue("outcome", "SUCCESS")
                .log("subscription document issued billingDocument={} number={} status={}",
                        documentId, documentNumber, issueStatus);
    }

    /**
     * Anulacion de una cuenta de cobro. Deja los cargos sellados dentro huerfanos
     * si nadie los libera, que es uno de los fallos silenciosos del catalogo del
     * modelo: sin este evento, la anulacion no ocurre en ningun registro.
     */
    public void subscriptionDocumentVoided(Long documentId, String documentNumber,
            Long subscriptionId, String reason) {
        audit.atInfo().addKeyValue("event", "subscription_document_voided")
                .addKeyValue("billing.document.id", documentId)
                .addKeyValue("billing.document.number", documentNumber)
                .addKeyValue("subscription.id", subscriptionId).addKeyValue("change.reason", reason)
                .addKeyValue("outcome", "SUCCESS")
                .log("subscription document voided billingDocument={} number={}", documentId,
                        documentNumber);
    }

    /** Registro de un pago recibido. */
    public void subscriptionPaymentRegistered(Long paymentId, String paymentMethod,
            BigDecimal amount, String toStatus) {
        audit.atInfo().addKeyValue("event", "subscription_payment_registered")
                .addKeyValue("payment.id", paymentId).addKeyValue("payment.method", paymentMethod)
                .addKeyValue("amount", amount).addKeyValue("to.status", toStatus)
                .addKeyValue("outcome", "SUCCESS")
                .log("subscription payment registered payment={} method={}", paymentId,
                        paymentMethod);
    }

    /**
     * Cambio de estado de un pago. {@code CONFIRMED -> REFUNDED} es plata que sale;
     * {@code PENDING -> FAILED}, plata que nunca entro y que alguien puede haber
     * dado por cobrada.
     */
    public void subscriptionPaymentStatusChanged(Long paymentId, String fromStatus,
            String toStatus) {
        audit.atInfo().addKeyValue("event", "subscription_payment_status_changed")
                .addKeyValue("payment.id", paymentId).addKeyValue("from.status", fromStatus)
                .addKeyValue("to.status", toStatus).addKeyValue("outcome", "SUCCESS")
                .log("subscription payment status changed payment={} {} -> {}", paymentId,
                        fromStatus, toStatus);
    }

    /**
     * Imputacion de una fuente contra una cuenta de cobro. {@code sourceKind}
     * separa el pago que entro de la nota credito y de la retencion, que saldan
     * igual pero no traen un peso.
     */
    public void subscriptionDocumentApplied(Long applicationId, Long documentId, String sourceKind,
            BigDecimal amount) {
        audit.atInfo().addKeyValue("event", "subscription_document_applied")
                .addKeyValue("application.id", applicationId)
                .addKeyValue("billing.document.id", documentId)
                .addKeyValue("source.kind", sourceKind).addKeyValue("amount", amount)
                .addKeyValue("outcome", "SUCCESS")
                .log("subscription document applied application={} billingDocument={} source={}",
                        applicationId, documentId, sourceKind);
    }

    /** Reverso de una imputacion: el saldo de la cuenta de cobro vuelve a subir. */
    public void subscriptionApplicationReversed(Long applicationId, Long documentId,
            BigDecimal amount) {
        audit.atInfo().addKeyValue("event", "subscription_application_reversed")
                .addKeyValue("application.id", applicationId)
                .addKeyValue("billing.document.id", documentId).addKeyValue("amount", amount)
                .addKeyValue("outcome", "SUCCESS")
                .log("subscription application reversed application={} billingDocument={}",
                        applicationId, documentId);
    }

    /**
     * Se reconstruyeron los permisos de una empresa desde su contrato.
     *
     * <p>
     * Es el evento que ata el dinero con el acceso: {@code company.id} mas
     * {@code trigger.reason} responde «por que esta clinica dejo de ver el modulo
     * que paga», que hoy solo se puede reconstruir cruzando dos tablas. El
     * recalculo borra y reinserta la tabla entera, asi que un fallo aqui deja a una
     * empresa sin authorities y a sus empleados con 403 en todo — el mismo corte
     * que vigila {@code vetsoftware_entitlement_resolution_empty_total}.
     */
    public void companyEntitlementsRecalculated(Long companyId, String triggerReason,
            Integer permissionRows) {
        audit.atInfo().addKeyValue("event", "company_entitlements_recalculated")
                .addKeyValue("company.id", companyId).addKeyValue("trigger.reason", triggerReason)
                .addKeyValue("entitlement.rows", permissionRows).addKeyValue("outcome", "SUCCESS")
                .log("company entitlements recalculated company={} trigger={} rows={}", companyId,
                        triggerReason, permissionRows);
    }
}
