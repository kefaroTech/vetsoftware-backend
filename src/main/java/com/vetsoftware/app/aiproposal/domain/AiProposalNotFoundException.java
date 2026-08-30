package com.vetsoftware.app.aiproposal.domain;

/**
 * La propuesta no existe.
 *
 * <p>
 * ⛔ <strong>El mensaje nunca hace eco del token recibido.</strong> El token es
 * la unica frontera de autorizacion de la feature, y
 * {@code RequestLoggingContextFilter} escribe el MDC en CloudWatch y en Loki
 * con 31 dias de retencion: un mensaje con el token dentro lo publicaria en
 * claro por la puerta de atras, que es exactamente lo que sacarlo del segmento
 * de ruta vino a impedir. Se identifica por id -que es interno- o por nada.
 */
public class AiProposalNotFoundException extends RuntimeException {

    public AiProposalNotFoundException(Long id) {
        super("AI proposal not found: " + id);
    }

    /** Para el camino del token, donde no hay nada seguro que nombrar. */
    public AiProposalNotFoundException() {
        super("AI proposal not found");
    }
}
