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
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orquestación de caja: registra el ingreso de una venta/abono en la caja OPEN de la sede y compensa (VOID_OUT) sus
 * anulaciones. Idempotente por (referencia, método, tipo) dentro de la sesión (el índice único de la BD es la red).
 * Si no hay caja abierta es no-op (el bloqueo "caja requerida" vive en la feature consumidora — F4).
 */
@Service
public class CashLedgerService implements CashLedgerUseCase {

    private final CashSessionRepository repository;
    private final CashRequiredPolicyPort cashRequiredPolicy;

    public CashLedgerService(CashSessionRepository repository, CashRequiredPolicyPort cashRequiredPolicy) {
        this.repository = repository;
        this.cashRequiredPolicy = cashRequiredPolicy;
    }

    @Override
    @Transactional
    public void registerInflow(RegisterCashInflowCommand command) {
        Optional<CashSession> open = resolveOpenSession(command);
        if (open.isEmpty()) return;
        CashSession session = open.get();
        CashMovementType type = inflowTypeFor(command.referenceType());
        boolean changed = false;
        for (Map.Entry<CashPaymentMethod, BigDecimal> e : aggregate(command.payments()).entrySet()) {
            if (session.hasReferencedMovement(command.referenceType(), command.referenceId(), e.getKey(), type)) {
                continue;
            }
            session.addMovement(CashMovement.create(type, e.getKey(), e.getValue(), command.referenceType(),
                command.referenceId(), command.employeeId(), null));
            changed = true;
        }
        if (changed) repository.save(session);
    }

    @Override
    @Transactional
    public void reverse(ReverseCashMovementsCommand command) {
        Optional<CashSession> open = repository.findOpen(
            command.companyId(), command.branchId(), resolveTerminal(command.terminal()));
        if (open.isEmpty()) return;
        CashSession session = open.get();
        boolean changed = false;
        for (Map.Entry<CashPaymentMethod, BigDecimal> e : aggregate(command.payments()).entrySet()) {
            if (session.hasReferencedMovement(command.referenceType(), command.referenceId(), e.getKey(),
                    CashMovementType.VOID_OUT)) {
                continue;
            }
            session.addMovement(CashMovement.create(CashMovementType.VOID_OUT, e.getKey(), e.getValue(),
                command.referenceType(), command.referenceId(), command.employeeId(), null));
            changed = true;
        }
        if (changed) repository.save(session);
    }

    @Override
    @Transactional(readOnly = true)
    public void ensureCashAvailable(Long companyId, Long branchId, String terminal) {
        if (!cashRequiredPolicy.isCashRequired(companyId)) return;
        if (!repository.existsOpen(companyId, branchId, resolveTerminal(terminal))) {
            throw new NoOpenCashSessionException(branchId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void ensureEmployeeCashAvailable(Long companyId, Long branchId, Long employeeId) {
        boolean available = repository.findOpenByEmployee(companyId, employeeId)
            .filter(session -> session.getBranchId().equals(branchId))
            .isPresent();
        if (!available) throw new EmployeeCashSessionRequiredException(branchId);
    }

    private Optional<CashSession> resolveOpenSession(RegisterCashInflowCommand command) {
        if (command.referenceType() == CashReferenceType.POS_DOCUMENT && command.employeeId() != null) {
            return repository.findOpenByEmployee(command.companyId(), command.employeeId())
                .filter(session -> session.getBranchId().equals(command.branchId()));
        }
        return repository.findOpen(
            command.companyId(), command.branchId(), resolveTerminal(command.terminal()));
    }

    /** Suma los pagos por método (positivos); descarta montos nulos o ≤ 0. Orden estable. */
    private static Map<CashPaymentMethod, BigDecimal> aggregate(List<CashPaymentLine> payments) {
        Map<CashPaymentMethod, BigDecimal> totals = new LinkedHashMap<>();
        if (payments == null) return totals;
        for (CashPaymentLine p : payments) {
            if (p.method() == null || p.amount() == null || p.amount().signum() <= 0) continue;
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
        return (terminal == null || terminal.isBlank()) ? CashSession.DEFAULT_TERMINAL : terminal.trim();
    }
}
