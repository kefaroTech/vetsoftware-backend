package com.vetsoftware.app.cashregister.application.usecase;

import com.vetsoftware.app.cashregister.application.command.CashPaymentLine;
import com.vetsoftware.app.cashregister.application.command.RegisterCashInflowCommand;
import com.vetsoftware.app.cashregister.application.command.ReverseCashMovementsCommand;
import com.vetsoftware.app.cashregister.application.port.in.CashLedgerUseCase;
import com.vetsoftware.app.cashregister.application.port.out.CashRequiredPolicyPort;
import com.vetsoftware.app.cashregister.application.port.out.CashSessionRepository;
import com.vetsoftware.app.cashregister.domain.CashMovement;
import com.vetsoftware.app.cashregister.domain.CashMovementType;
import com.vetsoftware.app.cashregister.domain.CashPaymentMethod;
import com.vetsoftware.app.cashregister.domain.CashReferenceType;
import com.vetsoftware.app.cashregister.domain.CashSession;
import com.vetsoftware.app.cashregister.domain.EmployeeCashSessionRequiredException;
import com.vetsoftware.app.cashregister.domain.NoOpenCashSessionException;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orquestación de caja: registra el ingreso de una venta/abono en la caja OPEN
 * de la sede y compensa (VOID_OUT) sus anulaciones. Idempotente por
 * (referencia, método, tipo) dentro de la sesión (el índice único de la BD es
 * la red). Si no hay caja abierta es no-op (el bloqueo "caja requerida" vive en
 * la feature consumidora — F4).
 */
@Service
public class CashLedgerService implements CashLedgerUseCase {

    private static final Logger log = LoggerFactory.getLogger(CashLedgerService.class);

    private final CashSessionRepository repository;
    private final CashRequiredPolicyPort cashRequiredPolicy;

    public CashLedgerService(CashSessionRepository repository,
            CashRequiredPolicyPort cashRequiredPolicy) {
        this.repository = repository;
        this.cashRequiredPolicy = cashRequiredPolicy;
    }

    @Override
    @Observed(name = "cash.register.register.inflow")
    @Transactional
    public void registerInflow(RegisterCashInflowCommand command) {
        Optional<CashSession> open = resolveOpenSession(command);
        if (open.isEmpty())
            return;
        CashSession session = open.get();
        CashMovementType type = inflowTypeFor(command.referenceType());
        boolean changed = false;
        for (Map.Entry<CashPaymentMethod, BigDecimal> e : aggregate(command.payments())
                .entrySet()) {
            if (session.hasReferencedMovement(command.referenceType(), command.referenceId(),
                    e.getKey(), type)) {
                continue;
            }
            session.addMovement(CashMovement.create(type, e.getKey(), e.getValue(),
                    command.referenceType(), command.referenceId(), command.employeeId(), null));
            changed = true;
        }
        if (changed)
            repository.save(session);
    }

    @Override
    @Observed(name = "cash.register.reverse.movements")
    @Transactional
    public void reverse(ReverseCashMovementsCommand command) {
        Optional<CashSession> open = resolveSessionForReversal(command);
        if (open.isEmpty())
            return;
        CashSession session = open.get();
        boolean changed = false;
        for (Map.Entry<CashPaymentMethod, BigDecimal> e : aggregate(command.payments())
                .entrySet()) {
            if (session.hasReferencedMovement(command.referenceType(), command.referenceId(),
                    e.getKey(), CashMovementType.VOID_OUT)) {
                continue;
            }
            session.addMovement(CashMovement.create(CashMovementType.VOID_OUT, e.getKey(),
                    e.getValue(), command.referenceType(), command.referenceId(),
                    command.employeeId(), null));
            changed = true;
        }
        if (changed)
            repository.save(session);
    }

    @Override
    @Observed(name = "cash.register.ensure.cash.available")
    @Transactional(readOnly = true)
    public void ensureCashAvailable(Long companyId, Long branchId, String terminal) {
        if (!cashRequiredPolicy.isCashRequired(companyId))
            return;
        if (!repository.existsOpen(companyId, branchId, resolveTerminal(terminal))) {
            throw new NoOpenCashSessionException(branchId);
        }
    }

    @Override
    @Observed(name = "cash.register.ensure.employee.cash.available")
    @Transactional(readOnly = true)
    public void ensureEmployeeCashAvailable(Long companyId, Long branchId, Long employeeId) {
        boolean available = repository.findOpenByEmployee(companyId, employeeId)
                .filter(session -> session.getBranchId().equals(branchId)).isPresent();
        if (!available)
            throw new EmployeeCashSessionRequiredException(branchId);
    }

    /**
     * Los cobros con actor se enrutan siempre a su caja; el terminal queda como
     * fallback interno. En el ingreso no hay movimiento anterior al que atarse, asi
     * que resolver por actor o por terminal es lo unico posible —y por eso la
     * anulacion, que si lo tiene, no se resuelve igual: ver
     * {@link #resolveSessionForReversal(ReverseCashMovementsCommand)}.
     */
    private Optional<CashSession> resolveOpenSession(RegisterCashInflowCommand command) {
        if (command.employeeId() != null) {
            return repository.findOpenByEmployee(command.companyId(), command.employeeId())
                    .filter(session -> session.getBranchId().equals(command.branchId()));
        }
        return repository.findOpen(command.companyId(), command.branchId(),
                resolveTerminal(command.terminal()));
    }

