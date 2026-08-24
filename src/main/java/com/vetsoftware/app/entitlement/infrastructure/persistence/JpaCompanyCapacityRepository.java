package com.vetsoftware.app.entitlement.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.entitlement.application.port.out.CompanyCapacityRepository;
import com.vetsoftware.app.entitlement.domain.CapacityUnit;
import com.vetsoftware.app.entitlement.domain.CompanyCapacity;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Adaptador de salida de los contadores contratados. */
@Repository
public class JpaCompanyCapacityRepository implements CompanyCapacityRepository {

    private final CompanyCapacityJpaRepository jpaRepository;
    private final CompanyCapacityJpaMapper mapper;
    private final CompanyJpaRepository companyJpaRepository;

    public JpaCompanyCapacityRepository(CompanyCapacityJpaRepository jpaRepository,
            CompanyCapacityJpaMapper mapper, CompanyJpaRepository companyJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.companyJpaRepository = companyJpaRepository;
    }

    @Override
    public List<CompanyCapacity> findAllByCompanyId(Long companyId) {
        return jpaRepository.findAllByCompany_IdOrderByCapacityUnitAsc(companyId).stream()
                .map(entity -> mapper.toDomain(entity, companyId)).toList();
    }

    @Override
    public Optional<CompanyCapacity> findByCompanyIdAndUnit(Long companyId, CapacityUnit unit) {
        return jpaRepository.findByCompany_IdAndCapacityUnit(companyId, unit.name())
                .map(entity -> mapper.toDomain(entity, companyId));
    }

    @Override
    public List<CompanyCapacity> saveAll(List<CompanyCapacity> capacities) {
        if (capacities.isEmpty()) {
            return List.of();
        }
        List<CompanyCapacityJpaEntity> rows = new ArrayList<>(capacities.size());
        for (CompanyCapacity capacity : capacities) {
            CompanyJpaEntity company = companyJpaRepository
                    .getReferenceById(capacity.getCompanyId());
            rows.add(mapper.toJpa(capacity, company));
        }
        List<CompanyCapacityJpaEntity> saved = jpaRepository.saveAll(rows);
        List<CompanyCapacity> result = new ArrayList<>(saved.size());
        for (int index = 0; index < saved.size(); index++) {
            result.add(mapper.toDomain(saved.get(index), capacities.get(index).getCompanyId()));
        }
        return List.copyOf(result);
    }

    @Override
    public int addUsage(Long companyId, CapacityUnit unit, int delta) {
        return jpaRepository.addUsage(companyId, unit.name(), delta);
    }
}
