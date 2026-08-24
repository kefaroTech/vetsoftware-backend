package com.vetsoftware.app.pricelist.application.usecase;

import com.vetsoftware.app.pricelist.application.command.PublishPriceListCommand;
import com.vetsoftware.app.pricelist.application.dto.PriceListDto;
import com.vetsoftware.app.pricelist.application.port.in.PublishPriceListUseCase;
import com.vetsoftware.app.pricelist.application.port.out.CatalogPriceRepository;
import com.vetsoftware.app.pricelist.application.port.out.PriceListRepository;
import com.vetsoftware.app.pricelist.domain.PriceList;
import com.vetsoftware.app.pricelist.domain.PriceListNotFoundException;
import com.vetsoftware.app.pricelist.domain.PriceListTierCoverage;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Congela la tarifa, y es la ULTIMA oportunidad de comprobar que esta completa.
 *
 * <p>
 * Publicar es irreversible en la practica: a partir de aqui manda R9 y corregir
 * un precio ya no es editarlo, es publicar otra lista y renegociar lo firmado.
 * Por eso la comprobacion de cobertura de tramos vive aqui y no en el alta de
 * cada precio (incidencia #378): en el alta seria imposible de satisfacer -el
 * primer tramo de una lista vacia ya seria un hueco- y despues de publicar
 * llegaria tarde.
 */
@Observed(name = "pricelist.publish")
@Service
public class PublishPriceListService implements PublishPriceListUseCase {

    private final PriceListRepository repository;
    private final CatalogPriceRepository catalogPriceRepository;
    private final Clock clock;

    public PublishPriceListService(PriceListRepository repository,
            CatalogPriceRepository catalogPriceRepository, Clock clock) {
        this.repository = repository;
        this.catalogPriceRepository = catalogPriceRepository;
        this.clock = clock;
    }

    /**
     * El orden importa: la cobertura se examina ANTES de mover el estado. Si se
     * comprobara despues, la excepcion abortaria la transaccion igual, pero el
     * dominio habria pasado por un estado PUBLISHED que nunca fue cierto y
     * cualquier {@code afterCommit} colgado del cambio de estado se habria
     * disparado sobre una lista que no se publico.
     */
    @Override
    @Transactional
    public PriceListDto execute(PublishPriceListCommand command) {
        PriceList priceList = repository.findById(command.id())
                .orElseThrow(() -> new PriceListNotFoundException(command.id()));
        PriceListTierCoverage.requireFullCoverage(command.id(),
                catalogPriceRepository.findAllTiers(command.id()));
        priceList.publish(command.publishedBySystemUserId(), LocalDateTime.now(clock));
        return PriceListDto.from(repository.save(priceList));
    }
}
