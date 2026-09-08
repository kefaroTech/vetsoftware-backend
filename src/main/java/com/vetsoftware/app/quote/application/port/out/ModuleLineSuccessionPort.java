package com.vetsoftware.app.quote.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Sucesión física de líneas de {@code subscription_items} para un artículo de
 * la empresa: cerrar una línea y abrir la sucesora sin pasar por el otrosí
 * general de {@code subscription}, que no sabe fechar una apertura futura ni
 * encadenar {@code succeeds_item_id}. Solo cubre el camino {@code TRIAL} con
 * fecha futura y sin cobro; abrir una línea de pago hoy va por
 * {@link ImmediatePaidLineOpeningPort}, y {@link #findNextBillingDate} es la
 * fecha a devolver como {@code firstChargeDate} en ese caso.
 */
public interface ModuleLineSuccessionPort {

    Optional<CurrentLine> findCurrentLine(Long companyId, Long catalogItemId);

    Optional<Long> findCurrentSubscriptionId(Long companyId);

    Optional<LocalDate> findNextBillingDate(Long companyId);

    void closeLine(Long companyId, Long itemId, LocalDate closeDate);

    /**
     * Cierra {@code priorLineId} (si no es {@code null}) en {@code closeDate} y
     * abre, con el mismo instante como {@code effectiveFrom}, una línea
     * {@code PAID} nueva con los términos congelados en {@code price} y
     * {@code succeedsItemId = priorLineId}. Sin cobro: el devengo lo recoge el
     * corte de ciclo normal del contrato cuando alcance esa fecha.
     *
     * @return el id de la línea nueva
     */
    Long succeedLine(Long companyId, Long subscriptionId, Long priorLineId, LocalDate closeDate,
            LocalDate newEffectiveFrom, LinePriceSnapshot price);

    record CurrentLine(Long itemId, Long subscriptionId, String chargeMode,
            LocalDate trialEndDate) {
    }

    record LinePriceSnapshot(Long catalogItemId, String itemCode, String itemName, String itemType,
            int includedQuantity, String taxTreatment, BigDecimal unitAmount, BigDecimal taxRate) {
    }
}
