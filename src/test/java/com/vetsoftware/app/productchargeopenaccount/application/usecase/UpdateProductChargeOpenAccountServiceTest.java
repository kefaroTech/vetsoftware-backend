package com.vetsoftware.app.productchargeopenaccount.application.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.vetsoftware.app.productchargeopenaccount.application.command.UpdateProductChargeOpenAccountCommand;
import com.vetsoftware.app.productchargeopenaccount.application.dto.ProductChargeOpenAccountDto;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.OpenAccountQueryPort;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.ProductChargeOpenAccountRepository;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.ProductQueryPort;
import com.vetsoftware.app.productchargeopenaccount.domain.AnimalRef;
import com.vetsoftware.app.productchargeopenaccount.domain.EmployeeRef;
import com.vetsoftware.app.productchargeopenaccount.domain.OpenAccountRef;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductChargeOpenAccount;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductRef;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UpdateProductChargeOpenAccountServiceTest {

    private final AnimalRef animal = new AnimalRef(2L, "Rex", "A-2");
    private final ProductRef product = new ProductRef(3L, "Vaccine", "P-3", new BigDecimal("20.00"));
    private final EmployeeRef createdBy = new EmployeeRef(7L, "Dr. House");

    private final ProductChargeOpenAccount existing = new ProductChargeOpenAccount(1L, animal, product,
            new OpenAccountRef(10L, 5L), createdBy, LocalDateTime.now(), true);
    private ProductChargeOpenAccount saved;
    private final List<Long> refreshed = new ArrayList<>();

    private final ProductChargeOpenAccountRepository repository = new ProductChargeOpenAccountRepository() {
        @Override public ProductChargeOpenAccount save(ProductChargeOpenAccount charge) {
            saved = charge;
            return charge;
        }
        @Override public Optional<ProductChargeOpenAccount> findById(Long id) {
            return id == 1L ? Optional.of(existing) : Optional.empty();
        }
        @Override public List<ProductChargeOpenAccount> findAll() { return List.of(); }
        @Override public List<ProductChargeOpenAccount> findByOpenAccountId(Long openAccountId) { return List.of(); }
        @Override public void delete(Long id) {}
        @Override public int reactivate(Long id) { return 0; }
    };

    private final OpenAccountRefresher refresher = refreshed::add;

    private OpenAccountQueryPort openAccountQueryPort(Optional<OpenAccountRef> result) {
        return new OpenAccountQueryPort() {
            @Override public Optional<OpenAccountRef> findById(Long id) { return result; }
            @Override public boolean isOpen(Long id) { return result.isPresent(); }
        };
    }
    private AnimalQueryPort animalQueryPort(Optional<AnimalRef> result) { return id -> result; }
    private ProductQueryPort productQueryPort(Optional<ProductRef> result) { return id -> result; }

    @Test
    void updates_and_refreshes_same_account() {
        var service = new UpdateProductChargeOpenAccountService(repository,
                animalQueryPort(Optional.of(animal)),
                productQueryPort(Optional.of(product)),
                openAccountQueryPort(Optional.of(new OpenAccountRef(10L, 5L))),
                refresher);

        ProductChargeOpenAccountDto dto = service.execute(
                new UpdateProductChargeOpenAccountCommand(1L, 2L, 3L, 10L, 5L));

        assertEquals(10L, dto.openAccount().id());
        assertEquals(List.of(10L), refreshed);
    }

    @Test
    void refreshes_both_accounts_when_open_account_changed() {
        var service = new UpdateProductChargeOpenAccountService(repository,
                animalQueryPort(Optional.of(animal)),
                productQueryPort(Optional.of(product)),
                openAccountQueryPort(Optional.of(new OpenAccountRef(20L, 5L))),
                refresher);

        service.execute(new UpdateProductChargeOpenAccountCommand(1L, 2L, 3L, 20L, 5L));

        assertTrue(refreshed.contains(20L));
        assertTrue(refreshed.contains(10L));
    }

    @Test
    void fails_when_not_found() {
        var service = new UpdateProductChargeOpenAccountService(repository,
                animalQueryPort(Optional.of(animal)),
                productQueryPort(Optional.of(product)),
                openAccountQueryPort(Optional.of(new OpenAccountRef(10L, 5L))),
                refresher);

        assertThrows(RuntimeException.class, () -> service.execute(
                new UpdateProductChargeOpenAccountCommand(99L, 2L, 3L, 10L, 5L)));
    }

    @Test
    void fails_when_open_account_not_found() {
        var service = new UpdateProductChargeOpenAccountService(repository,
                animalQueryPort(Optional.of(animal)),
                productQueryPort(Optional.of(product)),
                openAccountQueryPort(Optional.empty()),
                refresher);

        assertThrows(IllegalArgumentException.class, () -> service.execute(
                new UpdateProductChargeOpenAccountCommand(1L, 2L, 3L, 10L, 5L)));
    }

    @Test
    void fails_when_company_mismatch() {
        var service = new UpdateProductChargeOpenAccountService(repository,
                animalQueryPort(Optional.of(animal)),
                productQueryPort(Optional.of(product)),
                openAccountQueryPort(Optional.of(new OpenAccountRef(10L, 5L))),
                refresher);

        assertThrows(IllegalArgumentException.class, () -> service.execute(
                new UpdateProductChargeOpenAccountCommand(1L, 2L, 3L, 10L, 999L)));
    }

    @Test
    void fails_when_product_not_found() {
        var service = new UpdateProductChargeOpenAccountService(repository,
                animalQueryPort(Optional.of(animal)),
                productQueryPort(Optional.empty()),
                openAccountQueryPort(Optional.of(new OpenAccountRef(10L, 5L))),
                refresher);

        assertThrows(IllegalArgumentException.class, () -> service.execute(
                new UpdateProductChargeOpenAccountCommand(1L, 2L, 3L, 10L, 5L)));
    }
}
