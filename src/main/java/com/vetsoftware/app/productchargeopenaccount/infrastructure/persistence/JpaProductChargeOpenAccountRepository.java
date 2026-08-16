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
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductChargeOpenAccount;
import com.vetsoftware.app.tax.infrastructure.persistence.TaxJpaEntity;
import com.vetsoftware.app.tax.infrastructure.persistence.TaxJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
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

    public JpaProductChargeOpenAccountRepository(
            ProductChargeOpenAccountJpaRepository jpaRepository,
            ProductChargeOpenAccountJpaMapper mapper, AnimalJpaRepository animalJpaRepository,
            ProductJpaRepository productJpaRepository, TaxJpaRepository taxJpaRepository,
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
        TaxJpaEntity tax = charge.getTax() == null
                ? null
                : taxJpaRepository.getReferenceById(charge.getTax().id());
        OpenAccountJpaEntity openAccount = openAccountJpaRepository
                .getReferenceById(charge.getOpenAccount().id());
        EmployeeJpaEntity createdBy = charge.getCreatedBy() == null
                ? null
                : employeeJpaRepository.getReferenceById(charge.getCreatedBy().id());
        EmployeeJpaEntity voidedBy = charge.getVoidedBy() == null
                ? null
                : employeeJpaRepository.getReferenceById(charge.getVoidedBy().id());
        ProductChargeOpenAccountJpaEntity saved = jpaRepository
                .save(mapper.toJpa(charge, animal, product, tax, openAccount, createdBy, voidedBy));
        return mapper.toDomain(saved, charge.getAnimal(), charge.getProduct(), charge.getTax(),
                charge.getOpenAccount(), charge.getCreatedBy(), charge.getVoidedBy());
    }

    @Override
    public Optional<ProductChargeOpenAccount> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<ProductChargeOpenAccount> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndOpenAccount_Company_Id(id, companyId).map(mapper::toDomain);
    }

    @Override
    public Optional<ProductChargeOpenAccount> findByOpenAccountIdAndClientRequestId(
            Long openAccountId, String clientRequestId) {
        return jpaRepository.findByOpenAccount_IdAndClientRequestId(openAccountId, clientRequestId)
                .map(mapper::toDomain);
    }

    @Override
    public List<ProductChargeOpenAccount> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public PageResult<ProductChargeOpenAccount> findAllByCompanyId(Long companyId, int page,
            int pageSize) {
        // El orden por id descendente es estable y devuelve primero lo mas reciente;
        // sin
        // orden explicito la paginacion no es determinista y una misma fila puede salir
        // en dos paginas.
        Page<ProductChargeOpenAccountJpaEntity> result = jpaRepository
                .findAllByOpenAccount_Company_Id(companyId,
                        Pages.request(page, pageSize, Sort.by(Sort.Direction.DESC, "id")));
        return Pages.result(result, mapper::toDomain);
    }

    @Override
    public List<ProductChargeOpenAccount> findByOpenAccountIdAndCompanyId(Long openAccountId,
            Long companyId) {
        return jpaRepository.findByOpenAccount_IdAndOpenAccount_Company_Id(openAccountId, companyId)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public int reactivate(Long id, Long companyId) {
        return jpaRepository.reactivate(id, companyId);
    }
}
