package com.vetsoftware.app.catalogitemlimit.application.usecase;

import com.vetsoftware.app.catalogitemlimit.application.command.UpdateCatalogItemLimitCommand;
import com.vetsoftware.app.catalogitemlimit.application.dto.CatalogItemLimitDto;
import com.vetsoftware.app.catalogitemlimit.application.port.in.UpdateCatalogItemLimitUseCase;
import com.vetsoftware.app.catalogitemlimit.application.port.out.CatalogItemLimitRepository;
import com.vetsoftware.app.catalogitemlimit.domain.CatalogItemLimit;
import com.vetsoftware.app.catalogitemlimit.domain.CatalogItemLimitNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cambia el techo de fábrica.
 *
 * <p>
 * <strong>Carga por el par (techo, artículo) y no por el id suelto.</strong> No
 * hay empresa con la que acotar —la tabla es catálogo global y su puerto está
 * cerrado a un principal cross-tenant—, pero sí hay un padre que la ruta nombra
 * y que hasta ahora no se comprobaba: editar el techo del artículo 7 entrando
 * por {@code /catalog-items/9/limits/{id}} funcionaba. Con la carga acotada,
 * ese desajuste responde 404 en vez de aplicar el cambio y devolver un 200 que
 * confirma una operación distinta de la que la URL decía.
 */
@Service
public class UpdateCatalogItemLimitService implements UpdateCatalogItemLimitUseCase {

    private final CatalogItemLimitRepository repository;

    public UpdateCatalogItemLimitService(CatalogItemLimitRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public CatalogItemLimitDto execute(UpdateCatalogItemLimitCommand command) {
        CatalogItemLimit limit = repository
                .findByIdAndCatalogItemId(command.id(), command.catalogItemId())
                .orElseThrow(() -> new CatalogItemLimitNotFoundException(command.id()));
        limit.update(command.mode(), command.limitQuantity(), command.resetPeriod(),
                command.enforcement(), command.overageUnitAmount(), command.warnThreshold(),
                command.trialMode(), command.trialLimitQuantity());
        return CatalogItemLimitDto.from(repository.save(limit));
    }
}
