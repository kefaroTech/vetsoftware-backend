package com.vetsoftware.app.cashregister.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.cashregister.application.dto.CashSessionView;
import com.vetsoftware.app.cashregister.domain.CashSession;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CashSessionQueryServiceTest {

    private static final long COMPANY = 1L;
    private static final long BRANCH_ONE = 10L;
    private static final long BRANCH_TWO = 20L;

    private FakeCashSessionRepository repository;
    private CashSessionQueryService service;

    @BeforeEach
    void setUp() {
        repository = new FakeCashSessionRepository();
        service = new CashSessionQueryService(repository);

        repository.save(CashSession.open(COMPANY, BRANCH_ONE, "principal", 101L, BigDecimal.TEN, null));
        repository.save(CashSession.open(COMPANY, BRANCH_TWO, "principal", 102L, BigDecimal.ONE, null));
        repository.save(CashSession.open(2L, 30L, "principal", 103L, BigDecimal.ZERO, null));
    }

    @Test
    void listOpen_limits_non_admin_to_assigned_branches() {
        List<CashSessionView> result = service.listOpen(COMPANY, Set.of(BRANCH_ONE));

        assertThat(result).extracting(CashSessionView::branchId).containsExactly(BRANCH_ONE);
    }

    @Test
    void listOpen_returns_every_company_branch_for_admin_scope() {
        List<CashSessionView> result = service.listOpen(COMPANY, null);

        assertThat(result).extracting(CashSessionView::branchId)
            .containsExactlyInAnyOrder(BRANCH_ONE, BRANCH_TWO);
    }
}