    /**
     * <b>Contra que caja se compensa una anulacion.</b> Una nota credito revierte
     * una venta concreta, y esa venta <em>ya tiene</em> su sesion de caja: es la
     * que contiene su movimiento de ingreso. Compensar ahi es lo unico trazable
     * —cuadra el mismo arqueo que se descuadro— y no depende de que la operacion
     * traiga actor.
     *
     * <p>
     * <b>Lo que esto sustituye.</b> Antes, una anulacion sin actor caia a
     * {@code findOpen(empresa, sede, DEFAULT_TERMINAL)}: una busqueda por la
     * <em>cadena</em> del terminal, que es la foto del codigo en el instante de
     * abrir la sesion y que dos terminales distintas pueden compartir —se renombra
     * la A, se crea la B reutilizando el codigo liberado—. Con las dos abiertas, el
     * {@code VOID_OUT} caia en una arbitraria. Es dinero, y el unico camino real
     * que llegaba ahi era el de la nota credito ({@code CreditNoteReversalApplier},
     * que no lleva actor).
     *
     * <p>
     * <b>Ningun desenlace es silencioso</b>, que era la otra mitad del defecto:
     *
     * <ul>
     * <li><b>Sesion original abierta</b>: se compensa ahi. La idempotencia sigue
     * valiendo, porque {@code uq_cash_movement_reference} es por sesion y esta es
     * la misma en la que entro el dinero.</li>
     * <li><b>Sesion original cerrada</b>: no se asienta. El dominio lo prohibe
     * —{@code CashSession.addMovement} lanza {@code CashSessionClosedException}— y
     * asentarlo en otra caja descuadraria <em>dos</em> arqueos en vez de uno. Se
     * avisa con lo necesario para que el operador registre la salida a mano, que es
     * la via que el dominio ya ofrece con los movimientos manuales.</li>
     * <li><b>Sin ingreso registrado y con actor</b>: la caja del actor. Es el
     * comportamiento que la cuenta abierta ya tenia y sigue siendo correcto.</li>
     * <li><b>Sin ingreso registrado y sin actor</b>: no se asienta nada y se avisa.
     * Ahi es exactamente donde antes se elegia una caja por defecto.</li>
     * </ul>
     */
    private Optional<CashSession> resolveSessionForReversal(ReverseCashMovementsCommand command) {
        Optional<CashSession> origen = repository.findSessionOfReferencedInflow(command.companyId(),
                command.referenceType(), command.referenceId());
        if (origen.isPresent()) {
            CashSession sesion = origen.get();
            if (sesion.isOpen())
                return origen;
            log.warn(
                    "La caja donde entro el dinero de {} #{} (sesion {}, sede {}) ya esta CERRADA y"
                            + " arqueada, asi que la devolucion NO se asento en caja: un movimiento"
                            + " nuevo sobre un arqueo ya firmado lo descuadraria, y asentarlo en"
                            + " otra caja descuadraria dos. El documento fiscal si quedo emitido."
                            + " Registra la salida a mano en la caja que entregue el dinero.",
                    command.referenceType(), command.referenceId(), sesion.getId(),
                    sesion.getBranchId());
            return Optional.empty();
        }
        if (command.employeeId() != null) {
            return repository.findOpenByEmployee(command.companyId(), command.employeeId())
                    .filter(session -> session.getBranchId().equals(command.branchId()));
        }
        log.warn(
                "No se pudo resolver contra que caja compensar {} #{} de la empresa {}: esa"
                        + " operacion no registro ningun ingreso en caja y la anulacion no trae"
                        + " actor. NO se asienta nada, en vez de caer a la caja del terminal por"
                        + " defecto como se hacia antes -que con dos terminales compartiendo"
                        + " codigo elegia una arbitraria-. Si la operacion si movio dinero,"
                        + " registralo a mano en la caja que lo entrego.",
                command.referenceType(), command.referenceId(), command.companyId());
        return Optional.empty();
    }

    /**
     * Suma los pagos por método (positivos); descarta montos nulos o ≤ 0. Orden
     * estable.
     */
    private static Map<CashPaymentMethod, BigDecimal> aggregate(List<CashPaymentLine> payments) {
        Map<CashPaymentMethod, BigDecimal> totals = new LinkedHashMap<>();
        if (payments == null)
            return totals;
        for (CashPaymentLine p : payments) {
            if (p.method() == null || p.amount() == null || p.amount().signum() <= 0)
                continue;
            totals.merge(p.method(), p.amount(), BigDecimal::add);
        }
        return totals;
    }

    private static CashMovementType inflowTypeFor(CashReferenceType referenceType) {
        return referenceType == CashReferenceType.OPEN_ACCOUNT_PAYMENT
                ? CashMovementType.OPEN_ACCOUNT_IN
                : CashMovementType.SALE_IN;
    }

    private static String resolveTerminal(String terminal) {
        return (terminal == null || terminal.isBlank())
                ? CashSession.DEFAULT_TERMINAL
                : terminal.trim();
    }
}
