package com.vetsoftware.app.cashregister.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.vetsoftware.app.cashregister.application.command.OpenCashSessionCommand;
import com.vetsoftware.app.cashregister.application.dto.CashSessionView;
import com.vetsoftware.app.cashregister.application.port.out.BranchQueryPort;
import com.vetsoftware.app.cashregister.application.port.out.CashTerminalQueryPort;
import com.vetsoftware.app.cashregister.application.port.out.CashMetrics;
import com.vetsoftware.app.cashregister.domain.CashSession;
import com.vetsoftware.app.cashregister.domain.CashSessionAlreadyOpenException;
import com.vetsoftware.app.cashregister.domain.CashSessionStatus;
import com.vetsoftware.app.cashregister.domain.EmployeeCashSessionAlreadyOpenException;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests del {@link OpenCashSessionService}: apertura con base, default de terminal, doble apertura y sede inválida. */
class OpenCashSessionServiceTest {

    private static final long CO = 1L;
    private static final long BR = 10L;
    private static final long OTHER_BR = 20L;
    private static final long USER = 7L;
    private static final long OTHER_USER = 8L;
    private static final long TERMINAL = 100L;

    private FakeCashSessionRepository repo;
    private FakeBranchQueryPort branchQuery;
    private OpenCashSessionService service;

    @BeforeEach
    void setUp() {
        repo = new FakeCashSessionRepository();
        branchQuery = new FakeBranchQueryPort(true);
        service = new OpenCashSessionService(repo, branchQuery,
            (terminalId, companyId, branchId) -> terminalId == null ? java.util.Optional.empty()
                : java.util.Optional.of(new CashTerminalQueryPort.TerminalRef(
                    terminalId, "Caja 2", "CAJA-2")),
            mock(CashMetrics.class));
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @Test
    void open_creates_open_session_with_base() {
        CashSessionView view = service.open(
            new OpenCashSessionCommand(CO, BR, TERMINAL, bd("100"), USER, "apertura"));

        assertThat(view.status()).isEqualTo(CashSessionStatus.OPEN);
        assertThat(view.openingFloat()).isEqualByComparingTo("100");
        assertThat(view.terminalId()).isEqualTo(TERMINAL);
        assertThat(view.terminal()).isEqualTo("CAJA-2");
        assertThat(repo.existsOpen(CO, BR, "CAJA-2")).isTrue();
    }

    @Test
    void open_rejects_a_missing_terminal() {
        assertThatThrownBy(() -> service.open(
            new OpenCashSessionCommand(CO, BR, null, bd("0"), USER, null)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void open_rejects_when_another_employee_uses_the_same_terminal() {
        repo.save(CashSession.open(CO, BR, TERMINAL, "CAJA-2", OTHER_USER, bd("50"), null));

        assertThatThrownBy(() -> service.open(
            new OpenCashSessionCommand(CO, BR, TERMINAL, bd("100"), USER, null)))
            .isInstanceOf(CashSessionAlreadyOpenException.class)
            .hasMessage("La terminal 'CAJA-2' de la sede 'Sede Centro' ya tiene una caja abierta. "
                + "Responsable: Laura Gómez.");
    }

    @Test
    void open_rejects_when_employee_has_an_open_session_in_another_branch_and_terminal() {
        repo.save(CashSession.open(CO, BR, 101L, "caja-1", USER, bd("50"), null));

        assertThatThrownBy(() -> service.open(
            new OpenCashSessionCommand(CO, OTHER_BR, TERMINAL, bd("100"), USER, null)))
            .isInstanceOf(EmployeeCashSessionAlreadyOpenException.class)
            .hasMessage("Ya tienes una caja abierta. Debes cerrarla antes de abrir otra.");
    }

    @Test
    void open_rejects_inactive_or_invalid_branch() {
        branchQuery.active = false;

        assertThatThrownBy(() -> service.open(
            new OpenCashSessionCommand(CO, BR, TERMINAL, bd("100"), USER, null)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    /** Fake de sede: responde activa/inactiva según el flag. */
    static class FakeBranchQueryPort implements BranchQueryPort {
        boolean active;

        FakeBranchQueryPort(boolean active) {
            this.active = active;
        }

        @Override
        public boolean existsActiveInCompany(Long branchId, Long companyId) {
            return active;
        }
    }
}
