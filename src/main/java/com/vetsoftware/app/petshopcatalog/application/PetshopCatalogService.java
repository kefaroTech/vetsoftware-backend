package com.vetsoftware.app.petshopcatalog.application;

import static com.vetsoftware.app.petshopcatalog.domain.PetshopCatalogRules.barcodes;
import static com.vetsoftware.app.petshopcatalog.domain.PetshopCatalogRules.defaultFactor;
import static com.vetsoftware.app.petshopcatalog.domain.PetshopCatalogRules.expectedVersion;
import static com.vetsoftware.app.petshopcatalog.domain.PetshopCatalogRules.nonNegative;
import static com.vetsoftware.app.petshopcatalog.domain.PetshopCatalogRules.positive;
import static com.vetsoftware.app.petshopcatalog.domain.PetshopCatalogRules.price;
import static com.vetsoftware.app.petshopcatalog.domain.PetshopCatalogRules.text;

import com.vetsoftware.app.catalog.domain.SellableItemType;
import com.vetsoftware.app.catalogbarcode.infrastructure.persistence.CatalogBarcodeJpaEntity;
import com.vetsoftware.app.catalogbarcode.infrastructure.persistence.CatalogBarcodeJpaRepository;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.petshopcatalog.domain.PetshopCatalogConflictException;
import com.vetsoftware.app.petshopcatalog.domain.PetshopCatalogNotFoundException;
import com.vetsoftware.app.product.infrastructure.persistence.ProductJpaEntity;
import com.vetsoftware.app.product.infrastructure.persistence.ProductJpaRepository;
import com.vetsoftware.app.productbundle.infrastructure.persistence.ProductBundleItemJpaEntity;
import com.vetsoftware.app.productbundle.infrastructure.persistence.ProductBundleItemJpaRepository;
import com.vetsoftware.app.productbundle.infrastructure.persistence.ProductBundleJpaEntity;
import com.vetsoftware.app.productbundle.infrastructure.persistence.ProductBundleJpaRepository;
import com.vetsoftware.app.productpresentation.infrastructure.persistence.ProductPresentationJpaEntity;
import com.vetsoftware.app.productpresentation.infrastructure.persistence.ProductPresentationJpaRepository;
import com.vetsoftware.app.unitmeasure.infrastructure.persistence.UnitMeasureCatalogJpaEntity;
import com.vetsoftware.app.unitmeasure.infrastructure.persistence.UnitMeasureCatalogJpaRepository;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PetshopCatalogService implements PetshopCatalogUseCase {
  private final CompanyJpaRepository companies;
  private final ProductJpaRepository products;
  private final UnitMeasureCatalogJpaRepository units;
  private final ProductPresentationJpaRepository presentations;
  private final ProductBundleJpaRepository bundles;
  private final ProductBundleItemJpaRepository bundleItems;
  private final CatalogBarcodeJpaRepository catalogBarcodes;

  public PetshopCatalogService(
      CompanyJpaRepository companies,
      ProductJpaRepository products,
      UnitMeasureCatalogJpaRepository units,
      ProductPresentationJpaRepository presentations,
      ProductBundleJpaRepository bundles,
      ProductBundleItemJpaRepository bundleItems,
      CatalogBarcodeJpaRepository catalogBarcodes) {
    this.companies = companies;
    this.products = products;
    this.units = units;
    this.presentations = presentations;
    this.bundles = bundles;
    this.bundleItems = bundleItems;
    this.catalogBarcodes = catalogBarcodes;
  }

  @Override
  @Transactional(readOnly = true)
  public List<UnitMeasureDto> listUnitMeasures() {
    return units.findAllByOrderByNameAsc().stream()
        .map(unit -> new UnitMeasureDto(unit.getCode(), unit.getName(), unit.getSymbol()))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<PresentationDto> listPresentations(Long productId, Long companyId) {
    requireProduct(productId, companyId);
    return presentations
        .findAllByCompany_IdAndProduct_IdOrderByDefaultPresentationDescNameAsc(companyId, productId)
        .stream()
        .map(this::toPresentationDto)
        .toList();
  }

  @Override
  public PresentationDto createPresentation(
      PresentationWrite command, Long companyId, Long actorId) {
    CompanyJpaEntity company = requireCompany(companyId);
    ProductJpaEntity product = requireProduct(command.productId(), companyId);
    String name = text(command.name(), "name", 120);
    String unitCode = text(command.unitMeasureCode(), "unitMeasureCode", 10);
    int factor = positive(command.conversionFactor(), "conversionFactor");
    boolean isDefault = Boolean.TRUE.equals(command.defaultPresentation());
    defaultFactor(isDefault, factor);
    if (presentations.existsByCompany_IdAndProduct_IdAndName(companyId, product.getId(), name)) {
      throw conflict(
          "PRESENTATION_NAME_ALREADY_EXISTS",
          "Ya existe una presentación con ese nombre para el producto.");
    }
    if (isDefault) clearPreviousDefault(companyId, product.getId(), null, actorId);

    ProductPresentationJpaEntity entity =
        ProductPresentationJpaEntity.create(
            company,
            product,
            name,
            requireUnit(unitCode),
            factor,
            price(command.salePrice()),
            isDefault,
            actorId);
    entity = presentations.save(entity);
    replacePresentationBarcodes(entity, company, command.barcodes(), actorId);
    if (isDefault) syncProductBase(product, entity);
    return toPresentationDto(entity);
  }

  @Override
  public PresentationDto updatePresentation(
      Long id, PresentationWrite command, Long companyId, Long actorId) {
    ProductPresentationJpaEntity entity = requirePresentation(id, companyId);
    expectedVersion(command.expectedVersion(), entity.getVersion());
    if (command.productId() != null && !command.productId().equals(entity.getProduct().getId())) {
      throw new IllegalArgumentException("A presentation cannot be moved to another product");
    }
    String name = text(command.name(), "name", 120);
    String unitCode = text(command.unitMeasureCode(), "unitMeasureCode", 10);
    int factor = positive(command.conversionFactor(), "conversionFactor");
    boolean requestedDefault = Boolean.TRUE.equals(command.defaultPresentation());
    if (entity.isDefaultPresentation() && !requestedDefault) {
      throw conflict(
          "DEFAULT_PRESENTATION_REQUIRED",
          "Asigna otra presentación predeterminada antes de desmarcar esta.");
    }
    defaultFactor(requestedDefault, factor);
    if (presentations.existsByCompany_IdAndProduct_IdAndNameAndIdNot(
        companyId, entity.getProduct().getId(), name, id)) {
      throw conflict(
          "PRESENTATION_NAME_ALREADY_EXISTS",
          "Ya existe una presentación con ese nombre para el producto.");
    }
    if (requestedDefault) {
      clearPreviousDefault(companyId, entity.getProduct().getId(), id, actorId);
    }
    entity.update(
        name, requireUnit(unitCode), factor, price(command.salePrice()), requestedDefault, actorId);
    replacePresentationBarcodes(entity, entity.getCompany(), command.barcodes(), actorId);
    if (requestedDefault) syncProductBase(entity.getProduct(), entity);
    return toPresentationDto(entity);
  }

  @Override
  public void deletePresentation(Long id, Long expectedVersion, Long companyId) {
    ProductPresentationJpaEntity entity = requirePresentation(id, companyId);
    expectedVersion(expectedVersion, entity.getVersion());
    if (entity.isDefaultPresentation()) {
      throw conflict(
          "DEFAULT_PRESENTATION_REQUIRED", "La presentación predeterminada no se puede eliminar.");
    }
    if (bundleItems.existsByCompany_IdAndPresentation_IdAndBundle_EnabledTrue(companyId, id)) {
      throw conflict(
          "PRESENTATION_IN_ACTIVE_BUNDLE", "La presentación pertenece a un combo activo.");
    }
    catalogBarcodes.deleteAll(
        catalogBarcodes.findAllByCompany_IdAndPresentation_IdOrderByBarcode(companyId, id));
    catalogBarcodes.flush();
    presentations.delete(entity);
  }

  @Override
  @Transactional(readOnly = true)
  public List<BundleDto> listBundles(Long companyId) {
    requireCompany(companyId);
    return bundles.findAllByCompany_IdOrderByNameAsc(companyId).stream()
        .map(this::toBundleDto)
        .toList();
  }

  @Override
  public BundleDto createBundle(BundleWrite command, Long companyId, Long actorId) {
    CompanyJpaEntity company = requireCompany(companyId);
    String name = text(command.name(), "name", 120);
    String code = text(command.code(), "code", 60);
    validateBundleUniqueness(companyId, null, name, code);
    List<BundleItemWrite> items = validateBundleItems(command.items());
    ProductBundleJpaEntity bundle =
        ProductBundleJpaEntity.create(
            company,
            name,
            code,
            requireUnit(text(command.unitMeasureCode(), "unitMeasureCode", 10)),
            price(command.salePrice()),
            actorId);
    bundle = bundles.save(bundle);
    replaceBundleItems(bundle, company, items);
    replaceBundleBarcodes(bundle, company, command.barcodes(), actorId);
    return toBundleDto(bundle);
  }

  @Override
  public BundleDto updateBundle(Long id, BundleWrite command, Long companyId, Long actorId) {
    ProductBundleJpaEntity bundle = requireBundle(id, companyId);
    expectedVersion(command.expectedVersion(), bundle.getVersion());
    String name = text(command.name(), "name", 120);
    String code = text(command.code(), "code", 60);
    validateBundleUniqueness(companyId, id, name, code);
    List<BundleItemWrite> items = validateBundleItems(command.items());
    bundle.update(
        name,
        code,
        requireUnit(text(command.unitMeasureCode(), "unitMeasureCode", 10)),
        price(command.salePrice()),
        actorId);
    replaceBundleItems(bundle, bundle.getCompany(), items);
    replaceBundleBarcodes(bundle, bundle.getCompany(), command.barcodes(), actorId);
    return toBundleDto(bundle);
  }

  @Override
  public void deleteBundle(Long id, Long expectedVersion, Long companyId) {
    ProductBundleJpaEntity bundle = requireBundle(id, companyId);
    expectedVersion(expectedVersion, bundle.getVersion());
    catalogBarcodes.deleteAll(
        catalogBarcodes.findAllByCompany_IdAndBundle_IdOrderByBarcode(companyId, id));
    catalogBarcodes.flush();
    bundles.delete(bundle);
  }

  @Override
  @Transactional(readOnly = true)
  public BarcodeLookupDto findByBarcode(String barcode, Long companyId) {
    String normalized = text(barcode, "barcode", 64);
    CatalogBarcodeJpaEntity found =
        catalogBarcodes
            .findByCompany_IdAndBarcode(companyId, normalized)
            .orElseThrow(() -> new PetshopCatalogNotFoundException("Barcode", normalized));
    if (found.getItemType() == SellableItemType.PRESENTATION) {
      ProductPresentationJpaEntity p = found.getPresentation();
      return new BarcodeLookupDto(
          normalized,
          found.getItemType(),
          p.getId(),
          p.getProduct().getName() + " - " + p.getName(),
          p.getUnitMeasure().getCode(),
          p.getSalePrice(),
          p.getConversionFactor());
    }
    ProductBundleJpaEntity b = found.getBundle();
    return new BarcodeLookupDto(
        normalized,
        found.getItemType(),
        b.getId(),
        b.getName(),
        b.getUnitMeasure().getCode(),
        b.getSalePrice(),
        null);
  }

  private void clearPreviousDefault(Long companyId, Long productId, Long exceptId, Long actorId) {
    ProductPresentationJpaEntity previous =
        presentations
            .findByCompany_IdAndProduct_IdAndDefaultPresentationTrue(companyId, productId)
            .filter(candidate -> exceptId == null || !exceptId.equals(candidate.getId()))
            .orElse(null);
    if (previous != null) {
      previous.markDefault(false, actorId);
      // Libera primero el índice único de presentación predeterminada.
      presentations.flush();
    }
  }

  private void syncProductBase(
      ProductJpaEntity product, ProductPresentationJpaEntity presentation) {
    product.setBaseUnitMeasureCode(presentation.getUnitMeasure().getCode());
    product.setSalePrice(presentation.getSalePrice());
    products.save(product);
  }

  private void validateBundleUniqueness(Long companyId, Long id, String name, String code) {
    boolean duplicateName =
        id == null
            ? bundles.existsByCompany_IdAndName(companyId, name)
            : bundles.existsByCompany_IdAndNameAndIdNot(companyId, name, id);
    boolean duplicateCode =
        id == null
            ? bundles.existsByCompany_IdAndCode(companyId, code)
            : bundles.existsByCompany_IdAndCodeAndIdNot(companyId, code, id);
    if (duplicateName) {
      throw conflict("BUNDLE_NAME_ALREADY_EXISTS", "Ya existe un combo con ese nombre.");
    }
    if (duplicateCode) {
      throw conflict("BUNDLE_CODE_ALREADY_EXISTS", "Ya existe un combo con ese código.");
    }
  }

  private List<BundleItemWrite> validateBundleItems(List<BundleItemWrite> items) {
    if (items == null || items.isEmpty()) {
      throw new IllegalArgumentException("A bundle must contain at least one item");
    }
    Set<Long> presentationIds = new HashSet<>();
    Set<Integer> displayOrders = new HashSet<>();
    for (BundleItemWrite item : items) {
      if (item == null || item.presentationId() == null) {
        throw new IllegalArgumentException("presentationId is required");
      }
      positive(item.quantity(), "quantity");
      nonNegative(item.displayOrder(), "displayOrder");
      if (!presentationIds.add(item.presentationId())) {
        throw new IllegalArgumentException("A presentation cannot be repeated in the same bundle");
      }
      if (!displayOrders.add(item.displayOrder())) {
        throw new IllegalArgumentException("displayOrder cannot be repeated");
      }
    }
    return List.copyOf(items);
  }

  private void replaceBundleItems(
      ProductBundleJpaEntity bundle, CompanyJpaEntity company, List<BundleItemWrite> desired) {
    Map<Long, ProductPresentationJpaEntity> resolved = new HashMap<>();
    for (BundleItemWrite item : desired) {
      resolved.put(
          item.presentationId(), requirePresentation(item.presentationId(), company.getId()));
    }
    bundleItems.deleteAllByCompany_IdAndBundle_Id(company.getId(), bundle.getId());
    bundleItems.flush();
    bundleItems.saveAll(
        desired.stream()
            .map(
                item ->
                    ProductBundleItemJpaEntity.create(
                        company,
                        bundle,
                        resolved.get(item.presentationId()),
                        item.quantity(),
                        item.displayOrder()))
            .toList());
  }

  private void replacePresentationBarcodes(
      ProductPresentationJpaEntity presentation,
      CompanyJpaEntity company,
      List<String> values,
      Long actorId) {
    List<String> desired = barcodes(values);
    List<CatalogBarcodeJpaEntity> current =
        catalogBarcodes.findAllByCompany_IdAndPresentation_IdOrderByBarcode(
            company.getId(), presentation.getId());
    replaceBarcodes(
        current,
        desired,
        company,
        barcode ->
            CatalogBarcodeJpaEntity.forPresentation(company, barcode, presentation, actorId));
  }

  private void replaceBundleBarcodes(
      ProductBundleJpaEntity bundle, CompanyJpaEntity company, List<String> values, Long actorId) {
    List<String> desired = barcodes(values);
    List<CatalogBarcodeJpaEntity> current =
        catalogBarcodes.findAllByCompany_IdAndBundle_IdOrderByBarcode(
            company.getId(), bundle.getId());
    replaceBarcodes(
        current,
        desired,
        company,
        barcode -> CatalogBarcodeJpaEntity.forBundle(company, barcode, bundle, actorId));
  }

  private void replaceBarcodes(
      List<CatalogBarcodeJpaEntity> current,
      List<String> desired,
      CompanyJpaEntity company,
      java.util.function.Function<String, CatalogBarcodeJpaEntity> factory) {
    Map<String, CatalogBarcodeJpaEntity> existing =
        current.stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    CatalogBarcodeJpaEntity::getBarcode, item -> item));
    List<CatalogBarcodeJpaEntity> removed =
        current.stream().filter(item -> !desired.contains(item.getBarcode())).toList();
    catalogBarcodes.deleteAll(removed);
    if (!removed.isEmpty()) catalogBarcodes.flush();
    for (String barcode : desired) {
      if (existing.containsKey(barcode)) continue;
      if (catalogBarcodes.existsByCompany_IdAndBarcode(company.getId(), barcode)) {
        throw conflict(
            "BARCODE_ALREADY_EXISTS", "El código de barras ya está asignado: " + barcode);
      }
      catalogBarcodes.save(factory.apply(barcode));
    }
  }

  private PresentationDto toPresentationDto(ProductPresentationJpaEntity entity) {
    List<String> values =
        catalogBarcodes
            .findAllByCompany_IdAndPresentation_IdOrderByBarcode(
                entity.getCompany().getId(), entity.getId())
            .stream()
            .map(CatalogBarcodeJpaEntity::getBarcode)
            .toList();
    return new PresentationDto(
        entity.getId(),
        entity.getProduct().getId(),
        entity.getProduct().getName(),
        entity.getName(),
        entity.getUnitMeasure().getCode(),
        entity.getConversionFactor(),
        entity.getSalePrice(),
        entity.isDefaultPresentation(),
        values,
        entity.getVersion());
  }

  private BundleDto toBundleDto(ProductBundleJpaEntity bundle) {
    List<BundleItemDto> items =
        bundleItems
            .findAllByCompany_IdAndBundle_IdOrderByDisplayOrderAsc(
                bundle.getCompany().getId(), bundle.getId())
            .stream()
            .map(
                item ->
                    new BundleItemDto(
                        item.getPresentation().getId(),
                        item.getPresentation().getName(),
                        item.getPresentation().getProduct().getName(),
                        item.getQuantity(),
                        item.getDisplayOrder()))
            .toList();
    List<String> values =
        catalogBarcodes
            .findAllByCompany_IdAndBundle_IdOrderByBarcode(
                bundle.getCompany().getId(), bundle.getId())
            .stream()
            .map(CatalogBarcodeJpaEntity::getBarcode)
            .toList();
    return new BundleDto(
        bundle.getId(),
        bundle.getName(),
        bundle.getCode(),
        bundle.getUnitMeasure().getCode(),
        bundle.getSalePrice(),
        items,
        values,
        bundle.getVersion());
  }

  private CompanyJpaEntity requireCompany(Long companyId) {
    return companies
        .findById(companyId)
        .orElseThrow(() -> new PetshopCatalogNotFoundException("Company", companyId));
  }

  private ProductJpaEntity requireProduct(Long id, Long companyId) {
    if (id == null) throw new IllegalArgumentException("productId is required");
    return products
        .findByIdAndCompany_Id(id, companyId)
        .orElseThrow(() -> new PetshopCatalogNotFoundException("Product", id));
  }

  private UnitMeasureCatalogJpaEntity requireUnit(String code) {
    return units
        .findById(code)
        .orElseThrow(() -> new PetshopCatalogNotFoundException("Unit measure", code));
  }

  private ProductPresentationJpaEntity requirePresentation(Long id, Long companyId) {
    return presentations
        .findByIdAndCompany_Id(id, companyId)
        .orElseThrow(() -> new PetshopCatalogNotFoundException("Presentation", id));
  }

  private ProductBundleJpaEntity requireBundle(Long id, Long companyId) {
    return bundles
        .findByIdAndCompany_Id(id, companyId)
        .orElseThrow(() -> new PetshopCatalogNotFoundException("Bundle", id));
  }

  private PetshopCatalogConflictException conflict(String code, String message) {
    return new PetshopCatalogConflictException(code, message);
  }
}
