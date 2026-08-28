package com.vetsoftware.app.subscriptionitemlimit.application.usecase;

import com.vetsoftware.app.subscriptionitemlimit.application.command.PropagateCatalogLimitImprovementCommand;
import com.vetsoftware.app.subscriptionitemlimit.application.port.in.PropagateCatalogLimitImprovementUseCase;
import com.vetsoftware.app.subscriptionitemlimit.application.port.out.SubscriptionItemLimitRepository;
import com.vetsoftware.app.subscriptionitemlimit.domain.SubscriptionItemLimit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Propaga una mejora del cupo de fábrica a los contratos vivos.
 *
 * <p>
 * <strong>Guarda solo lo que cambió.</strong> No es una optimización: escribir
 * las filas que no se movieron subiría su versión y su fecha, y el informe de
 * «qué techos se tocaron y cuándo» dejaría de distinguir una mejora real de un
 * barrido que pasó por encima.
 */
@Service
public class PropagateCatalogLimitImprovementService
        implements
            PropagateCatalogLimitImprovementUseCase {

    private final SubscriptionItemLimitRepository repository;

    public PropagateCatalogLimitImprovementService(SubscriptionItemLimitRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public int execute(PropagateCatalogLimitImprovementCommand command) {
        List<SubscriptionItemLimit> live = repository.findAllLiveByCatalogItemIdAndLimitDimensionId(
                command.catalogItemId(), command.limitDimensionId());
        List<SubscriptionItemLimit> improved = new ArrayList<>();
        for (SubscriptionItemLimit limit : live) {
            if (limit.improveFrom(command.factoryMode(), command.factoryLimitQuantity()))
                improved.add(limit);
        }
        if (improved.isEmpty())
            return 0;
        return repository.saveAll(improved).size();
    }
}
