package com.vetsoftware.app.catalogitem.testsupport;

import com.vetsoftware.app.catalogitem.application.command.CreateCatalogItemCommand;
import com.vetsoftware.app.catalogitem.domain.BundleComponent;
import com.vetsoftware.app.catalogitem.domain.CapacityUnit;
import com.vetsoftware.app.catalogitem.domain.CatalogItem;
import com.vetsoftware.app.catalogitem.domain.CatalogItemDependency;
import com.vetsoftware.app.catalogitem.domain.CatalogItemStatus;
import com.vetsoftware.app.catalogitem.domain.CatalogItemSubModule;
import com.vetsoftware.app.catalogitem.domain.ItemType;
import com.vetsoftware.app.catalogitem.domain.RelationType;
import com.vetsoftware.app.catalogitem.domain.SubModuleRef;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Fixtures de la feature. Vive dentro del slice porque el vertical slicing
 * aplica igual en {@code src/test}: no hay un paquete de fixtures compartido.
 */
public final class CatalogItemMother {

    /**
     * Reloj fijo. Ninguna factoría del dominio llama a {@code LocalDateTime.now()}
     * por su cuenta —el {@code Clock} entra por parámetro—, así que las fechas de
     * estos tests son exactas y no dependen de cuándo se ejecuten.
     */
    public static final Clock RELOJ = Clock.fixed(Instant.parse("2026-08-22T10:15:30Z"),
            ZoneOffset.UTC);

    public static final LocalDateTime CREADO = LocalDateTime.now(RELOJ);

    private CatalogItemMother() {
    }

    public static CatalogItem historiaClinica() {
        return new CatalogItem(1L, "CLINICAL_HISTORY", "Historia clínica",
                "Consultas, hospitalización y prescripciones", null, ItemType.MODULE, null, true, 1,
                1, 10, CatalogItemStatus.ACTIVE, CREADO, 0L, true);
    }

    public static CatalogItem usuarioExtra() {
        return new CatalogItem(2L, "EXTRA_USER", "Usuario adicional", null, null, ItemType.CAPACITY,
                CapacityUnit.USER, false, 0, null, 20, CatalogItemStatus.ACTIVE, CREADO, 0L, true);
    }

    public static CatalogItem paqueteBasico() {
        return new CatalogItem(3L, "BASIC", "Plan básico", null, null, ItemType.BUNDLE, null, false,
                1, 1, 0, CatalogItemStatus.ACTIVE, CREADO, 0L, true);
    }

    /** Artículo genérico con el id y el tipo que pida el caso bajo prueba. */
    public static CatalogItem conIdYTipo(Long id, ItemType itemType) {
        return new CatalogItem(id, "CODE_" + id, "Artículo " + id, null, null, itemType,
                itemType == ItemType.CAPACITY ? CapacityUnit.USER : null, false, 1, null, 0,
                CatalogItemStatus.ACTIVE, CREADO, 0L, true);
    }

    public static CreateCatalogItemCommand comandoValido() {
        return new CreateCatalogItemCommand("CLINICAL_HISTORY", "Historia clínica", "Corta",
                "Larga", ItemType.MODULE, null, true, 1, 1, 10, CatalogItemStatus.ACTIVE);
    }

    public static SubModuleRef consultas() {
        return new SubModuleRef(50L, "Consultas", "CONSULTATIONS");
    }

    public static CatalogItemSubModule vinculo(Long id, Long catalogItemId) {
        return new CatalogItemSubModule(id, catalogItemId, consultas(), CREADO, true);
    }

    public static CatalogItemDependency dependencia(Long id, Long catalogItemId, Long relatedItemId,
            RelationType relationType) {
        return new CatalogItemDependency(id, catalogItemId, relatedItemId, relationType,
                "Necesitas caja para facturar", CREADO, true);
    }

    public static BundleComponent componente(Long id, Long bundleItemId, Long componentItemId,
            int quantity) {
        return new BundleComponent(id, bundleItemId, componentItemId, quantity, CREADO, true);
    }
}
