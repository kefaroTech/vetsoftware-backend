package com.vetsoftware.app.passwordreset.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface PasswordResetTokenJpaRepository
        extends
            JpaRepository<PasswordResetTokenJpaEntity, Long> {

    Optional<PasswordResetTokenJpaEntity> findByTokenHash(String tokenHash);

    // Invalida (consume) todos los tokens vivos del empleado de una vez, sin
    // cargarlos como
    // entidades.
    //
    // El AND por empresa no defiende de un id elegido por el atacante —el flujo es
    // anonimo y el employeeId sale de EmployeeAccountLookupPort.findByCode, nunca
    // del request—, pero tampoco es tautologico: employeeId y companyId vienen de
    // esa lectura y esta fila es otra (password_reset_tokens tiene su propia
    // company_id). El AND afirma que el token que se consume es de esa empresa.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("""
            UPDATE PasswordResetTokenJpaEntity t
            SET t.consumedAt = :now
            WHERE t.employeeId = :employeeId
              AND t.companyId = :companyId
              AND t.consumedAt IS NULL
            """)
    int consumeActiveForEmployee(@Param("employeeId") Long employeeId,
            @Param("companyId") Long companyId, @Param("now") LocalDateTime now);
}
