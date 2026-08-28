package com.vetsoftware.app.paymentattempt.domain;

/**
 * Por que rebotó el cobro. Dominio cerrado, espejo exacto de
 * {@code chk_payment_attempts_decline_kind}: si aquí aparece un valor que la
 * constraint no admite, el {@code INSERT} lo rechaza la base y el fallo llega
 * como un 409 sin explicación.
 *
 * <p>
 * <strong>Son tres familias, no dos, y es la columna sobre la que se ramifica
 * todo el proceso de cobranza:</strong>
 *
 * <ul>
 * <li>{@link #SOFT} — fondos insuficientes, límite temporal. <strong>Se
 * reintenta</strong>, hasta {@link PaymentAttempt#MAX_SOFT_ATTEMPTS} veces
 * dentro de {@link PaymentAttempt#RETRY_WINDOW}.</li>
 * <li>{@link #HARD} — tarjeta perdida, robada, autorización revocada, operación
 * no permitida. <strong>No se reintenta jamás</strong>: se pide medio de pago
 * nuevo. Por eso {@code chk_payment_attempts_hard_has_no_retry} obliga a que su
 * {@code next_attempt_at} vaya vacío — no lejano, no nulo por descuido.</li>
 * <li>{@link #CONFIGURATION} — moneda no soportada, credencial mal puesta,
 * pasarela caída. Son errores <strong>propios</strong>, no del cliente. Sin
 * distinguirlos se queman contra un fallo que no es suyo los intentos que la
 * red permite, <strong>y las redes multan eso</strong>; además arrancaría
 * cobranza contra alguien que no ha hecho nada mal. Es lo que comprueba
 * {@link PaymentAttempt#consumesCustomerAttempts()}.</li>
 * </ul>
 *
 * <p>
 * La columna es {@code VARCHAR(15)} y no {@code VARCHAR(10)} precisamente
 * porque el tercer valor no cabía en la medida original — que es literalmente
 * por lo que las tres familias eran dos.
 */
public enum DeclineKind {
    SOFT, HARD, CONFIGURATION
}
