package com.vetsoftware.app.catalogitem.domain;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Una pieza que trae un paquete, con su cantidad.
 *
 * <p>
 * La cantidad permite que un pack traiga «3 usuarios» sin vender tres líneas.
 *
 * <p>
 * Lo que aquí <strong>no</strong> se puede comprobar es que
 * {@code bundleItemId} apunte a un artículo de tipo {@code BUNDLE} y que
 * {@code componentItemId} no apunte a otro {@code BUNDLE}: eso exige leer la
 * otra fila y ni un {@code CHECK} de MySQL ni el constructor de esta entidad
 * pueden hacerlo. Vive en {@code CreateBundleComponentService}, que ya tiene
 * los dos artículos cargados.
 */
public class BundleComponent {

    private Long id;
    private final Long bundleItemId;
    private final Long componentItemId;
    private int quantity;
    private final LocalDateTime createdDate;
    private boolean enabled;

    public BundleComponent(Long id, Long bundleItemId, Long componentItemId, int quantity,
            LocalDateTime createdDate, boolean enabled) {
        if (bundleItemId == null)
            throw new IllegalArgumentException("bundleItemId is required");
        if (componentItemId == null)
            throw new IllegalArgumentException("componentItemId is required");
        if (bundleItemId.equals(componentItemId))
            throw new IllegalArgumentException("a bundle cannot contain itself: " + bundleItemId);
        validateQuantity(quantity);
        this.id = id;
        this.bundleItemId = bundleItemId;
        this.componentItemId = componentItemId;
        this.quantity = quantity;
        this.createdDate = createdDate;
        this.enabled = enabled;
    }

    public static BundleComponent create(Long bundleItemId, Long componentItemId, int quantity,
            Clock clock) {
        return new BundleComponent(null, bundleItemId, componentItemId, quantity,
                LocalDateTime.now(clock), true);
    }

    /** Lo único editable de un componente: cuántas unidades trae el paquete. */
    public void changeQuantity(int quantity) {
        validateQuantity(quantity);
        this.quantity = quantity;
    }

    /** Espejo de {@code chk_bundle_components_quantity}. */
    private static void validateQuantity(int quantity) {
        if (quantity <= 0)
            throw new IllegalArgumentException("quantity must be greater than zero");
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

    public Long getBundleItemId() {
        return bundleItemId;
    }

    public Long getComponentItemId() {
        return componentItemId;
    }

    public int getQuantity() {
        return quantity;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
