package com.vetsoftware.app.catalogitemaihint.application.usecase;

import com.vetsoftware.app.catalogitemaihint.application.command.PublishCatalogItemAiHintCommand;
import com.vetsoftware.app.catalogitemaihint.application.dto.CatalogItemAiHintDto;
import com.vetsoftware.app.catalogitemaihint.application.port.in.PublishCatalogItemAiHintUseCase;
import com.vetsoftware.app.catalogitemaihint.application.port.out.CatalogItemAiHintRepository;
import com.vetsoftware.app.catalogitemaihint.application.port.out.CatalogItemQueryPort;
import com.vetsoftware.app.catalogitemaihint.domain.CatalogItemAiHint;
import com.vetsoftware.app.catalogitemaihint.domain.CatalogItemAiHintAlreadyPublishedException;
import com.vetsoftware.app.catalogitemaihint.domain.CatalogItemAiHintTextAlreadyPublishedException;
import com.vetsoftware.app.catalogitemaihint.domain.CatalogItemRef;
import com.vetsoftware.app.catalogitemaihint.domain.HintCatalogItemNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Publica la primera pista de un articulo.
 *
 * <p>
 * Las tres guardas de aqui arriba son las tres restricciones de la tabla, cada
 * una preguntada antes de que el motor la imponga —porque una violacion de
 * integridad sale como 500 y el cliente no la puede distinguir de una caida—:
 * la clave foranea del articulo ({@code fk_catalog_item_ai_hints_item}), la
 * unicidad de la vigente ({@code uq_catalog_item_ai_hints_current}) y la del
 * texto ({@code uq_catalog_item_ai_hints_text}).
 *
 * <p>
 * <strong>El numero de revision sale del ultimo publicado, no de cuantas hay
 * vigentes.</strong> La diferencia solo se ve en un caso, y es el que rompe:
 * despues de retirar, el articulo no tiene vigente pero su historial sigue
 * teniendo la revision 1, asi que reiniciar en 1 chocaria contra
 * {@code uq_catalog_item_ai_hints_revision}.
 */
@Observed(name = "catalogitemaihint.publish")
@Service
public class PublishCatalogItemAiHintService implements PublishCatalogItemAiHintUseCase {

    private final CatalogItemAiHintRepository repository;
    private final CatalogItemQueryPort catalogItemQueryPort;
    private final Clock clock;

    public PublishCatalogItemAiHintService(CatalogItemAiHintRepository repository,
            CatalogItemQueryPort catalogItemQueryPort, Clock clock) {
        this.repository = repository;
        this.catalogItemQueryPort = catalogItemQueryPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CatalogItemAiHintDto execute(PublishCatalogItemAiHintCommand command) {
        CatalogItemRef articulo = catalogItemQueryPort.findById(command.catalogItemId())
                .orElseThrow(() -> new HintCatalogItemNotFoundException(command.catalogItemId()));
        if (repository.findCurrentByCatalogItemId(command.catalogItemId()).isPresent()) {
            throw new CatalogItemAiHintAlreadyPublishedException(command.catalogItemId());
        }
        if (repository.existsPublishedText(command.catalogItemId(), command.hintText())) {
            throw new CatalogItemAiHintTextAlreadyPublishedException(command.catalogItemId());
        }
        LocalDateTime ahora = LocalDateTime.now(clock);
        int siguiente = repository.findLastRevision(command.catalogItemId()).orElse(0) + 1;
        CatalogItemAiHint nueva = CatalogItemAiHint.publish(command.catalogItemId(), siguiente,
                command.hintText(), command.publishedBySystemUserId(), ahora, ahora);
        return CatalogItemAiHintDto.from(repository.save(nueva), articulo);
    }
}
