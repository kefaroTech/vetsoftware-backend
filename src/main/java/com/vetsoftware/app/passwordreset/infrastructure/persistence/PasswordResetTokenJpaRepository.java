package com.vetsoftware.app.passwordreset.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface PasswordResetTokenJpaRepository extends JpaRepository<PasswordResetTokenJpaEntity, Long> {

    Optional<PasswordResetTokenJpaEntity> findByTokenHash(String tokenHash);

    // Invalida (consume) todos los tokens vivos del empleado de una vez, sin cargarlos como entidades.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("UPDATE PasswordResetTokenJpaEntity t SET t.consumedAt = :now "
        + "WHERE t.employeeId = :employeeId AND t.consumedAt IS NULL")
    int consumeActiveForEmployee(@Param("employeeId") Long employeeId, @Param("now") LocalDateTime now);
}
