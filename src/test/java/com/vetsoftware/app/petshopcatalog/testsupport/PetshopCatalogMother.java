package com.vetsoftware.app.petshopcatalog.testsupport;

import com.vetsoftware.app.catalogbarcode.infrastructure.persistence.CatalogBarcodeJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.product.infrastructure.persistence.ProductJpaEntity;
import com.vetsoftware.app.productbundle.infrastructure.persistence.ProductBundleItemJpaEntity;
import com.vetsoftware.app.productbundle.infrastructure.persistence.ProductBundleJpaEntity;
import com.vetsoftware.app.productpresentation.infrastructure.persistence.ProductPresentationJpaEntity;
import com.vetsoftware.app.unitmeasure.infrastructure.persistence.UnitMeasureCatalogJpaEntity;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;

/**
 * Fixtures de {@code petshopcatalog}. El servicio orquesta siete repositorios
 * JPA de otras features en vez de un puerto propio (ver {@code
 * PetshopCatalogService}), así que esta Mother construye directamente las
 * entidades JPA que esas features exponen.
 *
 * <p>
 * {@code CompanyJpaEntity}, {@code ProductJpaEntity} y
 * {@code UnitMeasureCatalogJpaEntity} solo tienen constructor protegido (para
 * su mapper, en su propio paquete) y ningún factory público, así que se
 * instancian por reflexión y se rellenan con sus setters públicos. Las demás
 * entidades de esta feature sí tienen factory público ({@code create},
 * {@code forPresentation}, {@code forBundle}) y se usan tal cual.
 */
public final class PetshopCatalogMother {
    public static final Long COMPANY_ID = 10L;
    public static final Long PRODUCT_ID = 20L;
    public static final Long ACTOR_ID = 30L;

    private PetshopCatalogMother() {
    }

    public static CompanyJpaEntity company(Long id) {
        CompanyJpaEntity company = instantiate(CompanyJpaEntity.class);
        company.setId(id);
        company.setName("Veterinaria Central");
        return company;
    }

    public static ProductJpaEntity product(Long id, CompanyJpaEntity company) {
        ProductJpaEntity product = instantiate(ProductJpaEntity.class);
        product.setId(id);
        product.setName("Amoxicilina 500mg");
        product.setCompany(company);
        product.setBaseUnitMeasureCode("94");
        return product;
    }

    public static UnitMeasureCatalogJpaEntity unit(String code, String name, String symbol) {
        UnitMeasureCatalogJpaEntity unit = instantiate(UnitMeasureCatalogJpaEntity.class);
        unit.setCode(code);
        unit.setName(name);
        unit.setSymbol(symbol);
        return unit;
    }

    public static ProductPresentationJpaEntity presentation(Long id, CompanyJpaEntity company,
            ProductJpaEntity product, String name, UnitMeasureCatalogJpaEntity unit, int factor,
            String salePrice, boolean defaultPresentation, Long version) {
        ProductPresentationJpaEntity entity = ProductPresentationJpaEntity.create(company, product,
                name, unit, factor, new BigDecimal(salePrice), defaultPresentation, ACTOR_ID);
        entity.setId(id);
        entity.setVersion(version);
        return entity;
    }

    public static ProductBundleJpaEntity bundle(Long id, CompanyJpaEntity company, String name,
            String code, UnitMeasureCatalogJpaEntity unit, String salePrice, Long version) {
        ProductBundleJpaEntity entity = ProductBundleJpaEntity.create(company, name, code, unit,
                new BigDecimal(salePrice), ACTOR_ID);
        entity.setId(id);
        entity.setVersion(version);
        return entity;
    }

    public static ProductBundleItemJpaEntity bundleItem(Long id, CompanyJpaEntity company,
            ProductBundleJpaEntity bundle, ProductPresentationJpaEntity presentation, int quantity,
            int displayOrder) {
        ProductBundleItemJpaEntity entity = ProductBundleItemJpaEntity.create(company, bundle,
                presentation, quantity, displayOrder);
        entity.setId(id);
        return entity;
    }

    public static CatalogBarcodeJpaEntity barcodeForPresentation(Long id, CompanyJpaEntity company,
            String barcode, ProductPresentationJpaEntity presentation) {
        CatalogBarcodeJpaEntity entity = CatalogBarcodeJpaEntity.forPresentation(company, barcode,
                presentation, ACTOR_ID);
        entity.setId(id);
        return entity;
    }

    public static CatalogBarcodeJpaEntity barcodeForBundle(Long id, CompanyJpaEntity company,
            String barcode, ProductBundleJpaEntity bundle) {
        CatalogBarcodeJpaEntity entity = CatalogBarcodeJpaEntity.forBundle(company, barcode, bundle,
                ACTOR_ID);
        entity.setId(id);
        return entity;
    }

    private static <T> T instantiate(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("No se pudo instanciar " + type, exception);
        }
    }
}
