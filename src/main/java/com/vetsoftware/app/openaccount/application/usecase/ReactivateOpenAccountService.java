package com.vetsoftware.app.openaccount.application.usecase;

import com.vetsoftware.app.openaccount.application.dto.OpenAccountDto;
import com.vetsoftware.app.openaccount.application.port.in.ReactivateOpenAccountUseCase;
import com.vetsoftware.app.openaccount.application.port.out.OpenAccountRepository;
import com.vetsoftware.app.openaccount.domain.OpenAccount;
import com.vetsoftware.app.openaccount.domain.OpenAccountNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "open.account.reactivate")
@Service
public class ReactivateOpenAccountService implements ReactivateOpenAccountUseCase {
    private final OpenAccountRepository repository;

    public ReactivateOpenAccountService(OpenAccountRepository repository) {
        this.repository = repository;
    }

    /**
     * La comprobacion de empresa precede a la escritura, y ademas viaja dentro del
     * UPDATE. Antes se reactivaba primero y se comparaba la empresa despues: la
     * unica barrera era el rollback de la transaccion al lanzar la excepcion, asi
     * que un cambio de propagacion o un manejo distinto de la excepcion habria
     * dejado escrita la cuenta de otro tenant. Cero filas afectadas es «no existe
     * en TU empresa» y se responde 404 sin revelar que el id exista.
     */
    @Override
    @Transactional
    public OpenAccountDto execute(Long id, Long companyId) {
        int rows = repository.reactivate(id, companyId);
        if (rows == 0)
            throw new OpenAccountNotFoundException(id);
        OpenAccount openAccount = repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new OpenAccountNotFoundException(id));
        return OpenAccountDto.from(openAccount);
    }
}
