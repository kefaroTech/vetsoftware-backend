package com.vetsoftware.app.debtopenaccount.infrastructure.persistence;

import com.vetsoftware.app.debtopenaccount.application.port.out.DebtOpenAccountRepository;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccount;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaEntity;
import com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaDebtOpenAccountRepository implements DebtOpenAccountRepository {
    private final DebtOpenAccountJpaRepository jpaRepository;
    private final DebtOpenAccountJpaMapper mapper;
    private final OpenAccountJpaRepository openAccountJpaRepository;
    private final EmployeeJpaRepository employeeJpaRepository;

    public JpaDebtOpenAccountRepository(DebtOpenAccountJpaRepository jpaRepository,
            DebtOpenAccountJpaMapper mapper, OpenAccountJpaRepository openAccountJpaRepository,
            EmployeeJpaRepository employeeJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.openAccountJpaRepository = openAccountJpaRepository;
        this.employeeJpaRepository = employeeJpaRepository;
    }

    @Override
    public DebtOpenAccount save(DebtOpenAccount debtOpenAccount) {
        OpenAccountJpaEntity openAccount = openAccountJpaRepository
                .getReferenceById(debtOpenAccount.getOpenAccount().id());
        EmployeeJpaEntity createdBy = employeeJpaRepository
                .getReferenceById(debtOpenAccount.getCreatedBy().id());
        EmployeeJpaEntity voidedBy = debtOpenAccount.getVoidedBy() == null
                ? null
                : employeeJpaRepository.getReferenceById(debtOpenAccount.getVoidedBy().id());
        DebtOpenAccountJpaEntity saved = jpaRepository
                .save(mapper.toJpa(debtOpenAccount, openAccount, createdBy, voidedBy));
        return mapper.toDomain(saved, debtOpenAccount.getOpenAccount(),
                debtOpenAccount.getCreatedBy(), debtOpenAccount.getVoidedBy());
    }

    @Override
    public Optional<DebtOpenAccount> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<DebtOpenAccount> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndOpenAccount_Company_Id(id, companyId).map(mapper::toDomain);
    }

    @Override
    public Optional<DebtOpenAccount> findByOpenAccountIdAndClientRequestId(Long openAccountId,
            String clientRequestId) {
        return jpaRepository.findByOpenAccount_IdAndClientRequestId(openAccountId, clientRequestId)
                .map(mapper::toDomain);
    }

    @Override
    public List<DebtOpenAccount> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<DebtOpenAccount> findAllByCompanyId(Long companyId) {
        return jpaRepository.findAllByOpenAccount_Company_Id(companyId).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<DebtOpenAccount> findByOpenAccountIdAndCompanyId(Long openAccountId,
            Long companyId) {
        return jpaRepository.findByOpenAccount_IdAndOpenAccount_Company_Id(openAccountId, companyId)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public void delete(Long id, Long companyId) {
        jpaRepository.findByIdAndOpenAccount_Company_Id(id, companyId)
                .ifPresent(jpaRepository::delete);
    }

    @Override
    public int reactivate(Long id, Long companyId) {
        return jpaRepository.reactivate(id, companyId);
    }
}
