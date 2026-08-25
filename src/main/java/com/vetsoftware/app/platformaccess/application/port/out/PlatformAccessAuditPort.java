package com.vetsoftware.app.platformaccess.application.port.out;

/**
 * Eventos de auditoria del flujo, emitidos por el canal AUDIT unico del
 * sistema. Es un puerto y no un uso directo del emisor porque la capa de
 * aplicacion no conoce infraestructura; el adaptador delega en el mismo
 * {@code AuditLogger} que usa el resto del repositorio, con el mismo
 * vocabulario cerrado.
 *
 * <p>
 * <b>Nunca entra aqui</b> el token, el enlace, el codigo, la contrasena ni el
 * hash de ninguno de ellos: un codigo de 6 digitos hasheado tiene 10^6
 * preimagenes, asi que publicar su verificador en Loki es publicar el codigo.
 * Tampoco el correo ni el nombre del solicitante, ni enmascarados: solo el
 * dominio del correo, que responde una pregunta operativa real sin identificar
 * a nadie.
 *
 * <p>
 * {@link #bindRequest(Long)} y {@link #unbindRequest()} enmarcan el MDC del
 * identificador de correlacion. <b>Siempre en {@code try/finally}</b>: un
 * {@code put} sin su {@code remove} en un pool de hilos etiqueta la peticion
 * del siguiente usuario con el id de una solicitud ajena.
 */
public interface PlatformAccessAuditPort {

    void bindRequest(Long requestId);

    void unbindRequest();

    void accessRequested(Long requestId, String emailDomain);

    /** {@code reason}: {@code form_closed} o {@code duplicate_request}. */
    void accessRequestDenied(String reason, Long requestId, String emailDomain);

    /** {@code reason}: {@code token_invalid} o {@code token_expired}. INFO. */
    void approvalDenied(String reason, Long requestId);

    /** Token de un solo uso reproducido. WARN: describe un ataque en curso. */
    void approvalDeniedByReplay(Long requestId, long secondsSinceConsumption);

    /** Codigo incorrecto con margen restante. INFO. */
    void approvalDeniedByCodeMismatch(Long requestId, int remainingAttempts);

    /** Intentos agotados: bloqueo terminal. WARN. */
    void approvalLocked(Long requestId);

    void requestApproved(Long requestId);

    void requestRejected(Long requestId);

    void invited(Long requestId, String emailDomain);

    /**
     * Aceptacion rechazada. {@code reason} es vocabulario cerrado:
     * {@code token_invalid}, {@code token_expired}, {@code token_consumed} o
     * {@code email_already_provisioned}.
     *
     * <p>
     * <b>Existe porque la respuesta esta disenada para callar.</b> Los cuatro casos
     * salen por el mismo 404 indistinguible —correcto, y no negociable: cualquier
     * diferencia seria un oraculo—, asi que el log es el unico sitio donde el hecho
     * puede existir. El lado del aprobador ya lo hace completo
     * ({@link #approvalDenied}); sin esto, el ultimo salto del flujo era el unico
     * mudo.
     *
     * <p>
     * {@code email_already_provisioned} es el que de verdad hacia falta: significa
     * que alguien presento una invitacion valida para un correo que <b>ya</b> tiene
     * superadministrador. O es un reenvio inocente, o alguien esta intentando
     * hacerse con una identidad existente; sin registro no se puede distinguir
     * despues.
     *
     * <p>
     * {@code requestId} puede ser {@code null}: con un token que no existe no hay
     * solicitud a la que atribuir el hecho.
     */
    void invitationDenied(String reason, Long requestId);

    /**
     * El correo de invitacion no salio y nadie lo reintenta. ERROR: sin que una
     * persona lo reenvie, la invitacion no llega jamas.
     */
    void invitationUndelivered(Long requestId, String emailDomain);

    /**
     * El correo de bienvenida no salio y nadie lo reintenta. ERROR por el mismo
     * motivo que {@link #invitationUndelivered}: el login de las cuentas de sistema
     * es por {@code code}, este correo es el unico canal por el que su dueno lo
     * conoce, y sin el la cuenta queda creada —con control total— y nadie puede
     * entrar en ella.
     */
    void welcomeUndelivered(Long requestId, String emailDomain);

    /**
     * Se creo la cuenta con control total de la plataforma. INFO deliberado: es un
     * hecho normal de un flujo que funciono, y su visibilidad viene del contador y
     * de la alerta, no de la severidad.
     */
    void systemUserProvisioned(Long requestId, Long systemUserId);
}
