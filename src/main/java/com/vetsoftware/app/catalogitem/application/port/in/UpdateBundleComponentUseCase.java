package com.vetsoftware.app.catalogitem.application.port.in;

import com.vetsoftware.app.catalogitem.application.command.UpdateBundleComponentCommand;
import com.vetsoftware.app.catalogitem.application.dto.BundleComponentDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateBundleComponentUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    BundleComponentDto execute(UpdateBundleComponentCommand command);
}
