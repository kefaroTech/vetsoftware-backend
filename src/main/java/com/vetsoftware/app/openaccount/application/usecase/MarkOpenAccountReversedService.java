package com.vetsoftware.app.openaccount.application.usecase;

import com.vetsoftware.app.openaccount.application.command.MarkOpenAccountReversedCommand;
import com.vetsoftware.app.openaccount.application.port.in.MarkOpenAccountReversedUseCase;
import com.vetsoftware.app.openaccount.application.port.out.OpenAccountRepository;
import com.vetsoftware.app.openaccount.domain.OpenAccount;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "open.account.mark.reversed")
@Service
public class MarkOpenAccountReversedService implements MarkOpenAccountReversedUseCase {
    private static final Logger log = LoggerFactory.getLogger(MarkOpenAccountReversedService.class);

    private final OpenAccountRepository repository;

    public MarkOpenAccountReversedService(OpenAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void execute(MarkOpenAccountReversedCommand command) {
        // Lock pesimista acotado a la empresa, el mismo de Recalculate: el reverso
        // reescribe la fila entera desde el snapshot de dominio, asi que sin
        // serializar contra un recalculo concurrente uno de los dos se pierde. Y
        // acotado, para que una cuenta ajena no llegue siquiera a bloquearse.
        OpenAccount openAccount = repository
                .findByIdForUpdateAndCompanyId(command.openAccountId(), command.companyId())
                .orElse(null);
        if (openAccount == null) {
            // No se lanza: esto corre dentro de la transaccion que acaba de registrar una
            // nota credito ya VALIDADA por la DIAN, y hacerla revertir dejaria el
            // documento fiscal sin persistir por una cuenta que ya no esta. Queda el
            // rastro para poder conciliarlo despues.
            log.warn("Reverso de cartera sin efecto: la cuenta {} no existe en la empresa {}",
                    command.openAccountId(), command.companyId());
            return;
        }
        // La regla entera —guarda de estado e idempotencia— es del dominio. Aqui solo
        // se persiste si de verdad cambio algo: repetir el reverso no debe mover la
        // version ni disparar un UPDATE.
        if (openAccount.markReversed(command.reversedAt())) {
            repository.save(openAccount);
        }
    }
}
