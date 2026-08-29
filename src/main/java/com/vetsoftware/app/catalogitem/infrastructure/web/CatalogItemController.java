package com.vetsoftware.app.catalogitem.infrastructure.web;

import com.vetsoftware.app.catalogitem.application.command.CreateCatalogItemCommand;
import com.vetsoftware.app.catalogitem.application.command.UpdateCatalogItemCommand;
import com.vetsoftware.app.catalogitem.application.dto.CatalogItemDto;
import com.vetsoftware.app.catalogitem.application.port.in.CreateCatalogItemUseCase;
import com.vetsoftware.app.catalogitem.application.port.in.DeleteCatalogItemUseCase;
import com.vetsoftware.app.catalogitem.application.port.in.FindCatalogItemUseCase;
import com.vetsoftware.app.catalogitem.application.port.in.ListCatalogItemsUseCase;
import com.vetsoftware.app.catalogitem.application.port.in.ReactivateCatalogItemUseCase;
import com.vetsoftware.app.catalogitem.application.port.in.UpdateCatalogItemUseCase;
import com.vetsoftware.app.catalogitem.infrastructure.web.request.CreateCatalogItemRequest;
import com.vetsoftware.app.catalogitem.infrastructure.web.request.UpdateCatalogItemRequest;
import com.vetsoftware.app.catalogitem.infrastructure.web.response.CatalogItemResponse;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * El catálogo comercial de la plataforma.
 *
 * <p>
 * Ningún endpoint toma la empresa del principal y ninguno la recibe en el
 * cuerpo: estas cuatro tablas no tienen tenant. La autorización vive entera en
 * el {@code @PreAuthorize} de cada puerto de entrada, que es
 * {@code hasRole("SYSTEM")} a secas.
 */
@RestController
@RequestMapping("/catalog-items")
public class CatalogItemController {

    /** Defaults de la ficha 1 para los campos que el cuerpo puede omitir. */
    private static final int MIN_QUANTITY_DEFAULT = 1;
    private static final int SORT_ORDER_DEFAULT = 0;

    private final CreateCatalogItemUseCase createUseCase;
    private final UpdateCatalogItemUseCase updateUseCase;
    private final FindCatalogItemUseCase findUseCase;
    private final ListCatalogItemsUseCase listUseCase;
    private final DeleteCatalogItemUseCase deleteUseCase;
    private final ReactivateCatalogItemUseCase reactivateUseCase;

    public CatalogItemController(CreateCatalogItemUseCase createUseCase,
            UpdateCatalogItemUseCase updateUseCase, FindCatalogItemUseCase findUseCase,
            ListCatalogItemsUseCase listUseCase, DeleteCatalogItemUseCase deleteUseCase,
            ReactivateCatalogItemUseCase reactivateUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
        this.reactivateUseCase = reactivateUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CatalogItemResponse create(@Valid @RequestBody CreateCatalogItemRequest request) {
        return toResponse(createUseCase.execute(new CreateCatalogItemCommand(request.code(),
                request.name(), request.shortDescription(), request.longDescription(),
                request.itemType(), request.capacityUnit(), request.core(),
                orDefault(request.minQuantity(), MIN_QUANTITY_DEFAULT), request.maxQuantity(),
                orDefault(request.sortOrder(), SORT_ORDER_DEFAULT), request.status())));
    }

    @GetMapping
    public PageResponse<CatalogItemResponse> listAll(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listUseCase.listAll(page, pageSize), this::toResponse);
    }

    @GetMapping("/{id}")
    public CatalogItemResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id));
    }

    @PutMapping("/{id}")
    public CatalogItemResponse update(@PathVariable Long id,
            @Valid @RequestBody UpdateCatalogItemRequest request) {
        return toResponse(updateUseCase.execute(new UpdateCatalogItemCommand(id, request.name(),
                request.shortDescription(), request.longDescription(), request.itemType(),
                request.capacityUnit(), request.core(),
                orDefault(request.minQuantity(), MIN_QUANTITY_DEFAULT), request.maxQuantity(),
                orDefault(request.sortOrder(), SORT_ORDER_DEFAULT), request.status())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id);
    }

    @PatchMapping("/{id}/enable")
    public CatalogItemResponse enable(@PathVariable Long id) {
        return toResponse(reactivateUseCase.execute(id));
    }

    private static int orDefault(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private CatalogItemResponse toResponse(CatalogItemDto dto) {
        return new CatalogItemResponse(dto.id(), dto.code(), dto.name(), dto.shortDescription(),
                dto.longDescription(), dto.itemType(), dto.capacityUnit(), dto.core(),
                dto.minQuantity(), dto.maxQuantity(), dto.sortOrder(), dto.status(),
                dto.createdDate(), dto.enabled(), dto.defaultTrialDays());
    }
}
