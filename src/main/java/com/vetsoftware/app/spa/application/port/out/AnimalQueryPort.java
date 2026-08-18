package com.vetsoftware.app.spa.application.port.out;

import com.vetsoftware.app.spa.domain.AnimalRef;
import java.util.Optional;

/**
 * Sin variante ancha a proposito: la unica forma de resolver el animal es
 * acotando por empresa. Dejar un {@code findById(id)} disponible es dejar la
 * fuga a mano del proximo copy-paste — una estancia de spa colgada del animal
 * de otro tenant.
 */
public interface AnimalQueryPort {
    Optional<AnimalRef> findByIdAndCompanyId(Long animalId, Long companyId);
}
