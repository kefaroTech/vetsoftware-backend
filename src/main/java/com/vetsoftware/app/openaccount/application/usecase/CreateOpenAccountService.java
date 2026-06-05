package com.vetsoftware.app.openaccount.application.usecase;

import com.vetsoftware.app.openaccount.application.command.CreateOpenAccountCommand;
import com.vetsoftware.app.openaccount.application.dto.OpenAccountDto;
import com.vetsoftware.app.openaccount.application.port.in.CreateOpenAccountUseCase;
import com.vetsoftware.app.openaccount.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.openaccount.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.openaccount.application.port.out.OpenAccountRepository;
import com.vetsoftware.app.openaccount.application.port.out.OwnerQueryPort;
import com.vetsoftware.app.openaccount.domain.CompanyRef;
import com.vetsoftware.app.openaccount.domain.EmployeeRef;
import com.vetsoftware.app.openaccount.domain.OpenAccount;
import com.vetsoftware.app.openaccount.domain.OwnerAlreadyHasOpenAccountException;
import com.vetsoftware.app.openaccount.domain.OwnerRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "open_account.create")
@Service
public class CreateOpenAccountService implements CreateOpenAccountUseCase {
    private final OpenAccountRepository repository;
    private final OwnerQueryPort ownerQueryPort;
    private final CompanyQueryPort companyQueryPort;
    private final EmployeeQueryPort employeeQueryPort;

    public CreateOpenAccountService(OpenAccountRepository repository,
                                    OwnerQueryPort ownerQueryPort,
                                    CompanyQueryPort companyQueryPort,
                                    EmployeeQueryPort employeeQueryPort) {
        this.repository = repository;
        this.ownerQueryPort = ownerQueryPort;
        this.companyQueryPort = companyQueryPort;
        this.employeeQueryPort = employeeQueryPort;
    }

    // NOTA (regla de negocio): una cuenta abierta no debe existir sin cargos. Como los
    // cargos (product/service/general charge) son features aparte que referencian la cuenta
    // ya creada, esta invariante la GARANTIZA EL FLUJO DEL FRONT (siempre crea cuenta + ≥1
    // cargo juntos; nunca llama a este create de forma aislada sin cargos a continuación).
    // El back no la enforza atómicamente para no acoplar openaccount con las features de cargos.
    @Override
    public OpenAccountDto execute(CreateOpenAccountCommand command) {
        OwnerRef owner = ownerQueryPort.findById(command.ownerId())
            .orElseThrow(() -> new IllegalArgumentException("Owner not found: " + command.ownerId()));
        // Regla de negocio: un propietario solo puede tener una cuenta abierta a la vez.
        if (repository.existsActiveByOwnerId(command.ownerId())) {
            throw new OwnerAlreadyHasOpenAccountException(command.ownerId());
        }
        CompanyRef company = companyQueryPort.findById(command.companyId())
            .orElseThrow(() -> new IllegalArgumentException("Company not found: " + command.companyId()));
        EmployeeRef createdBy = employeeQueryPort.findById(command.createdById())
            .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + command.createdById()));

        OpenAccount openAccount = OpenAccount.create(owner, company, createdBy);
        return OpenAccountDto.from(repository.save(openAccount));
    }
}
