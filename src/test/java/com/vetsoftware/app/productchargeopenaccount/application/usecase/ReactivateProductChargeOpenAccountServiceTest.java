package com.vetsoftware.app.productchargeopenaccount.application.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.vetsoftware.app.productchargeopenaccount.application.dto.ProductChargeOpenAccountDto;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.ProductChargeOpenAccountRepository;
import com.vetsoftware.app.productchargeopenaccount.domain.AnimalRef;
import com.vetsoftware.app.productchargeopenaccount.domain.EmployeeRef;
import com.vetsoftware.app.productchargeopenaccount.domain.OpenAccountRef;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductChargeOpenAccount;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductChargeOpenAccountNotFoundException;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductRef;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ReactivateProductChargeOpenAccountServiceTest {

    private final ProductChargeOpenAccount existing = new ProductChargeOpenAccount(1L,
            new AnimalRef(2L, "Rex", "A-2"),
            new ProductRef(3L, "Vaccine", "P-3", new BigDecimal("20.00")),
            new BigDecimal("20.00"), new OpenAccountRef(10L, 5L),
            new EmployeeRef(7L, "Dr. House"), LocalDateTime.now(), true);

    private final List<Long> refreshed = new ArrayList<>();

    private ProductChargeOpenAccountRepository repository(ProductChargeOpenAccount stored) {
        return new ProductChargeOpenAccountRepository() {
            @Override public ProductChargeOpenAccount save(ProductChargeOpenAccount charge) { return charge; }
            @Override public Optional<ProductChargeOpenAccount> findById(Long id) { return Optional.ofNullable(stored); }
            @Override public List<ProductChargeOpenAccount> findAll() { return List.of(); }
            @Override public List<ProductChargeOpenAccount> findByOpenAccountId(Long openAccountId) { return List.of(); }
            @Override public void delete(Long id) {}
            @Override public int reactivate(Long id) { return stored == null ? 0 : 1; }
        };
    }

    private final OpenAccountRefresher refresher = refreshed::add;

    @Test
    void reactivates_and_refreshes_for_own_company() {
        var service = new ReactivateProductChargeOpenAccountService(repository(existing), refresher);

        ProductChargeOpenAccountDto dto = service.execute(1L, 5L);

        assertEquals(1L, dto.id());
        assertEquals(List.of(10L), refreshed);
    }

    @Test
    void fails_when_not_found() {
        var service = new ReactivateProductChargeOpenAccountService(repository(null), refresher);

        assertThrows(ProductChargeOpenAccountNotFoundException.class, () -> service.execute(99L, 5L));
    }

    @Test
    void rejects_reactivate_when_charge_belongs_to_other_company() {
        var service = new ReactivateProductChargeOpenAccountService(repository(existing), refresher);

        assertThrows(IllegalArgumentException.class, () -> service.execute(1L, 999L));
        assertTrue(refreshed.isEmpty(), "no debe refrescar al rechazar por empresa");
    }
}
