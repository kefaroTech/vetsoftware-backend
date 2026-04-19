package com.vetsoftware.app.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyJpaRepository extends JpaRepository<CompanyJpaEntity, String> {}
