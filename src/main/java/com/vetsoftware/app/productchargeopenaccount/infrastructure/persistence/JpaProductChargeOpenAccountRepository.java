package com.vetsoftware.app.productchargeopenaccount.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaEntity;
import com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaRepository;
import com.vetsoftware.app.product.infrastructure.persistence.ProductJpaEntity;
import com.vetsoftware.app.product.infrastructure.persistence.ProductJpaRepository;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.ProductChargeOpenAccountRepository;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductChargeOpenAccount;
import com.vetsoftware.app.tax.infrastructure.persistence.TaxJpaEntity;
import com.vetsoftware.app.tax.infrastructure.persistence.TaxJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaProductChargeOpenAccountRepository implements ProductChargeOpenAccountRepository {
    private final ProductChargeOpenAccountJpaRepository jpaRepository;
    private final ProductChargeOpenAccountJpaMapper mapper;
    private final AnimalJpaRepository animalJpaRepository;
    private final ProductJpaRepository productJpaRepository;
    private final TaxJpaRepository taxJpaRepository;
    private final OpenAccountJpaRepository openAccountJpaRepository;
    private final EmployeeJpaRepository employeeJpaRepository;

    public JpaProductChargeOpenAccountRepository(ProductChargeOpenAccountJpaRepository jpaRepository,
                                                 ProductChargeOpenAccountJpaMapper mapper,
                                                 AnimalJpaRepository animalJpaRepository,
                                                 ProductJpaRepository productJpaRepository,
                                                 TaxJpaRepository taxJpaRepository,
                                                 OpenAccountJpaRepository openAccountJpaRepository,
                                                 EmployeeJpaRepository employeeJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.animalJpaRepository = animalJpaRepository;
        this.productJpaRepository = productJpaRepository;
        this.taxJpaRepository = taxJpaRepository;
        this.openAccountJpaRepository = openAccountJpaRepository;
        this.employeeJpaRepository = employeeJpaRepository;
    }

    @Override
    public ProductChargeOpenAccount save(ProductChargeOpenAccount charge) {
        AnimalJpaEntity animal = animalJpaRepository.getReferenceById(charge.getAnimal().id());
        ProductJpaEntity product = productJpaRepository.getReferenceById(charge.getProduct().id());
        TaxJpaEntity tax = charge.getTax() == null ? null
            : taxJpaRepository.getReferenceById(charge.getTax().id());
        OpenAccountJpaEntity openAccount = openAccountJpaRepository.getReferenceById(charge.getOpenAccount().id());
        EmployeeJpaEntity createdBy = charge.getCreatedBy() == null ? null
            : employeeJpaRepository.getReferenceById(charge.getCreatedBy().id());
        EmployeeJpaEntity voidedBy = charge.getVoidedBy() == null ? null
            : employeeJpaRepository.getReferenceById(charge.getVoidedBy().id());
        ProductChargeOpenAccountJpaEntity saved =
            jpaRepository.save(mapper.toJpa(charge, animal, product, tax, openAccount, createdBy, voidedBy));
        return mapper.toDomain(saved, charge.getAnimal(), charge.getProduct(), charge.getTax(),
            charge.getOpenAccount(), charge.getCreatedBy(), charge.getVoidedBy());
    }

    @Override
    public Optional<ProductChargeOpenAccount> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<ProductChargeOpenAccount> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<ProductChargeOpenAccount> findByOpenAccountId(Long openAccountId) {
        return jpaRepository.findByOpenAccountId(openAccountId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public int reactivate(Long id) {
        return jpaRepository.reactivate(id);
    }
}
