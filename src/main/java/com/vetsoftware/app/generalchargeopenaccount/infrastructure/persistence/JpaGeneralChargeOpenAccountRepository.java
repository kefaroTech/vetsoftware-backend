package com.vetsoftware.app.generalchargeopenaccount.infrastructure.persistence;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.GeneralChargeOpenAccountRepository;
import com.vetsoftware.app.generalchargeopenaccount.domain.GeneralChargeOpenAccount;
import com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaEntity;
import com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaRepository;
import com.vetsoftware.app.tax.infrastructure.persistence.TaxJpaEntity;
import com.vetsoftware.app.tax.infrastructure.persistence.TaxJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaGeneralChargeOpenAccountRepository implements GeneralChargeOpenAccountRepository {
    private final GeneralChargeOpenAccountJpaRepository jpaRepository;
    private final GeneralChargeOpenAccountJpaMapper mapper;
    private final TaxJpaRepository taxJpaRepository;
    private final OpenAccountJpaRepository openAccountJpaRepository;
    private final EmployeeJpaRepository employeeJpaRepository;

    public JpaGeneralChargeOpenAccountRepository(GeneralChargeOpenAccountJpaRepository jpaRepository,
                                                 GeneralChargeOpenAccountJpaMapper mapper,
                                                 TaxJpaRepository taxJpaRepository,
                                                 OpenAccountJpaRepository openAccountJpaRepository,
                                                 EmployeeJpaRepository employeeJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.taxJpaRepository = taxJpaRepository;
        this.openAccountJpaRepository = openAccountJpaRepository;
        this.employeeJpaRepository = employeeJpaRepository;
    }

    @Override
    public GeneralChargeOpenAccount save(GeneralChargeOpenAccount charge) {
        TaxJpaEntity tax = charge.getTax() == null ? null
            : taxJpaRepository.getReferenceById(charge.getTax().id());
        OpenAccountJpaEntity openAccount =
            openAccountJpaRepository.getReferenceById(charge.getOpenAccount().id());
        EmployeeJpaEntity createdBy =
            employeeJpaRepository.getReferenceById(charge.getCreatedBy().id());
        EmployeeJpaEntity voidedBy = charge.getVoidedBy() == null ? null
            : employeeJpaRepository.getReferenceById(charge.getVoidedBy().id());
        GeneralChargeOpenAccountJpaEntity saved =
            jpaRepository.save(mapper.toJpa(charge, tax, openAccount, createdBy, voidedBy));
        return mapper.toDomain(saved, charge.getTax(), charge.getOpenAccount(), charge.getCreatedBy(),
            charge.getVoidedBy());
    }

    @Override
    public Optional<GeneralChargeOpenAccount> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<GeneralChargeOpenAccount> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndOpenAccount_Company_Id(id, companyId).map(mapper::toDomain);
    }

    @Override
    public Optional<GeneralChargeOpenAccount> findByOpenAccountIdAndClientRequestId(Long openAccountId, String clientRequestId) {
        return jpaRepository.findByOpenAccount_IdAndClientRequestId(openAccountId, clientRequestId).map(mapper::toDomain);
    }

    @Override
    public List<GeneralChargeOpenAccount> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<GeneralChargeOpenAccount> findByOpenAccountId(Long openAccountId) {
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
