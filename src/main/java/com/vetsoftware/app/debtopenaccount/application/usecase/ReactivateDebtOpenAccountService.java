package com.vetsoftware.app.debtopenaccount.application.usecase;

import com.vetsoftware.app.debtopenaccount.application.command.ReactivateDebtOpenAccountCommand;
import com.vetsoftware.app.debtopenaccount.application.dto.DebtOpenAccountDto;
import com.vetsoftware.app.debtopenaccount.application.port.in.ReactivateDebtOpenAccountUseCase;
import com.vetsoftware.app.debtopenaccount.application.port.out.CashPort;
import com.vetsoftware.app.debtopenaccount.application.port.out.DebtOpenAccountRepository;
import com.vetsoftware.app.debtopenaccount.application.port.out.OpenAccountQueryPort;
import com.vetsoftware.app.debtopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccount;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccountNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reactiva un abono dado de baja. <b>Devolver un abono a la vida es mover
 * dinero</b>: vuelve a descontarse del saldo pendiente de la cuenta, porque
 * {@code sumPaymentsByOpenAccountId} filtra {@code enabled = true}, y vuelve a
 * contar como cobrado. Eran nueve lineas —un UPDATE a ciegas, una recarga y un
 * recalculo— sin lock, sin comprobar que la cuenta siguiera abierta, sin guard
 * de sobrepago y sin tocar la caja, mientras sus tres hermanos hacian las
 * cuatro cosas (#218).
 *
 * <p>
 * <b>Los tres fallos que producia.</b> (1) <em>Saldo negativo</em>: entre la
 * baja y la reactivacion pueden haber entrado abonos nuevos legitimos, y el
 * viejo se sumaba encima; hoy lo corta el guard de sobrepago, con el mismo
 * mensaje que el alta, en vez de morir abajo con el error opaco de
 * {@code OpenAccount.recalculate}. (2) <em>Perdida de actualizacion
 * silenciosa</em>: sin lock, el recalculo leia el saldo de antes de esperar al
 * lock ajeno y lo pisaba (el patron de #110). (3) <em>La caja no cuadraba</em>:
 * el abono volvia a contar como cobrado y en el cajon no entraba nada.
 *
 * <p>
 * <b>El orden es la garantia, no las llamadas sueltas.</b> Primero la lectura
 * de bloqueo del abono —que ademas revela su cuenta y, a diferencia de una
 * lectura plana, no abre el snapshot REPEATABLE READ—, despues el lock acotado
 * de la cuenta, y solo con los dos locks tomados se lee estado y se decide.
 * Mutar antes de preguntar es exactamente lo que hacia la version vieja.
 */
@Observed(name = "debt.open.account.reactivate")
@Service
public class ReactivateDebtOpenAccountService implements ReactivateDebtOpenAccountUseCase {
    private final DebtOpenAccountRepository repository;
    private final OpenAccountQueryPort openAccountQueryPort;
    private final OpenAccountRefresher refresher;
    private final CashPort cashPort;

    public ReactivateDebtOpenAccountService(DebtOpenAccountRepository repository,
            OpenAccountQueryPort openAccountQueryPort, OpenAccountRefresher refresher,
            CashPort cashPort) {
        this.repository = repository;
        this.openAccountQueryPort = openAccountQueryPort;
        this.refresher = refresher;
        this.cashPort = cashPort;
    }

    @Override
    @Transactional
    public DebtOpenAccountDto execute(ReactivateDebtOpenAccountCommand command) {
        // PRIMERA SENTENCIA: lectura de bloqueo del abono, que ademas revela su
        // cuenta. Tiene que ser la variante "including disabled" porque el
        // @SQLRestriction("enabled = true") de la entidad esconde de todos los finders
        // JPA justo la fila que hay que encontrar: la del abono apagado. Y tiene que
        // ser de BLOQUEO y no plana, porque una lectura plana abriria aqui el snapshot
        // REPEATABLE READ y el saldo que leyera el guard de sobrepago seria el de antes
        // de esperar al lock de la cuenta.
        Long openAccountId = repository.lockAndFindOpenAccountIdIncludingDisabled(command.id())
                .orElseThrow(() -> new DebtOpenAccountNotFoundException(command.id()));
        // Lock pesimista ACOTADO de la cuenta: serializa la reactivacion frente a
        // cargos/abonos/cierre concurrentes y cierra el TOCTOU del isOpen. Acotado
        // porque la variante ancha tomaba el PESSIMISTIC_WRITE sobre la fila de otro
        // tenant antes de cualquier comprobacion.
        openAccountQueryPort.lockForUpdate(openAccountId, command.companyId());
        // Primera lectura del abono, ya con los dos locks tomados y acotada por
        // empresa: es lo que convierte el lock ancho de la fila del abono en un 404 con
        // rollback cuando el abono es de otro tenant. Usa el MISMO EXISTS que el UPDATE
        // de reactivar, asi que las dos sentencias apuntan siempre a la misma fila.
        DebtOpenAccount debtOpenAccount = repository
                .findByIdIncludingDisabledAndCompanyId(command.id(), command.companyId())
                .orElseThrow(() -> new DebtOpenAccountNotFoundException(command.id()));
        // Una cuenta cerrada o cancelada no admite abonos de vuelta: reactivar sobre
        // ella baja su saldo pendiente despues de que changeStatus comprobara la
        // invariante contable, que solo se mira al cerrar y nunca despues.
        if (!openAccountQueryPort.isOpen(openAccountId)) {
            throw new IllegalStateException("open account is not OPEN");
        }

        // Reactivar solo mueve dinero si la fila esta HOY escondida y NO anulada. Un
        // abono anulado no entra en la suma de abonos ni tiene ingreso vivo en caja
        // —se compenso al anularlo—, y volver a registrarlo descuadraria el cajon;
        // reactivarlo solo devuelve la fila a la vista. Y si ya estaba encendido, el
        // UPDATE es un no-op: su importe YA esta descontado del saldo, asi que aplicar
        // el guard de sobrepago aqui rechazaria una operacion legitima.
        boolean mueveDinero = !debtOpenAccount.isEnabled() && !debtOpenAccount.isVoided();
        if (mueveDinero) {
            // Mismo guard —y mismo mensaje— que el alta: el abono no puede exceder el
            // saldo pendiente. Entre la baja y la reactivacion pueden haber entrado
            // abonos nuevos legitimos, y sin esto el viejo se suma encima y deja la
            // cuenta en negativo. Se lee DENTRO del lock, que es lo que hace que el
            // saldo sea el ya committeado por la operacion rival.
            BigDecimal outstanding = openAccountQueryPort.outstandingAmount(openAccountId);
            if (debtOpenAccount.getAmount().compareTo(outstanding) > 0) {
                throw new IllegalArgumentException("El abono (" + debtOpenAccount.getAmount()
                        + ") no puede exceder el saldo pendiente (" + outstanding + ").");
            }
            // El cobro vuelve a la caja propia del actor y exige que este abierta en la
            // sede de la cuenta. Va ANTES del UPDATE: si no hay caja, no se reactiva.
            cashPort.requireOpenSession(command.companyId(), openAccountId,
                    command.reactivatedById());
        }

        int rows = repository.reactivate(command.id(), command.companyId());
        if (rows == 0)
            throw new DebtOpenAccountNotFoundException(command.id());
        DebtOpenAccount reactivado = repository
                .findByIdAndCompanyId(command.id(), command.companyId())
                .orElseThrow(() -> new DebtOpenAccountNotFoundException(command.id()));
        refresher.refresh(command.companyId(), openAccountId);
        if (mueveDinero) {
            // El abono vuelve a contar como cobrado: el ingreso tiene que volver a
            // entrar en la caja OPEN del actor (OPEN_ACCOUNT_IN, idempotente por
            // abono+medio). Sin esto la cuenta daba el abono por cobrado y en el cajon
            // no habia nada que lo respaldara.
            cashPort.registerPayment(command.companyId(), openAccountId, reactivado.getId(),
                    reactivado.getPaymentMethod(), reactivado.getAmount(),
                    command.reactivatedById());
        }
        return DebtOpenAccountDto.from(reactivado);
    }
}
