package com.vetsoftware.app.companyusageevent.application.port.out;

/**
 * Escribe el portazo en la bitácora de límites de la empresa. Mismo patrón que
 * {@code entitlement.application.port.out.LimitDenialPort}: el único archivo de
 * esta rodaja que conoce {@code companylimitevent} es el adaptador de
 * {@code infrastructure/orchestration} que implementa este puerto.
 */
public interface UsageLimitDenialPort {

    void limitDenied(Long companyId, Long limitDimensionId, int limitQuantity, int usedQuantity,
            int requestedDelta);
}
