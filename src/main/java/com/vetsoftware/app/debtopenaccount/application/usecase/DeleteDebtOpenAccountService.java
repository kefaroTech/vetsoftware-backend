package com.vetsoftware.app.debtopenaccount.application.usecase;

import com.vetsoftware.app.debtopenaccount.application.command.DeleteDebtOpenAccountCommand;
import com.vetsoftware.app.debtopenaccount.application.port.in.DeleteDebtOpenAccountUseCase;
import com.vetsoftware.app.debtopenaccount.application.port.out.CashPort;
import com.vetsoftware.app.debtopenaccount.application.port.out.DebtOpenAccountRepository;
import com.vetsoftware.app.debtopenaccount.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.debtopenaccount.application.port.out.OpenAccountQueryPort;
import com.vetsoftware.app.debtopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.debtopenaccount.application.port.out.OpenAccountVersionGuard;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccount;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccountNotFoundException;
import com.vetsoftware.app.debtopenaccount.domain.EmployeeRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Da de baja un abono. <b>Quitar un abono es mover dinero</b>: sube el saldo
 * pendiente de la cuenta y deja en la caja un ingreso que ya no corresponde a
 * ningun cobro. Este caso de uso eran nueve lineas —cargar, comprobar empresa,
 * borrar, recalcular— sin bloqueo util, sin version esperada, sin motivo y sin
 * compensacion de caja, mientras su hermano {@code VoidDebtOpenAccountService}
 * hacia las cuatro cosas. La baja es hoy <b>una anulacion mas el ocultado de la
 * fila</b>, y por eso exige lo mismo que la anulacion.
 *
 * <p>
 * <b>Lo que se arreglo del bloqueo.</b> Habia lock pesimista, pero llegaba
 * tarde: lo tomaba el recalculo del final, despues de que la carga del abono
 * —que ademas trae la cuenta por {@code @EntityGraph}— hubiera fijado el
 * snapshot REPEATABLE READ. El {@code SELECT} que suma los abonos no veia lo
 * que otra transaccion habia confirmado mientras esperabamos el lock, y el
 * saldo recalculado pisaba el suyo sin excepcion y sin log. Ahora la primera
 * sentencia es una lectura de bloqueo
 * ({@link DebtOpenAccountRepository#lockAndFindOpenAccountId(Long)}), que no
 * abre snapshot, y el lock de la cuenta va justo detras: cuando se lee el saldo
 * ya esta todo committeado.
 */
@Observed(name = "debt.open.account.delete")
@Service
public class DeleteDebtOpenAccountService implements DeleteDebtOpenAccountUseCase {
    private final DebtOpenAccountRepository repository;
    private final OpenAccountQueryPort openAccountQueryPort;
    private final EmployeeQueryPort employeeQueryPort;
    private final OpenAccountRefresher refresher;
    private final OpenAccountVersionGuard versionGuard;
    private final CashPort cashPort;

    public DeleteDebtOpenAccountService(DebtOpenAccountRepository repository,
            OpenAccountQueryPort openAccountQueryPort, EmployeeQueryPort employeeQueryPort,
            OpenAccountRefresher refresher, OpenAccountVersionGuard versionGuard,
            CashPort cashPort) {
        this.repository = repository;
        this.openAccountQueryPort = openAccountQueryPort;
        this.employeeQueryPort = employeeQueryPort;
        this.refresher = refresher;
        this.versionGuard = versionGuard;
        this.cashPort = cashPort;
    }

    @Override
    @Transactional
    public void execute(DeleteDebtOpenAccountCommand command) {
        // PRIMERA SENTENCIA: lectura de bloqueo del abono, que ademas revela su cuenta.
        // Va antes de cualquier lectura consistente para que el recalculo lea el saldo
        // ya committeado por la operacion rival, no el de antes de esperar al lock.
        Long openAccountId = repository.lockAndFindOpenAccountId(command.id())
                .orElseThrow(() -> new DebtOpenAccountNotFoundException(command.id()));
        openAccountQueryPort.lockForUpdate(openAccountId, command.companyId());

        // Primera lectura plana, ya con los dos locks tomados. Acotada por empresa: es
        // lo que convierte el lock ancho de la fila del abono en un 404 con rollback
        // cuando el abono es de otro tenant.
        DebtOpenAccount debtOpenAccount = repository
                .findByIdAndCompanyId(command.id(), command.companyId())
                .orElseThrow(() -> new DebtOpenAccountNotFoundException(command.id()));
        if (!debtOpenAccount.getOpenAccount().companyId().equals(command.companyId())) {
            throw new IllegalArgumentException("debt open account does not belong to company");
        }

        // Detección temprana de conflicto sobre la cuenta del abono.
        versionGuard.assertVersion(command.companyId(), openAccountId, command.expectedVersion());
        if (!openAccountQueryPort.isOpen(openAccountId)) {
            throw new IllegalStateException("open account is not OPEN");
        }

        // Un abono ya anulado no aporta al saldo ni tiene ingreso vivo en caja: su
        // compensacion se hizo al anularlo, y volver a compensar aqui descuadraria la
        // caja del actor. En ese caso la baja solo oculta la fila y conserva el motivo
        // de anulacion original.
        boolean compensates = !debtOpenAccount.isVoided();
        if (compensates) {
            EmployeeRef deletedBy = employeeQueryPort
                    .findByIdAndCompanyId(command.deletedById(), command.companyId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Employee not found: " + command.deletedById()));
            // La baja mueve dinero en la caja propia del actor y exige que este abierta
            // en la sede de la cuenta.
            cashPort.requireOpenSession(command.companyId(), openAccountId, command.deletedById());
            // El motivo se persiste como motivo de anulacion: sin esto el "reason" seria
            // un parametro muerto y la baja de un cobro quedaria sin rastro de quien la
            // hizo ni por que.
            debtOpenAccount.voidPayment(deletedBy, command.reason());
            repository.save(debtOpenAccount);
        }
        repository.delete(command.id(), command.companyId());
        refresher.refresh(command.companyId(), openAccountId);
        if (compensates) {
            // Compensa el abono en la caja OPEN del actor (VOID_OUT). Idempotente por
            // abono+medio, y va despues del recalculo por el mismo orden que la
            // anulacion.
            cashPort.reversePayment(command.companyId(), openAccountId, debtOpenAccount.getId(),
                    debtOpenAccount.getPaymentMethod(), debtOpenAccount.getAmount(),
                    command.deletedById());
        }
    }
}
