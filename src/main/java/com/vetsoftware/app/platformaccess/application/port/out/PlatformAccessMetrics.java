package com.vetsoftware.app.platformaccess.application.port.out;

/**
 * Contadores de negocio del alta de superadministradores. Los valores de
 * {@code result} son vocabulario cerrado y estan declarados ademas en la lista
 * blanca de cardinalidad: si uno falta, el filtro deniega el medidor ENTERO y
 * el hueco del panel es indistinguible de "no hubo actividad".
 *
 * <p>
 * Ninguna etiqueta lleva correo, dominio, id, token ni ip. Esos van a atributos
 * de span o a campos de log, que aguantan la alta cardinalidad; una etiqueta de
 * metrica con un id es una serie por solicitud.
 */
public interface PlatformAccessMetrics {

    void requested(RequestResult result);

    void resolved(ApprovalResult result);

    void invitation(InvitationResult result);

    /** Sin etiquetas: es el hecho irreversible del flujo y su unica alerta. */
    void provisioned();

    enum RequestResult {
        SUCCESS("success"), DUPLICATE_IGNORED("duplicate_ignored"), FORM_CLOSED("form_closed");

        private final String value;

        RequestResult(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    enum ApprovalResult {
        APPROVED("approved"), REJECTED("rejected"), TOKEN_INVALID("token_invalid"), TOKEN_EXPIRED(
                "token_expired"), TOKEN_CONSUMED("token_consumed"), CODE_MISMATCH(
                        "code_mismatch"), ATTEMPTS_EXHAUSTED("attempts_exhausted");

        private final String value;

        ApprovalResult(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    /**
     * Desenlaces de la invitacion, del envio y de la aceptacion.
     *
     * <p>
     * Los cuatro ultimos cuentan los rechazos al aceptar. Existen porque el
     * endpoint responde el mismo 404 en los cuatro casos —opacidad deliberada— y
     * sin contador no hay forma de ver la tasa: un pico de
     * {@code email_already_provisioned} es alguien presentando invitaciones validas
     * contra identidades que ya existen.
     */
    enum InvitationResult {
        SENT("sent"), FAILED("failed"), SKIPPED("skipped"), ACCEPTED("accepted"), EXPIRED(
                "expired"), TOKEN_INVALID("token_invalid"), TOKEN_CONSUMED(
                        "token_consumed"), EMAIL_ALREADY_PROVISIONED("email_already_provisioned");

        private final String value;

        InvitationResult(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }
}
