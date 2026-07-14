package com.vetsoftware.app.electronicdocument.application.usecase;

import com.vetsoftware.app.electronicdocument.application.command.RegisterPosSaleCommand;
import com.vetsoftware.app.electronicdocument.application.command.RegisterPosSaleCommand.SaleLine;
import com.vetsoftware.app.electronicdocument.application.command.SaleLineKind;
import com.vetsoftware.app.electronicdocument.application.dto.ElectronicDocumentDto;
import com.vetsoftware.app.electronicdocument.application.port.in.RegisterPosSaleUseCase;
import com.vetsoftware.app.electronicdocument.application.port.out.CashPort;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.application.port.out.InventoryLedgerPort;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import io.micrometer.observation.annotation.Observed;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra y emite una venta de POS: construye el documento PENDIENTE desde el payload y lo pasa por el
 * emisor (mismo gate BILLING que el cierre de cuenta). Con BILLING: numera + transmite. Sin BILLING: queda
 * PENDIENTE (datos guardados, emision a la DIAN diferida y re-emitible al habilitar el modulo).
 *
 * <p>Efecto inventario (F2b): cada línea de producto descuenta el kardex de la sede emisora (ref {@code POS_DOCUMENT},
 * idempotente por el id del documento). Ocurre aunque el documento quede PENDIENTE (la venta física ya sucedió) y
 * SIEMPRE permite negativo (mostrador no se frena por stock). Las líneas de servicio/general no mueven inventario.
 */
@Observed(name = "electronicDocument.posSale")
@Service
public class RegisterPosSaleService implements RegisterPosSaleUseCase {
    private final PosSaleDocumentBuilder documentBuilder;
    private final ElectronicDocumentEmitter emitter;
    private final DeliverElectronicDocumentService deliverService;
    private final ElectronicDocumentRepository repository;
    private final InventoryLedgerPort inventoryLedger;
    private final CashPort cashPort;

    public RegisterPosSaleService(PosSaleDocumentBuilder documentBuilder,
                                  ElectronicDocumentEmitter emitter,
                                  DeliverElectronicDocumentService deliverService,
                                  ElectronicDocumentRepository repository,
                                  InventoryLedgerPort inventoryLedger,
                                  CashPort cashPort) {
        this.documentBuilder = documentBuilder;
        this.emitter = emitter;
        this.deliverService = deliverService;
        this.repository = repository;
        this.inventoryLedger = inventoryLedger;
        this.cashPort = cashPort;
    }

    @Override
    @Transactional
    public ElectronicDocumentDto execute(RegisterPosSaleCommand command) {
        // Idempotencia: si el POST se reintenta con la misma key (respuesta perdida en transporte), se
        // devuelve el documento ya emitido en vez de registrar y transmitir OTRA venta a la DIAN. El índice
        // único (company_id, client_request_id) respalda la carrera concurrente (la 2ª inserción la rechaza la BD).
        if (command.clientRequestId() != null && !command.clientRequestId().isBlank()) {
            Optional<ElectronicDocument> existing =
                    repository.findByCompanyIdAndClientRequestId(command.companyId(), command.clientRequestId());
            if (existing.isPresent()) {
                return ElectronicDocumentDto.from(existing.get());
            }
        }
        // Rechaza cantidades de producto no enteras ANTES de construir/transmitir: no queremos transmitir a la DIAN
        // una venta que luego revertiríamos localmente. Un producto se vende por unidad entera.
        validateProductQuantities(command);

        ElectronicDocument document = documentBuilder.build(command);
        // Bloqueo "caja requerida" (F4): si la empresa lo exige y la sede no tiene caja OPEN, corta ANTES de emitir
        // (nada se transmite a la DIAN). No-op si la empresa no exige caja.
        cashPort.requireOpenSession(command.companyId(), document.getBranchId(), command.issuedByEmployeeId());
        ElectronicDocument emitted = emitter.emit(document);
        // Descuenta inventario por cada línea de producto (misma transacción). El documento POS no lleva cuenta
        // abierta, así que ninguna otra ruta descontó estas líneas.
        discountStock(command, emitted);
        // Registra el cobro en la caja OPEN de la sede (SALE_IN por método, ref POS_DOCUMENT). Idempotente y no-op si
        // no hay caja abierta. Solo POS directo: una emisión desde cuenta cerrada ya movió caja como abono (F3).
        registerCash(command, emitted);
        deliverService.deliverIfValidated(emitted);
        return ElectronicDocumentDto.from(emitted);
    }

    private void registerCash(RegisterPosSaleCommand command, ElectronicDocument document) {
        if (document.getOpenAccountId() != null || document.getPayments().isEmpty()) return;
        List<CashPort.PaymentLine> payments = document.getPayments().stream()
            .map(p -> new CashPort.PaymentLine(p.getPaymentMeans(), p.getAmount()))
            .toList();
        cashPort.registerSale(command.companyId(), document.getBranchId(), document.getId(), payments,
            command.issuedByEmployeeId());
    }

    private void validateProductQuantities(RegisterPosSaleCommand command) {
        for (SaleLine line : command.lines()) {
            if (line.kind() == SaleLineKind.PRODUCT) {
                toUnits(line.refId(), line.quantity());
            }
        }
    }

    private void discountStock(RegisterPosSaleCommand command, ElectronicDocument document) {
        for (SaleLine line : command.lines()) {
            if (line.kind() != SaleLineKind.PRODUCT) continue;
            inventoryLedger.recordPosSale(command.companyId(), document.getBranchId(), line.refId(),
                toUnits(line.refId(), line.quantity()), document.getId(), command.issuedByEmployeeId());
        }
    }

    /** Convierte la cantidad a unidades enteras; rechaza fracciones (un producto se vende por unidad). */
    private static int toUnits(Long productId, BigDecimal quantity) {
        if (quantity == null || quantity.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException(
                "La cantidad de un producto debe ser entera (producto " + productId + "): " + quantity);
        }
        return quantity.intValueExact();
    }
}
