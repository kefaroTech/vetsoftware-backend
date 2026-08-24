package com.vetsoftware.app.catalogitem.application.dto;

import com.vetsoftware.app.catalogitem.domain.BundleComponent;
import com.vetsoftware.app.catalogitem.domain.LinkOutcome;
import java.time.LocalDateTime;

/**
 * @param outcome
 *            SOLO en la respuesta del alta: si la fila se inserto o se revivio.
 *            {@code null} en las lecturas, donde la pregunta no tiene sentido.
 *            Ver {@link com.vetsoftware.app.catalogitem.domain.LinkOutcome} e
 *            incidencia #465.
 */
public record BundleComponentDto(Long id, Long bundleItemId, Long componentItemId, int quantity,
        LocalDateTime createdDate, boolean enabled, LinkOutcome outcome) {

    /** Sin desenlace: el de las lecturas. */
    public BundleComponentDto(Long id, Long bundleItemId, Long componentItemId, int quantity,
            LocalDateTime createdDate, boolean enabled) {
        this(id, bundleItemId, componentItemId, quantity, createdDate, enabled, null);
    }

    public static BundleComponentDto from(BundleComponent component) {
        return from(component, null);
    }

    public static BundleComponentDto from(BundleComponent component, LinkOutcome outcome) {
        return new BundleComponentDto(component.getId(), component.getBundleItemId(),
                component.getComponentItemId(), component.getQuantity(), component.getCreatedDate(),
                component.isEnabled(), outcome);
    }
}
