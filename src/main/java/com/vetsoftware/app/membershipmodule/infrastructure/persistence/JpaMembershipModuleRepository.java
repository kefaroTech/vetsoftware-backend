package com.vetsoftware.app.membershipmodule.infrastructure.persistence;

import com.vetsoftware.app.membership.infrastructure.persistence.MembershipJpaRepository;
import com.vetsoftware.app.membershipmodule.application.port.out.MembershipModuleRepository;
import com.vetsoftware.app.membershipmodule.domain.MembershipModule;
import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaMembershipModuleRepository implements MembershipModuleRepository {
    private final MembershipModuleJpaRepository jpaRepository;
    private final MembershipModuleJpaMapper mapper;
    private final MembershipJpaRepository membershipJpaRepository;
    private final SubModuleJpaRepository subModuleJpaRepository;

    public JpaMembershipModuleRepository(MembershipModuleJpaRepository jpaRepository,
                                          MembershipModuleJpaMapper mapper,
                                          MembershipJpaRepository membershipJpaRepository,
                                          SubModuleJpaRepository subModuleJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.membershipJpaRepository = membershipJpaRepository;
        this.subModuleJpaRepository = subModuleJpaRepository;
    }

    @Override
    public MembershipModule save(MembershipModule membershipModule) {
        var membership = membershipJpaRepository.getReferenceById(membershipModule.getMembershipId());
        var subModule = subModuleJpaRepository.getReferenceById(membershipModule.getSubModuleId());
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(membershipModule, membership, subModule)));
    }

    @Override
    public Optional<MembershipModule> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<MembershipModule> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }
}
