package com.vetsoftware.app.productchargeopenaccount.application.usecase;

import com.vetsoftware.app.productchargeopenaccount.application.command.CreateProductChargeOpenAccountCommand;
import com.vetsoftware.app.productchargeopenaccount.application.dto.ProductChargeOpenAccountDto;
import com.vetsoftware.app.productchargeopenaccount.application.port.in.CreateProductChargeOpenAccountUseCase;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.OpenAccountQueryPort;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.OpenAccountVersionGuard;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.ProductChargeOpenAccountRepository;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.ProductQueryPort;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.ProductStockPort;
import com.vetsoftware.app.productchargeopenaccount.domain.AnimalRef;
import com.vetsoftware.app.productchargeopenaccount.domain.EmployeeRef;
import com.vetsoftware.app.productchargeopenaccount.domain.OpenAccountRef;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductChargeOpenAccount;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductRef;
import io.micrometer.observation.annotation.Observed;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "product_charge_open_account.create")
@Service
public class CreateProductChargeOpenAccountService implements CreateProductChargeOpenAccountUseCase {
    private final ProductChargeOpenAccountRepository repository;
    private final AnimalQueryPort animalQueryPort;
    private final ProductQueryPort productQueryPort;
    private final OpenAccountQueryPort openAccountQueryPort;
    private final EmployeeQueryPort employeeQueryPort;
    private final OpenAccountRefresher refresher;
    private final OpenAccountVersionGuard versionGuard;
    private final ProductStockPort stockPort;

    public CreateProductChargeOpenAccountService(ProductChargeOpenAccountRepository repository,
                                                 AnimalQueryPort animalQueryPort,
                                                 ProductQueryPort productQueryPort,
                                                 OpenAccountQueryPort openAccountQueryPort,
                                                 EmployeeQueryPort employeeQueryPort,
                                                 OpenAccountRefresher refresher,
                                                 OpenAccountVersionGuard versionGuard,
                                                 ProductStockPort stockPort) {
        this.repository = repository;
        this.animalQueryPort = animalQueryPort;
        this.productQueryPort = productQueryPort;
        this.openAccountQueryPort = openAccountQueryPort;
        this.employeeQueryPort = employeeQueryPort;
        this.refresher = refresher;
        this.versionGuard = versionGuard;
        this.stockPort = stockPort;
    }

    @Override
    @Transactional
    public ProductChargeOpenAccountDto execute(CreateProductChargeOpenAccountCommand command) {
        // Lock pesimista como PRIMERA sentencia: serializa cargos/abonos concurrentes desde la validación de
        // estado hasta el recálculo (cierra el TOCTOU del isOpen/recálculo), no solo durante el recálculo final.
        openAccountQueryPort.lockForUpdate(command.openAccountId());
        // Idempotencia: si el cargo ya se registró con esta clave (reintento/doble-submit), devolverlo sin
        // duplicar. Va DESPUÉS del lock (no antes): así un reintento concurrente que llega segundo lee —ya dentro
        // del lock— el cargo committeado por el rival y lo devuelve, en vez de chocar con la constraint única
        // (500). Mismo orden que el abono (CreateDebtOpenAccountService).
        if (command.clientRequestId() != null && !command.clientRequestId().isBlank()) {
            Optional<ProductChargeOpenAccount> existing = repository.findByOpenAccountIdAndClientRequestId(
                command.openAccountId(), command.clientRequestId());
            if (existing.isPresent()) {
                return ProductChargeOpenAccountDto.from(existing.get());
            }
        }
        OpenAccountRef openAccount = openAccountQueryPort.findById(command.openAccountId())
            .orElseThrow(() -> new IllegalArgumentException("OpenAccount not found: " + command.openAccountId()));
        if (!openAccount.companyId().equals(command.companyId())) {
            throw new IllegalArgumentException("open account does not belong to company");
        }
        // Detección temprana de conflicto: dentro del lock, antes de crear el cargo.
        versionGuard.assertVersion(command.companyId(), command.openAccountId(), command.expectedVersion());
        if (!openAccountQueryPort.isOpen(command.openAccountId())) {
            throw new IllegalStateException("open account is not OPEN");
        }
        AnimalRef animal = animalQueryPort.findByIdAndCompanyId(command.animalId(), command.companyId())
            .orElseThrow(() -> new IllegalArgumentException("Animal not found: " + command.animalId()));
        ProductRef product = productQueryPort.findByIdAndCompanyId(command.productId(), command.companyId())
            .orElseThrow(() -> new IllegalArgumentException("Product not found: " + command.productId()));
        EmployeeRef createdBy = employeeQueryPort.findByIdAndCompanyId(command.createdById(), command.companyId())
            .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + command.createdById()));

        ProductChargeOpenAccount charge = ProductChargeOpenAccount.create(animal, product, command.quantity(),
            openAccount, createdBy, command.clientRequestId());
        ProductChargeOpenAccountDto dto = ProductChargeOpenAccountDto.from(repository.save(charge));
        // Descuenta stock del catálogo por la venta (atómico; permite negativo). Va dentro de la misma
        // transacción: si algo falla después, el descuento se revierte con el resto.
        stockPort.decreaseStock(command.productId(), command.companyId(), command.quantity());
        refresher.refresh(command.companyId(), command.openAccountId());
        return dto;
    }
}
