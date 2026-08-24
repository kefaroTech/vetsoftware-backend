package com.vetsoftware.app.catalogitem.application.port.in;

import com.vetsoftware.app.catalogitem.application.dto.BundleComponentDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListBundleComponentsUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    List<BundleComponentDto> listByBundle(Long bundleItemId);
}
