package com.vetsoftware.app.electronicdocument.application.usecase;

import com.vetsoftware.app.electronicdocument.application.command.ConvertPosToInvoiceCommand;
import com.vetsoftware.app.electronicdocument.application.dto.ElectronicDocumentDto;
import com.vetsoftware.app.electronicdocument.application.port.in.ConvertPosToInvoiceUseCase;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentNotFoundException;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Convierte un documento equivalente POS en factura electrónica de venta: emite una nueva FE_VENTA
 * desde la misma cuenta del POS.
 *
 * TODO: vincular formalmente la FE con el POS (referencedCude) — depende del soporte del proveedor; hoy
 * la conversión emite una FE independiente sobre la misma venta.
 */
@Observed(name = "electronicDocument.convertPos")
@Service
public class ConvertPosToInvoiceService implements ConvertPosToInvoiceUseCase {
    private final ElectronicDocumentRepository repository;
    private final DocumentBuilder documentBuilder;
    private final ElectronicDocumentEmitter emitter;
    private final DeliverElectronicDocumentService deliverService;

    public ConvertPosToInvoiceService(ElectronicDocumentRepository repository,
                                      DocumentBuilder documentBuilder,
                                      ElectronicDocumentEmitter emitter,
                                      DeliverElectronicDocumentService deliverService) {
        this.repository = repository;
        this.documentBuilder = documentBuilder;
        this.emitter = emitter;
        this.deliverService = deliverService;
    }

    @Override
    @Transactional
    public ElectronicDocumentDto execute(ConvertPosToInvoiceCommand command) {
        ElectronicDocument pos = repository.findById(command.posDocumentId())
                .orElseThrow(() -> new ElectronicDocumentNotFoundException(command.posDocumentId()));
        if (!command.companyId().equals(pos.getCompanyId())) {
            throw new ElectronicDocumentNotFoundException(command.posDocumentId());
        }
        if (pos.getDocumentType() != ElectronicDocumentType.DOC_EQUIV_POS) {
            throw new IllegalStateException("Solo un documento equivalente POS puede convertirse a factura.");
        }
        if (pos.getOpenAccountId() == null) {
            throw new IllegalStateException("El documento POS no referencia una cuenta para reconstruir la factura.");
        }

        ElectronicDocument invoice = documentBuilder.build(
                pos.getOpenAccountId(), ElectronicDocumentType.FE_VENTA, command.companyId(), false);
        ElectronicDocument emitted = emitter.emit(invoice);
        deliverService.deliverIfValidated(emitted);
        return ElectronicDocumentDto.from(emitted);
    }
}
