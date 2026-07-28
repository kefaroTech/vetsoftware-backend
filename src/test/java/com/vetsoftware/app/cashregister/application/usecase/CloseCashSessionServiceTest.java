package com.vetsoftware.app.cashregister.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.vetsoftware.app.cashregister.application.command.CloseCashSessionCommand;
import com.vetsoftware.app.cashregister.application.dto.CashSessionCountView;
import com.vetsoftware.app.cashregister.application.dto.CashSessionView;
import com.vetsoftware.app.cashregister.application.port.out.CashMetrics;
import com.vetsoftware.app.cashregister.domain.CashMovement;
import com.vetsoftware.app.cashregister.domain.CashMovementType;
import com.vetsoftware.app.cashregister.domain.CashPaymentMethod;
import com.vetsoftware.app.cashregister.domain.CashReferenceType;
import com.vetsoftware.app.cashregister.domain.CashSession;
import com.vetsoftware.app.cashregister.domain.CashSessionClosedException;
import com.vetsoftware.app.cashregister.domain.CashSessionNotFoundException;
import com.vetsoftware.app.cashregister.domain.CashSessionStatus;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

/** Tests del {@link CloseCashSessionService}: cálculo esperado/contado/diferencia, guardas de estado y not-found. */
class CloseCashSessionServiceTest {

    private static final long CO = 1L;
    private static final long BR = 10L;
    private static final long USER = 7L;
    private static final long OTHER_USER = 8L;

    private FakeCashSessionRepository repo;
    private CloseCashSessionService service;

    @BeforeEach
    void setUp() {
        repo = new FakeCashSessionRepository();
        service = new CloseCashSessionService(repo, mock(CashMetrics.class));
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    /** Sesión OPEN con base 100 efectivo + venta 50 efectivo + 30 tarjeta. Esperado: CASH 150, CARD 30. */
    private CashSession seedOpenSession() {
        CashSession s = CashSession.open(CO, BR, 100L, "principal", USER, bd("100"), null);
        s.addMovement(CashMovement.create(CashMovementType.SALE_IN, CashPaymentMethod.CASH, bd("50"),
            CashReferenceType.POS_DOCUMENT, 1L, USER, null));
        s.addMovement(CashMovement.create(CashMovementType.SALE_IN, CashPaymentMethod.CARD, bd("30"),
            CashReferenceType.POS_DOCUMENT, 1L, USER, null));
        return repo.save(s);
    }

    @Test
    void close_computes_expected_counted_difference_and_persists_counts() {
        CashSession seeded = seedOpenSession();

        CashSessionView view = service.close(new CloseCashSessionCommand(CO, seeded.getId(), USER, "cierre",
            List.of(new CloseCashSessionCommand.Count(CashPaymentMethod.CASH, bd("140")),
                new CloseCashSessionCommand.Count(CashPaymentMethod.CARD, bd("30")))), false);

        assertThat(view.status()).isEqualTo(CashSessionStatus.CLOSED);
        assertThat(view.closingTotal()).isEqualByComparingTo("170");
        CashSessionCountView cash = view.counts().stream()
            .filter(c -> c.method() == CashPaymentMethod.CASH).findFirst().orElseThrow();
        assertThat(cash.expectedAmount()).isEqualByComparingTo("150");
        assertThat(cash.countedAmount()).isEqualByComparingTo("140");
        assertThat(cash.difference()).isEqualByComparingTo("-10");

        CashSessionCountView card = view.counts().stream()
            .filter(c -> c.method() == CashPaymentMethod.CARD).findFirst().orElseThrow();
        assertThat(card.expectedAmount()).isEqualByComparingTo("30");
        assertThat(card.difference()).isEqualByComparingTo("0");
    }

    @Test
    void close_rejects_an_already_closed_session() {
        CashSession seeded = seedOpenSession();
        service.close(new CloseCashSessionCommand(CO, seeded.getId(), USER, null, List.of()), false);

        assertThatThrownBy(() -> service.close(
            new CloseCashSessionCommand(CO, seeded.getId(), USER, null, List.of()), false))
            .isInstanceOf(CashSessionClosedException.class);
    }

    @Test
    void close_of_unknown_session_throws_not_found() {
        assertThatThrownBy(() -> service.close(
            new CloseCashSessionCommand(CO, 999L, USER, null, List.of()), false))
            .isInstanceOf(CashSessionNotFoundException.class);
    }

    @Test
    void close_rejects_an_employee_who_did_not_open_the_session() {
        CashSession seeded = seedOpenSession();

        assertThatThrownBy(() -> service.close(
            new CloseCashSessionCommand(CO, seeded.getId(), OTHER_USER, null, List.of()), false))
            .isInstanceOf(AccessDeniedException.class);
        assertThat(seeded.getStatus()).isEqualTo(CashSessionStatus.OPEN);
    }

    @Test
    void admin_can_close_a_session_opened_by_another_employee() {
        CashSession seeded = seedOpenSession();

        CashSessionView view = service.close(
            new CloseCashSessionCommand(CO, seeded.getId(), OTHER_USER, null, List.of()), true);

        assertThat(view.status()).isEqualTo(CashSessionStatus.CLOSED);
        assertThat(view.closedByEmployeeId()).isEqualTo(OTHER_USER);
    }

    @Test
    void movements_are_rejected_once_the_session_is_closed() {
        CashSession seeded = seedOpenSession();
        service.close(new CloseCashSessionCommand(CO, seeded.getId(), USER, null, List.of()), false);

        assertThatThrownBy(() -> seeded.addMovement(CashMovement.create(CashMovementType.MANUAL_IN,
            CashPaymentMethod.CASH, bd("10"), CashReferenceType.MANUAL, null, USER, null)))
            .isInstanceOf(CashSessionClosedException.class);
    }
}
