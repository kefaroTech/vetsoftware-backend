package com.vetsoftware.app.productchargeopenaccount.application.port.in;

import com.vetsoftware.app.productchargeopenaccount.application.dto.ProductChargeOpenAccountDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListProductChargeOpenAccountsUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    List<ProductChargeOpenAccountDto> listAll();
}
