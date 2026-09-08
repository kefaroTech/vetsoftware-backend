package com.vetsoftware.app.spa.application.port.out;

import java.time.LocalDateTime;

/**
 * Techo gratuito del eje {@code GROOMING_SERVICE}, contado sobre spa y
 * guardería a la vez (309: {@code GROOMING} abre las dos submódulos juntos).
 */
public interface SpaUsageLimitPort {

    void checkNotExceeded(Long companyId);

    void record(Long companyId, Long spaId, LocalDateTime occurredAt);
}
