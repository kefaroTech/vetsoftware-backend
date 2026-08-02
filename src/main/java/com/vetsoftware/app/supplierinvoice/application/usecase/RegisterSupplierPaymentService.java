package com.vetsoftware.app.supplierinvoice.application.usecase;

import com.vetsoftware.app.supplierinvoice.application.command.RegisterSupplierPaymentCommand;
import com.vetsoftware.app.supplierinvoice.application.dto.SupplierInvoiceDto;
import com.vetsoftware.app.supplierinvoice.application.port.in.RegisterSupplierPaymentUseCase;
import com.vetsoftware.app.supplierinvoice.application.port.out.SupplierInvoiceRepository;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoice;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoiceNotFoundException;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoicePayment;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra un abono (total o parcial) sobre la factura y persiste el nuevo estado (PARTIAL/PAID).
 */
@Observed(name = "supplier.invoice.register.payment")
@Service
public class RegisterSupplierPaymentService implements RegisterSupplierPaymentUseCase {
  private final SupplierInvoiceRepository repository;

  public RegisterSupplierPaymentService(SupplierInvoiceRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public SupplierInvoiceDto execute(RegisterSupplierPaymentCommand command) {
    SupplierInvoice invoice =
        repository
            .findByIdAndCompanyId(command.invoiceId(), command.companyId())
            .orElseThrow(() -> new SupplierInvoiceNotFoundException(command.invoiceId()));
    SupplierInvoicePayment payment =
        SupplierInvoicePayment.create(
            command.amount(),
            command.paymentDate(),
            command.method(),
            command.reference(),
            command.note(),
            command.actorId());
    invoice.registerPayment(payment, command.actorId(), command.version());
    return SupplierInvoiceDto.from(repository.save(invoice));
  }
}
