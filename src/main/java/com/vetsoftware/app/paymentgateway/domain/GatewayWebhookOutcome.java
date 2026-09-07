package com.vetsoftware.app.paymentgateway.domain;

/**
 * Desenlace de procesar un webhook de Wompi, tal como lo persiste
 * {@code gateway_webhook_events.processing_outcome} (changeset 411). El CHECK
 * de esa columna admite además {@code DUPLICATE}, que no se declara aquí porque
 * nunca se escribe: un duplicado se detecta por la unicidad
 * {@code (gateway, event_checksum)} antes de insertar y no llega a persistirse
 * una segunda vez.
 *
 * <p>
 * {@code REJECTED_STALE} es un desenlace distinto de {@code REJECTED_CHECKSUM}:
 * el primero es un evento ya autenticado que llegó fuera de la ventana de
 * frescura —un reintento demorado de Wompi, o un ataque de repetición con un
 * checksum robado—, y por eso sí se persiste; el segundo es un checksum que no
 * coincide con el secreto y ni siquiera llega a persistirse, porque es tráfico
 * que no se pudo autenticar.
 */
public enum GatewayWebhookOutcome {
    APPLIED, IGNORED_UNKNOWN_EVENT, IGNORED_ALREADY_FINAL, PAYMENT_NOT_FOUND, REJECTED_CHECKSUM, REJECTED_STALE, REJECTED_AMOUNT
}
