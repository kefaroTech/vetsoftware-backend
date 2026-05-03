package com.vetsoftware.app.vaccinationtype.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VaccinationTypeJpaRepository extends JpaRepository<VaccinationTypeJpaEntity, Long> {}
