package com.vetsoftware.app.externalinvoicereconciliation.application.usecase;

import com.vetsoftware.app.externalinvoicereconciliation.application.command.OpenExternalInvoiceReconciliationCommand;
import com.vetsoftware.app.externalinvoicereconciliation.application.dto.ExternalInvoiceReconciliationDto;
import com.vetsoftware.app.externalinvoicereconciliation.application.port.in.OpenExternalInvoiceReconciliationUseCase;
import com.vetsoftware.app.externalinvoicereconciliation.application.port.out.BillingDocumentValidationPort;
import com.vetsoftware.app.externalinvoicereconciliation.application.port.out.ExternalInvoiceReconciliationRepository;
import com.vetsoftware.app.externalinvoicereconciliation.domain.ExternalInvoiceReconciliation;
import com.vetsoftware.app.externalinvoicereconciliation.domain.ExternalInvoiceReconciliationAlreadyExistsException;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Abre la ficha del documento de cobro devengado.
 *
 * <p>
 * <strong>La fila nace incompleta y esa es su razon de ser.</strong> Se crea en
 * {@code MISSING_EXTERNAL}, sin factura del tercero, y mientras siga asi es la
 * unica senal de que hay dinero devengado que nadie facturo. La alternativa
 * -crear la conciliacion solo cuando llega la factura externa- dejaria
 * precisamente el caso peligroso sin ninguna fila que mirar.
 *
 * <p>
 * <strong>El duplicado se consulta antes de escribir.</strong>
 * {@code uq_eir_document} lo impediria igual, pero llegaria como una violacion
 * de indice unico: un 500 sin explicacion en la cara de quien concilia. La
 * comprobacion previa lo convierte en un 409 que dice cual es el documento.
 */
@Observed(name = "external.invoice.reconciliation.open")
@Service
public class OpenExternalInvoiceReconciliationService
        implements
            OpenExternalInvoiceReconciliationUseCase {

    private final ExternalInvoiceReconciliationRepository repository;
    private final BillingDocumentValidationPort billingDocumentValidationPort;
    private final Clock clock;

    public OpenExternalInvoiceReconciliationService(
            ExternalInvoiceReconciliationRepository repository,
            BillingDocumentValidationPort billingDocumentValidationPort, Clock clock) {
        this.repository = repository;
        this.billingDocumentValidationPort = billingDocumentValidationPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ExternalInvoiceReconciliationDto execute(
            OpenExternalInvoiceReconciliationCommand command) {
        if (!billingDocumentValidationPort.existsByIdAndCompanyId(command.billingDocumentId(),
                command.companyId()))
            throw new IllegalArgumentException(
                    "Billing document not found: " + command.billingDocumentId());
        if (repository.existsByCompanyIdAndBillingDocumentId(command.companyId(),
                command.billingDocumentId()))
            throw new ExternalInvoiceReconciliationAlreadyExistsException(command.companyId(),
                    command.billingDocumentId());

        ExternalInvoiceReconciliation reconciliation = ExternalInvoiceReconciliation.open(
                command.companyId(), command.billingDocumentId(), command.computedTotal(),
                command.computedTax(), LocalDateTime.now(clock));
        return ExternalInvoiceReconciliationDto.from(repository.save(reconciliation));
    }
}
