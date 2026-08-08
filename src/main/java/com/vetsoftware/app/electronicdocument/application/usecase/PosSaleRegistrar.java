package com.vetsoftware.app.electronicdocument.application.usecase;

import com.vetsoftware.app.electronicdocument.application.command.RegisterPosSaleCommand;
import com.vetsoftware.app.electronicdocument.application.command.RegisterPosSaleCommand.SaleLine;
import com.vetsoftware.app.electronicdocument.application.command.SaleLineKind;
import com.vetsoftware.app.electronicdocument.application.port.out.CashPort;
import com.vetsoftware.app.electronicdocument.application.port.out.InventoryLedgerPort;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tiempo 1 de la venta POS: todo lo que toca la base de datos, en una
 * transaccion corta que commitea ANTES de que empiece el I/O externo.
 *
 * <p>
 * Vive en su propio bean y no como metodo privado de
 * {@link RegisterPosSaleService} porque Spring aplica {@code @Transactional}
 * por proxy: una llamada entre metodos de la misma clase no lo atraviesa y la
 * transaccion nunca se abriria.
 *
 * <p>
 * El orden importa. La sesion de caja se valida antes de escribir nada, para
 * que una caja cerrada aborte la venta sin dejar rastro. El descuento de stock
 * y el registro de caja quedan dentro de esta misma transaccion: o se registran
 * los dos, o ninguno.
 */
@Component
public class PosSaleRegistrar {

    private final PosSaleDocumentBuilder documentBuilder;
    private final InventoryLedgerPort inventoryLedger;
    private final CashPort cashPort;

    public PosSaleRegistrar(PosSaleDocumentBuilder documentBuilder,
            InventoryLedgerPort inventoryLedger, CashPort cashPort) {
        this.documentBuilder = documentBuilder;
        this.inventoryLedger = inventoryLedger;
        this.cashPort = cashPort;
    }

    @Transactional
    public ElectronicDocument registerPending(RegisterPosSaleCommand command) {
        ElectronicDocument document = documentBuilder.build(command);
        // Si la empresa exige caja, se valida antes de mover inventario o dinero.
        cashPort.requireOpenSession(command.companyId(), document.getBranchId(),
                command.issuedByEmployeeId());
        discountStock(command, document);
        registerCash(command, document);
        return document;
    }

    private void discountStock(RegisterPosSaleCommand command, ElectronicDocument document) {
        for (SaleLine line : command.lines()) {
            if (line.kind() != SaleLineKind.PRODUCT)
                continue;
            inventoryLedger.recordPosSale(command.companyId(), document.getBranchId(), line.refId(),
                    toUnits(line.refId(), line.quantity()), document.getId(),
                    command.issuedByEmployeeId());
        }
    }

    private void registerCash(RegisterPosSaleCommand command, ElectronicDocument document) {
        if (document.getOpenAccountId() != null || document.getPayments().isEmpty())
            return;
        List<CashPort.PaymentLine> payments = document.getPayments().stream()
                .map(p -> new CashPort.PaymentLine(p.getPaymentMeans(), p.getAmount())).toList();
        cashPort.registerSale(command.companyId(), document.getBranchId(), document.getId(),
                payments, command.issuedByEmployeeId());
    }

    /**
     * Convierte la cantidad a unidades enteras; rechaza fracciones (un producto se
     * vende por unidad).
     */
    private static int toUnits(Long productId, BigDecimal quantity) {
        if (quantity == null || quantity.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException(
                    "La cantidad de un producto debe ser entera (producto " + productId + "): "
                            + quantity);
        }
        return quantity.intValueExact();
    }
}
