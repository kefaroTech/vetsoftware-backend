package com.vetsoftware.app.aiproposal.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data sobre {@code ai_proposals}.
 *
 * <p>
 * No declara ni un metodo con {@code companyId} ni con {@code Company} en el
 * nombre, y no puede declararlo: esta rodaja no tiene empresa de la que tirar y
 * esa es la senal que miran las reglas de aislamiento. Tampoco declara ningun
 * listado: la propuesta se lee de una en una y por su token.
 */
public interface AiProposalJpaRepository extends JpaRepository<AiProposalJpaEntity, Long> {

    /**
     * El camino real de lectura. El token es la unica frontera de autorizacion de
     * la feature: 43 caracteres de base64url sobre 32 bytes de
     * {@code SecureRandom}.
     */
    Optional<AiProposalJpaEntity> findByPublicToken(String publicToken);

    /**
     * Idempotencia acotada al solicitante. El correo llega normalizado a
     * minusculas, que es lo que hace que esta consulta y
     * {@code uq_ai_proposals_idempotency} -sobre la columna generada
     * {@code UNHEX(SHA2(LOWER(contact_email),256))}- hablen de la misma fila.
     */
    Optional<AiProposalJpaEntity> findByIdempotencyKeyAndContactEmail(String idempotencyKey,
            String contactEmail);
}
