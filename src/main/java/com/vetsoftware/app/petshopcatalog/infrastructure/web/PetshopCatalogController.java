package com.vetsoftware.app.petshopcatalog.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.petshopcatalog.application.PetshopCatalogUseCase;
import com.vetsoftware.app.petshopcatalog.application.PetshopCatalogUseCase.BarcodeLookupDto;
import com.vetsoftware.app.petshopcatalog.application.PetshopCatalogUseCase.BundleDto;
import com.vetsoftware.app.petshopcatalog.application.PetshopCatalogUseCase.BundleItemWrite;
import com.vetsoftware.app.petshopcatalog.application.PetshopCatalogUseCase.BundleWrite;
import com.vetsoftware.app.petshopcatalog.application.PetshopCatalogUseCase.PresentationDto;
import com.vetsoftware.app.petshopcatalog.application.PetshopCatalogUseCase.PresentationWrite;
import com.vetsoftware.app.petshopcatalog.application.PetshopCatalogUseCase.UnitMeasureDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/petshop-catalog")
public class PetshopCatalogController {
    public record PresentationRequest(@NotNull Long productId,
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 10) String unitMeasureCode,
            @NotNull @Positive Integer conversionFactor,
            @NotNull @DecimalMin("0.00") BigDecimal salePrice, @NotNull Boolean defaultPresentation,
            List<@NotBlank @Size(max = 64) String> barcodes, Long expectedVersion) {
    }

    public record BundleItemRequest(@NotNull Long presentationId,
            @NotNull @Positive Integer quantity, @NotNull @PositiveOrZero Integer displayOrder) {
    }

    public record BundleRequest(@NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 60) String code, @NotBlank @Size(max = 10) String unitMeasureCode,
            @NotNull @DecimalMin("0.00") BigDecimal salePrice,
            @NotEmpty List<@Valid BundleItemRequest> items,
            List<@NotBlank @Size(max = 64) String> barcodes, Long expectedVersion) {
    }

    private final PetshopCatalogUseCase useCase;
    private final Authz authz;

    public PetshopCatalogController(PetshopCatalogUseCase useCase, Authz authz) {
        this.useCase = useCase;
        this.authz = authz;
    }

    @GetMapping("/unit-measures")
    public List<UnitMeasureDto> listUnitMeasures() {
        return useCase.listUnitMeasures();
    }

    @GetMapping("/presentations")
    public List<PresentationDto> listPresentations(@RequestParam Long productId) {
        return useCase.listPresentations(productId, authz.currentCompanyId());
    }

    @PostMapping("/presentations")
    @ResponseStatus(HttpStatus.CREATED)
    public PresentationDto createPresentation(@Valid @RequestBody PresentationRequest request) {
        return useCase.createPresentation(toCommand(request), authz.currentCompanyId(),
                authz.currentEmployeeIdOrNull());
    }

    @PutMapping("/presentations/{id}")
    public PresentationDto updatePresentation(@PathVariable Long id,
            @Valid @RequestBody PresentationRequest request) {
        return useCase.updatePresentation(id, toCommand(request), authz.currentCompanyId(),
                authz.currentEmployeeIdOrNull());
    }

    @DeleteMapping("/presentations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePresentation(@PathVariable Long id, @RequestParam Long expectedVersion) {
        useCase.deletePresentation(id, expectedVersion, authz.currentCompanyId());
    }

    @GetMapping("/bundles")
    public List<BundleDto> listBundles() {
        return useCase.listBundles(authz.currentCompanyId());
    }

    @PostMapping("/bundles")
    @ResponseStatus(HttpStatus.CREATED)
    public BundleDto createBundle(@Valid @RequestBody BundleRequest request) {
        return useCase.createBundle(toCommand(request), authz.currentCompanyId(),
                authz.currentEmployeeIdOrNull());
    }

    @PutMapping("/bundles/{id}")
    public BundleDto updateBundle(@PathVariable Long id,
            @Valid @RequestBody BundleRequest request) {
        return useCase.updateBundle(id, toCommand(request), authz.currentCompanyId(),
                authz.currentEmployeeIdOrNull());
    }

    @DeleteMapping("/bundles/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBundle(@PathVariable Long id, @RequestParam Long expectedVersion) {
        useCase.deleteBundle(id, expectedVersion, authz.currentCompanyId());
    }

    @GetMapping("/barcodes")
    public BarcodeLookupDto findByBarcode(@RequestParam String value) {
        return useCase.findByBarcode(value, authz.currentCompanyId());
    }

    private PresentationWrite toCommand(PresentationRequest request) {
        return new PresentationWrite(request.productId(), request.name(), request.unitMeasureCode(),
                request.conversionFactor(), request.salePrice(), request.defaultPresentation(),
                request.barcodes(), request.expectedVersion());
    }

    private BundleWrite toCommand(BundleRequest request) {
        return new BundleWrite(request.name(), request.code(), request.unitMeasureCode(),
                request.salePrice(),
                request.items().stream()
                        .map(item -> new BundleItemWrite(item.presentationId(), item.quantity(),
                                item.displayOrder()))
                        .toList(),
                request.barcodes(), request.expectedVersion());
    }
}
