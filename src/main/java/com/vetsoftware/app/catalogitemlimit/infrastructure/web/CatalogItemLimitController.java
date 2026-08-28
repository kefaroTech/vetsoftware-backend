package com.vetsoftware.app.catalogitemlimit.infrastructure.web;

import com.vetsoftware.app.catalogitemlimit.application.command.CreateCatalogItemLimitCommand;
import com.vetsoftware.app.catalogitemlimit.application.command.UpdateCatalogItemLimitCommand;
import com.vetsoftware.app.catalogitemlimit.application.port.in.CreateCatalogItemLimitUseCase;
import com.vetsoftware.app.catalogitemlimit.application.port.in.ListCatalogItemLimitsUseCase;
import com.vetsoftware.app.catalogitemlimit.application.port.in.UpdateCatalogItemLimitUseCase;
import com.vetsoftware.app.catalogitemlimit.infrastructure.web.request.CreateCatalogItemLimitRequest;
import com.vetsoftware.app.catalogitemlimit.infrastructure.web.request.UpdateCatalogItemLimitRequest;
import com.vetsoftware.app.catalogitemlimit.infrastructure.web.response.CatalogItemLimitResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Los techos que un artículo del catálogo concede de fábrica.
 *
 * <p>
 * Cuelga de {@code /catalog-items/{catalogItemId}} como sus hermanas
 * {@code /sub-modules}, {@code /dependencies} y {@code /components}: el techo
 * no existe sin el artículo que lo concede.
 *
 * <p>
 * <strong>Sigue siendo catálogo global</strong>: ninguna de estas dos tablas
 * tiene {@code company_id} y ningún endpoint deriva empresa del principal. Los
 * tres puertos van cerrados a {@code hasRole('SYSTEM')} a secas —el listado
 * porque acota por artículo y una FK ajena no cuenta como filtro de empresa
 * ({@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}), y la edición porque el {@code id}
 * lo escribe el cliente y la fila no pertenece a nadie a quien revalidar—.
 *
 * <p>
 * <strong>El {@code {catalogItemId}} de la ruta del {@code PUT} se
 * comprueba.</strong> Durante un tiempo no fue así:
 * {@code UpdateCatalogItemLimitCommand} no llevaba el artículo —al contrario
 * que su hermano {@code UpdateBundleComponentCommand}—, de modo que editar el
 * techo del artículo 7 entrando por la ruta del 9 funcionaba y devolvía 200. No
 * era una fuga entre empresas —aquí no hay empresas y el gate es SYSTEM— pero
 * sí una URL que miente: cualquiera que la guardara o la compartiera estaba
 * documentando una operación distinta de la que ocurre. Hoy el artículo viaja
 * en el command y la carga se acota por el par, así que el desajuste responde
 * 404.
 */
@RestController
@RequestMapping("/catalog-items/{catalogItemId}/limits")
public class CatalogItemLimitController {

    private final CreateCatalogItemLimitUseCase createUseCase;
    private final UpdateCatalogItemLimitUseCase updateUseCase;
    private final ListCatalogItemLimitsUseCase listUseCase;

    public CatalogItemLimitController(CreateCatalogItemLimitUseCase createUseCase,
            UpdateCatalogItemLimitUseCase updateUseCase, ListCatalogItemLimitsUseCase listUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.listUseCase = listUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CatalogItemLimitResponse create(@PathVariable Long catalogItemId,
            @Valid @RequestBody CreateCatalogItemLimitRequest request) {
        return CatalogItemLimitResponse.from(createUseCase.execute(
                new CreateCatalogItemLimitCommand(catalogItemId, request.limitDimensionId(),
                        request.mode(), request.limitQuantity(), request.resetPeriod(),
                        request.enforcement(), request.overageUnitAmount(), request.warnThreshold(),
                        request.trialMode(), request.trialLimitQuantity())));
    }

    @GetMapping
    public List<CatalogItemLimitResponse> listByCatalogItem(@PathVariable Long catalogItemId) {
        return listUseCase.listByCatalogItemId(catalogItemId).stream()
                .map(CatalogItemLimitResponse::from).toList();
    }

    @PutMapping("/{id}")
    public CatalogItemLimitResponse update(@PathVariable Long catalogItemId, @PathVariable Long id,
            @Valid @RequestBody UpdateCatalogItemLimitRequest request) {
        return CatalogItemLimitResponse
                .from(updateUseCase.execute(new UpdateCatalogItemLimitCommand(catalogItemId, id,
                        request.mode(), request.limitQuantity(), request.resetPeriod(),
                        request.enforcement(), request.overageUnitAmount(), request.warnThreshold(),
                        request.trialMode(), request.trialLimitQuantity())));
    }
}
