package com.vetsoftware.app.catalogitem.application.command;

/**
 * Igual que su hermano de dependencias: {@code bundleItemId} viaja para que el
 * caso de uso confirme que el componente que se edita pertenece al paquete de
 * la ruta.
 */
public record UpdateBundleComponentCommand(Long id, Long bundleItemId, int quantity) {
}
