package com.vetsoftware.app.supplierinvoice.application.usecase;

import com.vetsoftware.app.supplierinvoice.application.command.SearchSupplierInvoicesCommand;
import com.vetsoftware.app.supplierinvoice.application.dto.PageResult;
import com.vetsoftware.app.supplierinvoice.application.dto.SupplierInvoiceDto;
import com.vetsoftware.app.supplierinvoice.application.port.in.SearchSupplierInvoicesUseCase;
import com.vetsoftware.app.supplierinvoice.application.port.out.SupplierInvoiceRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "supplier.invoice.search")
@Service
public class SearchSupplierInvoicesService implements SearchSupplierInvoicesUseCase {
  private final SupplierInvoiceRepository repository;

  public SearchSupplierInvoicesService(SupplierInvoiceRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional(readOnly = true)
  public PageResult<SupplierInvoiceDto> execute(SearchSupplierInvoicesCommand command) {
    return repository.search(command).map(SupplierInvoiceDto::from);
  }
}
