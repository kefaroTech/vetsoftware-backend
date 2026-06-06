package com.vetsoftware.app.productchargeopenaccount.application.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.vetsoftware.app.productchargeopenaccount.application.command.CreateProductChargeOpenAccountCommand;
import com.vetsoftware.app.productchargeopenaccount.application.dto.ProductChargeOpenAccountDto;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.EmployeeQueryPort;
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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CreateProductChargeOpenAccountServiceTest {

    private final OpenAccountRef openAccount = new OpenAccountRef(10L, 5L);
    private final AnimalRef animal = new AnimalRef(2L, "Rex", "A-2");
    private final ProductRef product = new ProductRef(3L, "Vaccine", "P-3", new BigDecimal("20.00"));
    private final EmployeeRef createdBy = new EmployeeRef(7L, "Dr. House");

    private ProductChargeOpenAccount saved;
    private Long refreshedOpenAccountId;

    private final ProductChargeOpenAccountRepository repository = new ProductChargeOpenAccountRepository() {
        @Override public ProductChargeOpenAccount save(ProductChargeOpenAccount charge) {
            saved = new ProductChargeOpenAccount(1L, charge.getAnimal(), charge.getProduct(),
                    charge.getUnitPrice(), charge.getOpenAccount(), charge.getCreatedBy(),
                    charge.getCreatedDate(), charge.isEnabled(), charge.isVoided(),
                    charge.getVoidedBy(), charge.getVoidedAt(), charge.getVoidReason());
            return saved;
        }
        @Override public Optional<ProductChargeOpenAccount> findById(Long id) { return Optional.ofNullable(saved); }
        @Override public List<ProductChargeOpenAccount> findAll() { return List.of(); }
        @Override public List<ProductChargeOpenAccount> findByOpenAccountId(Long openAccountId) { return List.of(); }
        @Override public void delete(Long id) {}
        @Override public int reactivate(Long id) { return 0; }
    };

    private final OpenAccountRefresher refresher = id -> refreshedOpenAccountId = id;

    private OpenAccountQueryPort openAccountQueryPort(Optional<OpenAccountRef> result) {
        return new OpenAccountQueryPort() {
            @Override public Optional<OpenAccountRef> findById(Long id) { return result; }
            @Override public boolean isOpen(Long id) { return result.isPresent(); }
            @Override public java.math.BigDecimal outstandingAmount(Long id) { return java.math.BigDecimal.ZERO; }
        };
    }
    private AnimalQueryPort animalQueryPort(Optional<AnimalRef> result) { return id -> result; }
    private ProductQueryPort productQueryPort(Optional<ProductRef> result) { return id -> result; }
    private EmployeeQueryPort employeeQueryPort(Optional<EmployeeRef> result) { return id -> result; }

    private CreateProductChargeOpenAccountCommand command(Long companyId) {
        return new CreateProductChargeOpenAccountCommand(2L, 3L, 10L, companyId, 7L);
    }

    @Test
    void creates_product_charge_open_account_and_refreshes() {
        var service = new CreateProductChargeOpenAccountService(repository,
                animalQueryPort(Optional.of(animal)),
                productQueryPort(Optional.of(product)),
                openAccountQueryPort(Optional.of(openAccount)),
                employeeQueryPort(Optional.of(createdBy)),
                refresher);

        ProductChargeOpenAccountDto dto = service.execute(command(5L));

        assertEquals(1L, dto.id());
        assertEquals(2L, dto.animal().id());
        assertEquals(3L, dto.product().id());
        assertEquals(new BigDecimal("20.00"), dto.unitPrice());
        assertEquals(10L, dto.openAccount().id());
        assertEquals(7L, dto.createdBy().id());
        assertTrue(dto.enabled());
        assertEquals(10L, refreshedOpenAccountId);
    }

    @Test
    void fails_when_open_account_not_found() {
        var service = new CreateProductChargeOpenAccountService(repository,
                animalQueryPort(Optional.of(animal)),
                productQueryPort(Optional.of(product)),
                openAccountQueryPort(Optional.empty()),
                employeeQueryPort(Optional.of(createdBy)),
                refresher);

        assertThrows(IllegalArgumentException.class, () -> service.execute(command(5L)));
    }

    @Test
    void fails_when_company_mismatch() {
        var service = new CreateProductChargeOpenAccountService(repository,
                animalQueryPort(Optional.of(animal)),
                productQueryPort(Optional.of(product)),
                openAccountQueryPort(Optional.of(openAccount)),
                employeeQueryPort(Optional.of(createdBy)),
                refresher);

        assertThrows(IllegalArgumentException.class, () -> service.execute(command(999L)));
    }

    @Test
    void fails_when_product_not_found() {
        var service = new CreateProductChargeOpenAccountService(repository,
                animalQueryPort(Optional.of(animal)),
                productQueryPort(Optional.empty()),
                openAccountQueryPort(Optional.of(openAccount)),
                employeeQueryPort(Optional.of(createdBy)),
                refresher);

        assertThrows(IllegalArgumentException.class, () -> service.execute(command(5L)));
    }
}
