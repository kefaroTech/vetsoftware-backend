package com.vetsoftware.app.configurator.application.dto;

import com.vetsoftware.app.configurator.domain.SelectedItem;
import java.util.List;

/**
 * Lo que el configurador mete en el carrito con unas respuestas dadas.
 *
 * <p>
 * Es el contrato que consumirá {@code quote}: a partir de aquí, quien cotiza
 * resuelve precios, resta {@code included_quantity} (regla R15) y congela las
 * líneas. El configurador no sabe de dinero.
 */
public record ConfiguratorSelectionDto(List<SelectedItemDto> items) {

    public ConfiguratorSelectionDto {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public static ConfiguratorSelectionDto from(List<SelectedItem> items) {
        return new ConfiguratorSelectionDto(items.stream().map(SelectedItemDto::from).toList());
    }
}
