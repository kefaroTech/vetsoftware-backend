package com.vetsoftware.app.testtype.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TestTypeJpaRepository extends JpaRepository<TestTypeJpaEntity, Long> {}
