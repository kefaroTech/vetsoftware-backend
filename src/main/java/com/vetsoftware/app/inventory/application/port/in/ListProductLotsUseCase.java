package com.vetsoftware.app.inventory.application.port.in;

import com.vetsoftware.app.inventory.application.command.ListLotsCommand;
import com.vetsoftware.app.inventory.application.dto.StockLotView;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListProductLotsUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or "
          + "(hasAuthority('inventory.read') and @authz.isMyCompany(#command.companyId))")
  List<StockLotView> listLots(ListLotsCommand command);
}
