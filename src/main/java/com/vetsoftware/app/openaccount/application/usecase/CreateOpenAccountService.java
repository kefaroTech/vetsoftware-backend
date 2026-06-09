package com.vetsoftware.app.openaccount.application.usecase;

import com.vetsoftware.app.openaccount.application.command.CreateOpenAccountCommand;
import com.vetsoftware.app.openaccount.application.command.SearchOpenAccountsCommand;
import com.vetsoftware.app.openaccount.application.dto.OpenAccountDto;
import com.vetsoftware.app.openaccount.application.port.in.CreateOpenAccountUseCase;
import com.vetsoftware.app.openaccount.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.openaccount.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.openaccount.application.port.out.OpenAccountRepository;
import com.vetsoftware.app.openaccount.application.port.out.OwnerQueryPort;
import com.vetsoftware.app.openaccount.domain.CompanyRef;
import com.vetsoftware.app.openaccount.domain.EmployeeRef;
import com.vetsoftware.app.openaccount.domain.OpenAccount;
import com.vetsoftware.app.openaccount.domain.OpenAccountStatus;
import com.vetsoftware.app.openaccount.domain.OwnerRef;
import io.micrometer.observation.annotation.Observed;
import java.util.Optional;
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
    // Si un fallo parcial deja una cuenta vacía, el get-or-create de abajo la REUTILIZA (no se
    // crea otra ni bloquea el cupo de 1-por-dueño).
    @Override
    public OpenAccountDto execute(CreateOpenAccountCommand command) {
        // Get-or-create idempotente por dueño: la regla es UNA cuenta abierta por propietario,
        // así que el ownerId ES la clave de idempotencia. Si ya existe una cuenta OPEN se
        // devuelve esa en vez de fallar — un reintento tras perder la respuesta no duplica ni
        // choca con el 409 de la constraint, y una cuenta abierta vacía se reutiliza.
        Optional<OpenAccount> existing = repository
            .search(new SearchOpenAccountsCommand(command.companyId(), command.ownerId(), true, 0, 50))
            .content().stream()
            .filter(a -> a.getStatus() == OpenAccountStatus.OPEN)
            .findFirst();
        if (existing.isPresent()) {
            return OpenAccountDto.from(existing.get());
        }

        OwnerRef owner = ownerQueryPort.findById(command.ownerId())
            .orElseThrow(() -> new IllegalArgumentException("Owner not found: " + command.ownerId()));
        CompanyRef company = companyQueryPort.findById(command.companyId())
            .orElseThrow(() -> new IllegalArgumentException("Company not found: " + command.companyId()));
        EmployeeRef createdBy = employeeQueryPort.findById(command.createdById())
            .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + command.createdById()));

        OpenAccount openAccount = OpenAccount.create(owner, company, createdBy);
        return OpenAccountDto.from(repository.save(openAccount));
    }
}
