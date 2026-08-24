package com.vetsoftware.app.subscription.application.usecase;

import com.vetsoftware.app.subscription.application.dto.SubscriptionItemOverlapDto;
import com.vetsoftware.app.subscription.application.port.in.FindOverlappingSubscriptionItemsUseCase;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionItemRepository;
import java.util.List;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

/**
 * La vigilancia R7 como caso de uso, para el trabajo programado diario y para
 * despues de cada despliegue que toque altas y bajas de linea. Cero filas =
 * sano; cualquier fila es un incidente de doble facturacion.
 */
@Observed(name = "subscription.item.find.overlapping")
@Service
public class FindOverlappingSubscriptionItemsService
        implements
            FindOverlappingSubscriptionItemsUseCase {

    private final SubscriptionItemRepository repository;

    public FindOverlappingSubscriptionItemsService(SubscriptionItemRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<SubscriptionItemOverlapDto> findAllOverlaps() {
        return repository.findAllOverlaps();
    }
}
