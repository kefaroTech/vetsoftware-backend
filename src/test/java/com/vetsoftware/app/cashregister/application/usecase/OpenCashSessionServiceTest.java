package com.vetsoftware.app.cashregister.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.cashregister.application.command.OpenCashSessionCommand;
import com.vetsoftware.app.cashregister.application.dto.CashSessionView;
import com.vetsoftware.app.cashregister.application.port.out.BranchQueryPort;
import com.vetsoftware.app.cashregister.domain.CashSession;
import com.vetsoftware.app.cashregister.domain.CashSessionAlreadyOpenException;
import com.vetsoftware.app.cashregister.domain.CashSessionStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests del {@link OpenCashSessionService}: apertura con base, default de terminal, doble apertura y sede inválida. */
class OpenCashSessionServiceTest {

    private static final long CO = 1L;
    private static final long BR = 10L;
    private static final long USER = 7L;

    private FakeCashSessionRepository repo;
    private FakeBranchQueryPort branchQuery;
    private OpenCashSessionService service;

    @BeforeEach
    void setUp() {
        repo = new FakeCashSessionRepository();
        branchQuery = new FakeBranchQueryPort(true);
        service = new OpenCashSessionService(repo, branchQuery);
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @Test
    void open_creates_open_session_with_base() {
        CashSessionView view = service.open(
            new OpenCashSessionCommand(CO, BR, "caja-2", bd("100"), USER, "apertura"));

        assertThat(view.status()).isEqualTo(CashSessionStatus.OPEN);
        assertThat(view.openingFloat()).isEqualByComparingTo("100");
        assertThat(view.terminal()).isEqualTo("caja-2");
        assertThat(repo.existsOpen(CO, BR, "caja-2")).isTrue();
    }

    @Test
    void open_defaults_terminal_to_principal_when_blank() {
        CashSessionView view = service.open(new OpenCashSessionCommand(CO, BR, "  ", bd("0"), USER, null));

        assertThat(view.terminal()).isEqualTo("principal");
        assertThat(repo.existsOpen(CO, BR, "principal")).isTrue();
    }

    @Test
    void open_rejects_when_a_session_is_already_open() {
        repo.save(CashSession.open(CO, BR, "principal", USER, bd("50"), null));

        assertThatThrownBy(() -> service.open(new OpenCashSessionCommand(CO, BR, null, bd("100"), USER, null)))
            .isInstanceOf(CashSessionAlreadyOpenException.class);
    }

    @Test
    void open_rejects_inactive_or_invalid_branch() {
        branchQuery.active = false;

        assertThatThrownBy(() -> service.open(new OpenCashSessionCommand(CO, BR, null, bd("100"), USER, null)))
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
