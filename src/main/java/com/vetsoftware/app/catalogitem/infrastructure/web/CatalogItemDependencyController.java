package com.vetsoftware.app.catalogitem.infrastructure.web;

import com.vetsoftware.app.catalogitem.application.command.CreateCatalogItemDependencyCommand;
import com.vetsoftware.app.catalogitem.application.command.UpdateCatalogItemDependencyCommand;
import com.vetsoftware.app.catalogitem.application.dto.CatalogItemDependencyDto;
import com.vetsoftware.app.catalogitem.application.port.in.CreateCatalogItemDependencyUseCase;
import com.vetsoftware.app.catalogitem.application.port.in.DeleteCatalogItemDependencyUseCase;
import com.vetsoftware.app.catalogitem.application.port.in.ListCatalogItemDependenciesUseCase;
import com.vetsoftware.app.catalogitem.application.port.in.UpdateCatalogItemDependencyUseCase;
import com.vetsoftware.app.catalogitem.infrastructure.web.request.CreateCatalogItemDependencyRequest;
import com.vetsoftware.app.catalogitem.infrastructure.web.request.UpdateCatalogItemDependencyRequest;
import com.vetsoftware.app.catalogitem.infrastructure.web.response.CatalogItemDependencyResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Las reglas del configurador entre artículos. El alta y la edición pueden
 * responder 409 con {@code CATALOG_ITEM_DEPENDENCY_CYCLE} cuando el arco
 * {@code REQUIRES} cerraría un bucle (regla R16).
 */
@RestController
@RequestMapping("/catalog-items/{catalogItemId}/dependencies")
public class CatalogItemDependencyController {

    private final CreateCatalogItemDependencyUseCase createUseCase;
    private final UpdateCatalogItemDependencyUseCase updateUseCase;
    private final ListCatalogItemDependenciesUseCase listUseCase;
    private final DeleteCatalogItemDependencyUseCase deleteUseCase;

    public CatalogItemDependencyController(CreateCatalogItemDependencyUseCase createUseCase,
            UpdateCatalogItemDependencyUseCase updateUseCase,
            ListCatalogItemDependenciesUseCase listUseCase,
            DeleteCatalogItemDependencyUseCase deleteUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CatalogItemDependencyResponse create(@PathVariable Long catalogItemId,
            @Valid @RequestBody CreateCatalogItemDependencyRequest request) {
        return toResponse(createUseCase.execute(new CreateCatalogItemDependencyCommand(
                catalogItemId, request.relatedItemId(), request.relationType(), request.note())));
    }

    @GetMapping
    public List<CatalogItemDependencyResponse> listByCatalogItem(@PathVariable Long catalogItemId) {
        return listUseCase.listByCatalogItem(catalogItemId).stream().map(this::toResponse).toList();
    }

    @PutMapping("/{id}")
    public CatalogItemDependencyResponse update(@PathVariable Long catalogItemId,
            @PathVariable Long id, @Valid @RequestBody UpdateCatalogItemDependencyRequest request) {
        return toResponse(updateUseCase.execute(new UpdateCatalogItemDependencyCommand(id,
                catalogItemId, request.relationType(), request.note())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long catalogItemId, @PathVariable Long id) {
        deleteUseCase.execute(catalogItemId, id);
    }

    private CatalogItemDependencyResponse toResponse(CatalogItemDependencyDto dto) {
        return new CatalogItemDependencyResponse(dto.id(), dto.catalogItemId(), dto.relatedItemId(),
                dto.relationType(), dto.note(), dto.createdDate(), dto.enabled(), dto.outcome());
    }
}
