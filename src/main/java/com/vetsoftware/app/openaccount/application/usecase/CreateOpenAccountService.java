package com.vetsoftware.app.openaccount.application.usecase;

import com.vetsoftware.app.openaccount.application.command.CreateOpenAccountCommand;
import com.vetsoftware.app.openaccount.application.command.SearchOpenAccountsCommand;
import com.vetsoftware.app.openaccount.application.dto.OpenAccountDto;
import com.vetsoftware.app.openaccount.application.port.in.CreateOpenAccountUseCase;
import com.vetsoftware.app.openaccount.application.port.out.BranchQueryPort;
import com.vetsoftware.app.openaccount.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.openaccount.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.openaccount.application.port.out.OpenAccountRepository;
import com.vetsoftware.app.openaccount.application.port.out.OwnerQueryPort;
import com.vetsoftware.app.openaccount.domain.BranchRef;
import com.vetsoftware.app.openaccount.domain.CompanyRef;
import com.vetsoftware.app.openaccount.domain.EmployeeRef;
import com.vetsoftware.app.openaccount.domain.OpenAccount;
import com.vetsoftware.app.openaccount.domain.OpenAccountStatus;
import com.vetsoftware.app.openaccount.domain.OwnerRef;
import io.micrometer.observation.annotation.Observed;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Observed(name = "open.account.create")
@Service
public class CreateOpenAccountService implements CreateOpenAccountUseCase {
    private final OpenAccountRepository repository;
    private final OwnerQueryPort ownerQueryPort;
    private final CompanyQueryPort companyQueryPort;
    private final EmployeeQueryPort employeeQueryPort;
    private final BranchQueryPort branchQueryPort;

    public CreateOpenAccountService(OpenAccountRepository repository,
                                    OwnerQueryPort ownerQueryPort,
                                    CompanyQueryPort companyQueryPort,
                                    EmployeeQueryPort employeeQueryPort,
                                    BranchQueryPort branchQueryPort) {
        this.repository = repository;
        this.ownerQueryPort = ownerQueryPort;
        this.companyQueryPort = companyQueryPort;
        this.employeeQueryPort = employeeQueryPort;
        this.branchQueryPort = branchQueryPort;
    }

    // NOTA (regla de negocio): una cuenta abierta no debe existir sin cargos. Como los
    // cargos (product/service/general charge) son features aparte que referencian la cuenta
    // ya creada, esta invariante la GARANTIZA EL FLUJO DEL FRONT (siempre crea cuenta + ≥1
    // cargo juntos; nunca llama a este create de forma aislada sin cargos a continuación).
    // El back no la enforza atómicamente para no acoplar openaccount con las features de cargos.
    // Si un fallo parcial deja una cuenta vacía, el get-or-create de abajo la REUTILIZA (no se
    // crea otra ni bloquea el cupo de 1-por-dueño-y-sede).
    @Override
    public OpenAccountDto execute(CreateOpenAccountCommand command) {
        // La cuenta se administra por sede: primero se resuelve la sede solicitada y el get-or-create
        // se limita a (empresa, sede, propietario). El mismo propietario puede tener una cuenta OPEN
        // independiente en otra sede, pero nunca dos en la misma.
        BranchRef branch = command.branchId() != null
            ? resolveRequestedBranch(command.branchId(), command.companyId())
            : branchQueryPort.findDefaultActiveByCompanyId(command.companyId())
                .orElseThrow(() -> new IllegalArgumentException(
                    "Company has no active branch: " + command.companyId()));

        Optional<OpenAccount> existing = repository
            .search(new SearchOpenAccountsCommand(
                command.companyId(), command.ownerId(), true, 0, 50, branch.id()))
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
        OpenAccount openAccount = OpenAccount.create(owner, company, branch, createdBy);
        return OpenAccountDto.from(repository.save(openAccount));
    }

    // Sede solicitada explícitamente: activa y de la empresa. Distingue "inactiva" de "inexistente" para dar
    // un error preciso (la sede existe pero fue desactivada vs. no pertenece a la empresa / no existe).
    private BranchRef resolveRequestedBranch(Long branchId, Long companyId) {
        return branchQueryPort.findActiveByIdAndCompanyId(branchId, companyId)
            .orElseThrow(() -> branchQueryPort.existsByIdAndCompanyId(branchId, companyId)
                ? new IllegalArgumentException("Branch is not active: " + branchId)
                : new IllegalArgumentException("Branch not found: " + branchId));
    }
}
