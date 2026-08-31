package com.vetsoftware.app.catalogitemaihint.application.port.in;

import com.vetsoftware.app.catalogitemaihint.application.command.PublishCatalogItemAiHintCommand;
import com.vetsoftware.app.catalogitemaihint.application.dto.CatalogItemAiHintDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Publica la primera pista de un articulo que no tiene ninguna.
 *
 * <p>
 * <strong>No existe un {@code UpdateCatalogItemAiHintUseCase}</strong>, y esa
 * ausencia es la decision: sobrescribir {@code hint_text} destruiria la unica
 * evidencia de que se le estaba diciendo al modelo cuando genero una propuesta
 * pasada. Corregir es {@link ReviseCatalogItemAiHintUseCase} —sucede la vigente
 * y publica la siguiente—; editar no existe.
 *
 * <p>
 * Y no sucede en silencio a la que hubiera: si el articulo ya tiene vigente,
 * esto responde 409. Un {@code POST} repetido por un doble clic no puede dejar
 * una revision de mas en el historial.
 */
public interface PublishCatalogItemAiHintUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    CatalogItemAiHintDto execute(PublishCatalogItemAiHintCommand command);
}
