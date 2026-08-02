package com.vetsoftware.app.debtopenaccount.infrastructure.cash;

import com.vetsoftware.app.cashregister.application.command.CashPaymentLine;
import com.vetsoftware.app.cashregister.application.command.RegisterCashInflowCommand;
import com.vetsoftware.app.cashregister.application.command.ReverseCashMovementsCommand;
import com.vetsoftware.app.cashregister.application.port.in.CashLedgerUseCase;
import com.vetsoftware.app.cashregister.domain.CashPaymentMethod;
import com.vetsoftware.app.cashregister.domain.CashReferenceType;
import com.vetsoftware.app.debtopenaccount.application.port.out.CashPort;
import com.vetsoftware.app.debtopenaccount.domain.PaymentMethod;
import com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaEntity;
import com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Adapter de orquestación cuenta abierta → caja. Único punto de esta feature
 * que conoce el {@code
 * CashLedgerUseCase} de {@code cashregister}; resuelve la sede de la cuenta
 * (vía la persistencia de {@code openaccount}) y traduce el
 * {@link PaymentMethod} al medio de caja ({@link CashPaymentMethod}),
 * registrando el ingreso/compensación con referencia
 * {@link CashReferenceType#OPEN_ACCOUNT_PAYMENT} y el id del abono
 * (idempotencia + compensación).
 */
@Component("debtOpenAccountCashRegisterAdapter")
public class CashRegisterAdapter implements CashPort {

    private final CashLedgerUseCase cashLedger;
    private final OpenAccountJpaRepository openAccountJpaRepository;

    public CashRegisterAdapter(CashLedgerUseCase cashLedger,
            OpenAccountJpaRepository openAccountJpaRepository) {
        this.cashLedger = cashLedger;
        this.openAccountJpaRepository = openAccountJpaRepository;
    }

    @Override
    public void requireOpenSession(Long companyId, Long openAccountId, Long employeeId) {
        Long branchId = resolveBranch(openAccountId);
        cashLedger.ensureEmployeeCashAvailable(companyId, branchId, employeeId);
    }

    @Override
    public void registerPayment(Long companyId, Long openAccountId, Long paymentId,
            PaymentMethod method, BigDecimal amount, Long employeeId) {
        Long branchId = resolveBranch(openAccountId);
        cashLedger.registerInflow(new RegisterCashInflowCommand(companyId, branchId, null,
                CashReferenceType.OPEN_ACCOUNT_PAYMENT, paymentId,
                List.of(new CashPaymentLine(toCashMethod(method), amount)), employeeId));
    }

    @Override
    public void reversePayment(Long companyId, Long openAccountId, Long paymentId,
            PaymentMethod method, BigDecimal amount, Long actorId) {
        Long branchId = resolveBranch(openAccountId);
        cashLedger.reverse(new ReverseCashMovementsCommand(companyId, branchId, null,
                CashReferenceType.OPEN_ACCOUNT_PAYMENT, paymentId,
                List.of(new CashPaymentLine(toCashMethod(method), amount)), actorId));
    }

    private Long resolveBranch(Long openAccountId) {
        return openAccountJpaRepository.findById(openAccountId).map(OpenAccountJpaEntity::getBranch)
                .map(b -> b.getId()).orElseThrow(() -> new IllegalArgumentException(
                        "OpenAccount not found: " + openAccountId));
    }

    /** CASH→CASH, CARD→CARD, BANK_TRANSFER→TRANSFER. */
    private static CashPaymentMethod toCashMethod(PaymentMethod method) {
        return switch (method) {
            case CASH -> CashPaymentMethod.CASH;
            case CARD -> CashPaymentMethod.CARD;
            case BANK_TRANSFER -> CashPaymentMethod.TRANSFER;
        };
    }
}
