package com.vetsoftware.app.platformbillingconfig.application.dto;

import com.vetsoftware.app.platformbillingconfig.domain.PlatformBillingConfig;
import java.time.LocalDateTime;

/**
 * Las políticas de facturación tal como salen de la aplicación.
 *
 * <p>
 * No lleva {@code enabled}: la tabla no tiene borrado lógico (choque C5). No
 * lleva ningún campo de corte de acceso: el máximo estado de restricción del
 * producto es solo lectura.
 */
public record PlatformBillingConfigDto(Long id, PriceListSummaryDto defaultPriceList,
        int defaultGraceDays, int defaultTrialDays, int invoiceDayOfMonth,
        int defaultPaymentTermDays, String externalBillingProvider, LocalDateTime createdDate) {

    public static PlatformBillingConfigDto from(PlatformBillingConfig config) {
        return new PlatformBillingConfigDto(config.getId(),
                PriceListSummaryDto.from(config.getDefaultPriceList()),
                config.getDefaultGraceDays(), config.getDefaultTrialDays(),
                config.getInvoiceDayOfMonth(), config.getDefaultPaymentTermDays(),
                config.getExternalBillingProvider(), config.getCreatedDate());
    }
}
