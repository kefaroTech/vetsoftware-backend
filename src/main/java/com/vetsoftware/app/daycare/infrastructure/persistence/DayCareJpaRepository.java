package com.vetsoftware.app.daycare.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DayCareJpaRepository extends JpaRepository<DayCareJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"animal", "company"})
    List<DayCareJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"animal", "company"})
    Optional<DayCareJpaEntity> findById(Long id);

    boolean existsByAnimal_Id(Long animalId);

    boolean existsByCompany_Id(Long companyId);
}
