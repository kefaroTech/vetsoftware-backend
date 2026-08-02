package com.vetsoftware.app.petshopcatalog.application;

import com.vetsoftware.app.catalog.domain.SellableItemType;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface PetshopCatalogUseCase {
    record UnitMeasureDto(String code, String name, String symbol) {
    }

    record PresentationWrite(Long productId, String name, String unitMeasureCode,
            Integer conversionFactor, BigDecimal salePrice, Boolean defaultPresentation,
            List<String> barcodes, Long expectedVersion) {
    }

    record PresentationDto(Long id, Long productId, String productName, String name,
            String unitMeasureCode, int conversionFactor, BigDecimal salePrice,
            boolean defaultPresentation, List<String> barcodes, Long version) {
    }

    record BundleItemWrite(Long presentationId, Integer quantity, Integer displayOrder) {
    }

    record BundleWrite(String name, String code, String unitMeasureCode, BigDecimal salePrice,
            List<BundleItemWrite> items, List<String> barcodes, Long expectedVersion) {
    }

    record BundleItemDto(Long presentationId, String presentationName, String productName,
            int quantity, int displayOrder) {
    }

    record BundleDto(Long id, String name, String code, String unitMeasureCode,
            BigDecimal salePrice, List<BundleItemDto> items, List<String> barcodes, Long version) {
    }

    record BarcodeLookupDto(String barcode, SellableItemType itemType, Long itemId, String name,
            String unitMeasureCode, BigDecimal salePrice, Integer conversionFactor) {
    }

    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('product.read')")
    List<UnitMeasureDto> listUnitMeasures();

    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('product.read') and @authz.isMyCompany(#companyId))")
    List<PresentationDto> listPresentations(Long productId, Long companyId);

    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('product.create') and @authz.isMyCompany(#companyId))")
    PresentationDto createPresentation(PresentationWrite command, Long companyId, Long actorId);

    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('product.update') and @authz.isMyCompany(#companyId))")
    PresentationDto updatePresentation(Long id, PresentationWrite command, Long companyId,
            Long actorId);

    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('product.delete') and @authz.isMyCompany(#companyId))")
    void deletePresentation(Long id, Long expectedVersion, Long companyId);

    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('product.read') and @authz.isMyCompany(#companyId))")
    List<BundleDto> listBundles(Long companyId);

    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('product.create') and @authz.isMyCompany(#companyId))")
    BundleDto createBundle(BundleWrite command, Long companyId, Long actorId);

    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('product.update') and @authz.isMyCompany(#companyId))")
    BundleDto updateBundle(Long id, BundleWrite command, Long companyId, Long actorId);

    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('product.delete') and @authz.isMyCompany(#companyId))")
    void deleteBundle(Long id, Long expectedVersion, Long companyId);

    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('product.read') and @authz.isMyCompany(#companyId))")
    BarcodeLookupDto findByBarcode(String barcode, Long companyId);
}
