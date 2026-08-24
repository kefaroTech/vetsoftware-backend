package com.vetsoftware.app.subscription.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

/**
 * El contrato de una empresa: la carpeta, no el numero.
 *
 * <p>
 * Esta clase guarda la cabecera —quien, con que tarifa, en que ciclo, en que
 * estado, hasta cuando— y nada de lo contratado: eso son las
 * {@link SubscriptionItem}, que son filas con fechas y crecen. Cambiar de plan
 * no sobreescribe esta fila; anade lineas y otrosies.
 */
public class Subscription {

    private static final int MAX_NUMBER_LENGTH = 30;

    /**
     * Que transiciones admite el contrato. Los cuatro estados vigentes se
     * intercomunican —una cuenta vuelve de {@code PAST_DUE} a {@code ACTIVE} al
     * pagar, y de {@code READ_ONLY} a {@code ACTIVE} al reactivarse— y los dos
     * terminales no tienen salida.
     */
    private static final Map<SubscriptionStatus, Set<SubscriptionStatus>> ALLOWED = Map.of(
            SubscriptionStatus.TRIALING, Set.of(SubscriptionStatus.ACTIVE,
                    SubscriptionStatus.PAST_DUE, SubscriptionStatus.READ_ONLY,
                    SubscriptionStatus.CANCELLED, SubscriptionStatus.EXPIRED),
            SubscriptionStatus.ACTIVE,
            Set.of(SubscriptionStatus.PAST_DUE, SubscriptionStatus.READ_ONLY,
                    SubscriptionStatus.CANCELLED, SubscriptionStatus.EXPIRED),
            SubscriptionStatus.PAST_DUE,
            Set.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.READ_ONLY,
                    SubscriptionStatus.CANCELLED, SubscriptionStatus.EXPIRED),
            SubscriptionStatus.READ_ONLY,
            Set.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.PAST_DUE,
                    SubscriptionStatus.CANCELLED, SubscriptionStatus.EXPIRED),
            SubscriptionStatus.CANCELLED, Set.of(), SubscriptionStatus.EXPIRED, Set.of());

    private final Long id;
    private final String subscriptionNumber;
    private final Long companyId;
    private final Long quoteId;
    private Long priceListId;
    private BillingCycle billingCycle;
    private SubscriptionStatus status;
    private final LocalDate startDate;
    private LocalDate trialEndDate;
    private LocalDate currentPeriodStart;
    private LocalDate currentPeriodEnd;
    private LocalDate nextBillingDate;
    private LocalDate commitmentEndDate;
    private final int graceDays;
    private LocalDate pastDueSince;
    private boolean autoRenew;
    private CancellationRequest cancellation;
    private final LocalDateTime createdDate;
    private final Long version;
    private final boolean enabled;

    public Subscription(Long id, String subscriptionNumber, Long companyId, Long quoteId,
            Long priceListId, BillingCycle billingCycle, SubscriptionStatus status,
            LocalDate startDate, LocalDate trialEndDate, LocalDate currentPeriodStart,
            LocalDate currentPeriodEnd, LocalDate nextBillingDate, LocalDate commitmentEndDate,
            int graceDays, LocalDate pastDueSince, boolean autoRenew,
            CancellationRequest cancellation, LocalDateTime createdDate, Long version,
            boolean enabled) {
        if (subscriptionNumber == null || subscriptionNumber.isBlank())
            throw new IllegalArgumentException("subscriptionNumber is required");
        if (subscriptionNumber.length() > MAX_NUMBER_LENGTH)
            throw new IllegalArgumentException(
                    "subscriptionNumber must be " + MAX_NUMBER_LENGTH + " chars or less");
        if (companyId == null)
            throw new IllegalArgumentException("companyId is required");
        if (priceListId == null)
            throw new IllegalArgumentException("priceListId is required");
        if (billingCycle == null)
            throw new IllegalArgumentException("billingCycle is required");
        if (status == null)
            throw new IllegalArgumentException("status is required");
        if (startDate == null)
            throw new IllegalArgumentException("startDate is required");
        if (currentPeriodStart == null || currentPeriodEnd == null)
            throw new IllegalArgumentException("current period is required");
        if (currentPeriodEnd.isBefore(currentPeriodStart))
            throw new IllegalArgumentException(
                    "currentPeriodEnd must not be before currentPeriodStart");
        if (graceDays < 0)
            throw new IllegalArgumentException("graceDays must not be negative");
        if (status == SubscriptionStatus.TRIALING && trialEndDate == null)
            throw new IllegalArgumentException("trialEndDate is required while TRIALING");
        if (commitmentEndDate != null && commitmentEndDate.isBefore(startDate))
            throw new IllegalArgumentException("commitmentEndDate must not be before startDate");
        if (pastDueSince != null && pastDueSince.isBefore(startDate))
            throw new IllegalArgumentException("pastDueSince must not be before startDate");
        this.id = id;
        this.subscriptionNumber = subscriptionNumber;
        this.companyId = companyId;
        this.quoteId = quoteId;
        this.priceListId = priceListId;
        this.billingCycle = billingCycle;
        this.status = status;
        this.startDate = startDate;
        this.trialEndDate = trialEndDate;
        this.currentPeriodStart = currentPeriodStart;
        this.currentPeriodEnd = currentPeriodEnd;
        this.nextBillingDate = nextBillingDate;
        this.commitmentEndDate = commitmentEndDate;
        this.graceDays = graceDays;
        this.pastDueSince = pastDueSince;
        this.autoRenew = autoRenew;
        this.cancellation = cancellation;
        this.createdDate = createdDate;
        this.version = version;
        this.enabled = enabled;
    }

    public static Subscription create(String subscriptionNumber, Long companyId, Long quoteId,
            Long priceListId, BillingCycle billingCycle, SubscriptionStatus status,
            LocalDate startDate, LocalDate trialEndDate, LocalDate currentPeriodStart,
            LocalDate currentPeriodEnd, LocalDate nextBillingDate, LocalDate commitmentEndDate,
            int graceDays, boolean autoRenew) {
        if (status != null && status.isTerminal())
            throw new IllegalArgumentException("a subscription cannot be born " + status);
        return new Subscription(null, subscriptionNumber, companyId, quoteId, priceListId,
                billingCycle, status, startDate, trialEndDate, currentPeriodStart, currentPeriodEnd,
                nextBillingDate, commitmentEndDate, graceDays, null, autoRenew, null, null, null,
                true);
    }

    /**
     * Cambia de estado y devuelve la fila de bitacora que lo documenta. Las dos
     * cosas salen del mismo metodo a proposito: una transicion sin su anotacion en
     * {@code subscription_status_history} es una cuenta en solo lectura que nadie
     * sabe explicar.
     */
    public SubscriptionStatusChange changeStatus(SubscriptionStatus target, String reason,
            String actor, LocalDateTime occurredAt) {
        if (target == null)
            throw new IllegalArgumentException("target status is required");
        if (!ALLOWED.getOrDefault(status, Set.of()).contains(target))
            throw new InvalidSubscriptionStatusTransitionException(status, target);
        SubscriptionStatus previous = status;
        status = target;
        if (target == SubscriptionStatus.PAST_DUE && pastDueSince == null && occurredAt != null)
            pastDueSince = occurredAt.toLocalDate();
        if (target == SubscriptionStatus.ACTIVE)
            pastDueSince = null;
        return SubscriptionStatusChange.record(companyId, id, previous, target, reason, actor,
                occurredAt);
    }

    /**
     * Anota la peticion de baja. <strong>No cambia el estado</strong>: el cliente
     * cancela el 10 y sigue siendo cliente hasta el 30, que es lo que ya pago. El
     * paso a {@code CANCELLED} lo hace {@link #changeStatus} cuando llega la fecha
     * efectiva, y hasta entonces {@link #isCurrent()} sigue diciendo que si.
     */
    public void requestCancellation(LocalDateTime requestedAt, LocalDate effectiveDate,
            String reason) {
        if (status.isTerminal())
            throw new InvalidSubscriptionStatusTransitionException(status,
                    SubscriptionStatus.CANCELLED);
        this.cancellation = new CancellationRequest(requestedAt, effectiveDate, reason);
        this.autoRenew = false;
    }

    /** Renueva el periodo facturable sin tocar nada de lo contratado. */
    public void renewPeriod(LocalDate periodStart, LocalDate periodEnd, LocalDate nextBilling) {
        if (periodStart == null || periodEnd == null)
            throw new IllegalArgumentException("current period is required");
        if (periodEnd.isBefore(periodStart))
            throw new IllegalArgumentException(
                    "currentPeriodEnd must not be before currentPeriodStart");
        this.currentPeriodStart = periodStart;
        this.currentPeriodEnd = periodEnd;
        this.nextBillingDate = nextBilling;
    }

    /**
     * Migracion de tarifa. Los precios ya firmados <strong>no se mueven</strong>:
     * viven congelados en cada {@link SubscriptionItem}, no en la lista.
     */
    public void migrateToPriceList(Long newPriceListId) {
        if (newPriceListId == null)
            throw new IllegalArgumentException("priceListId is required");
        this.priceListId = newPriceListId;
    }

    public void changeBillingCycle(BillingCycle newCycle) {
        if (newCycle == null)
            throw new IllegalArgumentException("billingCycle is required");
        this.billingCycle = newCycle;
    }

    /**
     * ¿Es el contrato vigente de su empresa? Mismo criterio que la columna generada
     * {@code active_marker}: habilitado y en uno de los cuatro estados de
     * {@link SubscriptionStatus#CURRENT}. {@code PAST_DUE} cuenta —debe, pero sigue
     * trabajando— y {@code READ_ONLY} tambien.
     */
    public boolean isCurrent() {
        return enabled && status.isCurrent();
    }

    public Long getId() {
        return id;
    }

    public String getSubscriptionNumber() {
        return subscriptionNumber;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public Long getQuoteId() {
        return quoteId;
    }

    public Long getPriceListId() {
        return priceListId;
    }

    public BillingCycle getBillingCycle() {
        return billingCycle;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getTrialEndDate() {
        return trialEndDate;
    }

    public LocalDate getCurrentPeriodStart() {
        return currentPeriodStart;
    }

    public LocalDate getCurrentPeriodEnd() {
        return currentPeriodEnd;
    }

    public LocalDate getNextBillingDate() {
        return nextBillingDate;
    }

    public LocalDate getCommitmentEndDate() {
        return commitmentEndDate;
    }

    public int getGraceDays() {
        return graceDays;
    }

    public LocalDate getPastDueSince() {
        return pastDueSince;
    }

    public boolean isAutoRenew() {
        return autoRenew;
    }

    public CancellationRequest getCancellation() {
        return cancellation;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Long getVersion() {
        return version;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
