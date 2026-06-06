package com.vetsoftware.app.productchargeopenaccount.application.usecase;

import com.vetsoftware.app.productchargeopenaccount.application.command.CreateProductChargeOpenAccountCommand;
import com.vetsoftware.app.productchargeopenaccount.application.dto.ProductChargeOpenAccountDto;
import com.vetsoftware.app.productchargeopenaccount.application.port.in.CreateProductChargeOpenAccountUseCase;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.OpenAccountQueryPort;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.ProductChargeOpenAccountRepository;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.ProductQueryPort;
import com.vetsoftware.app.productchargeopenaccount.domain.AnimalRef;
import com.vetsoftware.app.productchargeopenaccount.domain.EmployeeRef;
import com.vetsoftware.app.productchargeopenaccount.domain.OpenAccountRef;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductChargeOpenAccount;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductRef;
import io.micrometer.observation.annotation.Observed;
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

    public CreateProductChargeOpenAccountService(ProductChargeOpenAccountRepository repository,
                                                 AnimalQueryPort animalQueryPort,
                                                 ProductQueryPort productQueryPort,
                                                 OpenAccountQueryPort openAccountQueryPort,
                                                 EmployeeQueryPort employeeQueryPort,
                                                 OpenAccountRefresher refresher) {
        this.repository = repository;
        this.animalQueryPort = animalQueryPort;
        this.productQueryPort = productQueryPort;
        this.openAccountQueryPort = openAccountQueryPort;
        this.employeeQueryPort = employeeQueryPort;
        this.refresher = refresher;
    }

    @Override
    @Transactional
    public ProductChargeOpenAccountDto execute(CreateProductChargeOpenAccountCommand command) {
        OpenAccountRef openAccount = openAccountQueryPort.findById(command.openAccountId())
            .orElseThrow(() -> new IllegalArgumentException("OpenAccount not found: " + command.openAccountId()));
        if (!openAccount.companyId().equals(command.companyId())) {
            throw new IllegalArgumentException("open account does not belong to company");
        }
        if (!openAccountQueryPort.isOpen(command.openAccountId())) {
            throw new IllegalStateException("open account is not OPEN");
        }
        AnimalRef animal = animalQueryPort.findById(command.animalId())
            .orElseThrow(() -> new IllegalArgumentException("Animal not found: " + command.animalId()));
        ProductRef product = productQueryPort.findById(command.productId())
            .orElseThrow(() -> new IllegalArgumentException("Product not found: " + command.productId()));
        EmployeeRef createdBy = employeeQueryPort.findById(command.createdById())
            .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + command.createdById()));

        ProductChargeOpenAccount charge = ProductChargeOpenAccount.create(animal, product, openAccount, createdBy);
        ProductChargeOpenAccountDto dto = ProductChargeOpenAccountDto.from(repository.save(charge));
        refresher.refresh(command.openAccountId());
        return dto;
    }
}
