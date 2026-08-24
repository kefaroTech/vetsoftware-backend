package com.vetsoftware.app.subscription.application.port.in;

import com.vetsoftware.app.subscription.application.dto.SubscriptionItemOverlapDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * La consulta de vigilancia R7 expuesta como caso de uso, para el trabajo
 * programado diario y para correrla justo despues de cada despliegue que toque
 * las altas y bajas de linea, que es cuando se rompe.
 *
 * <p>
 * Barre todas las clinicas y no filtra por empresa, asi que solo la puede
 * servir {@code hasRole('SYSTEM')} a secas (BE-29). <strong>Cero filas =
 * sano.</strong>
 */
public interface FindOverlappingSubscriptionItemsUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    List<SubscriptionItemOverlapDto> findAllOverlaps();
}
