package com.vetsoftware.app.pricelist.infrastructure.web;

import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.pricelist.application.command.CreateCatalogPriceCommand;
import com.vetsoftware.app.pricelist.application.command.UpdateCatalogPriceCommand;
import com.vetsoftware.app.pricelist.application.dto.CatalogPriceDto;
import com.vetsoftware.app.pricelist.application.port.in.CreateCatalogPriceUseCase;
import com.vetsoftware.app.pricelist.application.port.in.DeleteCatalogPriceUseCase;
import com.vetsoftware.app.pricelist.application.port.in.FindCatalogPriceUseCase;
import com.vetsoftware.app.pricelist.application.port.in.ListCatalogPricesUseCase;
import com.vetsoftware.app.pricelist.application.port.in.UpdateCatalogPriceUseCase;
import com.vetsoftware.app.pricelist.infrastructure.web.request.CreateCatalogPriceRequest;
import com.vetsoftware.app.pricelist.infrastructure.web.request.UpdateCatalogPriceRequest;
import com.vetsoftware.app.pricelist.infrastructure.web.response.CatalogItemSummary;
import com.vetsoftware.app.pricelist.infrastructure.web.response.CatalogPriceResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * El precio de un artículo dentro de una lista. Se lista siempre acotado por
 * {@code priceListId} — que no es un filtro de empresa, porque aquí no hay
 * empresa — y el gate sigue siendo {@code hasRole('SYSTEM')}.
 */
@RestController
@RequestMapping("/catalog-prices")
public class CatalogPriceController {

    private final CreateCatalogPriceUseCase createUseCase;
    private final UpdateCatalogPriceUseCase updateUseCase;
    private final FindCatalogPriceUseCase findUseCase;
    private final ListCatalogPricesUseCase listUseCase;
    private final DeleteCatalogPriceUseCase deleteUseCase;

    public CatalogPriceController(CreateCatalogPriceUseCase createUseCase,
            UpdateCatalogPriceUseCase updateUseCase, FindCatalogPriceUseCase findUseCase,
            ListCatalogPricesUseCase listUseCase, DeleteCatalogPriceUseCase deleteUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CatalogPriceResponse create(@Valid @RequestBody CreateCatalogPriceRequest request) {
        return toResponse(createUseCase.execute(new CreateCatalogPriceCommand(request.priceListId(),
                request.catalogItemId(), request.billingCycle(), request.tierMin(),
                request.tierMax(), request.includedQuantity(), request.unitAmount(),
                request.setupAmount(), request.taxRate(), request.taxTreatment())));
    }

    @GetMapping
    public PageResponse<CatalogPriceResponse> listByPriceList(@RequestParam Long priceListId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listUseCase.listByPriceList(priceListId, page, pageSize),
                this::toResponse);
    }

    @GetMapping("/{id}")
    public CatalogPriceResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id));
    }

    @PutMapping("/{id}")
    public CatalogPriceResponse update(@PathVariable Long id,
            @Valid @RequestBody UpdateCatalogPriceRequest request) {
        return toResponse(updateUseCase.execute(
                new UpdateCatalogPriceCommand(id, request.billingCycle(), request.tierMin(),
                        request.tierMax(), request.includedQuantity(), request.unitAmount(),
                        request.setupAmount(), request.taxRate(), request.taxTreatment())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id);
    }

    private CatalogPriceResponse toResponse(CatalogPriceDto dto) {
        return new CatalogPriceResponse(dto.id(), dto.priceListId(), dto.catalogItemId(),
                dto.billingCycle(), dto.tierMin(), dto.tierMax(), dto.includedQuantity(),
                dto.unitAmount(), dto.setupAmount(), dto.taxRate(), dto.taxTreatment(),
                dto.createdDate(), dto.enabled(), toCatalogItemSummary(dto));
    }

    private static CatalogItemSummary toCatalogItemSummary(CatalogPriceDto dto) {
        return dto.catalogItem() == null
                ? null
                : new CatalogItemSummary(dto.catalogItem().id(), dto.catalogItem().code(),
                        dto.catalogItem().name());
    }
}
