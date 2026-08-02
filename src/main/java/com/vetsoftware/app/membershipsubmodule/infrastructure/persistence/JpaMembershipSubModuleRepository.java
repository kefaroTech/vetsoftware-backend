package com.vetsoftware.app.membershipsubmodule.infrastructure.persistence;

import com.vetsoftware.app.membership.infrastructure.persistence.MembershipJpaEntity;
import com.vetsoftware.app.membership.infrastructure.persistence.MembershipJpaRepository;
import com.vetsoftware.app.membershipsubmodule.application.port.out.MembershipSubModuleRepository;
import com.vetsoftware.app.membershipsubmodule.domain.MembershipSubModule;
import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaEntity;
import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaMembershipSubModuleRepository implements MembershipSubModuleRepository {
    private final MembershipSubModuleJpaRepository jpaRepository;
    private final MembershipSubModuleJpaMapper mapper;
    private final MembershipJpaRepository membershipJpaRepository;
    private final SubModuleJpaRepository subModuleJpaRepository;

    public JpaMembershipSubModuleRepository(MembershipSubModuleJpaRepository jpaRepository,
            MembershipSubModuleJpaMapper mapper, MembershipJpaRepository membershipJpaRepository,
            SubModuleJpaRepository subModuleJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.membershipJpaRepository = membershipJpaRepository;
        this.subModuleJpaRepository = subModuleJpaRepository;
    }

    @Override
    public MembershipSubModule save(MembershipSubModule membershipSubModule) {
        MembershipJpaEntity membership = membershipJpaRepository
                .getReferenceById(membershipSubModule.getMembership().id());
        SubModuleJpaEntity subModule = subModuleJpaRepository
                .getReferenceById(membershipSubModule.getSubModule().id());
        MembershipSubModuleJpaEntity saved = jpaRepository
                .save(mapper.toJpa(membershipSubModule, membership, subModule));
        return mapper.toDomain(saved, membershipSubModule.getMembership(),
                membershipSubModule.getSubModule());
    }

    @Override
    public Optional<MembershipSubModule> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<MembershipSubModule> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public int reactivate(Long id) {
        return jpaRepository.reactivate(id);
    }

    @Override
    public Optional<Long> findDisabledIdByMembershipAndSubModule(Long membershipId,
            Long subModuleId) {
        return jpaRepository.findDisabledIdByMembershipAndSubModule(membershipId, subModuleId);
    }
}
