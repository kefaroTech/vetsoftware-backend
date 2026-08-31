package com.vetsoftware.app.catalogitemaihint.application.usecase;

import com.vetsoftware.app.catalogitemaihint.application.dto.CatalogItemAiHintDto;
import com.vetsoftware.app.catalogitemaihint.application.port.in.ListCurrentCatalogItemAiHintsUseCase;
import com.vetsoftware.app.catalogitemaihint.application.port.out.CatalogItemAiHintRepository;
import com.vetsoftware.app.catalogitemaihint.application.port.out.CatalogItemQueryPort;
import com.vetsoftware.app.catalogitemaihint.domain.CatalogItemAiHint;
import com.vetsoftware.app.catalogitemaihint.domain.CatalogItemRef;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El listado de pistas vigentes de la consola de plataforma.
 *
 * <p>
 * <strong>Una consulta para los articulos de toda la pagina, no una por
 * fila.</strong> Resolver el codigo y el nombre dentro del {@code map} seria un
 * N+1 contra {@code catalog_items} que solo se nota cuando el catalogo crece,
 * que es justo cuando el listado importa.
 */
@Service
public class ListCurrentCatalogItemAiHintsService implements ListCurrentCatalogItemAiHintsUseCase {

    private final CatalogItemAiHintRepository repository;
    private final CatalogItemQueryPort catalogItemQueryPort;

    public ListCurrentCatalogItemAiHintsService(CatalogItemAiHintRepository repository,
            CatalogItemQueryPort catalogItemQueryPort) {
        this.repository = repository;
        this.catalogItemQueryPort = catalogItemQueryPort;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CatalogItemAiHintDto> listCurrent(int page, int pageSize) {
        PageResult<CatalogItemAiHint> pagina = repository.findAllCurrent(page, pageSize);
        Map<Long, CatalogItemRef> articulos = catalogItemQueryPort.findAllByIds(
                pagina.content().stream().map(CatalogItemAiHint::getCatalogItemId).toList());
        return pagina.map(
                hint -> CatalogItemAiHintDto.from(hint, articulos.get(hint.getCatalogItemId())));
    }
}
