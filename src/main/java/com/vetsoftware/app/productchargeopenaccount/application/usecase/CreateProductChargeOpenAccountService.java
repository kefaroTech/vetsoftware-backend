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

    public CreateProductChargeOpenAccountService(ProductChargeOpenAccountRepository repository,
                                                 AnimalQueryPort animalQueryPort,
                                                 ProductQueryPort productQueryPort,
                                                 OpenAccountQueryPort openAccountQueryPort,
                                                 EmployeeQueryPort employeeQueryPort,
                                                 OpenAccountRefresher refresher,
                                                 OpenAccountVersionGuard versionGuard) {
        this.repository = repository;
        this.animalQueryPort = animalQueryPort;
        this.productQueryPort = productQueryPort;
        this.openAccountQueryPort = openAccountQueryPort;
        this.employeeQueryPort = employeeQueryPort;
        this.refresher = refresher;
        this.versionGuard = versionGuard;
    }

    @Override
    @Transactional
    public ProductChargeOpenAccountDto execute(CreateProductChargeOpenAccountCommand command) {
        // Idempotencia: si el cargo ya se registró con esta clave (reintento/doble-submit), devolverlo sin
        // duplicar. Va al inicio para el camino rápido del reintento; la carrera concurrente la respalda la
        // constraint única. Pasa antes del lock para no bloquear la cuenta en un reintento ya resuelto.
        if (command.clientRequestId() != null && !command.clientRequestId().isBlank()) {
            Optional<ProductChargeOpenAccount> existing = repository.findByOpenAccountIdAndClientRequestId(
                command.openAccountId(), command.clientRequestId());
            if (existing.isPresent()) {
                return ProductChargeOpenAccountDto.from(existing.get());
            }
        }
        // Lock pesimista al inicio: serializa cargos/abonos concurrentes desde la validación de estado hasta
        // el recálculo (cierra el TOCTOU del isOpen/recálculo), no solo durante el recálculo final.
        openAccountQueryPort.lockForUpdate(command.openAccountId());
        OpenAccountRef openAccount = openAccountQueryPort.findById(command.openAccountId())
            .orElseThrow(() -> new IllegalArgumentException("OpenAccount not found: " + command.openAccountId()));
        if (!openAccount.companyId().equals(command.companyId())) {
            throw new IllegalArgumentException("open account does not belong to company");
        }
        // Detección temprana de conflicto: dentro del lock, antes de crear el cargo.
        versionGuard.assertVersion(command.openAccountId(), command.expectedVersion());
        if (!openAccountQueryPort.isOpen(command.openAccountId())) {
            throw new IllegalStateException("open account is not OPEN");
        }
        AnimalRef animal = animalQueryPort.findById(command.animalId())
            .orElseThrow(() -> new IllegalArgumentException("Animal not found: " + command.animalId()));
        ProductRef product = productQueryPort.findById(command.productId())
            .orElseThrow(() -> new IllegalArgumentException("Product not found: " + command.productId()));
        EmployeeRef createdBy = employeeQueryPort.findById(command.createdById())
            .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + command.createdById()));

        ProductChargeOpenAccount charge = ProductChargeOpenAccount.create(animal, product, openAccount, createdBy,
            command.clientRequestId());
        ProductChargeOpenAccountDto dto = ProductChargeOpenAccountDto.from(repository.save(charge));
        refresher.refresh(command.openAccountId());
        return dto;
    }
}
