package com.vetsoftware.app.membership.infrastructure.persistence;

import com.vetsoftware.app.membership.application.port.out.MembershipRepository;
import com.vetsoftware.app.membership.domain.Membership;
import com.vetsoftware.app.module.infrastructure.persistence.ModuleJpaEntity;
import com.vetsoftware.app.module.infrastructure.persistence.ModuleJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaMembershipRepository implements MembershipRepository {
    private final MembershipJpaRepository jpaRepository;
    private final MembershipJpaMapper mapper;
    private final ModuleJpaRepository moduleJpaRepository;

    public JpaMembershipRepository(MembershipJpaRepository jpaRepository, MembershipJpaMapper mapper,
                                   ModuleJpaRepository moduleJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.moduleJpaRepository = moduleJpaRepository;
    }

    @Override
    public Membership save(Membership membership) {
        List<ModuleJpaEntity> modules = moduleJpaRepository.findAllById(membership.getModuleIds());
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(membership, modules)));
    }

    @Override
    public Optional<Membership> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Membership> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }
}
