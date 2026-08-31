package com.vetsoftware.app.catalogitemaihint.application.port.in;

import com.vetsoftware.app.catalogitemaihint.application.command.ReviseCatalogItemAiHintCommand;
import com.vetsoftware.app.catalogitemaihint.application.dto.CatalogItemAiHintDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Corrige la pista de un articulo: marca la vigente como reemplazada y publica
 * la revision siguiente, que es la que devuelve.
 *
 * <p>
 * La anterior <b>no se borra ni se toca el texto</b>: solo se le pone
 * {@code superseded_at}. Es lo que permite que
 * {@link ListCatalogItemAiHintRevisionsUseCase} responda «con que texto se
 * genero aquella propuesta» meses despues.
 */
public interface ReviseCatalogItemAiHintUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    CatalogItemAiHintDto execute(ReviseCatalogItemAiHintCommand command);
}
