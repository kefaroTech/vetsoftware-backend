package com.vetsoftware.app.debtopenaccount.application.usecase;

import com.vetsoftware.app.debtopenaccount.application.command.UpdateDebtOpenAccountCommand;
import com.vetsoftware.app.debtopenaccount.application.dto.DebtOpenAccountDto;
import com.vetsoftware.app.debtopenaccount.application.port.in.UpdateDebtOpenAccountUseCase;
import com.vetsoftware.app.debtopenaccount.application.port.out.DebtOpenAccountRepository;
import com.vetsoftware.app.debtopenaccount.application.port.out.OpenAccountQueryPort;
import com.vetsoftware.app.debtopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.debtopenaccount.application.port.out.OpenAccountVersionGuard;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccount;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccountNotFoundException;
import com.vetsoftware.app.debtopenaccount.domain.OpenAccountRef;
import com.vetsoftware.app.debtopenaccount.domain.PaymentMethod;
import io.micrometer.observation.annotation.Observed;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Edita un abono ya registrado (importe, medio de pago y, opcionalmente,
 * traslado a otra cuenta de la misma empresa).
 *
 * <h2>Orden de bloqueo — por id de cuenta ASCENDENTE, siempre</h2>
 *
 * Este caso de uso es el unico de la feature que puede necesitar DOS cuentas
 * bloqueadas a la vez: trasladar un abono baja el saldo de la cuenta destino y
 * lo sube en la de origen, y las dos se reescriben en la misma transaccion. Si
 * cada transaccion las bloqueara en el orden en que le vienen —destino primero,
 * origen despues— bastarian dos traslados simultaneos en sentidos opuestos
 * entre las mismas dos cuentas (A→B y B→A) para que cada una retuviera el lock
 * que la otra espera: <b>deadlock</b>, y en hora punta no es hipotetico.
 *
 * <p>
 * Por eso las dos cuentas se bloquean <b>siempre por id ascendente</b>
 * ({@link #lockAccountsInAscendingOrder}), sin importar cual es el origen y
 * cual el destino: con un orden total unico sobre el recurso no puede haber
 * ciclo de espera. La cuenta de origen se descubre antes con
 * {@link DebtOpenAccountRepository#lockAndFindOpenAccountId(Long)}, que es una
 * lectura de bloqueo sobre la fila del abono —no consistente— y por tanto ni
 * abre el snapshot REPEATABLE READ ni mete la cuenta vieja en el contexto de
 * persistencia. Ese detalle es lo que permite que el guard de sobrepago y el
 * recalculo lean el saldo ya committeado por la operacion rival, en vez del que
 * habia antes de esperar al lock.
 *
 * <p>
 * El abono tambien queda bloqueado, asi que dos ediciones del mismo abono se
 * serializan aqui en vez de chocar mas tarde en el {@code @Version}.
 */
@Observed(name = "debt.open.account.update")
@Service
public class UpdateDebtOpenAccountService implements UpdateDebtOpenAccountUseCase {
    private final DebtOpenAccountRepository repository;
    private final OpenAccountQueryPort openAccountQueryPort;
    private final OpenAccountRefresher refresher;
    private final OpenAccountVersionGuard versionGuard;

    public UpdateDebtOpenAccountService(DebtOpenAccountRepository repository,
            OpenAccountQueryPort openAccountQueryPort, OpenAccountRefresher refresher,
            OpenAccountVersionGuard versionGuard) {
        this.repository = repository;
        this.openAccountQueryPort = openAccountQueryPort;
        this.refresher = refresher;
        this.versionGuard = versionGuard;
    }

    @Override
    @Transactional
    public DebtOpenAccountDto execute(UpdateDebtOpenAccountCommand command) {
        // PRIMERA SENTENCIA: bloqueo del abono, que ademas revela su cuenta actual.
        // Antes de cualquier lectura plana, por lo explicado en el javadoc de la clase.
        Long previousOpenAccountId = repository.lockAndFindOpenAccountId(command.id())
                .orElseThrow(() -> new DebtOpenAccountNotFoundException(command.id()));
        boolean transfer = !command.openAccountId().equals(previousOpenAccountId);
        lockAccountsInAscendingOrder(previousOpenAccountId, command.openAccountId(),
                command.companyId());

        // Primera lectura plana de la transaccion: aqui se abre el snapshot, ya con
        // todos los locks tomados. Sigue ACOTADA por empresa, que es lo que convierte
        // el lock ancho de la fila del abono en un 404 con rollback si el abono es de
        // otro tenant.
        DebtOpenAccount debtOpenAccount = repository
                .findByIdAndCompanyId(command.id(), command.companyId())
                .orElseThrow(() -> new DebtOpenAccountNotFoundException(command.id()));

        // Carga ACOTADA por empresa: la cuenta destino de otro tenant no se resuelve,
        // asi
        // que trasladar el abono a la cartera de la empresa vecina deja de ser posible.
        // Antes se cargaba ancha y la empresa se comparaba despues en Java.
        OpenAccountRef openAccount = openAccountQueryPort
                .findByIdAndCompanyId(command.openAccountId(), command.companyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "OpenAccount not found: " + command.openAccountId()));
        // Detección temprana de conflicto sobre la cuenta destino del abono.
        versionGuard.assertVersion(command.companyId(), command.openAccountId(),
                command.expectedVersion());

        // Una cuenta CLOSE tiene saldo cero por invariante y una CANCEL es una perdida
        // ya contabilizada: reescribirles el saldo desde una edicion de abono las
        // corrompe en silencio. Mismo guard que el alta y la anulacion.
        if (!openAccountQueryPort.isOpen(command.openAccountId())) {
            throw new IllegalStateException("open account is not OPEN");
        }
        // En un traslado tambien se reescribe el saldo de la cuenta de ORIGEN (pierde
        // el abono), asi que tiene que estar igual de abierta que la destino.
        if (transfer && !openAccountQueryPort.isOpen(previousOpenAccountId)) {
            throw new IllegalStateException("source open account is not OPEN");
        }

        // GUARD DE SOBREPAGO. El alta compara contra el saldo pendiente a secas, y esa
        // formula aqui rechazaria subidas legitimas: el importe VIEJO de este mismo
        // abono ya esta restado dentro de outstanding, asi que el margen real es
        // "outstanding + lo que este abono aporta hoy a ESTA cuenta". Lo que aporta es
        // su importe solo si cuenta para el saldo de la cuenta destino, es decir si no
        // es un traslado (en un traslado el abono todavia no suma aqui) y si esta
        // habilitado y sin anular —exactamente los dos filtros de
        // sumPaymentsByOpenAccountId, del que sale outstanding—. Sin esta guarda un
        // PUT con importe mayor que el total facturado dejaba el saldo en negativo con
        // HTTP 200 y sin necesidad de concurrencia ninguna.
        BigDecimal outstanding = openAccountQueryPort.outstandingAmount(command.openAccountId());
        boolean alreadyCountedInTarget = !transfer && debtOpenAccount.isEnabled()
                && !debtOpenAccount.isVoided();
        BigDecimal available = alreadyCountedInTarget
                ? outstanding.add(debtOpenAccount.getAmount())
                : outstanding;
        if (command.amount().compareTo(available) > 0) {
            throw new IllegalArgumentException("El abono (" + command.amount()
                    + ") no puede exceder el saldo pendiente (" + available + ").");
        }

        debtOpenAccount.update(command.amount(), PaymentMethod.valueOf(command.paymentMethod()),
                openAccount);
        DebtOpenAccountDto dto = DebtOpenAccountDto.from(repository.save(debtOpenAccount));
        refresher.refresh(command.companyId(), command.openAccountId());
        if (transfer) {
            refresher.refresh(command.companyId(), previousOpenAccountId);
        }
        return dto;
    }

    /**
     * Bloquea las cuentas implicadas por id ASCENDENTE — el orden total que evita
     * el deadlock entre dos traslados cruzados; ver el javadoc de la clase. Cuando
     * origen y destino coinciden (la edicion normal, sin traslado) hay un solo lock
     * y el orden es trivial. Va acotado por empresa: una cuenta ajena no devuelve
     * fila y no se bloquea nada, y la comprobacion de propiedad la remata la carga
     * acotada posterior.
     */
    private void lockAccountsInAscendingOrder(Long previousOpenAccountId, Long targetOpenAccountId,
            Long companyId) {
        if (previousOpenAccountId.equals(targetOpenAccountId)) {
            openAccountQueryPort.lockForUpdate(targetOpenAccountId, companyId);
            return;
        }
        boolean previousFirst = previousOpenAccountId.compareTo(targetOpenAccountId) < 0;
        Long first = previousFirst ? previousOpenAccountId : targetOpenAccountId;
        Long second = previousFirst ? targetOpenAccountId : previousOpenAccountId;
        openAccountQueryPort.lockForUpdate(first, companyId);
        openAccountQueryPort.lockForUpdate(second, companyId);
    }
}
