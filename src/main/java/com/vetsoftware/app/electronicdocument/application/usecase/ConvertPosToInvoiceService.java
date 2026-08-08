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
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Convierte un documento equivalente POS en factura electrónica de venta: emite
 * una nueva FE_VENTA desde la misma cuenta del POS.
 *
 * <p>
 * TODO: vincular formalmente la FE con el POS (referencedCude) — depende del
 * soporte del proveedor; hoy la conversión emite una FE independiente sobre la
 * misma venta.
 */
@Observed(name = "electronic.document.convert.pos")
@Service
public class ConvertPosToInvoiceService implements ConvertPosToInvoiceUseCase {
    private final ElectronicDocumentRepository repository;
    private final DocumentBuilder documentBuilder;
    private final ElectronicDocumentEmitter emitter;
    private final DeliverElectronicDocumentService deliverService;
    private final TransactionTemplate transactionTemplate;

    public ConvertPosToInvoiceService(ElectronicDocumentRepository repository,
            DocumentBuilder documentBuilder, ElectronicDocumentEmitter emitter,
            DeliverElectronicDocumentService deliverService,
            TransactionTemplate transactionTemplate) {
        this.repository = repository;
        this.documentBuilder = documentBuilder;
        this.emitter = emitter;
        this.deliverService = deliverService;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * Sin {@code @Transactional} en el metodo completo: la factura se persiste
     * PENDIENTE en una transaccion corta que commitea, y solo despues se numera, se
     * transmite y se entrega. El HTTP al proveedor —hasta 75 segundos— dejaria
     * retenidos la conexion del pool y el {@code FOR UPDATE} del consecutivo.
     */
    @Override
    public ElectronicDocumentDto execute(ConvertPosToInvoiceCommand command) {
        ElectronicDocument invoice = transactionTemplate.execute(status -> buildPending(command));
        ElectronicDocument emitted = emitter.emit(invoice);
        deliverService.deliverIfValidated(emitted);
        return ElectronicDocumentDto.from(emitted);
    }

    private ElectronicDocument buildPending(ConvertPosToInvoiceCommand command) {
        ElectronicDocument pos = repository.findById(command.posDocumentId()).orElseThrow(
                () -> new ElectronicDocumentNotFoundException(command.posDocumentId()));
        if (!command.companyId().equals(pos.getCompanyId())) {
            throw new ElectronicDocumentNotFoundException(command.posDocumentId());
        }
        if (pos.getDocumentType() != ElectronicDocumentType.DOC_EQUIV_POS) {
            throw new IllegalStateException(
                    "Solo un documento equivalente POS puede convertirse a factura.");
        }
        if (pos.getOpenAccountId() == null) {
            throw new IllegalStateException(
                    "El documento POS no referencia una cuenta para reconstruir la factura.");
        }
        // 3.11/B4 - idempotencia: si la cuenta ya tiene una FE_VENTA, el POS ya fue
        // convertido. Evita
        // emitir N
        // facturas sobre la misma venta (doble registro del ingreso).
        if (repository.existsByOpenAccountIdAndDocumentType(pos.getOpenAccountId(),
                ElectronicDocumentType.FE_VENTA)) {
            throw new IllegalStateException(
                    "La cuenta ya tiene una factura electrónica: el documento POS ya fue convertido.");
        }

        return documentBuilder.build(pos.getOpenAccountId(), ElectronicDocumentType.FE_VENTA,
                command.companyId(), false);
    }
}
