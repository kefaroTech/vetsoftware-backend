package com.vetsoftware.app.systemuser.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface SystemUserJpaRepository extends JpaRepository<SystemUserJpaEntity, Long> {

  Optional<SystemUserJpaEntity> findByCode(String code);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT u FROM SystemUserJpaEntity u WHERE u.id = :id")
  Optional<SystemUserJpaEntity> findByIdForUpdate(@Param("id") Long id);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Transactional
  @Query(
      value = "UPDATE system_users SET auth_version = auth_version + 1 WHERE id = :id",
      nativeQuery = true)
  int bumpAuthVersion(@Param("id") Long id);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Transactional
  @Query(
      value =
          "UPDATE system_users SET enabled = true, auth_version = auth_version + 1 WHERE id = :id",
      nativeQuery = true)
  int reactivate(@Param("id") Long id);
}
