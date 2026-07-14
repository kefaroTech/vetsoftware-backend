package com.vetsoftware.app.electronicdocument.infrastructure.cash;

import com.vetsoftware.app.cashregister.application.command.CashPaymentLine;
import com.vetsoftware.app.cashregister.application.command.RegisterCashInflowCommand;
import com.vetsoftware.app.cashregister.application.command.ReverseCashMovementsCommand;
import com.vetsoftware.app.cashregister.application.port.in.CashLedgerUseCase;
import com.vetsoftware.app.cashregister.domain.CashPaymentMethod;
import com.vetsoftware.app.cashregister.domain.CashReferenceType;
import com.vetsoftware.app.electronicdocument.application.port.out.CashPort;
import com.vetsoftware.app.electronicdocument.domain.PaymentMeans;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Adapter de orquestación POS → caja. Es el ÚNICO punto de esta feature que conoce el {@code CashLedgerUseCase} de
 * {@code cashregister}; traduce cada pago del documento ({@link PaymentMeans}) al medio de caja
 * ({@link CashPaymentMethod}) y registra el ingreso/compensación con referencia {@link CashReferenceType#POS_DOCUMENT}
 * y el id del documento (idempotencia + compensación).
 */
@Component("posCashRegisterAdapter")
public class CashRegisterAdapter implements CashPort {

    private final CashLedgerUseCase cashLedger;

    public CashRegisterAdapter(CashLedgerUseCase cashLedger) {
        this.cashLedger = cashLedger;
    }

    @Override
    public void requireOpenSession(Long companyId, Long branchId, Long employeeId) {
        cashLedger.ensureEmployeeCashAvailable(companyId, branchId, employeeId);
    }

    @Override
    public void registerSale(Long companyId, Long branchId, Long documentId, List<PaymentLine> payments,
                             Long employeeId) {
        cashLedger.registerInflow(new RegisterCashInflowCommand(companyId, branchId, null,
            CashReferenceType.POS_DOCUMENT, documentId, toCashLines(payments), employeeId));
    }

    @Override
    public void reverseSale(Long companyId, Long branchId, Long documentId, List<PaymentLine> payments, Long actorId) {
        cashLedger.reverse(new ReverseCashMovementsCommand(companyId, branchId, null,
            CashReferenceType.POS_DOCUMENT, documentId, toCashLines(payments), actorId));
    }

    private static List<CashPaymentLine> toCashLines(List<PaymentLine> payments) {
        return payments.stream()
            .map(p -> new CashPaymentLine(toCashMethod(p.paymentMeans()), p.amount()))
            .toList();
    }

    /** EFECTIVO→CASH, TARJETA_DEBITO/CREDITO→CARD, TRANSFERENCIA→TRANSFER. */
    private static CashPaymentMethod toCashMethod(PaymentMeans means) {
        return switch (means) {
            case EFECTIVO -> CashPaymentMethod.CASH;
            case TARJETA_DEBITO, TARJETA_CREDITO -> CashPaymentMethod.CARD;
            case TRANSFERENCIA -> CashPaymentMethod.TRANSFER;
        };
    }
}
