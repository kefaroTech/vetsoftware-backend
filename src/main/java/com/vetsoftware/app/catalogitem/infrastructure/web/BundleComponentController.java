package com.vetsoftware.app.catalogitem.infrastructure.web;

import com.vetsoftware.app.catalogitem.application.command.CreateBundleComponentCommand;
import com.vetsoftware.app.catalogitem.application.command.UpdateBundleComponentCommand;
import com.vetsoftware.app.catalogitem.application.dto.BundleComponentDto;
import com.vetsoftware.app.catalogitem.application.port.in.CreateBundleComponentUseCase;
import com.vetsoftware.app.catalogitem.application.port.in.DeleteBundleComponentUseCase;
import com.vetsoftware.app.catalogitem.application.port.in.ListBundleComponentsUseCase;
import com.vetsoftware.app.catalogitem.application.port.in.UpdateBundleComponentUseCase;
import com.vetsoftware.app.catalogitem.infrastructure.web.request.CreateBundleComponentRequest;
import com.vetsoftware.app.catalogitem.infrastructure.web.request.UpdateBundleComponentRequest;
import com.vetsoftware.app.catalogitem.infrastructure.web.response.BundleComponentResponse;
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

/** Qué trae un paquete. Aquí es donde aterrizan los planes comerciales. */
@RestController
@RequestMapping("/catalog-items/{bundleItemId}/components")
public class BundleComponentController {

    private final CreateBundleComponentUseCase createUseCase;
    private final UpdateBundleComponentUseCase updateUseCase;
    private final ListBundleComponentsUseCase listUseCase;
    private final DeleteBundleComponentUseCase deleteUseCase;

    public BundleComponentController(CreateBundleComponentUseCase createUseCase,
            UpdateBundleComponentUseCase updateUseCase, ListBundleComponentsUseCase listUseCase,
            DeleteBundleComponentUseCase deleteUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BundleComponentResponse create(@PathVariable Long bundleItemId,
            @Valid @RequestBody CreateBundleComponentRequest request) {
        return toResponse(createUseCase.execute(new CreateBundleComponentCommand(bundleItemId,
                request.componentItemId(), request.quantity())));
    }

    @GetMapping
    public List<BundleComponentResponse> listByBundle(@PathVariable Long bundleItemId) {
        return listUseCase.listByBundle(bundleItemId).stream().map(this::toResponse).toList();
    }

    @PutMapping("/{id}")
    public BundleComponentResponse update(@PathVariable Long bundleItemId, @PathVariable Long id,
            @Valid @RequestBody UpdateBundleComponentRequest request) {
        return toResponse(updateUseCase
                .execute(new UpdateBundleComponentCommand(id, bundleItemId, request.quantity())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long bundleItemId, @PathVariable Long id) {
        deleteUseCase.execute(bundleItemId, id);
    }

    private BundleComponentResponse toResponse(BundleComponentDto dto) {
        return new BundleComponentResponse(dto.id(), dto.bundleItemId(), dto.componentItemId(),
                dto.quantity(), dto.createdDate(), dto.enabled(), dto.outcome());
    }
}
