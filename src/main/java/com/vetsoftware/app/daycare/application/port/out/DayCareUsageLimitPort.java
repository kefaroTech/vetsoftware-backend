package com.vetsoftware.app.daycare.application.port.out;

import java.time.LocalDateTime;

/**
 * Techo gratuito del eje {@code GROOMING_SERVICE}, contado sobre spa y
 * guardería a la vez (309: {@code GROOMING} abre los dos submódulos juntos).
 */
public interface DayCareUsageLimitPort {

    void checkNotExceeded(Long companyId);

    void record(Long companyId, Long dayCareId, LocalDateTime occurredAt);
}
