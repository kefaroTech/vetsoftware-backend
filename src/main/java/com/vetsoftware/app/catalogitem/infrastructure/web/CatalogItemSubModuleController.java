package com.vetsoftware.app.catalogitem.infrastructure.web;

import com.vetsoftware.app.catalogitem.application.command.CreateCatalogItemSubModuleCommand;
import com.vetsoftware.app.catalogitem.application.dto.CatalogItemSubModuleDto;
import com.vetsoftware.app.catalogitem.application.dto.SubModuleSummaryDto;
import com.vetsoftware.app.catalogitem.application.port.in.CreateCatalogItemSubModuleUseCase;
import com.vetsoftware.app.catalogitem.application.port.in.DeleteCatalogItemSubModuleUseCase;
import com.vetsoftware.app.catalogitem.application.port.in.ListCatalogItemSubModulesUseCase;
import com.vetsoftware.app.catalogitem.infrastructure.web.request.CreateCatalogItemSubModuleRequest;
import com.vetsoftware.app.catalogitem.infrastructure.web.response.CatalogItemSubModuleResponse;
import com.vetsoftware.app.catalogitem.infrastructure.web.response.SubModuleSummary;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Qué submódulos abre un artículo del catálogo. */
@RestController
@RequestMapping("/catalog-items/{catalogItemId}/sub-modules")
public class CatalogItemSubModuleController {

    private final CreateCatalogItemSubModuleUseCase createUseCase;
    private final ListCatalogItemSubModulesUseCase listUseCase;
    private final DeleteCatalogItemSubModuleUseCase deleteUseCase;

    public CatalogItemSubModuleController(CreateCatalogItemSubModuleUseCase createUseCase,
            ListCatalogItemSubModulesUseCase listUseCase,
            DeleteCatalogItemSubModuleUseCase deleteUseCase) {
        this.createUseCase = createUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CatalogItemSubModuleResponse create(@PathVariable Long catalogItemId,
            @Valid @RequestBody CreateCatalogItemSubModuleRequest request) {
        return toResponse(createUseCase.execute(
                new CreateCatalogItemSubModuleCommand(catalogItemId, request.subModuleId())));
    }

    @GetMapping
    public List<CatalogItemSubModuleResponse> listByCatalogItem(@PathVariable Long catalogItemId) {
        return listUseCase.listByCatalogItem(catalogItemId).stream().map(this::toResponse).toList();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long catalogItemId, @PathVariable Long id) {
        deleteUseCase.execute(catalogItemId, id);
    }

    private CatalogItemSubModuleResponse toResponse(CatalogItemSubModuleDto dto) {
        SubModuleSummaryDto subModule = dto.subModule();
        return new CatalogItemSubModuleResponse(dto.id(), dto.catalogItemId(),
                new SubModuleSummary(subModule.id(), subModule.name(), subModule.code()),
                dto.createdDate(), dto.enabled(), dto.outcome());
    }
}
