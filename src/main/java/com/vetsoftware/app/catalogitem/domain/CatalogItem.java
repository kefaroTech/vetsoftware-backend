package com.vetsoftware.app.catalogitem.domain;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Un artículo del catálogo comercial: una cosa que se puede comprar.
 *
 * <p>
 * <strong>No es multi-tenant.</strong> {@code catalog_items} es el estante de
 * la tienda, global de plataforma, y no lleva {@code company_id}. Por eso todos
 * sus puertos de entrada están cerrados a {@code hasRole("SYSTEM")} a secas y
 * {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM} (BE-29) se satisface por ahí.
 *
 * <p>
 * Las invariantes del constructor son el espejo exacto de los cinco
 * {@code CHECK} de la ficha 1 de {@code suscripciones-tablas.md}. Están las dos
 * veces a propósito: la base es la última línea de defensa, pero una violación
 * de constraint le llega al cliente como un 500 que no le dice qué campo
 * corregir.
 */
public class CatalogItem {

    /**
     * El ancho de {@code limit_dimensions.code}, que es lo que la columna guarda.
     */
    private static final int MAX_CAPACITY_UNIT_LENGTH = 50;

    private Long id;
    private String code;
    private String name;
    private String shortDescription;
    private String longDescription;
    private ItemType itemType;
    private String capacityUnit;
    private boolean core;
    private int minQuantity;
    private Integer maxQuantity;
    private int sortOrder;
    private CatalogItemStatus status;
    private final LocalDateTime createdDate;
    private Long version;
    private boolean enabled;

    public CatalogItem(Long id, String code, String name, String shortDescription,
            String longDescription, ItemType itemType, String capacityUnit, boolean core,
            int minQuantity, Integer maxQuantity, int sortOrder, CatalogItemStatus status,
            LocalDateTime createdDate, Long version, boolean enabled) {
        validateCode(code);
        validateTextos(name, shortDescription);
        validateTipo(itemType, capacityUnit);
        validateCantidades(minQuantity, maxQuantity, sortOrder);
        if (status == null)
            throw new IllegalArgumentException("status is required");
        this.id = id;
        this.code = code;
        this.name = name;
        this.shortDescription = shortDescription;
        this.longDescription = longDescription;
        this.itemType = itemType;
        this.capacityUnit = capacityUnit;
        this.core = core;
        this.minQuantity = minQuantity;
        this.maxQuantity = maxQuantity;
        this.sortOrder = sortOrder;
        this.status = status;
        this.createdDate = createdDate;
        this.version = version;
        this.enabled = enabled;
    }

    /**
     * Artículo nuevo. El {@link Clock} entra por parámetro en vez de llamar a
     * {@code LocalDateTime.now()} aquí dentro: un test que compara contra el reloj
     * de pared se cae solo el día que este cruce medianoche entre dos líneas.
     */
    public static CatalogItem create(String code, String name, String shortDescription,
            String longDescription, ItemType itemType, String capacityUnit, boolean core,
            int minQuantity, Integer maxQuantity, int sortOrder, CatalogItemStatus status,
            Clock clock) {
        return new CatalogItem(null, code, name, shortDescription, longDescription, itemType,
                capacityUnit, core, minQuantity, maxQuantity, sortOrder,
                status == null ? CatalogItemStatus.DRAFT : status, LocalDateTime.now(clock), null,
                true);
    }

    /**
     * El {@code code} NO se actualiza. La ficha lo declara «estable e inmutable:
     * nunca cambia aunque cambie el nombre comercial», y es lo que copian congelado
     * las líneas de cotización y de contrato — dejarlo mutar reescribiría el pasado
     * de documentos ya firmados.
     */
    public void update(String name, String shortDescription, String longDescription,
            ItemType itemType, String capacityUnit, boolean core, int minQuantity,
            Integer maxQuantity, int sortOrder, CatalogItemStatus status) {
        validateTextos(name, shortDescription);
        validateTipo(itemType, capacityUnit);
        validateCantidades(minQuantity, maxQuantity, sortOrder);
        if (status == null)
            throw new IllegalArgumentException("status is required");
        this.name = name;
        this.shortDescription = shortDescription;
        this.longDescription = longDescription;
        this.itemType = itemType;
        this.capacityUnit = capacityUnit;
        this.core = core;
        this.minQuantity = minQuantity;
        this.maxQuantity = maxQuantity;
        this.sortOrder = sortOrder;
        this.status = status;
    }

