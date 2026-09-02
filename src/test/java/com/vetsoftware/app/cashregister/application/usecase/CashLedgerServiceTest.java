package com.vetsoftware.app.cashregister.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.cashregister.application.command.CashPaymentLine;
import com.vetsoftware.app.cashregister.application.command.RegisterCashInflowCommand;
import com.vetsoftware.app.cashregister.application.command.ReverseCashMovementsCommand;
import com.vetsoftware.app.cashregister.application.port.out.CashRequiredPolicyPort;
import com.vetsoftware.app.cashregister.domain.CashMovementType;
import com.vetsoftware.app.cashregister.domain.CashPaymentMethod;
import com.vetsoftware.app.cashregister.domain.CashReferenceType;
import com.vetsoftware.app.cashregister.domain.CashSession;
import com.vetsoftware.app.cashregister.domain.EmployeeCashSessionRequiredException;
import com.vetsoftware.app.cashregister.domain.NoOpenCashSessionException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CashLedgerServiceTest {

    private static final long CO = 1L;
    private static final long BR = 10L;
    private static final long OTHER_BR = 20L;
    private static final long USER = 7L;
    private static final long OTHER_USER = 8L;
    private static final long TERMINAL = 100L;

    private FakeCashSessionRepository repo;
    private FakeCashRequiredPolicy policy;
    private CashLedgerService service;

    @BeforeEach
    void setUp() {
        repo = new FakeCashSessionRepository();
        policy = new FakeCashRequiredPolicy(true);
        service = new CashLedgerService(repo, policy);
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private CashSession seedOpen(long branch) {
        return repo
                .save(CashSession.open(CO, branch, TERMINAL, "principal", USER, bd("100"), null));
    }

    private long cashMovements(CashSession s, CashMovementType type) {
        return s.getMovements().stream().filter(m -> m.getType() == type).count();
    }

    @Test
    void register_inflow_adds_sale_in_and_is_idempotent() {
        CashSession s = seedOpen(BR);
        RegisterCashInflowCommand cmd = new RegisterCashInflowCommand(CO, BR, null,
                CashReferenceType.POS_DOCUMENT, 5L,
                List.of(new CashPaymentLine(CashPaymentMethod.CASH, bd("60"))), USER);

        service.registerInflow(cmd);
        service.registerInflow(cmd); // reintento: no debe duplicar

        assertThat(cashMovements(s, CashMovementType.SALE_IN)).isEqualTo(1);
        assertThat(s.expectedByMethod().get(CashPaymentMethod.CASH)).isEqualByComparingTo("160");
    }

    @Test
    void register_inflow_is_noop_when_no_open_session() {
        RegisterCashInflowCommand cmd = new RegisterCashInflowCommand(CO, BR, null,
                CashReferenceType.POS_DOCUMENT, 5L,
                List.of(new CashPaymentLine(CashPaymentMethod.CASH, bd("60"))), USER);

        assertThatCode(() -> service.registerInflow(cmd)).doesNotThrowAnyException();
        assertThat(repo.findOpen(CO, BR, "principal")).isEmpty();
    }

    @Test
    void pos_inflow_uses_the_employees_open_session_even_with_a_custom_terminal() {
        CashSession session = repo
                .save(CashSession.open(CO, BR, 200L, "caja-2", USER, bd("100"), null));
        RegisterCashInflowCommand command = new RegisterCashInflowCommand(CO, BR, null,
                CashReferenceType.POS_DOCUMENT, 6L,
                List.of(new CashPaymentLine(CashPaymentMethod.CASH, bd("25"))), USER);

        service.registerInflow(command);

        assertThat(cashMovements(session, CashMovementType.SALE_IN)).isEqualTo(1);
        assertThat(session.expectedByMethod().get(CashPaymentMethod.CASH))
                .isEqualByComparingTo("125");
    }

    @Test
    void open_account_inflow_uses_the_employees_custom_terminal() {
        CashSession session = repo
                .save(CashSession.open(CO, BR, 200L, "CAJA-2", USER, bd("100"), null));
        RegisterCashInflowCommand command = new RegisterCashInflowCommand(CO, BR, null,
                CashReferenceType.OPEN_ACCOUNT_PAYMENT, 77L,
                List.of(new CashPaymentLine(CashPaymentMethod.CARD, bd("40"))), USER);

        service.registerInflow(command);

        assertThat(cashMovements(session, CashMovementType.OPEN_ACCOUNT_IN)).isEqualTo(1);
        assertThat(session.expectedByMethod().get(CashPaymentMethod.CARD))
                .isEqualByComparingTo("40");
    }

    @Test
    void open_account_reversal_uses_the_actors_own_session() {
        CashSession collector = repo
                .save(CashSession.open(CO, BR, 200L, "CAJA-2", USER, bd("100"), null));
        CashSession actor = repo
                .save(CashSession.open(CO, BR, 300L, "CAJA-3", OTHER_USER, bd("50"), null));
        ReverseCashMovementsCommand command = new ReverseCashMovementsCommand(CO, BR, null,
                CashReferenceType.OPEN_ACCOUNT_PAYMENT, 77L,
                List.of(new CashPaymentLine(CashPaymentMethod.CASH, bd("25"))), OTHER_USER);

        service.reverse(command);

        assertThat(cashMovements(collector, CashMovementType.VOID_OUT)).isZero();
        assertThat(cashMovements(actor, CashMovementType.VOID_OUT)).isEqualTo(1);
        assertThat(actor.expectedByMethod().get(CashPaymentMethod.CASH)).isEqualByComparingTo("25");
    }

    @Test
    void reverse_adds_void_out_and_is_idempotent() {
        CashSession s = seedOpen(BR);
        service.registerInflow(
                new RegisterCashInflowCommand(CO, BR, null, CashReferenceType.POS_DOCUMENT, 5L,
                        List.of(new CashPaymentLine(CashPaymentMethod.CASH, bd("60"))), USER));

        ReverseCashMovementsCommand rev = new ReverseCashMovementsCommand(CO, BR, null,
                CashReferenceType.POS_DOCUMENT, 5L,
                List.of(new CashPaymentLine(CashPaymentMethod.CASH, bd("60"))), USER);
        service.reverse(rev);
        service.reverse(rev); // reintento: no debe duplicar

        assertThat(cashMovements(s, CashMovementType.VOID_OUT)).isEqualTo(1);
        // 100 base + 60 venta − 60 reversa = 100
        assertThat(s.expectedByMethod().get(CashPaymentMethod.CASH)).isEqualByComparingTo("100");
    }

    @Test
    void ensure_cash_available_throws_when_required_and_no_open_session() {
        assertThatThrownBy(() -> service.ensureCashAvailable(CO, OTHER_BR, null))
                .isInstanceOf(NoOpenCashSessionException.class);
    }

    @Test
    void ensure_cash_available_passes_when_open_session_exists() {
        seedOpen(BR);
        assertThatCode(() -> service.ensureCashAvailable(CO, BR, null)).doesNotThrowAnyException();
    }

    @Test
    void ensure_cash_available_is_noop_when_company_does_not_require_cash() {
        policy.required = false;
        assertThatCode(() -> service.ensureCashAvailable(CO, OTHER_BR, null))
                .doesNotThrowAnyException();
    }

    @Test
    void ensure_employee_cash_available_rejects_another_employees_session() {
        repo.save(CashSession.open(CO, BR, TERMINAL, "principal", OTHER_USER, bd("100"), null));

        assertThatThrownBy(() -> service.ensureEmployeeCashAvailable(CO, BR, USER))
                .isInstanceOf(EmployeeCashSessionRequiredException.class);
    }

    @Test
    void ensure_employee_cash_available_requires_the_same_branch() {
        seedOpen(BR);

        assertThatThrownBy(() -> service.ensureEmployeeCashAvailable(CO, OTHER_BR, USER))
                .isInstanceOf(EmployeeCashSessionRequiredException.class);
        assertThatCode(() -> service.ensureEmployeeCashAvailable(CO, BR, USER))
                .doesNotThrowAnyException();
    }

    @Test
    void reverse_is_noop_when_no_open_session() {
        ReverseCashMovementsCommand cmd = new ReverseCashMovementsCommand(CO, BR, null,
                CashReferenceType.POS_DOCUMENT, 5L,
                List.of(new CashPaymentLine(CashPaymentMethod.CASH, bd("60"))), USER);

        assertThatCode(() -> service.reverse(cmd)).doesNotThrowAnyException();
        assertThat(repo.findOpen(CO, BR, "principal")).isEmpty();
    }

    @Test
    void register_inflow_without_an_actor_resolves_by_the_default_terminal() {
        CashSession s = seedOpen(BR); // terminal "principal" (resolveTerminal por defecto)
        RegisterCashInflowCommand cmd = new RegisterCashInflowCommand(CO, BR, null,
                CashReferenceType.POS_DOCUMENT, 5L,
                List.of(new CashPaymentLine(CashPaymentMethod.CASH, bd("60"))), null);

        service.registerInflow(cmd);

        assertThat(cashMovements(s, CashMovementType.SALE_IN)).isEqualTo(1);
    }

    @Test
    void register_inflow_without_an_actor_resolves_by_a_custom_terminal() {
        CashSession s = repo.save(CashSession.open(CO, BR, 300L, "caja-3", null, bd("100"), null));
        RegisterCashInflowCommand cmd = new RegisterCashInflowCommand(CO, BR, "caja-3",
                CashReferenceType.POS_DOCUMENT, 9L,
                List.of(new CashPaymentLine(CashPaymentMethod.CASH, bd("40"))), null);

        service.registerInflow(cmd);

        assertThat(cashMovements(s, CashMovementType.SALE_IN)).isEqualTo(1);
    }

    @Test
    void register_inflow_ignores_payment_lines_without_a_positive_amount() {
        CashSession s = seedOpen(BR);
        RegisterCashInflowCommand cmd = new RegisterCashInflowCommand(CO, BR, null,
                CashReferenceType.POS_DOCUMENT, 5L,
                List.of(new CashPaymentLine(null, bd("10")),
                        new CashPaymentLine(CashPaymentMethod.CASH, null),
                        new CashPaymentLine(CashPaymentMethod.CASH, BigDecimal.ZERO),
                        new CashPaymentLine(CashPaymentMethod.CASH, bd("-5"))),
                USER);

        service.registerInflow(cmd);

        assertThat(s.getMovements()).isEmpty();
    }

    @Test
    void register_inflow_ignores_a_cash_session_from_another_company() {
        // Misma sede/terminal/empleado, pero la caja abierta es de OTRA empresa: el
        // repositorio (real o fake) siempre scoped por companyId, así que un
        // companyId equivocado nunca debe encontrar ni tocar la caja ajena.
        CashSession ajena = repo
                .save(CashSession.open(999L, BR, TERMINAL, "principal", USER, bd("100"), null));
        RegisterCashInflowCommand cmd = new RegisterCashInflowCommand(CO, BR, null,
                CashReferenceType.POS_DOCUMENT, 5L,
                List.of(new CashPaymentLine(CashPaymentMethod.CASH, bd("60"))), USER);

        service.registerInflow(cmd);

        assertThat(ajena.getMovements()).isEmpty();
    }

    @Test
    void reverse_ignores_a_cash_session_from_another_company() {
        CashSession ajena = repo
                .save(CashSession.open(999L, BR, TERMINAL, "principal", USER, bd("100"), null));
        ReverseCashMovementsCommand cmd = new ReverseCashMovementsCommand(CO, BR, null,
                CashReferenceType.POS_DOCUMENT, 5L,
                List.of(new CashPaymentLine(CashPaymentMethod.CASH, bd("60"))), USER);

        service.reverse(cmd);

        assertThat(ajena.getMovements()).isEmpty();
    }

    /** Fake del flag por empresa. */
    static class FakeCashRequiredPolicy implements CashRequiredPolicyPort {
        boolean required;

        FakeCashRequiredPolicy(boolean required) {
            this.required = required;
        }

        @Override
        public boolean isCashRequired(Long companyId) {
            return required;
        }
    }
}
