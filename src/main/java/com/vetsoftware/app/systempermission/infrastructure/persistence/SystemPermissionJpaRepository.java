package com.vetsoftware.app.systempermission.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemPermissionJpaRepository
    extends JpaRepository<SystemPermissionJpaEntity, Long> {

  @org.springframework.data.jpa.repository.Modifying(
      flushAutomatically = true,
      clearAutomatically = true)
  @org.springframework.transaction.annotation.Transactional
  @org.springframework.data.jpa.repository.Query(
      value = "UPDATE system_permissions SET enabled = true WHERE id = :id",
      nativeQuery = true)
  int reactivate(@org.springframework.data.repository.query.Param("id") Long id);
}