    private static void validateCode(String code) {
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("code is required");
        if (code.length() > 50)
            throw new IllegalArgumentException("code must be 50 chars or less");
    }

    private static void validateTextos(String name, String shortDescription) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("name is required");
        if (name.length() > 120)
            throw new IllegalArgumentException("name must be 120 chars or less");
        if (shortDescription != null && shortDescription.length() > 255)
            throw new IllegalArgumentException("shortDescription must be 255 chars or less");
    }

    /**
     * Espejo de la mitad estructural de {@code chk_catalog_items_capacity_unit}: la
     * unidad va atada al tipo, en los dos sentidos.
     *
     * <p>
     * <strong>Lo que ya NO comprueba, y por que.</strong> Hasta el changeset 333 el
     * tipo del parametro era un enumerado de cuatro valores —{@code USER},
     * {@code BRANCH}, {@code TERMINAL}, {@code STORAGE_GB}—, asi que esta firma
     * cerraba tambien el dominio de la columna. Eso hacia que el catalogo comercial
     * solo supiera vender cuatro ejes cuando el contador ya sabia contar ocho
     * (#655): un paquete de mascotas necesitaba tocar el enumerado, el
     * {@code CHECK} del esquema y desplegar, que es exactamente lo que la capa J
     * existe para eliminar.
     *
     * <p>
     * Ahora {@code capacityUnit} es el <strong>codigo del eje</strong> —lo que
     * {@code limit_dimensions.code} guarda— y quien decide si ese codigo existe es
     * el catalogo, no esta clase. Comprobarlo es una <em>consulta</em>, y una
     * consulta no cabe en el constructor de una entidad: vive en el caso de uso, a
     * traves de {@code LimitDimensionQueryPort}, y en la base como clave foranea.
     * Lo que si se queda aqui es la invariante que no necesita mirar otra tabla, y
     * que la clave foranea no cubre porque una columna nula la satisface siempre.
     *
     * <p>
     * El limite de longitud es el de la columna del eje ({@code VARCHAR(50)}): un
     * codigo mas largo no puede corresponder a ninguna fila, asi que se rechaza
     * aqui con el nombre del campo en vez de llegar al motor como un truncamiento.
     */
    private static void validateTipo(ItemType itemType, String capacityUnit) {
        if (itemType == null)
            throw new IllegalArgumentException("itemType is required");
        if (itemType == ItemType.CAPACITY && (capacityUnit == null || capacityUnit.isBlank()))
            throw new IllegalArgumentException("capacityUnit is required for CAPACITY items");
        if (itemType != ItemType.CAPACITY && capacityUnit != null)
            throw new IllegalArgumentException(
                    "capacityUnit is only allowed on CAPACITY items, but itemType is " + itemType);
        if (capacityUnit != null && capacityUnit.length() > MAX_CAPACITY_UNIT_LENGTH)
            throw new IllegalArgumentException(
                    "capacityUnit must be " + MAX_CAPACITY_UNIT_LENGTH + " chars or less");
    }

    /**
     * Espejo de {@code chk_catalog_items_quantity_range} y
     * {@code chk_catalog_items_sort_order}.
     */
    private static void validateCantidades(int minQuantity, Integer maxQuantity, int sortOrder) {
        if (minQuantity < 0)
            throw new IllegalArgumentException("minQuantity cannot be negative");
        if (maxQuantity != null && maxQuantity < minQuantity)
            throw new IllegalArgumentException("maxQuantity cannot be lower than minQuantity");
        if (sortOrder < 0)
            throw new IllegalArgumentException("sortOrder cannot be negative");
    }

    /** {@code true} si el artículo puede llevar componentes colgando. */
    public boolean isBundle() {
        return itemType == ItemType.BUNDLE;
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public String getLongDescription() {
        return longDescription;
    }

    public ItemType getItemType() {
        return itemType;
    }

    public String getCapacityUnit() {
        return capacityUnit;
    }

    public boolean isCore() {
        return core;
    }

    public int getMinQuantity() {
        return minQuantity;
    }

    public Integer getMaxQuantity() {
        return maxQuantity;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public CatalogItemStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Long getVersion() {
        return version;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
