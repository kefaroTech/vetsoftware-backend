package com.vetsoftware.app.catalogitem.application.port.in;

import com.vetsoftware.app.catalogitem.application.command.CreateBundleComponentCommand;
import com.vetsoftware.app.catalogitem.application.dto.BundleComponentDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Alta de una pieza dentro de un paquete. Comprueba lo que la base no puede:
 * que el padre sea de tipo {@code BUNDLE} y que la pieza no sea otro
 * {@code BUNDLE}.
 */
public interface CreateBundleComponentUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    BundleComponentDto execute(CreateBundleComponentCommand command);
}
