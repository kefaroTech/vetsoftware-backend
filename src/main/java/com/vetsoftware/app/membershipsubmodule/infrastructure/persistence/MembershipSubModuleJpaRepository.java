package com.vetsoftware.app.membershipsubmodule.infrastructure.persistence;

import java.util.Collection;
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

    @EntityGraph(attributePaths = {"membership", "subModule"})
    List<MembershipSubModuleJpaEntity> findByMembershipIdIn(Collection<Long> membershipIds);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = "UPDATE membership_sub_modules SET enabled = true WHERE id = :id", nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id);

    @org.springframework.data.jpa.repository.Query(
        value = "SELECT id FROM membership_sub_modules WHERE membership_id = :membershipId AND sub_module_id = :subModuleId AND enabled = false LIMIT 1",
        nativeQuery = true)
    Optional<Long> findDisabledIdByMembershipAndSubModule(
        @org.springframework.data.repository.query.Param("membershipId") Long membershipId,
        @org.springframework.data.repository.query.Param("subModuleId") Long subModuleId);

    boolean existsByMembership_Id(Long membershipId);

    boolean existsBySubModule_Id(Long subModuleId);
}
