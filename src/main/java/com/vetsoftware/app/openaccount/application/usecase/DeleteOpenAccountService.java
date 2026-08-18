package com.vetsoftware.app.openaccount.application.usecase;

import com.vetsoftware.app.openaccount.application.port.in.DeleteOpenAccountUseCase;
import com.vetsoftware.app.openaccount.application.port.out.OpenAccountRepository;
import com.vetsoftware.app.openaccount.domain.OpenAccountNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "open.account.delete")
@Service
public class DeleteOpenAccountService implements DeleteOpenAccountUseCase {
    private final OpenAccountRepository repository;

    public DeleteOpenAccountService(OpenAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void execute(Long id, Long companyId) {
        // El filtro por empresa va EN la consulta, no en un if posterior: el
        // companyId lo inyecta el controller desde el principal
        // (authz.currentCompanyId(), nunca null), asi que la cuenta de otro tenant
        // ni se carga. Un 404 no revela que la fila existe en otra empresa.
        repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new OpenAccountNotFoundException(id));
        repository.delete(id);
    }
}
