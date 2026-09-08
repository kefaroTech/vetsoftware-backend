package com.vetsoftware.app.owner.application.port.out;

import java.time.LocalDateTime;

/**
 * Techo gratuito del eje {@code OWNER}, mismo patrón que
 * {@code BranchCapacityPort}.
 */
public interface OwnerUsageLimitPort {

    void checkNotExceeded(Long companyId);

    void record(Long companyId, Long ownerId, LocalDateTime occurredAt);
}
