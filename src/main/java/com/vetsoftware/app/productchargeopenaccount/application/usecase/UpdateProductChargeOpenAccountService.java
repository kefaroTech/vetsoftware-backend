package com.vetsoftware.app.productchargeopenaccount.application.usecase;

import com.vetsoftware.app.productchargeopenaccount.application.command.UpdateProductChargeOpenAccountCommand;
import com.vetsoftware.app.productchargeopenaccount.application.dto.ProductChargeOpenAccountDto;
import com.vetsoftware.app.productchargeopenaccount.application.port.in.UpdateProductChargeOpenAccountUseCase;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.OpenAccountQueryPort;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.OpenAccountVersionGuard;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.ProductChargeOpenAccountRepository;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.ProductQueryPort;
import com.vetsoftware.app.productchargeopenaccount.domain.AnimalRef;
import com.vetsoftware.app.productchargeopenaccount.domain.OpenAccountRef;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductChargeOpenAccount;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductChargeOpenAccountNotFoundException;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "product.charge.open.account.update")
@Service
public class UpdateProductChargeOpenAccountService
        implements
            UpdateProductChargeOpenAccountUseCase {
    private final ProductChargeOpenAccountRepository repository;
    private final AnimalQueryPort animalQueryPort;
    private final ProductQueryPort productQueryPort;
    private final OpenAccountQueryPort openAccountQueryPort;
    private final OpenAccountRefresher refresher;
    private final OpenAccountVersionGuard versionGuard;

    public UpdateProductChargeOpenAccountService(ProductChargeOpenAccountRepository repository,
            AnimalQueryPort animalQueryPort, ProductQueryPort productQueryPort,
            OpenAccountQueryPort openAccountQueryPort, OpenAccountRefresher refresher,
            OpenAccountVersionGuard versionGuard) {
        this.repository = repository;
        this.animalQueryPort = animalQueryPort;
        this.productQueryPort = productQueryPort;
        this.openAccountQueryPort = openAccountQueryPort;
        this.refresher = refresher;
        this.versionGuard = versionGuard;
    }

    @Override
    @Transactional
    public ProductChargeOpenAccountDto execute(UpdateProductChargeOpenAccountCommand command) {
        ProductChargeOpenAccount charge = repository
                .findByIdAndCompanyId(command.id(), command.companyId())
                .orElseThrow(() -> new ProductChargeOpenAccountNotFoundException(command.id()));
        Long previousOpenAccountId = charge.getOpenAccount().id();

        // Carga ACOTADA por empresa: la cuenta destino de otro tenant no se resuelve,
        // asi
        // que mover el cargo a la cuenta de la empresa vecina deja de ser posible.
        // Antes
        // se cargaba ancha y la empresa se comparaba despues en Java.
        OpenAccountRef openAccount = openAccountQueryPort
                .findByIdAndCompanyId(command.openAccountId(), command.companyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "OpenAccount not found: " + command.openAccountId()));
        // Detección temprana de conflicto sobre la cuenta destino del cargo.
        versionGuard.assertVersion(command.companyId(), command.openAccountId(),
                command.expectedVersion());
        AnimalRef animal = animalQueryPort
                .findByIdAndCompanyId(command.animalId(), command.companyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Animal not found: " + command.animalId()));
        ProductRef product = productQueryPort
                .findByIdAndCompanyId(command.productId(), command.companyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Product not found: " + command.productId()));

        charge.update(animal, product, openAccount);
        ProductChargeOpenAccountDto dto = ProductChargeOpenAccountDto.from(repository.save(charge));
        refresher.refresh(command.companyId(), command.openAccountId());
        if (!command.openAccountId().equals(previousOpenAccountId)) {
            refresher.refresh(command.companyId(), previousOpenAccountId);
        }
        return dto;
    }
}
