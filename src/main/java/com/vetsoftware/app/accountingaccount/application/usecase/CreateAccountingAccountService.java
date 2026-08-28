package com.vetsoftware.app.accountingaccount.application.usecase;

import com.vetsoftware.app.accountingaccount.application.command.CreateAccountingAccountCommand;
import com.vetsoftware.app.accountingaccount.application.dto.AccountingAccountDto;
import com.vetsoftware.app.accountingaccount.application.port.in.CreateAccountingAccountUseCase;
import com.vetsoftware.app.accountingaccount.application.port.out.AccountingAccountRepository;
import com.vetsoftware.app.accountingaccount.domain.AccountingAccount;
import com.vetsoftware.app.accountingaccount.domain.AccountingAccountNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Da de alta una cuenta del plan contable.
 *
 * <p>
 * <strong>El service hace exactamente dos cosas, y ninguna es validar la
 * cuenta.</strong> Comprueba que el padre exista —es un hecho externo, hay que
 * preguntarselo a otra fila— y sella la fecha de creacion con el reloj
 * inyectado. Lo demas —que solo el nivel 6 admita asiento, que la raiz sea la
 * unica sin padre, que la vigencia no se cierre antes de abrirse— son
 * invariantes y viven en el constructor de {@link AccountingAccount}.
 *
 * <p>
 * <strong>La unicidad del codigo no se comprueba preguntando antes.</strong>
 * {@code uq_accounting_accounts_code} la cuida la base; un {@code exists}
 * previo seria una comprobacion que dos peticiones concurrentes pasarian las
 * dos. El duplicado llega como violacion de integridad, que es la unica
 * respuesta que no miente.
 *
 * <p>
 * <strong>El reloj va inyectado</strong>: un {@code LocalDateTime.now()} aqui
 * seria una fecha que ningun test puede fijar y
 * {@code RELOJ_INYECTADO_EN_VEZ_DE_NOW} rompe el build por ello.
 */
@Observed(name = "accounting.account.create")
@Service
public class CreateAccountingAccountService implements CreateAccountingAccountUseCase {

    private final AccountingAccountRepository repository;
    private final Clock clock;

    public CreateAccountingAccountService(AccountingAccountRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public AccountingAccountDto execute(CreateAccountingAccountCommand command) {
        validateParent(command);
        AccountingAccount account = AccountingAccount.create(command.code(), command.name(),
                command.accountClass(), command.parentCode(), command.accountLevel(),
                command.postable(), command.requiresThirdParty(), command.validFrom(),
                command.validTo(), LocalDateTime.now(clock));
        return AccountingAccountDto.from(repository.save(account));
    }

    /**
     * {@code fk_accounting_accounts_parent} es autorreferente y {@code RESTRICT}:
     * sin esta comprobacion el padre inexistente saldria como un error de
     * integridad en vez de como el «esa cuenta padre no existe» que corresponde.
     *
     * <p>
     * Que el padre solo sea legitimo por debajo del nivel 1 no se comprueba aqui:
     * lo hace el dominio, y comprobarlo dos veces invita a que un dia solo quede la
     * copia de este lado.
     */
    private void validateParent(CreateAccountingAccountCommand command) {
        if (command.parentCode() == null)
            return;
        if (!repository.existsByCode(command.parentCode()))
            throw new AccountingAccountNotFoundException(command.parentCode());
    }
}
