package com.vetsoftware.app.country.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CountryJpaRepository extends JpaRepository<CountryJpaEntity, Long> {}
