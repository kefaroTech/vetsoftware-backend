package com.vetsoftware.app.servicechargeopenaccount.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaEntity;
import com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaRepository;
import com.vetsoftware.app.service.infrastructure.persistence.ServiceJpaEntity;
import com.vetsoftware.app.service.infrastructure.persistence.ServiceJpaRepository;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.ServiceChargeOpenAccountRepository;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceChargeOpenAccount;
import com.vetsoftware.app.tax.infrastructure.persistence.TaxJpaEntity;
import com.vetsoftware.app.tax.infrastructure.persistence.TaxJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaServiceChargeOpenAccountRepository implements ServiceChargeOpenAccountRepository {
    private final ServiceChargeOpenAccountJpaRepository jpaRepository;
    private final ServiceChargeOpenAccountJpaMapper mapper;
    private final AnimalJpaRepository animalJpaRepository;
    private final ServiceJpaRepository serviceJpaRepository;
    private final TaxJpaRepository taxJpaRepository;
    private final OpenAccountJpaRepository openAccountJpaRepository;
    private final EmployeeJpaRepository employeeJpaRepository;

    public JpaServiceChargeOpenAccountRepository(ServiceChargeOpenAccountJpaRepository jpaRepository,
                                                 ServiceChargeOpenAccountJpaMapper mapper,
                                                 AnimalJpaRepository animalJpaRepository,
                                                 ServiceJpaRepository serviceJpaRepository,
                                                 TaxJpaRepository taxJpaRepository,
                                                 OpenAccountJpaRepository openAccountJpaRepository,
                                                 EmployeeJpaRepository employeeJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.animalJpaRepository = animalJpaRepository;
        this.serviceJpaRepository = serviceJpaRepository;
        this.taxJpaRepository = taxJpaRepository;
        this.openAccountJpaRepository = openAccountJpaRepository;
        this.employeeJpaRepository = employeeJpaRepository;
    }

    @Override
    public ServiceChargeOpenAccount save(ServiceChargeOpenAccount charge) {
        AnimalJpaEntity animal = animalJpaRepository.getReferenceById(charge.getAnimal().id());
        ServiceJpaEntity service = serviceJpaRepository.getReferenceById(charge.getService().id());
        TaxJpaEntity tax = charge.getTax() == null ? null
            : taxJpaRepository.getReferenceById(charge.getTax().id());
        OpenAccountJpaEntity openAccount = openAccountJpaRepository.getReferenceById(charge.getOpenAccount().id());
        EmployeeJpaEntity createdBy = charge.getCreatedBy() == null ? null
            : employeeJpaRepository.getReferenceById(charge.getCreatedBy().id());
        EmployeeJpaEntity voidedBy = charge.getVoidedBy() == null ? null
            : employeeJpaRepository.getReferenceById(charge.getVoidedBy().id());
        ServiceChargeOpenAccountJpaEntity saved =
            jpaRepository.save(mapper.toJpa(charge, animal, service, tax, openAccount, createdBy, voidedBy));
        return mapper.toDomain(saved, charge.getAnimal(), charge.getService(), charge.getTax(),
            charge.getOpenAccount(), charge.getCreatedBy(), charge.getVoidedBy());
    }

    @Override
    public Optional<ServiceChargeOpenAccount> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<ServiceChargeOpenAccount> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndOpenAccount_Company_Id(id, companyId).map(mapper::toDomain);
    }

    @Override
    public Optional<ServiceChargeOpenAccount> findByOpenAccountIdAndClientRequestId(Long openAccountId, String clientRequestId) {
        return jpaRepository.findByOpenAccount_IdAndClientRequestId(openAccountId, clientRequestId).map(mapper::toDomain);
    }

    @Override
    public List<ServiceChargeOpenAccount> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<ServiceChargeOpenAccount> findAllByCompanyId(Long companyId) {
        return jpaRepository.findAllByOpenAccount_Company_Id(companyId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<ServiceChargeOpenAccount> findByOpenAccountIdAndCompanyId(Long openAccountId, Long companyId) {
        return jpaRepository.findByOpenAccount_IdAndOpenAccount_Company_Id(openAccountId, companyId).stream()
            .map(mapper::toDomain).toList();
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
