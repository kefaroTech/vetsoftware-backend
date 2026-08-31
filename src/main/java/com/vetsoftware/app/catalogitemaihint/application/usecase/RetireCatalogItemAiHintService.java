package com.vetsoftware.app.catalogitemaihint.application.usecase;

import com.vetsoftware.app.catalogitemaihint.application.command.RetireCatalogItemAiHintCommand;
import com.vetsoftware.app.catalogitemaihint.application.port.in.RetireCatalogItemAiHintUseCase;
import com.vetsoftware.app.catalogitemaihint.application.port.out.CatalogItemAiHintRepository;
import com.vetsoftware.app.catalogitemaihint.domain.CatalogItemAiHint;
import com.vetsoftware.app.catalogitemaihint.domain.CatalogItemAiHintNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Retira la pista de un articulo: cierra la vigente y no publica sucesora.
 *
 * <p>
 * Es <em>media</em> correccion, literalmente la misma primera escritura de
 * {@link ReviseCatalogItemAiHintService} sin la segunda. Que el efecto de
 * negocio sea el opuesto —el modelo deja de proponer el articulo— y la
 * escritura sea la misma es lo que hace que las dos operaciones tengan que ser
 * puertos distintos: si fueran una sola con el texto opcional, un cuerpo mal
 * formado retiraria la pista en vez de fallar.
 *
 * <p>
 * <strong>Y es la mitad que hasta el changeset 393 no dejaba firma.</strong>
 * Corregir escribia un firmante nuevo en la revision sucesora; retirar no
 * escribia ninguno, asi que la fila retirada seguia mostrando al que la habia
 * <em>publicado</em> y quien decidio apagar la pista no constaba en ningun
 * sitio. Ahora el actor llega en el command —desde la sesion, no desde el
 * cuerpo— y viaja hasta {@code superseded_by_system_user_id}.
 */
@Observed(name = "catalogitemaihint.retire")
@Service
public class RetireCatalogItemAiHintService implements RetireCatalogItemAiHintUseCase {

    private final CatalogItemAiHintRepository repository;
    private final Clock clock;

    public RetireCatalogItemAiHintService(CatalogItemAiHintRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void retire(RetireCatalogItemAiHintCommand command) {
        Long catalogItemId = command.catalogItemId();
        CatalogItemAiHint vigente = repository.findCurrentByCatalogItemId(catalogItemId)
                .orElseThrow(() -> new CatalogItemAiHintNotFoundException(catalogItemId));
        vigente.supersede(LocalDateTime.now(clock), command.retiredBySystemUserId());
        repository.supersede(vigente);
    }
}
