package com.vetsoftware.app.membershipsubmodule.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipSubModuleJpaRepository extends JpaRepository<MembershipSubModuleJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"membership", "subModule"})
    List<MembershipSubModuleJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"membership", "subModule"})
    Optional<MembershipSubModuleJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = "subModule")
    List<MembershipSubModuleJpaEntity> findByMembershipId(Long membershipId);
}
