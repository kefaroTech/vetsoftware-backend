package com.vetsoftware.app.subscription.application.port.out;

import com.vetsoftware.app.subscription.domain.SubscriptionStatus;
import com.vetsoftware.app.subscription.domain.SubscriptionStatusChangeReason;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Rastro de auditoría de las mutaciones del contrato.
 *
 * <p>
 * <b>Es un puerto y no una llamada directa a {@code AuditLogger} por una regla
 * de arquitectura, no por gusto</b>: {@code ..application..} no puede depender
 * de {@code ..infrastructure..}, y la regla está congelada. El adaptador vive
 * en {@code subscription/infrastructure/audit}, exactamente como
 * {@code PlatformAccessAuditPort}.
 *
 * <p>
 * <b>Qué NO viaja por aquí.</b> Ni el actor, ni la empresa, ni la IP: los pone
 * el MDC —{@code AuthFilter} y {@code RequestLoggingContextFilter} en el borde
 * HTTP, {@code ScheduledJobTelemetry} cuando el origen es un barrido—. Esa es
 * la propiedad que hace que un cambio de estado escrito por la cobranza
 * nocturna salga con el mismo formato que uno hecho por una persona y con un
 * actor distinto.
 *
 * <p>
 * Tampoco viaja el motivo tecleado a mano. {@code reason} es vocabulario
 * cerrado —{@link SubscriptionStatusChangeReason}— y <b>ahora lo impone el
 * tipo, no este párrafo</b>. Durante un tiempo esta frase describía una
 * barandilla que no existía: el controlador pasaba {@code request.reason()} del
 * cuerpo HTTP tal cual y el barrido de ciclo de vida concatenaba frases en
 * español. Texto libre de usuario en un canal de auditoría es log injection
 * esperando a ocurrir (ASVS V7.3.1) y no agrega nada que la fila no tenga.
 */
public interface SubscriptionAuditPort {

    /**
     * @param monthlyDeltaAmount
     *            lo que sube la cuota recurrente. <b>Es el campo que prueba qué
     *            importe se le mostró al cliente antes de confirmar</b>, que es lo
     *            único que se puede oponer cuando niegue haber aceptado la
     *            ampliación que le duplicó la factura.
     */
    void itemAdded(Long subscriptionId, Long itemId, Long catalogItemId, Integer quantity,
            BigDecimal monthlyDeltaAmount, Long amendmentId);

    void itemRemoved(Long subscriptionId, Long itemId, BigDecimal monthlyDeltaAmount,
            Long amendmentId);

    void itemQuantityChanged(Long subscriptionId, Long itemId, Integer previousQuantity,
            Integer newQuantity, BigDecimal monthlyDeltaAmount, Long amendmentId);

    /**
     * El evento del escenario que motivó el issue: la clínica que amanece en solo
     * lectura.
     */
    void statusChanged(Long subscriptionId, SubscriptionStatus fromStatus,
            SubscriptionStatus toStatus, SubscriptionStatusChangeReason reason);

    void cancellationRequested(Long subscriptionId, LocalDate effectiveOn);

    /**
     * El lazo entre el dinero y el acceso: el contrato cambió y los permisos se
     * reconstruyeron.
     */
    void entitlementsRecalculated(Long companyId, String triggerReason);
}
