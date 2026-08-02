package com.vetsoftware.app.petshopcatalog.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vetsoftware.app.catalogbarcode.infrastructure.persistence.CatalogBarcodeJpaEntity;
import com.vetsoftware.app.inventory.infrastructure.persistence.SaleInventoryAllocationJpaEntity;
import com.vetsoftware.app.product.infrastructure.persistence.ProductJpaEntity;
import com.vetsoftware.app.productbundle.infrastructure.persistence.ProductBundleItemJpaEntity;
import com.vetsoftware.app.productbundle.infrastructure.persistence.ProductBundleJpaEntity;
import com.vetsoftware.app.productpresentation.infrastructure.persistence.ProductPresentationJpaEntity;
import com.vetsoftware.app.unitmeasure.infrastructure.persistence.UnitMeasureCatalogJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.parser.ChangeLogParserFactory;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.ResourceAccessor;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

class PetshopCatalogPersistenceContractTest {
  private static final String MASTER = "db/changelog/db.changelog-master.xml";
  private static final String MIGRATION = "db/changelog/migrations/212_create_petshop_catalog.xml";

  @Test
  void masterIncludesPetshopCatalogMigration() throws Exception {
    Document document = parse(MASTER);
    NodeList includes = document.getElementsByTagNameNS("*", "include");

    boolean found = false;
    for (int i = 0; i < includes.getLength(); i++) {
      Element include = (Element) includes.item(i);
      if (MIGRATION.equals(include.getAttribute("file"))) {
        found = true;
        break;
      }
    }

    assertTrue(found, "El master debe incluir la migración 212");
  }

  @Test
  void liquibaseParsesTheCompleteChangelog() throws Exception {
    ResourceAccessor resources = new ClassLoaderResourceAccessor();
    DatabaseChangeLog changeLog =
        ChangeLogParserFactory.getInstance()
            .getParser(MASTER, resources)
            .parse(MASTER, new ChangeLogParameters(), resources);

    assertTrue(
        changeLog.getChangeSets().stream()
            .anyMatch(
                changeSet -> "212_08_create_sale_inventory_allocations".equals(changeSet.getId())));
  }

  @Test
  void migrationDefinesAllPersistentStructuresAndBackfill() throws Exception {
    Document document = parse(MIGRATION);
    Set<String> tables = attributeValues(document, "createTable", "tableName");

    assertTrue(
        tables.containsAll(
            Set.of(
                "unit_measure_catalog",
                "product_presentations",
                "product_bundles",
                "product_bundle_items",
                "catalog_barcodes",
                "sale_inventory_allocations")));

    String migrationText = document.getDocumentElement().getTextContent();
    assertTrue(migrationText.contains("INSERT INTO product_presentations"));
    assertTrue(migrationText.contains("p.base_unit_measure_code, 1, p.sale_price"));
    assertTrue(migrationText.contains("uq_catalog_barcodes_company_active"));
    assertTrue(
        attributeValues(document, "addUniqueConstraint", "constraintName")
            .contains("uq_sale_inventory_allocations_idempotency"));

    NodeList changeSets = document.getElementsByTagNameNS("*", "changeSet");
    Set<String> ids = new HashSet<>();
    for (int i = 0; i < changeSets.getLength(); i++) {
      Element changeSet = (Element) changeSets.item(i);
      assertTrue(ids.add(changeSet.getAttribute("id")), "Los ids de changeset deben ser únicos");
      assertTrue(
          changeSet.getElementsByTagNameNS("*", "rollback").getLength() > 0,
          "Cada changeset de la fase 2 debe declarar rollback");
    }
  }

  @Test
  void jpaModelMatchesMigrationContract() throws Exception {
    assertTable(UnitMeasureCatalogJpaEntity.class, "unit_measure_catalog");
    assertTable(ProductPresentationJpaEntity.class, "product_presentations");
    assertTable(ProductBundleJpaEntity.class, "product_bundles");
    assertTable(ProductBundleItemJpaEntity.class, "product_bundle_items");
    assertTable(CatalogBarcodeJpaEntity.class, "catalog_barcodes");
    assertTable(SaleInventoryAllocationJpaEntity.class, "sale_inventory_allocations");

    Field baseUnit = ProductJpaEntity.class.getDeclaredField("baseUnitMeasureCode");
    assertEquals(String.class, baseUnit.getType());
    assertEquals("base_unit_measure_code", baseUnit.getAnnotation(Column.class).name());

    assertEquals(
        int.class,
        ProductPresentationJpaEntity.class.getDeclaredField("conversionFactor").getType());
    assertEquals(
        int.class,
        SaleInventoryAllocationJpaEntity.class.getDeclaredField("baseQuantity").getType());
    assertNotNull(
        ProductPresentationJpaEntity.class
            .getDeclaredField("version")
            .getAnnotation(Version.class));
    assertNotNull(
        ProductBundleJpaEntity.class.getDeclaredField("version").getAnnotation(Version.class));
    assertNotNull(
        CatalogBarcodeJpaEntity.class.getDeclaredField("version").getAnnotation(Version.class));
  }

  private static Document parse(String resource) throws Exception {
    try (InputStream input =
        Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
      assertNotNull(input, "No se encontró " + resource);
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      return factory.newDocumentBuilder().parse(input);
    }
  }

  private static Set<String> attributeValues(
      Document document, String elementName, String attributeName) {
    NodeList elements = document.getElementsByTagNameNS("*", elementName);
    Set<String> values = new HashSet<>();
    for (int i = 0; i < elements.getLength(); i++) {
      values.add(((Element) elements.item(i)).getAttribute(attributeName));
    }
    return values;
  }

  private static void assertTable(Class<?> entityType, String expectedTable) {
    Table table = entityType.getAnnotation(Table.class);
    assertNotNull(table);
    assertEquals(expectedTable, table.name());
  }
}
