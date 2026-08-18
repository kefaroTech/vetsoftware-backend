package com.vetsoftware.app.productchargeopenaccount.application.usecase;

import com.vetsoftware.app.productchargeopenaccount.application.command.VoidProductChargeOpenAccountCommand;
import com.vetsoftware.app.productchargeopenaccount.application.dto.ProductChargeOpenAccountDto;
import com.vetsoftware.app.productchargeopenaccount.application.port.in.VoidProductChargeOpenAccountUseCase;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.InventoryLedgerPort;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.OpenAccountQueryPort;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.OpenAccountVersionGuard;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.ProductChargeOpenAccountRepository;
import com.vetsoftware.app.productchargeopenaccount.domain.EmployeeRef;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductChargeOpenAccount;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductChargeOpenAccountNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "product.charge.open.account.void")
@Service
public class VoidProductChargeOpenAccountService implements VoidProductChargeOpenAccountUseCase {
    private final ProductChargeOpenAccountRepository repository;
    private final OpenAccountQueryPort openAccountQueryPort;
    private final EmployeeQueryPort employeeQueryPort;
    private final OpenAccountRefresher refresher;
    private final OpenAccountVersionGuard versionGuard;
    private final InventoryLedgerPort inventoryLedger;

    public VoidProductChargeOpenAccountService(ProductChargeOpenAccountRepository repository,
            OpenAccountQueryPort openAccountQueryPort, EmployeeQueryPort employeeQueryPort,
            OpenAccountRefresher refresher, OpenAccountVersionGuard versionGuard,
            InventoryLedgerPort inventoryLedger) {
        this.repository = repository;
        this.openAccountQueryPort = openAccountQueryPort;
        this.employeeQueryPort = employeeQueryPort;
        this.refresher = refresher;
        this.versionGuard = versionGuard;
        this.inventoryLedger = inventoryLedger;
    }

    @Override
    @Transactional
    public ProductChargeOpenAccountDto execute(VoidProductChargeOpenAccountCommand command) {
        ProductChargeOpenAccount charge = repository
                .findByIdAndCompanyId(command.id(), command.companyId())
                .orElseThrow(() -> new ProductChargeOpenAccountNotFoundException(command.id()));
        Long openAccountId = charge.getOpenAccount().id();
        if (!charge.getOpenAccount().companyId().equals(command.companyId())) {
            throw new IllegalArgumentException("product charge does not belong to company");
        }
        // Lock pesimista de la cuenta antes de leer su estado/saldo: serializa la
        // anulación frente a
        // cargos/abonos/cierre concurrentes (cierra el TOCTOU del isOpen/saldo), no
        // solo en el
        // recálculo.
        // Acotado por empresa como en el alta: aqui la cuenta ya se demostro propia (el
        // cargo se cargo acotado), asi que el companyId no cambia el resultado, pero el
        // puerto no ofrece variante ancha para que no vuelva a colarse una.
        openAccountQueryPort.lockForUpdate(openAccountId, command.companyId());
        // Detección temprana de conflicto sobre la cuenta del cargo.
        versionGuard.assertVersion(command.companyId(), openAccountId, command.expectedVersion());
        if (!openAccountQueryPort.isOpen(openAccountId)) {
            throw new IllegalStateException("open account is not OPEN");
        }
        // No se puede anular un cargo si eso dejaría el saldo pendiente negativo (hay
        // abonos que
        // ya cubren más de lo que quedaría). Regla: monto del cargo <= saldo pendiente
        // actual.
        BigDecimal outstanding = openAccountQueryPort.outstandingAmount(openAccountId);
        if (charge.getTotalAmount().compareTo(outstanding) > 0) {
            throw new IllegalStateException(
                    "No se puede anular el cargo: el saldo pendiente quedaría negativo. "
                            + "Hay abonos que lo cubren; anula primero los abonos necesarios.");
        }
        EmployeeRef voidedBy = employeeQueryPort
                .findByIdAndCompanyId(command.voidedById(), command.companyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Employee not found: " + command.voidedById()));

        charge.voidCharge(voidedBy, command.reason());
        ProductChargeOpenAccountDto dto = ProductChargeOpenAccountDto.from(repository.save(charge));
        // Repone el inventario descontado al crear el cargo: compensa en el kardex los
        // movimientos de
        // esta referencia
        // (idempotente). La sede/lote se toman de los movimientos originales, no hace
        // falta
        // recalcularlos.
        inventoryLedger.reverseSale(command.id(), command.companyId(), command.voidedById());
        refresher.refresh(command.companyId(), openAccountId);
        return dto;
    }
}
