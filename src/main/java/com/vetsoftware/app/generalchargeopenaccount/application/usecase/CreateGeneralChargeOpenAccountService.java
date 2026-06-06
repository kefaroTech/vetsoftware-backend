package com.vetsoftware.app.generalchargeopenaccount.application.usecase;

import com.vetsoftware.app.generalchargeopenaccount.application.command.CreateGeneralChargeOpenAccountCommand;
import com.vetsoftware.app.generalchargeopenaccount.application.dto.GeneralChargeOpenAccountDto;
import com.vetsoftware.app.generalchargeopenaccount.application.port.in.CreateGeneralChargeOpenAccountUseCase;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.GeneralChargeOpenAccountRepository;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.OpenAccountQueryPort;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.TaxQueryPort;
import com.vetsoftware.app.generalchargeopenaccount.domain.EmployeeRef;
import com.vetsoftware.app.generalchargeopenaccount.domain.GeneralChargeOpenAccount;
import com.vetsoftware.app.generalchargeopenaccount.domain.OpenAccountRef;
import com.vetsoftware.app.generalchargeopenaccount.domain.TaxRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "general_charge_open_account.create")
@Service
public class CreateGeneralChargeOpenAccountService implements CreateGeneralChargeOpenAccountUseCase {
    private final GeneralChargeOpenAccountRepository repository;
    private final OpenAccountQueryPort openAccountQueryPort;
    private final TaxQueryPort taxQueryPort;
    private final EmployeeQueryPort employeeQueryPort;
    private final OpenAccountRefresher refresher;

    public CreateGeneralChargeOpenAccountService(GeneralChargeOpenAccountRepository repository,
                                                 OpenAccountQueryPort openAccountQueryPort,
                                                 TaxQueryPort taxQueryPort,
                                                 EmployeeQueryPort employeeQueryPort,
                                                 OpenAccountRefresher refresher) {
        this.repository = repository;
        this.openAccountQueryPort = openAccountQueryPort;
        this.taxQueryPort = taxQueryPort;
        this.employeeQueryPort = employeeQueryPort;
        this.refresher = refresher;
    }

    @Override
    @Transactional
    public GeneralChargeOpenAccountDto execute(CreateGeneralChargeOpenAccountCommand command) {
        OpenAccountRef openAccount = openAccountQueryPort.findById(command.openAccountId())
            .orElseThrow(() -> new IllegalArgumentException("OpenAccount not found: " + command.openAccountId()));
        if (!openAccount.companyId().equals(command.companyId())) {
            throw new IllegalArgumentException("open account does not belong to company");
        }
        if (!openAccountQueryPort.isOpen(command.openAccountId())) {
            throw new IllegalStateException("open account is not OPEN");
        }
        TaxRef tax = command.taxId() == null ? null
            : taxQueryPort.findById(command.taxId())
                .orElseThrow(() -> new IllegalArgumentException("Tax not found: " + command.taxId()));
        EmployeeRef createdBy = employeeQueryPort.findById(command.createdById())
            .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + command.createdById()));

        GeneralChargeOpenAccount charge = GeneralChargeOpenAccount.create(
            command.name(), command.unitAmount(), command.quantity(), tax, command.hasTax(),
            openAccount, createdBy);
        GeneralChargeOpenAccountDto dto = GeneralChargeOpenAccountDto.from(repository.save(charge));
        refresher.refresh(openAccount.id());
        return dto;
    }
}
