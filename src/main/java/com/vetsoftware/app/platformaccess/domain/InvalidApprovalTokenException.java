package com.vetsoftware.app.platformaccess.domain;

/**
 * <b>Una sola excepción para los tres estados muertos</b> del token de
 * aprobación: no existe, caducó o ya se usó. Es deliberado y es la pieza
 * anti-enumeración del flujo.
 *
 * <p>
 * Tres excepciones distintas producirían tres {@code code} distintos en el
 * {@code ProblemDetail}, y con eso quien pruebe tokens al azar sabría cuáles
 * existieron alguna vez. El front ya está construido sobre la premisa
 * contraria: colapsa 404 y 410 en el mismo estado de pantalla justamente para
 * no delatar nada.
 *
 * <p>
 * El motivo real —cuál de los tres fue— vive solo en el evento de auditoría
 * ({@code token_invalid} / {@code token_expired} / {@code token_consumed}), que
 * no sale al cliente. Por la misma razón esta excepción <b>no</b> se registra
 * en el handler de la familia 404 de {@code GlobalExceptionHandler}: ese deriva
 * el {@code code} del nombre de la clase y devuelve {@code getMessage()} crudo.
 */
public class InvalidApprovalTokenException extends RuntimeException {

    public InvalidApprovalTokenException(String message) {
        super(message);
    }
}
