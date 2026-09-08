package com.vetsoftware.app.animal.application.port.out;

import java.time.LocalDateTime;

/**
 * Techo gratuito del eje {@code ANIMAL}, mismo patrón que
 * {@code BranchCapacityPort} para el eje {@code BRANCH}.
 */
public interface AnimalUsageLimitPort {

    void checkNotExceeded(Long companyId);

    void record(Long companyId, Long animalId, LocalDateTime occurredAt);
}
